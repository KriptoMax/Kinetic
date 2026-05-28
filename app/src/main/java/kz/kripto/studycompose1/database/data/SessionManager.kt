package kz.kripto.studycompose1.database.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        // ИСПРАВЛЕНО: Добавили пропущенный ключ для логина!
        private const val KEY_USERNAME = "current_username"
    }

    // Твой старый метод сохранения ID (оставляем без изменений)
    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    // НОВЫЙ МЕТОД: Сохранение логина/имени (вызови его в AuthScreen при успешном входе)
    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    // Теперь KEY_USERNAME подсвечивается правильно и всё работает!
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}