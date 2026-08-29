package com.jehadalomour.flowvan.feature.customer

/**
 * One picked image, tracked from the moment the rep chooses it through its
 * upload. The server [uploadedId] (from POST /customers/photo) is what travels on
 * save; [failed] marks an upload the rep can remove and retake.
 */
data class CustomerPhoto(
    val localId: Long,
    val label: String,
    val uploading: Boolean = false,
    val uploadedId: String? = null,
    val failed: Boolean = false,
)

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

    // ── Photos ────────────────────────────────────────────────────────────────
    /** At least one is required; the rep may add more images of the shop. */
    val photos: List<CustomerPhoto> = emptyList(),
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
    /** Uploaded photo ids ready to travel on save (primary first, then extras). */
    val uploadedPhotoIds: List<String> get() = photos.mapNotNull { it.uploadedId }
    val isUploadingAnyPhoto: Boolean get() = photos.any { it.uploading }
    val hasLocation: Boolean get() = lat != null && lng != null
    val hasDocument: Boolean get() = uploadedPhotoIds.isNotEmpty()
    /** A shop phone is a real number, not one stray digit. */
    val phoneValid: Boolean get() = phone.trim().length >= 7

    /**
     * All four are required for a field customer, by request: name, a phone
     * number, a captured GPS location, and at least one uploaded photo. The
     * backend also rejects a salesman create without a photo, so letting the
     * button through on any of these would only produce a failed round trip.
     */
    val canSave: Boolean
        get() = name.trim().length >= 2 &&
            phoneValid &&
            hasLocation &&
            hasDocument &&
            !isUploadingAnyPhoto &&
            !isSaving &&
            // Already submitted: pressing save again would file a SECOND request
            // for the same shop, and the office would approve both.
            !awaitingApproval && approvalDecision == null
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
    /** Remove one photo (uploaded, uploading, or failed) by its local id. */
    data class RemovePhoto(val localId: Long) : CreateCustomerEvent
}

/**
 * Values carried in when the create screen is opened from customer search. All
 * optional except the lead id — the rep may edit any of them before saving.
 */
data class CreateCustomerPrefill(
    val name: String?,
    val phone: String?,
    val lat: Double?,
    val lng: Double?,
    val prospectId: String,
)
