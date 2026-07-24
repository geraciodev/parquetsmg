package com.geraciodev.parquetsmg

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main(args: Array<String>) = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    val initialFile = args.firstOrNull()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "ParquetsMG - Lector de Parquet",
        state = windowState
    ) {
        App(initialFile)
    }
}
