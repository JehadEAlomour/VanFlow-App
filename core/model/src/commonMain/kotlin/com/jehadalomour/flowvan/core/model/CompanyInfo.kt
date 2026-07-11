package com.jehadalomour.flowvan.core.model

/** Company profile shown on the printed voucher header (name, tax number, logo). */
data class CompanyInfo(
    val nameAr: String = "",
    val nameEn: String = "",
    val taxNumber: String = "",
    /** Company logo as a `data:<mime>;base64,...` URI (or bare base64); blank = use the default. */
    val logo: String = "",
)
