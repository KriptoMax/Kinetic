package kz.kripto.studycompose1.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.database.dao.TaskDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// Мой фоновый помощник для проверки дедлайнов
class DeadlineWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val taskDao: TaskDao by inject()

    // Главный метод, который запускается системой в фоне
    override suspend fun doWork(): Result {
        // Достаю все задачи из базы
        val allTasks = taskDao.getAllTasksWithSubTasks().first()
        val now = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L

        allTasks.forEach { taskWithSubTasks ->
            val deadline = taskWithSubTasks.task.deadline
            // Проверяю только незавершенные задачи с установленным сроком
            if ((deadline != null) && !taskWithSubTasks.task.isCompleted) {
                val timeUntilDeadline = deadline - now
                
                // Если сдача завтра (осталось меньше 24 часов), шлю уведомление
                if (timeUntilDeadline in 0..oneDayInMillis) {
                    showNotification(taskWithSubTasks.task.title)
                }
            }
        }

        return Result.success()
    }

    // Метод для создания и показа самого пуш-уведомления на экране
    private fun showNotification(taskTitle: String) {
        val channelId = "deadline_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Настраиваю канал уведомлений (нужно для Android 8+)
        val channel = NotificationChannel(channelId, "Напоминания о дедлайнах", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // Собираю текст и иконку уведомления
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Скоро дедлайн!")
            .setContentText("Задача \"$taskTitle\" должна быть выполнена завтра.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Показываю уведомление пользователю
        notificationManager.notify(taskTitle.hashCode(), notification)
    }
}
