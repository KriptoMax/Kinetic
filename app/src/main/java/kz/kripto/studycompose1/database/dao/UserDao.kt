package kz.kripto.studycompose1.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kz.kripto.studycompose1.database.entities.UserEntity

@Dao
interface UserDao {
    // Вставить пользователя в базу, если такой уже есть — заменить его новыми данными
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registerUser(user: UserEntity): Long

    // Достать всё из таблицы пользователей, где имя совпадает с присланным (только одну запись)
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    // Достать всё из таблицы пользователей, где почта совпадает с присланной (только одну запись)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Достать всё из таблицы пользователей, где глобальный ID Firebase совпадает с присланным
    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1")
    suspend fun getUserByFirebaseUid(uid: String): UserEntity?

    // Достать всё из таблицы пользователей по его внутреннему порядковому номеру (ID)
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?
}