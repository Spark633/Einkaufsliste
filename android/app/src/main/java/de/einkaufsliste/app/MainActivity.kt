package de.einkaufsliste.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.ui.App
import de.einkaufsliste.app.ui.AppTheme

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

    /** Eine über „Teilen“ (z. B. aus WhatsApp) geschickte Liste empfangen. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: return
        if (!vm.receiveShared(text)) {
            Toast.makeText(this, "Die geteilte Nachricht enthält keine Einkaufsliste", Toast.LENGTH_LONG).show()
        }
    }
}
