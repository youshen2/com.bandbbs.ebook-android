package com.bandbbs.ebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.startImport(it)
        }
    }
    
    private val coverPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCoverForBook(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conn = InterHandshake(this, lifecycleScope)
        (application as App).conn = conn
        viewModel.setConnection(conn)

        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pushState.collect { pushState ->
                    if (pushState.isTransferring && !pushState.isFinished) {
                        
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        setContent {
            EbookTheme {
                MainScreen(
                    viewModel = viewModel,
                    onImportClick = {
                        // 只允许 TXT、EPUB 和 NVB 格式
                        filePickerLauncher.launch(arrayOf("text/plain", "application/epub+zip", "application/octet-stream"))
                    },
                    onImportCoverClick = {
                        
                        coverPickerLauncher.launch(arrayOf("image/*"))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reconnect()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let {
            viewModel.startImport(it)
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            (application as App).conn.destroy().await()
        }
        super.onDestroy()
    }
}
