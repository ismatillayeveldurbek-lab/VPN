package uz.tulpor.vpn.ui.settings

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.tulpor.vpn.R
import uz.tulpor.vpn.ui.components.appear
import uz.tulpor.vpn.ui.components.bouncyClickable
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors
import uz.tulpor.vpn.vpn.ConnectionLog

/** Shows the last connection's log (app events + Xray errors) and lets the user share it. */
@Composable
fun LogScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(ConnectionLog.read(ctx)) }
    // Refresh while open so a connection in progress can be watched live.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500); text = ConnectionLog.read(ctx)
        }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(text.length) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Text(
                stringResource(R.string.log_title), style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LogAction(Icons.Rounded.Share, stringResource(R.string.share), Modifier.weight(1f)) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Tulpor VPN log")
                    .putExtra(Intent.EXTRA_TEXT, text.ifBlank { "(empty)" })
                ctx.startActivity(Intent.createChooser(send, null))
            }
            LogAction(Icons.Rounded.Refresh, stringResource(R.string.refresh), Modifier.weight(1f)) {
                text = ConnectionLog.read(ctx)
            }
            LogAction(Icons.Rounded.DeleteOutline, stringResource(R.string.clear), Modifier.weight(1f)) {
                ConnectionLog.clear(ctx); text = ""
            }
        }
        Box(
            Modifier
                .appear(0)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .fillMaxSize()
                .glass(RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            if (text.isBlank()) {
                Text(stringResource(R.string.log_empty), color = TulporColors.Text3, fontSize = 14.sp)
            } else {
                SelectionContainer {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 16.sp,
                        color = TulporColors.Text2,
                        modifier = Modifier.verticalScroll(scroll).horizontalScroll(rememberScrollState()),
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .bouncyClickable(CircleShape, onClick = onClick)
            .glass(CircleShape)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = TulporColors.Gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
