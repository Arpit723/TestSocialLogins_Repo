package com.bk.testsocialloginskmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform