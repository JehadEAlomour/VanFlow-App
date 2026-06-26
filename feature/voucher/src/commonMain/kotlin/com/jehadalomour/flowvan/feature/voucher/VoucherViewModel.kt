package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.LineTaxType
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.TaxType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.jehadalomour.flowvan.core.domain.usecase.CancelApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CommitApprovedReturnUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.core.domain.usecase.GetCustomerSalesUseCase
import com.jehadalomour.flowvan.core.domain.usecase.PollApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RequestReturnApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.RequestDiscountApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CommitApprovedSaleUseCase
import com.jehadalomour.flowvan.core.domain.usecase.StockShortageException
import com.jehadalomour.flowvan.feature.voucher.DiscountType
import com.jehadalomour.flowvan.feature.voucher.VoucherView
import com.jehadalomour.flowvan.core.designsystem.resources.Res
import com.jehadalomour.flowvan.core.designsystem.resources.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoucherViewModel(
    private val customerId: String,
    private val type: VoucherType,
    customers: CustomerRepository,
    products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val session: SessionStore,
    private val createSale: CreateSaleVoucherUseCase,
    private val createReturn: CreateReturnVoucherUseCase,
    private val createRequest: CreateRequestVoucherUseCase,
    private val appSettings: AppSettingsRepository,
    private val invoiceDao: InvoiceDao,
    private val json: Json,
    private val getCustomerSales: GetCustomerSalesUseCase,
    private val requestReturnApproval: RequestReturnApprovalUseCase,
    private val pollApproval: PollApprovalUseCase,
    private val cancelApproval: CancelApprovalUseCase,
    private val commitApprovedReturn: CommitApprovedReturnUseCase,
    private val requestDiscountApproval: RequestDiscountApprovalUseCase,
    private val commitApprovedSale: CommitApprovedSaleUseCase,
) : ViewModel() {

    /** Which pending approval is in flight, so the poll commits the right voucher. */
    private enum class PendingKind { RETURN, SALE_DISCOUNT }
    private var pendingKind: PendingKind = PendingKind.RETURN

    /** Active poll loop for a pending approval; cancelled when decided/left. */
    private var approvalPollJob: Job? = null

    private val _state = MutableStateFlow(
        VoucherState(type = type, showSourcePicker = type == VoucherType.RETURN),
    )
    val state: StateFlow<VoucherState> = _state.asStateFlow()

    init {
        // Salesman permissions (set by the dashboard, delivered via permKeys on
        // login). Gate the discount + price-edit UI on them.
        _state.update {
            it.copy(
                canDiscount = session.can("vouchers.discount.direct"),
                canRequestDiscount = session.can("vouchers.discount.approval"),
                canEditPrice = session.can("vouchers.priceOverride"),
                canCreateReturn = session.can("vouchers.return.create"),
                returnNeedsApproval = session.can("vouchers.return.approval"),
            )
        }

        customers.observeById(customerId)
            .onEach { c -> _state.update { it.copy(customer = c) } }
            .launchIn(viewModelScope)

        // RETURN: the customer's locally-saved sales are the return sources. Their
        // numbers match the server (the server keeps the app's number on upload), so
        // they're valid references. Sales made elsewhere use the lookup-by-number box.
        if (type == VoucherType.RETURN) {
            invoiceDao.observeByCustomerAndType(customerId, "SALE")
                .onEach { sales ->
                    _state.update { it.copy(sourceInvoices = sales.filter { inv -> inv.status != "CANCELLED" }) }
                }
                .launchIn(viewModelScope)
        }

        products.observeAll()
            .onEach { list -> _state.update { it.copy(products = list) }; applySearch() }
            .launchIn(viewModelScope)

        productUnits.observeAll()
            .onEach { units ->
                _state.update { it.copy(productUnits = units.groupBy { u -> u.productId }) }
            }
            .launchIn(viewModelScope)

        appSettings.observe()
            .onEach { settings ->
                val lineTaxType = settings.taxType.toLineTaxType()
                _state.update { s ->
                    // Also re-stamp existing cart lines so live totals stay in sync
                    s.copy(
                        taxType = lineTaxType,
                        cart = s.cart.map { it.copy(lineTaxType = lineTaxType) },
                    )
                }
            }
            .launchIn(viewModelScope)

    }

    fun onEvent(event: VoucherEvent) {
        when (event) {
            is VoucherEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.q) }
                applySearch()
            }
            is VoucherEvent.StepItem -> stepItem(event.product, event.delta)
            is VoucherEvent.ConfirmItemDialog -> confirmDialog(event)
            is VoucherEvent.ChangeQty -> changeQty(event.productId, event.qty)
            is VoucherEvent.RemoveLine -> _state.update { s ->
                s.copy(cart = s.cart.filterNot { it.productId == event.productId })
            }
            is VoucherEvent.PaymentMethodSelected -> _state.update { it.copy(paymentMethod = event.method) }
            is VoucherEvent.NotesChanged -> _state.update { it.copy(notes = event.notes) }
            is VoucherEvent.VoucherDiscountInputChanged -> _state.update { it.copy(voucherDiscountInput = event.input) }
            VoucherEvent.VoucherDiscountTypeToggled -> _state.update {
                it.copy(
                    voucherDiscountType = if (it.voucherDiscountType == DiscountType.PERCENT) DiscountType.VALUE else DiscountType.PERCENT,
                    voucherDiscountInput = "",
                )
            }
            is VoucherEvent.ReasonSelected -> _state.update { it.copy(reason = event.reason) }
            VoucherEvent.ToggleView -> _state.update {
                it.copy(view = if (it.view == VoucherView.PICKER) VoucherView.CART else VoucherView.PICKER)
            }
            VoucherEvent.Save -> {
                val s = _state.value
                when {
                    type == VoucherType.RETURN && s.referenceInvoiceId == null ->
                        viewModelScope.launch {
                            val msg = getString(Res.string.err_select_source_invoice)
                            _state.update { it.copy(errorAr = msg, showSourcePicker = true) }
                        }
                    s.cart.isEmpty() -> viewModelScope.launch {
                        val msg = getString(Res.string.err_cart_empty)
                        _state.update { it.copy(errorAr = msg) }
                    }
                    type == VoucherType.RETURN && s.reason == null ->
                        viewModelScope.launch {
                            val msg = getString(Res.string.err_select_return_reason)
                            _state.update { it.copy(errorAr = msg) }
                        }
                    s.canSave -> _state.update { it.copy(showSaveSheet = true) }
                }
            }
            VoucherEvent.ConfirmSave -> save()
            VoucherEvent.CancelApproval -> cancelPendingApproval()
            VoucherEvent.DismissSaveSheet -> _state.update { it.copy(showSaveSheet = false) }
            VoucherEvent.DismissError -> _state.update { it.copy(errorAr = null) }

            VoucherEvent.OpenSourcePicker -> _state.update { it.copy(showSourcePicker = true) }
            VoucherEvent.DismissSourcePicker -> _state.update { it.copy(showSourcePicker = false) }
            is VoucherEvent.SelectSourceInvoice -> selectSourceInvoice(event.invoiceId)
            is VoucherEvent.SourceLookupChanged ->
                _state.update { it.copy(sourceLookupQuery = event.q) }
            VoucherEvent.LookupSource -> lookupSourceByNumber()
        }
    }

    /** Pre-fill the return cart from a locally-saved sale invoice — same items/quantities. */
    private fun selectSourceInvoice(invoiceId: String) {
        _state.update { s ->
            val invoice = s.sourceInvoices.firstOrNull { it.id == invoiceId } ?: return@update s
            val lines = runCatching {
                json.decodeFromString<List<InvoiceLine>>(invoice.linesJson)
            }.getOrDefault(emptyList())

            fun conversionFor(line: InvoiceLine): Double =
                s.productUnits[line.productId]?.firstOrNull { it.name == line.unit }?.conversionQty ?: 1.0

            val cart = lines.map { line ->
                CartLine(
                    productId = line.productId,
                    sku = line.sku,
                    nameAr = line.nameAr,
                    unitPrice = line.unitPrice,
                    qty = line.qty,
                    discountPct = line.discountPct,
                    unit = line.unit,
                    unitConversionQty = conversionFor(line),
                    taxRate = line.taxRate,
                    lineTaxType = runCatching { LineTaxType.valueOf(line.taxType) }.getOrDefault(s.taxType),
                    imageUrl = s.products.firstOrNull { it.id == line.productId }?.imageUrl,
                )
            }
            val sold = lines.associate { it.productId to it.qty * conversionFor(it) }

            s.copy(
                cart = cart,
                referenceInvoiceId = invoice.id,
                referenceNumber = invoice.number,
                soldQtyByProduct = sold,
                showSourcePicker = false,
                view = VoucherView.CART,
            )
        }
    }

    /**
     * Manual fallback when the sale isn't on this device: look the SALE up on the
     * server by voucher number + customer, then pre-fill the return from its lines.
     * `referenceNumber` is the server voucher number, so the backend validates it.
     */
    private fun lookupSourceByNumber() {
        val number = _state.value.sourceLookupQuery.trim()
        val customerNumber = _state.value.customer?.code
        if (number.isBlank() || customerNumber.isNullOrBlank()) return
        _state.update { it.copy(isLookingUp = true, errorAr = null) }
        viewModelScope.launch {
            val sale = runCatching { getCustomerSales.byNumber(number, customerNumber) }.getOrNull()
            if (sale == null) {
                _state.update { it.copy(isLookingUp = false, errorAr = getString(Res.string.err_source_invoice_not_found)) }
                return@launch
            }
            val lines = runCatching { getCustomerSales.lines(sale.id) }.getOrDefault(emptyList())
            _state.update { s ->
                val cart = lines.map { sl ->
                    val product = s.products.firstOrNull { it.sku == sl.sku }
                    val productId = product?.id ?: sl.sku
                    val conversion = sl.unitBaseQty?.toDouble()
                        ?: s.productUnits[productId]?.firstOrNull { it.name == sl.unitName }?.conversionQty
                        ?: 1.0
                    CartLine(
                        productId = productId,
                        sku = sl.sku,
                        nameAr = product?.nameAr ?: sl.name,
                        unitPrice = sl.unitPrice,
                        qty = sl.qty,
                        discountPct = sl.discountPct,
                        unit = sl.unitName ?: product?.unit ?: "",
                        unitConversionQty = conversion,
                        taxRate = sl.taxRate,
                        lineTaxType = s.taxType,
                        imageUrl = product?.imageUrl,
                    )
                }
                val sold = cart.associate { it.productId to it.qty * it.unitConversionQty }
                s.copy(
                    cart = cart,
                    referenceInvoiceId = sale.id,
                    referenceNumber = sale.number,
                    soldQtyByProduct = sold,
                    isLookingUp = false,
                    showSourcePicker = false,
                    sourceLookupQuery = "",
                    view = VoucherView.CART,
                )
            }
        }
    }

    /** RETURN can't exceed what was sold on the source invoice (compared in base units). */
    private fun capReturnQty(state: VoucherState, productId: String, unitConversionQty: Double, qty: Double): Double {
        if (!state.requiresSourceInvoice) return qty
        val soldBase = state.soldQtyByProduct[productId] ?: return qty
        if (unitConversionQty <= 0.0) return qty
        return qty.coerceAtMost(soldBase / unitConversionQty)
    }

    private fun applySearch() {
        val q = _state.value.searchQuery.trim().lowercase()
        val filtered = if (q.isEmpty()) _state.value.products else _state.value.products.filter {
            it.nameAr.lowercase().contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
        _state.update { it.copy(visibleProducts = filtered) }
    }

    private fun stepItem(product: Product, delta: Int) {
        _state.update { s ->
            val existing = s.cart.firstOrNull { it.productId == product.id }
            val newCart = when {
                existing == null && delta > 0 -> {
                    val defaultUnit = s.productUnits[product.id]?.minByOrNull { it.conversionQty }
                    s.cart + CartLine(
                        productId = product.id,
                        sku = product.sku,
                        nameAr = product.nameAr,
                        unitPrice = defaultUnit?.price ?: product.salePrice,
                        qty = 1.0,
                        unit = defaultUnit?.name ?: product.unit,
                        unitConversionQty = defaultUnit?.conversionQty ?: 1.0,
                        taxRate = product.taxRate,
                        lineTaxType = s.taxType,
                        imageUrl = product.imageUrl,
                    )
                }
                existing == null -> s.cart
                (existing.qty + delta) <= 0 -> s.cart.filterNot { it.productId == product.id }
                else -> s.cart.map { if (it.productId == product.id) it.copy(qty = it.qty + delta) else it }
            }
            s.copy(cart = newCart)
        }
    }

    private fun confirmDialog(event: VoucherEvent.ConfirmItemDialog) {
        _state.update { s ->
            val qty = capReturnQty(s, event.product.id, event.unitConversionQty, event.qty)
            val existing = s.cart.firstOrNull { it.productId == event.product.id }
            val newCart = when {
                qty <= 0 -> s.cart.filterNot { it.productId == event.product.id }
                existing == null -> s.cart + CartLine(
                    productId = event.product.id,
                    sku = event.product.sku,
                    nameAr = event.product.nameAr,
                    unitPrice = event.unitPrice,
                    qty = qty,
                    unit = event.unit,
                    unitConversionQty = event.unitConversionQty,
                    discountPct = event.discountPct,
                    taxRate = event.product.taxRate,
                    lineTaxType = s.taxType,
                    imageUrl = event.product.imageUrl,
                )
                else -> s.cart.map {
                    if (it.productId == event.product.id)
                        it.copy(
                            qty = qty,
                            unit = event.unit,
                            unitPrice = event.unitPrice,
                            unitConversionQty = event.unitConversionQty,
                            discountPct = event.discountPct,
                        )
                    else it
                }
            }
            s.copy(cart = newCart)
        }
    }

    private fun changeQty(productId: String, qty: Double) {
        _state.update { s ->
            val newCart = if (qty <= 0.0) s.cart.filterNot { it.productId == productId }
            else s.cart.map { if (it.productId == productId) it.copy(qty = qty) else it }
            s.copy(cart = newCart)
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        // Blocking return approval: file a request, then wait — no local save, no
        // print — until a manager approves (or we discard on reject/cancel).
        if (type == VoucherType.RETURN && s.returnNeedsApproval) {
            requestApproval()
            return
        }
        // Blocking discount approval: a SALE with a discount, when the salesman may
        // only request (not apply) discounts → file it and wait for the admin.
        if (type == VoucherType.SALE && s.needsDiscountApproval) {
            requestDiscountApprovalFlow()
            return
        }
        _state.update { it.copy(isSaving = true, showSaveSheet = false) }
        viewModelScope.launch {
            val salesmanId = session.currentUserId.orEmpty()
            val result = when (type) {
                VoucherType.SALE -> createSale(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    discountAmount = s.voucherDiscountAmount,
                    paymentMethod = s.paymentMethod,
                    notes = s.notes.takeIf { it.isNotBlank() },
                )
                VoucherType.RETURN -> createReturn(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    reason = s.reason!!.labelAr,
                    paymentMethod = s.paymentMethod,
                    extraNotes = s.notes.takeIf { it.isNotBlank() },
                    referenceInvoiceId = s.referenceInvoiceId,
                    referenceNumber = s.referenceNumber,
                )
                VoucherType.ORDER -> createRequest(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    expectedDeliveryAt = s.deliveryDate,
                    notes = s.notes.takeIf { it.isNotBlank() },
                )
            }
            result.fold(
                onSuccess = { entity ->
                    _state.update { it.copy(isSaving = false, savedNumber = entity.number, savedId = entity.id) }
                },
                onFailure = { ex ->
                    val msg = when (ex) {
                        is StockShortageException ->
                            getString(Res.string.err_stock_unavailable, ex.available, ex.requested)
                        is EmptyCartException -> getString(Res.string.err_cart_empty)
                        else -> getString(Res.string.err_unexpected)
                    }
                    _state.update { it.copy(isSaving = false, errorAr = msg) }
                },
            )
        }
    }

    /** File the return as a manager approval request, then poll until decided. */
    private fun requestApproval() {
        val s = _state.value
        pendingKind = PendingKind.RETURN
        _state.update { it.copy(isSaving = true, showSaveSheet = false, errorAr = null) }
        viewModelScope.launch {
            val result = requestReturnApproval(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                userCode = session.currentUserCode.orEmpty(),
                customerNumber = s.customer?.code,
                cart = s.cart,
                reason = s.reason!!.labelAr,
                paymentMethod = s.paymentMethod,
                extraNotes = s.notes.takeIf { it.isNotBlank() },
                referenceInvoiceId = s.referenceInvoiceId,
                referenceNumber = s.referenceNumber,
            )
            result.fold(
                onSuccess = { id ->
                    _state.update {
                        it.copy(isSaving = false, pendingApprovalId = id, approvalDecisionNote = null)
                    }
                    startApprovalPolling(id)
                },
                onFailure = {
                    _state.update { it.copy(isSaving = false, errorAr = getString(Res.string.err_approval_send_failed)) }
                },
            )
        }
    }

    /** File a discounted SALE as a manager approval request, then poll until decided. */
    private fun requestDiscountApprovalFlow() {
        val s = _state.value
        pendingKind = PendingKind.SALE_DISCOUNT
        _state.update { it.copy(isSaving = true, showSaveSheet = false, errorAr = null) }
        viewModelScope.launch {
            val result = requestDiscountApproval(
                customerId = customerId,
                salesmanId = session.currentUserId.orEmpty(),
                userCode = session.currentUserCode.orEmpty(),
                customerNumber = s.customer?.code,
                cart = s.cart,
                discountAmount = s.voucherDiscountAmount,
                paymentMethod = s.paymentMethod,
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            result.fold(
                onSuccess = { id ->
                    _state.update {
                        it.copy(isSaving = false, pendingApprovalId = id, approvalDecisionNote = null)
                    }
                    startApprovalPolling(id)
                },
                onFailure = {
                    _state.update { it.copy(isSaving = false, errorAr = getString(Res.string.err_approval_send_failed)) }
                },
            )
        }
    }

    /** Poll the pending request every 5s; commit + print on approve, discard on reject/cancel. */
    private fun startApprovalPolling(id: String) {
        approvalPollJob?.cancel()
        approvalPollJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                val decision = pollApproval(id).getOrNull() ?: continue
                when (decision.status) {
                    "approved" -> {
                        val s = _state.value
                        val number = decision.resultVoucher ?: id
                        val salesmanId = session.currentUserId.orEmpty()
                        val commit = when (pendingKind) {
                            PendingKind.RETURN -> commitApprovedReturn(
                                approvedNumber = number,
                                customerId = customerId,
                                salesmanId = salesmanId,
                                cart = s.cart,
                                reason = s.reason!!.labelAr,
                                paymentMethod = s.paymentMethod,
                                extraNotes = s.notes.takeIf { it.isNotBlank() },
                                referenceInvoiceId = s.referenceInvoiceId,
                                referenceNumber = s.referenceNumber,
                            )
                            PendingKind.SALE_DISCOUNT -> commitApprovedSale(
                                approvedNumber = number,
                                customerId = customerId,
                                salesmanId = salesmanId,
                                cart = s.cart,
                                discountAmount = s.voucherDiscountAmount,
                                paymentMethod = s.paymentMethod,
                                notes = s.notes.takeIf { it.isNotBlank() },
                            )
                        }
                        commit.fold(
                            onSuccess = { entity ->
                                _state.update {
                                    it.copy(
                                        pendingApprovalId = null,
                                        savedNumber = entity.number,
                                        savedId = entity.id,
                                    )
                                }
                            },
                            onFailure = {
                                _state.update {
                                    it.copy(pendingApprovalId = null, errorAr = getString(Res.string.err_approved_local_save_failed))
                                }
                            },
                        )
                        return@launch
                    }
                    "rejected" -> {
                        val note = decision.decisionNote?.takeIf { n -> n.isNotBlank() }
                        _state.update {
                            it.copy(
                                pendingApprovalId = null,
                                errorAr = if (note != null) getString(Res.string.err_return_rejected_note, note)
                                else getString(Res.string.err_return_rejected),
                            )
                        }
                        return@launch
                    }
                    "cancelled" -> {
                        _state.update { it.copy(pendingApprovalId = null) }
                        return@launch
                    }
                    else -> Unit   // still pending — keep polling
                }
            }
        }
    }

    /** Salesman backs out of the pending request (frees the cart screen). */
    private fun cancelPendingApproval() {
        val id = _state.value.pendingApprovalId ?: return
        approvalPollJob?.cancel()
        _state.update { it.copy(pendingApprovalId = null) }
        viewModelScope.launch { cancelApproval(id) }
    }

    override fun onCleared() {
        approvalPollJob?.cancel()
        super.onCleared()
    }
}

/** Maps the global settings tax treatment to the per-line calculator type. */
private fun TaxType.toLineTaxType(): LineTaxType = when (this) {
    TaxType.EXCLUDED_TAX -> LineTaxType.TAXABLE
    TaxType.INCLUDED_TAX -> LineTaxType.INCLUSIVE
}
