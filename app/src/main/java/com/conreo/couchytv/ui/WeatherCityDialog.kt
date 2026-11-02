package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.conreo.couchytv.data.Weather
import kotlinx.coroutines.launch

/**
 * Manual city picker: type a name, search, pick a result.
 * [onPick] gets null for "turn weather off".
 */
@Composable
fun WeatherCityDialog(
    onPick: (Weather.Place?) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Weather.Place>?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun search() {
        if (query.isBlank() || busy) return
        busy = true
        scope.launch { results = Weather.search(query); busy = false }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.width(400.dp).height(460.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Hava durumu şehri", style = MaterialTheme.typography.titleMedium)
                val f = remember { FocusRequester() }
                Box(
                    Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(f),
                    )
                    if (query.isEmpty()) Text("Şehir adı yazın…", color = Color.White.copy(alpha = 0.4f))
                }
                LaunchedEffect(Unit) { runCatching { f.requestFocus() } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { search() }) { Text(if (busy) "Aranıyor…" else "Ara") }
                    Button(onClick = { onPick(null) }) { Text("Kapat") }
                }
                val list = results
                when {
                    list == null -> {}
                    list.isEmpty() -> Text("Sonuç bulunamadı", color = Color.White.copy(alpha = 0.6f))
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(list.size) { i ->
                            val p = list[i]
                            ListItem(
                                selected = false,
                                onClick = { onPick(p) },
                                headlineContent = { Text(p.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}
