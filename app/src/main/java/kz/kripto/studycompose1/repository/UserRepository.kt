package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.UserDao
import kz.kripto.studycompose1.database.entities.UserEntity

// Мой репозиторий для управления профилем пользователя
class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Сохраняю данные моего профиля в облако Firestore
    suspend fun saveUserToFirestore(user: UserEntity) {
        val uid = auth.currentUser?.uid ?: return
        val userMap = hashMapOf(
            "localId" to user.id,
            "username" to user.username,
            "email" to user.email,
            "passwordHash" to user.passwordHash,
            "firebaseUid" to uid
        )
        try {
            firestore.collection("users_profiles").document(uid).set(userMap).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Когда захожу с нового телефона, этот метод скачивает мой профиль из облака в телефон
    suspend fun syncUserAfterLogin(): Long {
        val uid = auth.currentUser?.uid ?: return -1L
        try {
            val doc = firestore.collection("users_profiles").document(uid).get().await()
            if (doc.exists()) {
                val username = doc.getString("username") ?: ""
                val email = doc.getString("email") ?: ""
                val passwordHash = doc.getString("passwordHash") ?: ""
                val localIdFromCloud = (doc.get("localId") as? Number)?.toLong() ?: 0L

                val existingUser = userDao.getUserByFirebaseUid(uid)
                return if (existingUser == null) {
                    // Если в телефоне меня еще нет, регистрирую локально с данными из облака
                    userDao.registerUser(
                        UserEntity(
                            id = localIdFromCloud,
                            username = username,
                            email = email,
                            passwordHash = passwordHash,
                            firebaseUid = uid
                        )
                    )
                } else {
                    existingUser.id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1L
    }

    // Проверяю, не занят ли кем-то другим мой будущий логин в Firestore
    suspend fun isUsernameTaken(username: String): Boolean {
        val cleanName = username.trim().lowercase()
        return try {
            val query = firestore.collection("users_profiles")
                .get()
                .await()
            
            // Перебираю все профили и ищу совпадение (для маленькой базы сойдет)
            query.documents.any { it.getString("username")?.trim()?.lowercase() == cleanName }
        } catch (e: Exception) {
            false
        }
    }
}
