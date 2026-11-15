package uz.tulpor.vpn.ui.servers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.tulpor.vpn.R
import uz.tulpor.vpn.ui.components.GoldButton
import uz.tulpor.vpn.ui.theme.TulporColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerSheet(
    initialText: String = "",
    initialSubscription: String = "",
    onDismiss: () -> Unit,
    onAdd: (links: String, subscription: String) -> Unit,
) {
    var links by rememberSaveable { mutableStateOf(initialText) }
    var sub by rememberSaveable { mutableStateOf(initialSubscription) }
    @Suppress("DEPRECATION") val clipboard = LocalClipboardManager.current
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TulporColors.Gold,
        unfocusedBorderColor = Color.White.copy(alpha = .18f),
        cursorColor = TulporColors.Gold,
        focusedLabelColor = TulporColors.Gold,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xF21A1450),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().imePadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.add_server), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = links, onValueChange = { links = it },
                label = { Text(stringResource(R.string.add_server_hint)) },
                placeholder = { Text("vless://…", color = TulporColors.Text3) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(16.dp), colors = colors,
            )
            TextButton(onClick = {
                clipboard.getText()?.text?.let { links = if (links.isBlank()) it else links + "\n" + it }
            }) {
                Icon(Icons.Rounded.ContentPaste, null, tint = TulporColors.Gold)
                Text("  " + stringResource(R.string.paste_clipboard), color = TulporColors.Gold)
            }
            OutlinedTextField(
                value = sub, onValueChange = { sub = it },
                label = { Text(stringResource(R.string.subscription_url)) },
                placeholder = { Text("https://…", color = TulporColors.Text3) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), colors = colors,
            )
            Spacer(Modifier.height(20.dp))
            GoldButton(stringResource(R.string.add), enabled = links.isNotBlank() || sub.isNotBlank()) {
                onAdd(links, sub)
            }
        }
    }
}
