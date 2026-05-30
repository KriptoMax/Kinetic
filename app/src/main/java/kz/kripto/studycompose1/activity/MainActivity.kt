package kz.kripto.studycompose1.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kz.kripto.studycompose1.notifications.DeadlineWorker
import kz.kripto.studycompose1.ui.theme.StudyCompose1Theme
import kz.kripto.studycompose1.navigation.AppNavigation

// Моё главное Activity — входная точка в приложение
class MainActivity : ComponentActivity() {

    // Лаунчер для запроса разрешения на уведомления (нужно для новых Android)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Тут можно обработать ответ пользователя
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Растягиваю приложение на весь экран (под статус-бар и навигацию)
        enableEdgeToEdge()
        
        // Проверяю разрешения, получаю токен для пушей и запускаю тест дедлайнов
        checkNotificationPermission()
        fetchFcmToken()
        testDeadlineNotification()

        setContent {
            StudyCompose1Theme {
                // Запускаю основную навигацию приложения
                AppNavigation()
            }
        }
    }

    // Проверяю, разрешил ли пользователь слать ему пуши
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Если нет — запрашиваю
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Получаю уникальный токен устройства для тестов Firebase Messaging
    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TEST", "Не удалось получить токен", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM_TEST", "TOKEN_START: $token :TOKEN_END")
            println("FCM_TOKEN_PRINT: $token")
        }
    }

    // Принудительно запускаю проверку дедлайнов при старте (для отладки уведомлений)
    private fun testDeadlineNotification() {
        val request = OneTimeWorkRequestBuilder<DeadlineWorker>().build()
        WorkManager.getInstance(this).enqueue(request)
    }
}

// Превью для удобной верстки в студии
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
