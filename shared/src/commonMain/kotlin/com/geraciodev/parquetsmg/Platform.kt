package com.geraciodev.parquetsmg

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform