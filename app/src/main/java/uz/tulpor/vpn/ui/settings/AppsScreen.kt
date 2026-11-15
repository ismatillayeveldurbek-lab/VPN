package uz.tulpor.vpn.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.SplitMode
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

@Composable
fun AppsScreen(
    mode: SplitMode,
    selected: Set<String>,
    onMode: (SplitMode) -> Unit,
    onSelected: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != ctx.packageName }
                .map { AppEntry(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm).toBitmap(96)) }
                .sortedBy { it.label.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeChip(stringResource(R.string.split_all), mode == SplitMode.ALL) { onMode(SplitMode.ALL) }
            ModeChip(stringResource(R.string.split_only), mode == SplitMode.ONLY_SELECTED) { onMode(SplitMode.ONLY_SELECTED) }
            ModeChip(stringResource(R.string.split_except), mode == SplitMode.EXCEPT_SELECTED) { onMode(SplitMode.EXCEPT_SELECTED) }
        }
        if (mode != SplitMode.ALL) {
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().glass(RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = TulporColors.Text3, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_apps), color = TulporColors.Text3, fontSize = 14.sp)
                    BasicTextField(query, { query = it }, singleLine = true,
                        textStyle = TextStyle(color = TulporColors.Text1, fontSize = 14.sp),
                        cursorBrush = SolidColor(TulporColors.Gold), modifier = Modifier.fillMaxWidth())
                }
            }
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TulporColors.Gold) }
            } else {
                val shown = list.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
                    .sortedByDescending { it.pkg in selected }
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(shown, key = { it.pkg }) { app ->
                        val checked = app.pkg in selected
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(14.dp), selected = checked)
                                .clickable { onSelected(if (checked) selected - app.pkg else selected + app.pkg) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            app.icon?.let { Image(it, null, Modifier.size(36.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                                Text(app.pkg, color = TulporColors.Text3, fontSize = 11.sp, maxLines = 1)
                            }
                            Checkbox(checked, onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = TulporColors.Gold, checkmarkColor = TulporColors.OnGold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .then(if (selected) Modifier.clip(CircleShape).background(TulporColors.Gold) else Modifier.glass(CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 12.sp, color = if (selected) TulporColors.OnGold else TulporColors.Text2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private fun Drawable.toBitmap(size: Int): ImageBitmap? = runCatching {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    setBounds(0, 0, size, size)
    draw(c)
    bmp.asImageBitmap()
}.getOrNull()
