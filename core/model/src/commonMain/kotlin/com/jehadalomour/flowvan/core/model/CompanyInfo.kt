package com.jehadalomour.flowvan.core.model

/** Company profile shown on the printed voucher header (name + tax registration number). */
data class CompanyInfo(
    val nameAr: String = "",
    val nameEn: String = "",
    val taxNumber: String = "",
)
