package kz.kripto.studycompose1.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null, // ID из Firestore для синхронизации
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,

    // МОДИФИКАЦИЯ: Добавляем поля для связи
    val creatorId: Long,     // ID пользователя, который создал задачу
    val teamId: Long? = null, // null -> приватная задача, Long-номер -> задача команды
    val assigneeId: Long? = null // ID пользователя, назначенного на задачу
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentTaskId"])]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentTaskId: Long,
    val title: String,
    val isCompleted: Boolean = false
)

data class TaskWithSubTasks(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentTaskId"
    )
    val subTasks: List<SubTaskEntity>,

    @Relation(
        parentColumn = "assigneeId",
        entityColumn = "id"
    )
    val assignee: UserEntity? = null
)
