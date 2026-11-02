package com.conreo.couchytv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import java.security.MessageDigest

const val PIN_LENGTH = 4

fun hashPin(pin: String): String =
    MessageDigest.getInstance("SHA-256").digest(("couchy:$pin").toByteArray())
        .joinToString("") { "%02x".format(it) }

/**
 * Remote-friendly numeric PIN pad. Digits can be entered with the on-screen
 * keypad (D-pad + OK) or directly with the remote's number keys.
 * [onSubmit] returns true if the PIN is accepted (dialog closes), false to show an error.
 */
@Composable
fun PinDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun add(d: Char) {
        if (pin.length >= PIN_LENGTH) return
        error = false
        pin += d
        if (pin.length == PIN_LENGTH) {
            if (!onSubmit(pin)) { error = true; pin = "" }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = when (e.key) {
                    Key.Zero, Key.NumPad0 -> '0'; Key.One, Key.NumPad1 -> '1'
                    Key.Two, Key.NumPad2 -> '2'; Key.Three, Key.NumPad3 -> '3'
                    Key.Four, Key.NumPad4 -> '4'; Key.Five, Key.NumPad5 -> '5'
                    Key.Six, Key.NumPad6 -> '6'; Key.Seven, Key.NumPad7 -> '7'
                    Key.Eight, Key.NumPad8 -> '8'; Key.Nine, Key.NumPad9 -> '9'
                    else -> null
                }
                when {
                    digit != null -> { add(digit); true }
                    e.key == Key.Backspace || e.key == Key.Delete -> { pin = pin.dropLast(1); true }
                    else -> false
                }
            },
        ) {
            Column(
                Modifier.width(300.dp).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = (0 until PIN_LENGTH).joinToString(" ") { if (it < pin.length) "●" else "○" },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    if (error) "Hatalı PIN" else " ",
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val first = remember { FocusRequester() }
                val rows = listOf("123", "456", "789", "<0✓")
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            Button(
                                onClick = {
                                    when (c) {
                                        '<' -> pin = pin.dropLast(1)
                                        '✓' -> onDismiss()
                                        else -> add(c)
                                    }
                                },
                                modifier = Modifier.size(64.dp, 48.dp)
                                    .then(if (c == '1') Modifier.focusRequester(first) else Modifier),
                            ) {
                                Text(
                                    when (c) { '<' -> "⌫"; '✓' -> "✕"; else -> c.toString() },
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
            }
        }
    }
}
