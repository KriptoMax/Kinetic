package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.entities.SubTaskEntity

/**
 * Репозиторий для управления подзадачами (чекбоксами внутри задач).
 * Здесь лежит логика: что делать, когда нажали на галочку подзадачи.
 */
class SubTaskRepository(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val taskRepository: TaskRepository
) {
    /**
     * Меняю статус готовности подзадачи (галочку) и синхроню это с облаком.
     * Тут же срабатывает "авто-завершение": если все подзадачи выполнены, главная задача тоже закроется.
     */
    suspend fun toggleSubTaskStatus(taskId: Long, subTaskId: Long, isCompleted: Boolean) {
        val taskWithSubTasks = taskDao.getTaskByIdOnce(taskId) ?: return
        val uid = auth.currentUser?.uid ?: return
        val remoteId = taskWithSubTasks.task.remoteId ?: return

        // 1. Сначала ставим галочку локально в телефоне для мгновенного отклика
        taskDao.updateSubTaskStatus(subTaskId, isCompleted)
        
        // Достаем свежие данные из базы после клика
        val updatedData = taskDao.getTaskByIdOnce(taskId) ?: return
        
        // 2. ЛОГИКА АВТО-ЗАВЕРШЕНИЯ:
        // Считаем: если все подзадачи теперь с галочками — значит и вся задача выполнена.
        val allSubTasksDone = updatedData.subTasks.isNotEmpty() && updatedData.subTasks.all { it.isCompleted }
        val newStatus = if (updatedData.subTasks.isNotEmpty()) allSubTasksDone else updatedData.task.isCompleted
        
        // Если статус главной задачи поменялся (например, закрылась последняя подзадача) — обновляем базу
        if (updatedData.task.isCompleted != newStatus) {
            taskDao.updateTask(updatedData.task.copy(isCompleted = newStatus))
        }

        // Блокируем "эхо" от облака, чтобы UI не прыгал
        taskRepository.updatingRemoteIds.add(remoteId)

        // Готовим список подзадач для отправки в Firebase
        val subTasksMapList = updatedData.subTasks.map {
            mapOf("title" to it.title, "isCompleted" to it.isCompleted)
        }

        try {
            // Определяем, куда слать данные (личная папка или командная)
            val collection = if (updatedData.task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            
            // 3. ОТПРАВКА В ОБЛАКО: Обновляем и список подзадач, и общий статус задачи за один запрос
            collection.document(remoteId)
                .update(
                    "subTasks", subTasksMapList,
                    "isCompleted", newStatus
                )
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            // Если сеть подвела — снимаем блокировку ID
            taskRepository.updatingRemoteIds.remove(remoteId)
        }
    }
}
