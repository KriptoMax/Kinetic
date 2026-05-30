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
        
        // Говорю основному репозиторию игнорировать ответ от облака по этой задаче
        taskRepository.updatingRemoteIds.add(remoteId)

        // Достаю обновленный список подзадач для отправки в Firebase
        val updatedData = taskDao.getTaskByIdOnce(taskId) ?: return
        val subTasksMapList = updatedData.subTasks.map {
            mapOf(
                "title" to it.title,
                "isCompleted" to it.isCompleted
            )
        }

        try {
            // Отправляю обновленный массив подзадач в нужную коллекцию (личную или командную)
            val collection = if (taskWithSubTasks.task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            
            collection.document(remoteId)
                .update("subTasks", subTasksMapList)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            taskRepository.updatingRemoteIds.remove(remoteId)
        }
    }
}
