package kz.kripto.studycompose1.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kz.kripto.studycompose1.ui.theme.StudyCompose1Theme

//import kz.kripto.studycompose1.components.Greeting
import kz.kripto.studycompose1.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyCompose1Theme {
                AppNavigation()
            }
        }
    }
}


@Preview(showBackground = true,
    device = "spec:width=412dp,height=915dp,dpi=450",
    showSystemUi = true
)
@Composable
fun MainPreview() {
    StudyCompose1Theme {
        AppNavigation()
    }
}