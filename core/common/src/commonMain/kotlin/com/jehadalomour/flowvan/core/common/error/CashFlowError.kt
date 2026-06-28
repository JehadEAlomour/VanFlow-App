package com.jehadalomour.flowvan.core.common.error

sealed class CashFlowError(open val messageAr: String, open val messageEn: String) {

    data object Unknown : CashFlowError(
        messageAr = "حدث خطأ غير متوقع",
        messageEn = "An unexpected error occurred",
    )

    sealed class Auth(messageAr: String, messageEn: String) : CashFlowError(messageAr, messageEn) {
        data object UserNotFound : Auth(
            messageAr = "رقم الهاتف غير مسجل",
            messageEn = "Phone not registered",
        )

        data object WrongPassword : Auth(
            messageAr = "كلمة المرور خاطئة",
            messageEn = "Wrong password",
        )

        data object InvalidPhone : Auth(
            messageAr = "رقم الهاتف غير صحيح",
            messageEn = "Invalid phone number",
        )

        data object InvalidPassword : Auth(
            messageAr = "كلمة المرور قصيرة جداً",
            messageEn = "Password too short",
        )

        data object LocationDenied : Auth(
            messageAr = "تم رفض إذن الموقع — سيتم المتابعة بدون موقع",
            messageEn = "Location permission denied — proceeding without location",
        )
    }

    sealed class Network(messageAr: String, messageEn: String) : CashFlowError(messageAr, messageEn) {
        data object NotConfigured : Network(
            messageAr = "لم يتم ضبط عنوان الخادم",
            messageEn = "Backend server is not configured",
        )

        data object Unreachable : Network(
            messageAr = "تعذّر الوصول إلى الخادم — تحقق من الاتصال",
            messageEn = "Cannot reach the server — check your connection",
        )

        data object Unauthorized : Network(
            messageAr = "انتهت الجلسة — يرجى تسجيل الدخول من جديد",
            messageEn = "Session expired — please sign in again",
        )

        data object Forbidden : Network(
            messageAr = "لا تملك صلاحية لهذا الإجراء",
            messageEn = "You are not permitted to do this",
        )

        data object NotFound : Network(
            messageAr = "العنصر غير موجود على الخادم",
            messageEn = "Resource not found on the server",
        )

        data object Conflict : Network(
            messageAr = "العنصر موجود مسبقاً على الخادم",
            messageEn = "Resource already exists on the server",
        )

        data object Server : Network(
            messageAr = "خطأ في الخادم — حاول لاحقاً",
            messageEn = "Server error — try again later",
        )

        data class Validation(val detail: String) : Network(
            messageAr = "بيانات غير صالحة: $detail",
            messageEn = "Invalid request: $detail",
        )

        /** F10 — the action needs a manager approval request (403 APPROVAL_REQUIRED:<type>). */
        data class ApprovalRequired(val type: String) : Network(
            messageAr = "هذا الإجراء يتطلب موافقة المدير",
            messageEn = "This action requires manager approval",
        )
    }
}