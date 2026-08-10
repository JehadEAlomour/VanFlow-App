package com.jehadalomour.flowvan.feature.customer

data class CreateCustomerState(
    val name: String = "",
    val phone: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val isCapturingLocation: Boolean = false,
    val locationErrorAr: String? = null,
    val isSaving: Boolean = false,
    val savedCustomerId: String? = null,
    val errorAr: String? = null,

    // ── Document photo ────────────────────────────────────────────────────────
    /** Bytes of the chosen photo, held only until it is uploaded. */
    val document: PickedDocument? = null,
    val isUploadingDocument: Boolean = false,
    /** Server id from POST /customers/photo — what actually travels on save. */
    val documentPhotoId: String? = null,
    val documentErrorAr: String? = null,

    /**
     * What we expect to happen on save, read from the session before the rep
     * types anything. Only to warn them in advance — the outcome below is what
     * actually happened, and comes from the server.
     */
    val willNeedApproval: Boolean = true,

    /**
     * True when the save produced an APPROVAL REQUEST instead of a customer,
     * because this salesman lacks canCreateCustomerDirect. The screen says so
     * rather than pretending the customer is ready to sell to.
     */
    val awaitingApproval: Boolean = false,
    /** The request being waited on, so the rep can cancel it. */
    val pendingApprovalId: String? = null,
    /** Set once the office decides; null while still waiting. */
    val approvalDecision: ApprovalDecision? = null,
) {
    /**
     * The photo is part of "can I save", not a nicety: the backend rejects a
     * salesman create without one (400 "A customer document photo is required"),
     * so letting the button through would only produce a failed round trip.
     */
    val canSave: Boolean
        get() = name.trim().length >= 2 && !isSaving && !isUploadingDocument &&
            documentPhotoId != null &&
            // Already submitted: pressing save again would file a SECOND request
            // for the same shop, and the office would approve both.
            !awaitingApproval && approvalDecision == null
    val hasLocation: Boolean get() = lat != null && lng != null
    val hasDocument: Boolean get() = documentPhotoId != null
}

enum class ApprovalDecision { Approved, Rejected }

sealed interface CreateCustomerEvent {
    data class NameChanged(val v: String) : CreateCustomerEvent
    data class PhoneChanged(val v: String) : CreateCustomerEvent
    data object CaptureLocation : CreateCustomerEvent
    data object ClearLocation : CreateCustomerEvent
    data object Save : CreateCustomerEvent
    data object DismissError : CreateCustomerEvent

    /** The rep picked a photo (camera or gallery); upload starts immediately. */
    data class DocumentPicked(val doc: PickedDocument) : CreateCustomerEvent
    data object ClearDocument : CreateCustomerEvent
}
