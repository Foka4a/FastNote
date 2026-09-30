package com.quicknotes.app.ui.components

import com.quicknotes.app.domain.model.CaptureSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val metaDateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

fun sourceLabel(source: CaptureSource): String = when (source) {
    CaptureSource.WIDGET_VOICE -> "voz"
    CaptureSource.WIDGET_TEXT -> "texto"
    CaptureSource.APP -> "app"
}

fun noteMeta(source: CaptureSource, timestamp: Long): String =
    "${sourceLabel(source)} · ${metaDateFormat.format(Date(timestamp))}"
