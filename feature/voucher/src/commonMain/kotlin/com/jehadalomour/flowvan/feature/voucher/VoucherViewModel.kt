package com.jehadalomour.flowvan.feature.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.location.LatLng
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.location.isWithinProximity
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.OfferRepository
import com.jehadalomour.flowvan.core.data.repository.PriceListRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.LineTaxType
import com.jehadalomour.flowvan.core.model.OfferEvaluation
import com.jehadalomour.flowvan.core.model.OfferTotals
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.TaxType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.FlowPreview
import com.jehadalomour.flowvan.core.domain.usecase.CancelApprovalUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CommitApprovedReturnUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateRequestVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateReturnVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreateSaleVoucherUseCase
import com.jehadalomour.flowvan.core.domain.usecase.CreditLimitExceededException
import com.jehadalomour.flowvan.core.domain.usecase.EmptyCartException
import com.jehadalomour.flowvan.core.domain.usecase.EvaluateOffersUseCase
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    private val evaluateOffers: EvaluateOffersUseCase,
    private val requestReturnApproval: RequestReturnApprovalUseCase,
    private val pollApproval: PollApprovalUseCase,
    private val cancelApproval: CancelApprovalUseCase,
    private val commitApprovedReturn: CommitApprovedReturnUseCase,
    private val requestDiscountApproval: RequestDiscountApprovalUseCase,
    private val commitApprovedSale: CommitApprovedSaleUseCase,
    private val offerRepository: OfferRepository,
    private val priceLists: PriceListRepository,
    private val location: LocationProvider,
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
        _state.update {
            it.copy(
                canDiscount = session.can("vouchers.discount.direct"),
                canRequestDiscount = session.can("vouchers.discount.approval"),
                canEditPrice = session.can("vouchers.priceOverride"),
                canCreateReturn = session.can("vouchers.return.create"),
                returnNeedsApproval = session.can("vouchers.return.approval"),
            )
        }

        // SALE only: re-evaluate offers (debounced) whenever the cart, payment method,
        // or the rep's gift picks change. RETURN/ORDER never evaluate offers.
        if (type == VoucherType.SALE) {
            observeCartForOffers()
            // Freshen the offline offers cache on open so the first offline evaluation is
            // as current as possible. Best-effort — a failure leaves the existing cache.
            viewModelScope.launch { offerRepository.refresh() }
        }

        customers.observeById(customerId)
            .onEach { c ->
                _state.update { it.copy(customer = c) }
                // Load the customer's price list (sku→price) so items price at the
                // contracted list price, falling back to the base catalog price.
                val prices = priceLists.pricesForList(c?.priceListId)
                _state.update { it.copy(customerPrices = prices) }
            }
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

    // ── Offers (SALE only) ─────────────────────────────────────────────────────

    /** A cart fingerprint that changes only when offer-relevant cart data changes. */
    private fun cartKey(s: VoucherState): List<Pair<String, Double>> =
        s.cart.map { it.sku to it.qty }

    @OptIn(FlowPreview::class)
    private fun observeCartForOffers() {
        _state
            .map { Triple(it.paymentMethod, cartKey(it), it.chosenFreeItems) }
            .distinctUntilChanged()
            .onEach { _state.update { s -> s.copy(isEvaluatingOffers = s.cart.isNotEmpty()) } }
            .debounce(300)
            .onEach { evaluateOffersNow() }
            .launchIn(viewModelScope)
    }

    private suspend fun evaluateOffersNow() {
        val s = _state.value
        if (s.cart.isEmpty()) {
            applyEvaluation(OfferEvaluation.EMPTY)
            return
        }
        val result = evaluateOffers(
            cart = s.cart,
            customerId = customerId,                // offline eligibility / new-customer lookup
            customerNumber = s.customer?.code,
            repId = session.currentUserId,
            // The customer number identifies the buyer; the backend resolves the store.
            storeNumber = null,
            paymentMethod = s.paymentMethod.name,   // CASH/CREDIT condition for payment-method offers
            chosenFreeItems = s.chosenFreeItems,    // rep's GIFT picks → server returns FREE lines
            // The customer has a contracted price list → evaluate on-device so offers apply on
            // the list prices instead of the server re-pricing the cart at base catalog prices.
            hasContractPrices = s.customerPrices.isNotEmpty(),
        )
        result.fold(
            onSuccess = { applyEvaluation(it) },
            // Offline / failure: never break the sale — fall back to on-device totals.
            onFailure = { applyOffline() },
        )
    }

    /**
     * Merge an evaluation result into state. Prune the rep's gift picks down to items
     * still offered by some pending choice, so a cart edit that drops the trigger doesn't
     * leave a stale gift selected.
     */
    private fun applyEvaluation(eval: OfferEvaluation) {
        _state.update { s ->
            // Keep only picks still offered (multiset — duplicates preserved), and cap the total
            // to the current quota so a cart edit that shrinks the entitlement drops extra picks.
            val validGiftPool = eval.pendingChoices.flatMap { it.choices }.toSet()
            val totalQuota = eval.pendingChoices.sumOf { it.qty }
            val prunedGifts = s.chosenFreeItems.filter { it in validGiftPool }.take(totalQuota)
            s.copy(
                appliedOffers = eval.appliedOffers,
                freeLines = eval.freeLines,
                pendingChoices = eval.pendingChoices,
                chosenFreeItems = prunedGifts,
                serverLines = eval.serverLines,
                serverTotals = eval.totals,
                offersSource = eval.source,
                isEvaluatingOffers = false,
            )
        }
    }

    /**
     * Both the server and the offline evaluator failed (e.g. empty offers cache) — drop all
     * offers and fall back to on-device totals. [offersSource] = null flags "no evaluation".
     */
    private fun applyOffline() {
        _state.update { s ->
            s.copy(
                appliedOffers = emptyList(),
                freeLines = emptyList(),
                pendingChoices = emptyList(),
                serverLines = emptyList(),
                serverTotals = OfferTotals.ZERO,
                offersSource = null,
                isEvaluatingOffers = false,
            )
        }
    }

    /**
     * Add one GIFT pick for an ITEM_QTY_REWARD offer (SALE). [chosenFreeItems] is a multiset —
     * the same pool item may be picked several times (e.g. "buy 10 water → 3 free water" =
     * water ×3). We DO NOT add a cart line; the server (and the offline evaluator) turns the
     * picks into FREE lines on the next debounced re-evaluate and on upload. Ignored once the
     * offer's `qty` quota is filled.
     */
    private fun addFreeItem(offerId: String, itemNumber: String) {
        _state.update { s ->
            val choice = s.pendingChoices.firstOrNull { it.offerId == offerId } ?: return@update s
            val poolForOffer = choice.choices.toSet()
            if (itemNumber !in poolForOffer) return@update s
            val picksForOffer = s.chosenFreeItems.count { it in poolForOffer }
            if (picksForOffer >= choice.qty) return@update s   // quota filled
            s.copy(chosenFreeItems = s.chosenFreeItems + itemNumber)
        }
    }

    /** Remove one instance of a GIFT pick (decrement its stepper). */
    private fun removeFreeItem(itemNumber: String) {
        _state.update { s ->
            val idx = s.chosenFreeItems.indexOfLast { it == itemNumber }
            if (idx < 0) return@update s
            s.copy(chosenFreeItems = s.chosenFreeItems.toMutableList().apply { removeAt(idx) })
        }
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
            is VoucherEvent.PaymentMethodChosen -> _state.update {
                it.copy(paymentMethod = event.method, paymentChosen = true)
            }
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

            is VoucherEvent.ChooseFreeItem -> addFreeItem(event.offerId, event.itemNumber)
            is VoucherEvent.RemoveFreeItem -> removeFreeItem(event.itemNumber)
            VoucherEvent.DismissFreeItemSheet -> _state.update { it.copy(pendingChoices = emptyList()) }
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
                    // Price-list price (base-unit) for this customer, else the base catalog price.
                    val listBase = s.customerPrices[product.sku]
                    val resolvedPrice = if (listBase != null) {
                        listBase * (defaultUnit?.conversionQty ?: 1.0)
                    } else {
                        defaultUnit?.price ?: product.salePrice
                    }
                    s.cart + CartLine(
                        productId = product.id,
                        sku = product.sku,
                        nameAr = product.nameAr,
                        unitPrice = resolvedPrice,
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
            // Location lock: a restricted rep must be at the customer to create the
            // voucher. Blocks a rep who opened the screen in range then drifted away,
            // so we never create a local sale the backend will reject on sync.
            proximityBlockMessage(s)?.let { msg ->
                _state.update { it.copy(isSaving = false, errorAr = msg) }
                return@launch
            }
            val salesmanId = session.currentUserId.orEmpty()
            val result = when (type) {
                VoucherType.SALE -> createSale(
                    customerId = customerId,
                    salesmanId = salesmanId,
                    cart = s.cart,
                    discountAmount = s.voucherDiscountAmount,
                    paymentMethod = s.paymentMethod,
                    notes = s.notes.takeIf { it.isNotBlank() },
                    chosenFreeItems = s.chosenFreeItems,   // GIFT picks → server adds free lines on upload
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
                        is CreditLimitExceededException ->
                            getString(
                                Res.string.err_credit_limit,
                                ex.creditLimit.toInt(),
                                ex.available.toInt(),
                            )
                        else -> getString(Res.string.err_unexpected)
                    }
                    _state.update { it.copy(isSaving = false, errorAr = msg) }
                },
            )
        }
    }

    /**
     * Location lock (customers.requireProximity). Returns a localized error when the
     * rep may not act here, else null. Fail closed: no GPS fix → blocked. When the
     * customer has no saved location the create use case seeds it from the rep's fix,
     * so we only require a fix (not proximity) in that case.
     */
    private suspend fun proximityBlockMessage(s: VoucherState): String? {
        if (!session.can("customers.requireProximity")) return null
        val fix = location.lastLocation()
            ?: return getString(Res.string.proximity_blocked_gps)
        val lat = s.customer?.lat
        val lng = s.customer?.lng
        if (lat != null && lng != null && !isWithinProximity(fix, LatLng(lat, lng))) {
            return getString(Res.string.proximity_blocked_far)
        }
        return null
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
