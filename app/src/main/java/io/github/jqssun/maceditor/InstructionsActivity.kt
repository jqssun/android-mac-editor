package io.github.jqssun.maceditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.mukesh.MarkDown
import java.io.File

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        val markdownSource = File.createTempFile(R.string.app_name.toString(), "tmp")
        markdownSource.deleteOnExit()
        markdownSource.writeBytes(resources.openRawResource(R.raw.instructions).readBytes())

        findViewById<ComposeView>(R.id.instructions_md_renderer).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    MarkDown(
                        file = markdownSource,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
