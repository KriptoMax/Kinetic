package kz.kripto.studycompose1.database.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Этот класс — мой "сейф" для данных сессии. 
 * Он хранит ID и имя пользователя прямо в памяти телефона, 
 * чтобы приложение не забывало нас после перезагрузки.
 */
class SessionManager(context: Context) {
    // Открываю файл настроек "user_session". MODE_PRIVATE значит, что только моё приложение имеет к нему доступ.
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        // Ключи-метки, по которым я буду искать нужные значения в "сейфе"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_USERNAME = "current_username"
    }

    // Сохраняю локальный ID пользователя. Открываю редактор, кладу число под метку KEY_USER_ID и подтверждаю (apply).
    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    // Сохраняю логин пользователя. Процесс такой же: открыл редактор -> положил строку -> подтвердил.
    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    // Достаю ID пользователя. Если в "сейфе" пусто — возвращаю -1 (значит, никто не залогинен).
    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    // Достаю имя пользователя. Если его нет — возвращаю null.
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    // Полная очистка "сейфа". Стираю всё, что сохранял ранее. Обычно вызываю при выходе из аккаунта.
    fun logout() {
        prefs.edit().clear().apply()
    }
}
