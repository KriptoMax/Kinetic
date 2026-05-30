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

// Внутри аннотации @Database добавь новые классы и увеличь версию (version), так как структура изменилась!
@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        UserEntity::class,       // Добавили
        TeamEntity::class,       // Добавили
        TeamMemberEntity::class  // Добавили
    ],
    version = 7, // Увеличили версию, так как добавили поле creatorUid в Team и Task
    exportSchema = false
)
abstract class KineticDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao // Добавили
    abstract fun teamDao(): TeamDao // Добавили
}