package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.common.error.CashFlowError

/** Thrown by the login flow; carries a typed [CashFlowError] the UI maps to a message. */
class AuthException(val error: CashFlowError) : Exception(error.messageEn)
