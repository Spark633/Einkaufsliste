package de.einkaufsliste.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.ui.App
import de.einkaufsliste.app.ui.AppTheme
import de.einkaufsliste.app.ui.readQrFromImage

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            AppTheme { App(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Eine über „Teilen“ geschickte Liste empfangen: als Text (z. B. aus WhatsApp) oder als Bild mit QR-Code. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("image/") == true) {
            val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
            readQrFromImage(this, uri, onResult = { text ->
                if (!vm.receiveShared(text)) toast("Dieser QR-Code enthält keine Einkaufsliste")
            }, onError = ::toast)
            return
        }
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: return
        if (!vm.receiveShared(text)) toast("Die geteilte Nachricht enthält keine Einkaufsliste")
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
