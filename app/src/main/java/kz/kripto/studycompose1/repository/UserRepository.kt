package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.UserDao
import kz.kripto.studycompose1.database.entities.UserEntity

class UserRepository(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun saveUserToFirestore(user: UserEntity) {
        val uid = auth.currentUser?.uid ?: return
        val userMap = hashMapOf(
            "username" to user.username,
            "email" to user.email,
            "firebaseUid" to uid
        )
        try {
            firestore.collection("users_profiles").document(uid).set(userMap).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncUserAfterLogin(): Long {
        val uid = auth.currentUser?.uid ?: return -1L
        return fetchAndSaveUserProfile(uid)
    }

    // Скачивает профиль любого пользователя по UID и сохраняет/обновляет в Room
    suspend fun fetchAndSaveUserProfile(uid: String): Long {
        var existingUser = userDao.getUserByFirebaseUid(uid)
        
        try {
            val doc = firestore.collection("users_profiles").document(uid).get().await()
            if (doc.exists()) {
                val firestoreUsername = doc.getString("username") ?: "User"
                
                // Если по UID не нашли, попробуем найти по имени пользователя (для склейки)
                if (existingUser == null) {
                    existingUser = userDao.getUserByUsername(firestoreUsername)
                }

                val user = UserEntity(
                    id = existingUser?.id ?: 0L,
                    username = firestoreUsername,
                    email = doc.getString("email") ?: "",
                    passwordHash = existingUser?.passwordHash ?: "",
                    firebaseUid = uid
                )
                
                val newId = userDao.registerUser(user)
                return if (existingUser == null) newId else existingUser.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return existingUser?.id ?: -1L
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        val cleanName = username.trim().lowercase()
        return try {
            val query = firestore.collection("users_profiles").get().await()
            query.documents.any { it.getString("username")?.trim()?.lowercase() == cleanName }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserById(id: Long): UserEntity? {
        return userDao.getUserById(id)
    }
}
