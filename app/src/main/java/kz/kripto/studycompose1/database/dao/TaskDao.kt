package kz.kripto.studycompose1.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.database.entities.TaskEntity
import kz.kripto.studycompose1.database.entities.TaskWithSubTasks

@Dao
interface TaskDao {

    // --- Главные задачи ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // МОДИФИКАЦИЯ 1: Получить ТОЛЬКО личные задачи пользователя (где teamId пустой)
    @Transaction
    @Query("SELECT * FROM tasks WHERE creatorId = :userId AND teamId IS NULL ORDER BY createdAt DESC")
    fun getUserPrivateTasks(userId: Long): Flow<List<TaskWithSubTasks>>

    // МОДИФИКАЦИЯ 2: Получить задачи ОПРЕДЕЛЕННОЙ команды (доступные всем её участникам)
    @Transaction
    @Query("SELECT * FROM tasks WHERE teamId = :teamId ORDER BY createdAt DESC")
    fun getTeamTasks(teamId: Long): Flow<List<TaskWithSubTasks>>

    // Старый запрос (может пригодиться для тестов или админ-панели)
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskByIdOnce(taskId: Long): TaskWithSubTasks?

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Long): Flow<TaskWithSubTasks?>

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: String): TaskEntity?

    // --- Подзадачи (Одиночные операции) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity)

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subTaskId")
    suspend fun updateSubTaskStatus(subTaskId: Long, isCompleted: Boolean)

    // --- Подзадачи (Массовые операции для экрана редактирования) ---
    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteSubTasksByTaskId(parentTaskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    // Транзакция обновления для безопасной перезаписи
    @Transaction
    suspend fun updateTaskWithSubTasks(task: TaskEntity, subTasks: List<SubTaskEntity>) {
        updateTask(task)
        deleteSubTasksByTaskId(task.id)
        insertSubTasks(subTasks)
    }

    @Transaction
    suspend fun syncTaskAndSubTasks(task: TaskEntity, subTasks: List<SubTaskEntity>) {
        val parentId = insertTask(task)
        deleteSubTasksByTaskId(parentId)
        insertSubTasks(subTasks.map { it.copy(parentTaskId = parentId) })
    }
}