package com.jehadalomour.flowvan.shared.presentation.format

import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

private val levantineMonths = mapOf(
    Month.JANUARY to "كانون الثاني",
    Month.FEBRUARY to "شباط",
    Month.MARCH to "آذار",
    Month.APRIL to "نيسان",
    Month.MAY to "أيار",
    Month.JUNE to "حزيران",
    Month.JULY to "تموز",
    Month.AUGUST to "آب",
    Month.SEPTEMBER to "أيلول",
    Month.OCTOBER to "تشرين الأول",
    Month.NOVEMBER to "تشرين الثاني",
    Month.DECEMBER to "كانون الأول",
)

private val arabicWeekdays = mapOf(
    DayOfWeek.SATURDAY to "السبت",
    DayOfWeek.SUNDAY to "الأحد",
    DayOfWeek.MONDAY to "الإثنين",
    DayOfWeek.TUESDAY to "الثلاثاء",
    DayOfWeek.WEDNESDAY to "الأربعاء",
    DayOfWeek.THURSDAY to "الخميس",
    DayOfWeek.FRIDAY to "الجمعة",
)

private val englishWeekdays = mapOf(
    DayOfWeek.SATURDAY to "Saturday",
    DayOfWeek.SUNDAY to "Sunday",
    DayOfWeek.MONDAY to "Monday",
    DayOfWeek.TUESDAY to "Tuesday",
    DayOfWeek.WEDNESDAY to "Wednesday",
    DayOfWeek.THURSDAY to "Thursday",
    DayOfWeek.FRIDAY to "Friday",
)

private val englishMonths = mapOf(
    Month.JANUARY to "January", Month.FEBRUARY to "February", Month.MARCH to "March",
    Month.APRIL to "April", Month.MAY to "May", Month.JUNE to "June",
    Month.JULY to "July", Month.AUGUST to "August", Month.SEPTEMBER to "September",
    Month.OCTOBER to "October", Month.NOVEMBER to "November", Month.DECEMBER to "December",
)

/** Example AR: "الخميس 15 أيار" · EN: "Thursday 15 May" */
fun LocalDate.formatLevantine(language: AppLanguage = AppLanguage.AR): String = when (language) {
    AppLanguage.AR -> "${arabicWeekdays[dayOfWeek]} $dayOfMonth ${levantineMonths[month]}"
    AppLanguage.EN -> "${englishWeekdays[dayOfWeek]} $dayOfMonth ${englishMonths[month]}"
}
