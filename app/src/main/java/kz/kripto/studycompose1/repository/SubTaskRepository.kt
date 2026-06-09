package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.entities.SubTaskEntity

// Мой репозиторий для управления подзадачами (чекбоксами внутри задач)
class SubTaskRepository(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val taskRepository: TaskRepository
) {
    // Меняю статус готовности подзадачи (галочку) и синхроню это с облаком
    suspend fun toggleSubTaskStatus(taskId: Long, subTaskId: Long, isCompleted: Boolean) {
        val taskWithSubTasks = taskDao.getTaskByIdOnce(taskId) ?: return
        val uid = auth.currentUser?.uid ?: return
        val remoteId = taskWithSubTasks.task.remoteId ?: return

        // Сначала ставлю галочку локально в телефоне
        taskDao.updateSubTaskStatus(subTaskId, isCompleted)
        
        // Достаю обновленный список подзадач и статус задачи
        val updatedData = taskDao.getTaskByIdOnce(taskId) ?: return
        
        // ЛОГИКА АВТО-ЗАВЕРШЕНИЯ: Считаем новый статус
        val allSubTasksDone = updatedData.subTasks.isNotEmpty() && updatedData.subTasks.all { it.isCompleted }
        val newStatus = if (updatedData.subTasks.isNotEmpty()) allSubTasksDone else updatedData.task.isCompleted
        
        // Если статус должен измениться — меняем его локально
        if (updatedData.task.isCompleted != newStatus) {
            taskDao.updateTask(updatedData.task.copy(isCompleted = newStatus))
        }

        // Говорю основному репозиторию игнорировать ответ от облака
        taskRepository.updatingRemoteIds.add(remoteId)

        val subTasksMapList = updatedData.subTasks.map {
            mapOf("title" to it.title, "isCompleted" to it.isCompleted)
        }

        try {
            val collection = if (updatedData.task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            
            // ОБЪЕДИНЕННЫЙ ЗАПРОС: Обновляем и подзадачи, и статус одним махом
            collection.document(remoteId)
                .update(
                    "subTasks", subTasksMapList,
                    "isCompleted", newStatus
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            taskRepository.updatingRemoteIds.remove(remoteId)
        }
    }
}
