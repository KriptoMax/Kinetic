package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.database.entities.TaskEntity

import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration

class TaskRepository(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var taskListener: ListenerRegistration? = null

    init {
        // Автоматически запускаем синхронизацию при смене пользователя
        auth.addAuthStateListener {
            startRealtimeSync()
        }
    }

    private val currentUid: String?
        get() = auth.currentUser?.uid

    /**
     * Запускает прослушивание изменений в Firestore.
     * Все изменения в облаке будут автоматически записаны в Room.
     */
    fun startRealtimeSync() {
        val uid = currentUid
        
        // Обязательно останавливаем старый листенер
        taskListener?.remove()

        if (uid == null) return

        taskListener = firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val doc = change.document
                    val remoteId = doc.id
                    val data = doc.data

                    repositoryScope.launch {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val existingTask = taskDao.getTaskByRemoteId(remoteId)
                                
                                val task = TaskEntity(
                                    id = existingTask?.id ?: 0,
                                    remoteId = remoteId,
                                    title = data["title"] as? String ?: "",
                                    isCompleted = data["isCompleted"] as? Boolean ?: false,
                                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    deadline = (data["deadline"] as? Number)?.toLong(),
                                    creatorId = (data["creatorId"] as? Number)?.toLong() ?: 0L,
                                    teamId = (data["teamId"] as? Number)?.toLong(),
                                    assigneeId = (data["assigneeId"] as? Number)?.toLong()
                                )
                                val parentId = taskDao.insertTask(task)

                                // Синхронизируем подзадачи
                                val subTasks = data["subTasks"] as? List<String> ?: emptyList()
                                if (subTasks.isNotEmpty()) {
                                    taskDao.deleteSubTasksByTaskId(parentId)
                                    val subTaskEntities = subTasks.map { title ->
                                        SubTaskEntity(parentTaskId = parentId, title = title, isCompleted = false)
                                    }
                                    taskDao.insertSubTasks(subTaskEntities)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Логика удаления по remoteId
                            }
                        }
                    }
                }
            }
    }

    /**
     * Сохраняет задачу сначала локально, затем пытается отправить в Firebase.
     */
    suspend fun saveTask(
        task: TaskEntity,
        subTaskTitles: List<String>,
        editingTaskId: Long? = null
    ) {
        val uid = currentUid
        
        // Генерируем или сохраняем remoteId, чтобы избежать дублей при обратной синхронизации
        val remoteId = task.remoteId ?: java.util.UUID.randomUUID().toString()
        val taskToSave = task.copy(remoteId = remoteId)

        if (editingTaskId != null && editingTaskId != 0L) {
            // Режим редактирования
            taskDao.updateTask(taskToSave)
            taskDao.deleteSubTasksByTaskId(editingTaskId)
            val updatedSubTasks = subTaskTitles.map { subTitle ->
                SubTaskEntity(parentTaskId = editingTaskId, title = subTitle, isCompleted = false)
            }
            taskDao.insertSubTasks(updatedSubTasks)

            // Синхронизация с Firestore
            if (uid != null) {
                syncTaskToFirestore(taskToSave, subTaskTitles, remoteId)
            }
        } else {
            // Режим создания
            val parentId = taskDao.insertTask(taskToSave)
            val finalTask = taskToSave.copy(id = parentId)
            
            val newSubTasks = subTaskTitles.map { subTitle ->
                SubTaskEntity(parentTaskId = parentId, title = subTitle, isCompleted = false)
            }
            taskDao.insertSubTasks(newSubTasks)

            // Синхронизация с Firestore
            if (uid != null) {
                syncTaskToFirestore(finalTask, subTaskTitles, remoteId)
            }
        }
    }

    private suspend fun syncTaskToFirestore(task: TaskEntity, subTasks: List<String>, docId: String) {
        val uid = currentUid ?: return
        val taskMap = hashMapOf(
            "title" to task.title,
            "isCompleted" to task.isCompleted,
            "createdAt" to task.createdAt,
            "deadline" to task.deadline,
            "creatorId" to task.creatorId, // Сохраняем локальный ID создателя
            "teamId" to task.teamId,
            "assigneeId" to task.assigneeId,
            "subTasks" to subTasks
        )

        try {
            firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document(docId)
                .set(taskMap)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        val uid = currentUid ?: return
        val docId = task.remoteId ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document(docId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleTaskStatus(task: TaskEntity) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        taskDao.updateTask(updatedTask)
        
        val uid = currentUid ?: return
        val docId = task.remoteId ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("tasks")
                .document(docId)
                .update("isCompleted", updatedTask.isCompleted)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
