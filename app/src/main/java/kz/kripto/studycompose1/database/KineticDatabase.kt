package kz.kripto.studycompose1.database

import androidx.room.Database
import androidx.room.RoomDatabase
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.dao.UserDao
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.database.entities.TaskEntity
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.database.entities.TeamMemberEntity
import kz.kripto.studycompose1.database.entities.UserEntity

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        UserEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class
    ],
    version = 11, // Добавили уникальные индексы для защиты от дубликатов
    exportSchema = false
)
abstract class KineticDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun teamDao(): TeamDao
}