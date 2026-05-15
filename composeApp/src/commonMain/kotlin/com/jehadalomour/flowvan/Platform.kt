package com.jehadalomour.flowvan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform