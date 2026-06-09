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
    // Добавить задачу в базу, если ID совпадает — перезаписать
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    // Обновить данные уже существующей задачи
    @Update
    suspend fun updateTask(task: TaskEntity)

    // Полностью удалить задачу из локальной базы
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // Выбрать всё из задач, где я создатель и нет привязки к команде, сортировать от новых к старым
    @Transaction
    @Query("SELECT * FROM tasks WHERE creatorId = :userId AND teamId IS NULL ORDER BY createdAt DESC")
    fun getUserPrivateTasks(userId: Long): Flow<List<TaskWithSubTasks>>

    // Выбрать все задачи, которые относятся к конкретной команде по её ID
    @Transaction
    @Query("SELECT * FROM tasks WHERE teamId = :teamId ORDER BY createdAt DESC")
    fun getTeamTasks(teamId: Long): Flow<List<TaskWithSubTasks>>

    // Выбрать абсолютно все задачи из базы с их подзадачами
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    // Найти одну конкретную задачу по её ID (разовый запрос)
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskByIdOnce(taskId: Long): TaskWithSubTasks?

    // Следить за изменениями конкретной задачи по её ID в реальном времени
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Long): Flow<TaskWithSubTasks?>

    // Найти задачу по её уникальному идентификатору из облака (Firestore)
    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: String): TaskEntity?

    // --- Подзадачи (Одиночные операции) ---
    // Добавить новую подзадачу
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity)

    // Обновить название или статус подзадачи
    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    // Удалить одну подзадачу
    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    // Быстро обновить только статус "выполнено" у конкретной подзадачи по её ID
    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subTaskId")
    suspend fun updateSubTaskStatus(subTaskId: Long, isCompleted: Boolean)

    // --- Подзадачи (Массовые операции для экрана редактирования) ---
    // Стереть все подзадачи, которые принадлежат одной главной задаче
    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteSubTasksByTaskId(parentTaskId: Long)

    // Вставить сразу целый список подзадач
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    // Сначала обновить задачу, потом удалить старые подзадачи и вставить новые (всё за один раз)
    @Transaction
    suspend fun updateTaskWithSubTasks(task: TaskEntity, subTasks: List<SubTaskEntity>) {
        updateTask(task)
        deleteSubTasksByTaskId(task.id)
        insertSubTasks(subTasks)
    }

    // Сохранить задачу и её подзадачи из облака, обеспечив атомарность (без мерцания)
    @Transaction
    suspend fun syncTaskAndSubTasks(task: TaskEntity, subTasks: List<SubTaskEntity>) {
        val parentId = insertTask(task)
        deleteSubTasksByTaskId(parentId)
        insertSubTasks(subTasks.map { it.copy(parentTaskId = parentId) })
    }
}