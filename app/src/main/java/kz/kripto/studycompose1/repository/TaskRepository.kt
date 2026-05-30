package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.database.entities.TaskEntity
import java.util.Collections

// Мой репозиторий для управления задачами: связывает Room и Firestore
class TaskRepository(
    private val taskDao: TaskDao,
    private val teamDao: TeamDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeListeners = mutableListOf<ListenerRegistration>()
    
    // Храню список ID задач, которые сейчас обновляются, чтобы не ловить "эхо" от облака
    val updatingRemoteIds = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Слежу за входом/выходом из аккаунта
        auth.addAuthStateListener {
            startRealtimeSync()
        }
    }

    private val currentUid: String?
        get() = auth.currentUser?.uid

    // Запускаю прослушивание изменений в Firebase в реальном времени
    fun startRealtimeSync() {
        val uid = currentUid
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        
        if (uid == null) return

        // Подписываюсь на личные задачи
        val personalListener = firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshots, e -> handleTaskSnapshots(snapshots, e) }
        activeListeners.add(personalListener)

        // Подписываюсь на командные задачи
        val teamTasksListener = firestore.collection("teams_tasks")
            .addSnapshotListener { snapshots, e -> handleTaskSnapshots(snapshots, e) }
        activeListeners.add(teamTasksListener)
    }

    private fun handleTaskSnapshots(snapshots: com.google.firebase.firestore.QuerySnapshot?, e: com.google.firebase.firestore.FirebaseFirestoreException?) {
        if (e != null) return

        snapshots?.documentChanges?.forEach { change ->
            val doc = change.document
            val remoteId = doc.id
            val data = doc.data

            if (updatingRemoteIds.contains(remoteId)) {
                if (doc.metadata.hasPendingWrites().not()) {
                    updatingRemoteIds.remove(remoteId)
                }
                return@forEach
            }

            repositoryScope.launch {
                when (change.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        syncTaskWithSubTasksToLocal(remoteId, data)
                    }
                    DocumentChange.Type.REMOVED -> {
                        taskDao.getTaskByRemoteId(remoteId)?.let { taskDao.deleteTask(it) }
                    }
                }
            }
        }
    }

    // Метод для записи данных из облака в мою локальную базу
    private suspend fun syncTaskWithSubTasksToLocal(remoteId: String, data: Map<String, Any>) {
        val existingTask = taskDao.getTaskByRemoteId(remoteId)
        
        // Связываем с командой по коду, так как локальные ID могут не совпадать
        val teamInviteCode = data["teamInviteCode"] as? String
        val localTeamId = teamInviteCode?.let { teamDao.getTeamByCode(it)?.id }

        val task = TaskEntity(
            id = existingTask?.id ?: 0,
            remoteId = remoteId,
            title = data["title"] as? String ?: "",
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            deadline = (data["deadline"] as? Number)?.toLong(),
            creatorId = (data["creatorId"] as? Number)?.toLong() ?: 1L,
            creatorUid = data["creatorUid"] as? String, // Храню глобальный UID создателя
            teamId = localTeamId, 
            assigneeId = (data["assigneeId"] as? Number)?.toLong()
        )
        
        val subTasksRaw = data["subTasks"] as? List<*> ?: emptyList<Any>()
        val subTaskEntities = subTasksRaw.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> SubTaskEntity(parentTaskId = 0, title = item["title"] as? String ?: "", isCompleted = item["isCompleted"] as? Boolean ?: false)
                is String -> SubTaskEntity(parentTaskId = 0, title = item, isCompleted = false)
                else -> null
            }
        }

        val parentId = taskDao.insertTask(task)
        taskDao.deleteSubTasksByTaskId(parentId)
        taskDao.insertSubTasks(subTaskEntities.map { it.copy(parentTaskId = parentId) })
    }

    // Сохраняю задачу: сначала в телефон, потом в Firebase
    suspend fun saveTask(task: TaskEntity, subTaskTitles: List<String>, editingTaskId: Long? = null) {
        val uid = currentUid
        val remoteId = task.remoteId ?: java.util.UUID.randomUUID().toString()
        val taskToSave = task.copy(remoteId = remoteId, creatorUid = uid)

        if (editingTaskId != null && editingTaskId != 0L) {
            val updatedSubTasks = subTaskTitles.map { SubTaskEntity(parentTaskId = editingTaskId, title = it, isCompleted = false) }
            taskDao.updateTaskWithSubTasks(taskToSave, updatedSubTasks)
            if (uid != null) {
                updatingRemoteIds.add(remoteId)
                syncTaskWithSubTasksToFirestore(taskToSave, updatedSubTasks, remoteId)
            }
        } else {
            val parentId = taskDao.insertTask(taskToSave)
            val newSubTasks = subTaskTitles.map { SubTaskEntity(parentTaskId = parentId, title = it, isCompleted = false) }
            taskDao.insertSubTasks(newSubTasks)
            if (uid != null) {
                updatingRemoteIds.add(remoteId)
                syncTaskWithSubTasksToFirestore(taskToSave.copy(id = parentId), newSubTasks, remoteId)
            }
        }
    }

    // Отправляю данные в нужную коллекцию Firebase
    private suspend fun syncTaskWithSubTasksToFirestore(task: TaskEntity, subTasks: List<SubTaskEntity>, docId: String) {
        val uid = currentUid ?: return
        val teamInviteCode = task.teamId?.let { teamDao.getTeamByIdOnce(it)?.inviteCode }

        val taskMap = hashMapOf(
            "title" to task.title,
            "isCompleted" to task.isCompleted,
            "createdAt" to task.createdAt,
            "deadline" to task.deadline,
            "creatorId" to task.creatorId,
            "creatorUid" to (task.creatorUid ?: uid),
            "teamId" to task.teamId,
            "teamInviteCode" to teamInviteCode,
            "assigneeId" to task.assigneeId,
            "subTasks" to subTasks.map { mapOf("title" to it.title, "isCompleted" to it.isCompleted) }
        )

        try {
            val collection = if (task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            collection.document(docId).set(taskMap).await()
        } catch (e: Exception) { 
            updatingRemoteIds.remove(docId)
        }
    }

    // Удаляю задачу отовсюду
    suspend fun deleteTask(task: TaskEntity) {
        val remoteId = task.remoteId
        if (remoteId != null) updatingRemoteIds.add(remoteId)
        
        taskDao.deleteTask(task)
        val uid = currentUid ?: return
        val docId = remoteId ?: return
        try {
            val collection = if (task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            collection.document(docId).delete().await()
        } catch (e: Exception) { 
            updatingRemoteIds.remove(docId)
        }
    }

    // Меняю статус готовности задачи (чекбокс)
    suspend fun toggleTaskStatus(task: TaskEntity) {
        val remoteId = task.remoteId
        if (remoteId != null) updatingRemoteIds.add(remoteId)

        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        taskDao.updateTask(updatedTask)
        val uid = currentUid ?: return
        val docId = remoteId ?: return
        try {
            val collection = if (task.teamId != null) firestore.collection("teams_tasks") 
                             else firestore.collection("users").document(uid).collection("tasks")
            collection.document(docId).update("isCompleted", updatedTask.isCompleted).await()
        } catch (e: Exception) { 
            updatingRemoteIds.remove(docId)
        }
    }
}
