package com.jehadalomour.flowvan.feature.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.ApprovalApi
import com.jehadalomour.flowvan.core.network.api.CustomerApi
import com.jehadalomour.flowvan.core.network.api.CreateCustomerOutcome
import com.jehadalomour.flowvan.core.network.dto.CreateCustomerRequest
import com.jehadalomour.flowvan.core.network.mapper.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Create a new customer for the field: name + phone + a picked GPS location.
 * Posts to the backend (which assigns the customer number), caches the returned
 * customer locally so the list refreshes reactively, and exposes [savedCustomerId]
 * so the UI can navigate straight to the new customer's page. The customer is
 * assigned to the logged-in salesman via `repId`.
 */
class CreateCustomerViewModel(
    private val customerApi: CustomerApi,
    private val approvalApi: ApprovalApi,
    private val customers: CustomerRepository,
    private val session: SessionStore,
    private val locationProvider: LocationProvider,
    // Set when the screen was opened from customer search: the shop's name,
    // phone and location come prefilled, and the lead id rides through to save.
    private val prefill: CreateCustomerPrefill? = null,
) : ViewModel() {

    private var approvalWatch: Job? = null

    // Seeded from the session so the screen can warn BEFORE the rep fills a form
    // and photographs a document, rather than only in the answer to the save.
    private val _state = MutableStateFlow(
        CreateCustomerState(
            willNeedApproval = !session.canCreateCustomerDirect,
            name = prefill?.name.orEmpty(),
            phone = prefill?.phone.orEmpty(),
            lat = prefill?.lat,
            lng = prefill?.lng,
        ),
    )
    val state: StateFlow<CreateCustomerState> = _state.asStateFlow()

    fun onEvent(event: CreateCustomerEvent) {
        when (event) {
            is CreateCustomerEvent.NameChanged -> _state.update { it.copy(name = event.v) }
            is CreateCustomerEvent.PhoneChanged ->
                _state.update { it.copy(phone = event.v.filter { c -> c.isDigit() || c == '+' }) }
            CreateCustomerEvent.CaptureLocation -> captureLocation()
            CreateCustomerEvent.ClearLocation -> _state.update { it.copy(lat = null, lng = null) }
            CreateCustomerEvent.Save -> save()
            CreateCustomerEvent.DismissError ->
                _state.update { it.copy(errorAr = null, locationErrorAr = null, documentErrorAr = null) }
            is CreateCustomerEvent.DocumentPicked -> uploadDocument(event.doc)
            CreateCustomerEvent.ClearDocument ->
                _state.update {
                    it.copy(document = null, documentPhotoId = null, documentErrorAr = null)
                }
        }
    }

    private fun captureLocation() {
        _state.update { it.copy(isCapturingLocation = true, locationErrorAr = null) }
        viewModelScope.launch {
            val loc = locationProvider.lastLocation()
            _state.update {
                if (loc == null) it.copy(isCapturingLocation = false, locationErrorAr = LOCATION_UNAVAILABLE)
                else it.copy(isCapturingLocation = false, lat = loc.lat, lng = loc.lng)
            }
        }
    }

    /**
     * Upload as soon as the rep picks, not at save time.
     *
     * The photo is the slowest part of the form and the likeliest to fail on a
     * weak signal. Doing it here means the rep finds out while they are still
     * standing in the shop and can retake it — not after they have filled every
     * field and pressed save.
     */
    private fun uploadDocument(doc: PickedDocument) {
        _state.update {
            it.copy(document = doc, isUploadingDocument = true, documentErrorAr = null)
        }
        viewModelScope.launch {
            runCatching {
                customerApi.uploadDocumentPhoto(doc.fileName, doc.mimeType, doc.bytes).id
            }.fold(
                onSuccess = { id ->
                    _state.update { it.copy(isUploadingDocument = false, documentPhotoId = id) }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isUploadingDocument = false,
                            document = null,
                            documentPhotoId = null,
                            documentErrorAr = ERR_DOCUMENT,
                        )
                    }
                },
            )
        }
    }

    /**
     * Hold the rep on this screen until the office decides.
     *
     * They are standing in front of the shopkeeper: sending them back to a list
     * with no idea whether the customer exists is what makes them phone the
     * office. Polling rather than push because the app has no socket — a request
     * is normally decided in a minute or two, and the screen is in the
     * foreground the whole time.
     *
     * Cancelled automatically with viewModelScope when they do leave.
     */
    private fun watchApproval(approvalId: String) {
        approvalWatch?.cancel()
        approvalWatch = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val req = runCatching { approvalApi.one(approvalId) }.getOrNull() ?: continue
                when (req.status) {
                    "approved" -> {
                        // resultVoucher carries the new customer NUMBER; the screen
                        // needs the id to navigate, so resolve it once here — which
                        // also caches the customer for the list.
                        val number = req.resultVoucher
                        val id = if (number.isNullOrBlank()) null else {
                            runCatching {
                                customerApi.list(q = number, limit = 1).items.firstOrNull()
                            }.getOrNull()?.also { customers.save(it.toEntity()) }?.id
                        }
                        _state.update {
                            it.copy(
                                awaitingApproval = false,
                                approvalDecision = ApprovalDecision.Approved,
                                savedCustomerId = id,
                            )
                        }
                        return@launch
                    }
                    "rejected", "cancelled" -> {
                        _state.update {
                            it.copy(
                                awaitingApproval = false,
                                approvalDecision = ApprovalDecision.Rejected,
                                // The office's reason, so the rep can fix it and
                                // resubmit instead of guessing.
                                errorAr = req.decisionNote ?: ERR_REJECTED,
                            )
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private fun save() {
        val s = _state.value
        val name = s.name.trim()
        if (name.length < 2) {
            _state.update { it.copy(errorAr = ERR_NAME) }
            return
        }
        _state.update { it.copy(isSaving = true, errorAr = null) }
        viewModelScope.launch {
            val result = runCatching {
                customerApi.createOrRequest(
                    CreateCustomerRequest(
                        customerName = name,
                        nameAr = name,
                        phone = s.phone.trim().takeIf { it.isNotBlank() },
                        latitude = s.lat?.toString(),
                        longitude = s.lng?.toString(),
                        repId = session.currentRepId?.takeIf { it.isNotBlank() },
                        photoId = s.documentPhotoId,
                        sourceProspectId = prefill?.prospectId,
                    ),
                )
            }
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is CreateCustomerOutcome.Created -> {
                            // Cache the server customer so the list updates reactively.
                            customers.save(outcome.customer.toEntity())
                            _state.update {
                                it.copy(isSaving = false, savedCustomerId = outcome.customer.id)
                            }
                        }
                        is CreateCustomerOutcome.PendingApproval -> {
                            // Deliberately NOT cached: it is not a customer yet, and a
                            // rep who can see it in their list will try to sell to it.
                            _state.update {
                                it.copy(
                                    isSaving = false,
                                    awaitingApproval = true,
                                    pendingApprovalId = outcome.approvalId,
                                )
                            }
                            watchApproval(outcome.approvalId)
                        }
                    }
                },
                onFailure = { _state.update { it.copy(isSaving = false, errorAr = ERR_SAVE) } },
            )
        }
    }

    private companion object {
        const val ERR_NAME = "أدخل اسم العميل (حرفان على الأقل)"
        const val ERR_SAVE = "تعذّر حفظ العميل. تحقق من الاتصال وحاول مرة أخرى."
        const val ERR_DOCUMENT = "تعذّر رفع صورة الوثيقة. حاول مرة أخرى."
        const val ERR_REJECTED = "تم رفض طلب إضافة العميل."
        const val POLL_INTERVAL_MS = 5_000L
        const val LOCATION_UNAVAILABLE = "تعذّر تحديد الموقع. تأكد من تفعيل GPS والصلاحيات."
    }
}
