package com.jehadalomour.flowvan.core.domain

/**
 * Single app-wide error contract. ViewModels store `error: AppError?` in their
 * state; the UI layer translates [messageKey] into bilingual text via a
 * `StringResource` lookup. ViewModels never carry Arabic or English strings.
 */
sealed interface AppError {
    /** i18n key resolved to localized text at the UI layer. */
    val messageKey: String

    // Auth
    data object PhoneNotRegistered : AppError {
        override val messageKey = "err.auth.phone_not_registered"
    }

    data object WrongPassword : AppError {
        override val messageKey = "err.auth.wrong_password"
    }

    data object LocationDenied : AppError {
        override val messageKey = "err.auth.location_denied"
    }

    // Validation
    data class FieldRequired(val field: String) : AppError {
        override val messageKey = "err.validation.required"
    }

    data class InsufficientStock(
        val productName: String,
        val available: Int,
    ) : AppError {
        override val messageKey = "err.stock.insufficient"
    }

    // Data
    data object NotFound : AppError {
        override val messageKey = "err.data.not_found"
    }

    data object NetworkUnavailable : AppError {
        override val messageKey = "err.net.unavailable"
    }

    data class Unknown(val cause: Throwable?) : AppError {
        override val messageKey = "err.unknown"
    }
}
