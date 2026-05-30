package kz.kripto.studycompose1

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kz.kripto.studycompose1.di.appModule
import kz.kripto.studycompose1.notifications.DeadlineWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

// Мой основной класс приложения
class KineticApp : Application(){
    override fun onCreate(){
        super.onCreate()
        
        // Настраиваю Koin (Внедрение зависимостей)
        startKoin{
            // Передаю контекст приложения
            androidContext(this@KineticApp)
            // Загружаю свои модули
            modules(appModule)
        }

        // Запускаю фоновую проверку сроков выполнения задач
        setupDeadlineWorker()
    }

    // Метод для настройки периодической задачи (раз в 4 часа)
    private fun setupDeadlineWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DeadlineWorker>(4, TimeUnit.HOURS)
            .build()
        
        // Ставлю задачу в очередь WorkManager
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "deadline_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
