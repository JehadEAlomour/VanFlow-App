package com.jehadalomour.flowvan.shared.domain.error

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
}