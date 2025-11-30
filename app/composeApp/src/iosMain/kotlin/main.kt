import androidx.compose.ui.window.ComposeUIViewController
import org.company.app.ui.entrypoint.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
