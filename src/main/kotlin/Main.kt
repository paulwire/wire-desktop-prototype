import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.wire.desktop.data.greeting.DefaultGreetingRepository
import com.wire.desktop.presentation.main.MainViewModel
import com.wire.desktop.ui.main.MainScreen

fun main() = application {
    val viewModel = MainViewModel(DefaultGreetingRepository())

    Window(onCloseRequest = ::exitApplication, title = "Wire Desktop Prototype") {
        MainScreen(viewModel)
    }
}
