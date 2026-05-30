package kz.kripto.studycompose1.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.activity.MainActivity

// Мой сервис для приема Push-уведомлений от Firebase
class KineticMessagingService : FirebaseMessagingService() {

    // Срабатывает, когда прилетает уведомление из облака
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            // Показываю уведомление в шторке
            sendNotification(it.title ?: "Задача", it.body ?: "У вас есть важное уведомление")
        }
    }

    // Срабатывает, когда генерируется новый уникальный токен устройства
    override fun onNewToken(token: String) {
        // Тут в будущем буду отправлять токен на сервер, чтобы слать пуши конкретно мне
    }

    // Внутренний метод для сборки и показа уведомления
    private fun sendNotification(title: String, messageBody: String) {
        // Настраиваю, что произойдет при нажатии на пуш (откроется MainActivity)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)

        val channelId = "kinetic_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создаю канал для уведомлений (для свежих версий Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Дедлайны и задачи", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Вывожу уведомление в систему
        notificationManager.notify(0, notificationBuilder.build())
    }
}
