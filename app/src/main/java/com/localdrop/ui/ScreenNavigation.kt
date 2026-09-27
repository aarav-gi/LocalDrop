package com.localdrop.ui

sealed class Screen {
    object Home : Screen()
    object SendFiles : Screen()
    object ShareQr : Screen()
    object ReceiveFiles : Screen()
    object History : Screen()
    object Settings : Screen()
}

enum class BottomTab {
    HOME, HISTORY, SETTINGS
}
