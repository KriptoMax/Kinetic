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

/**
 * Мой репозиторий для управления задачами. 
 * Это "мост" между локальной базой Room и облаком Firestore.
 * Он следит, чтобы задачи на телефоне и в сети были одинаковыми.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val teamDao: TeamDao,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    // Область для запуска фоновых задач (чтобы не тормозить экран)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Список активных "ушей" (слушателей), которые следят за изменениями в Firebase
    private val activeListeners = mutableListOf<ListenerRegistration>()
    
    // Сет для блокировки "эха": когда мы сами меняем задачу, облако шлет нам это же изменение обратно.
    // Мы записываем ID сюда, чтобы игнорировать такие повторы.
    val updatingRemoteIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Как только статус авторизации меняется (вошли/вышли), перенастраиваем синхронизацию
        auth.addAuthStateListener {
            startRealtimeSync()
        }
    }

    // Глобальный ID текущего пользователя из Firebase
    private val currentUid: String?
        get() = auth.currentUser?.uid

    /**
     * Запускаю слежку за задачами в реальном времени.
     * Слушаю и личные задачи, и задачи команд.
     */
    fun startRealtimeSync() {
        val uid = currentUid
        activeListeners.forEach { it.remove() } // Убираю старые слушатели
        activeListeners.clear()
        
        if (uid == null) return

        // Подписываюсь на личные задачи пользователя
        val personalListener = firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshots, e -> handleTaskSnapshots(snapshots, e) }
        activeListeners.add(personalListener)

        // Подписываюсь на все задачи всех команд
        val teamTasksListener = firestore.collection("teams_tasks")
            .addSnapshotListener { snapshots, e -> handleTaskSnapshots(snapshots, e) }
        activeListeners.add(teamTasksListener)
    }

    /**
     * Обрабатываю "пакеты" изменений из облака.
     * Если задача добавлена, изменена или удалена в Firestore — повторяю это в Room.
     */
    private fun handleTaskSnapshots(snapshots: com.google.firebase.firestore.QuerySnapshot?, e: com.google.firebase.firestore.FirebaseFirestoreException?) {
        if (e != null) return

        snapshots?.documentChanges?.forEach { change ->
            val doc = change.document
            val remoteId = doc.id
            val data = doc.data

            // Проверка: если это мы сами только что отправили это изменение, пропускаем его
            if (updatingRemoteIds.contains(remoteId)) {
                if (doc.metadata.hasPendingWrites().not()) {
                    updatingRemoteIds.remove(remoteId)
                }
                return@forEach
            }

            repositoryScope.launch {
                when (change.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                        // Если задача появилась или обновилась в сети — качаем её в телефон
                        syncTaskWithSubTasksToLocal(remoteId, data)
                    }
                    DocumentChange.Type.REMOVED -> {
                        // Если кто-то удалил задачу в облаке — стираем её и у себя
                        taskDao.getTaskByRemoteId(remoteId)?.let { taskDao.deleteTask(it) }
                    }
                }
            }
        }
    }

    /**
     * Переливаю данные из Map (формат Firestore) в TaskEntity (формат Room).
     * Здесь же подтягиваю локальные ID для команд и исполнителей.
     */
    private suspend fun syncTaskWithSubTasksToLocal(remoteId: String, data: Map<String, Any>) {
        val existingTask = taskDao.getTaskByRemoteId(remoteId)
        
        // Ищем локальный ID команды по её коду приглашения
        val teamInviteCode = data["teamInviteCode"] as? String
        val localTeamId = teamInviteCode?.let { teamDao.getTeamByCode(it)?.id }

        // Пытаемся найти исполнителя по его UID (если не знаем — скачиваем профиль)
        val assigneeUid = data["assigneeUid"] as? String
        val localAssigneeId = assigneeUid?.let { userRepository.fetchAndSaveUserProfile(it) }

        val task = TaskEntity(
            id = existingTask?.id ?: 0,
            remoteId = remoteId,
            title = (data["title"] as? String) ?: "",
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            deadline = (data["deadline"] as? Number)?.toLong(),
            creatorId = (data["creatorId"] as? Number)?.toLong() ?: 1L,
            creatorUid = data["creatorUid"] as? String,
            teamId = localTeamId, 
            assigneeId = if (localAssigneeId != -1L) localAssigneeId else null,
            assigneeUid = assigneeUid
        )
        
        // Превращаем список подзадач из облака в объекты для базы
        val subTasksRaw = data["subTasks"] as? List<*> ?: emptyList<Any>()
        val subTaskEntities = subTasksRaw.mapNotNull { item ->
            when (item) {
                is Map<*, *> -> SubTaskEntity(parentTaskId = 0, title = item["title"] as? String ?: "", isCompleted = item["isCompleted"] as? Boolean ?: false)
                is String -> SubTaskEntity(parentTaskId = 0, title = item, isCompleted = false)
                else -> null
            }
        }

        // Записываем всё в базу одним махом (через транзакцию), чтобы не моргал экран
        taskDao.syncTaskAndSubTasks(task, subTaskEntities)
    }

    /**
     * Сохраняю задачу: сначала записываю в телефон (Room), 
     * а потом отправляю "копию" в облако (Firestore).
     */
    suspend fun saveTask(task: TaskEntity, subTaskTitles: List<String>, editingTaskId: Long? = null) {
        val uid = currentUid
        val remoteId = task.remoteId ?: java.util.UUID.randomUUID().toString()
        
        // Выясняем UID исполнителя, чтобы в облаке было понятно, кто назначен
        val assigneeUid = if (task.assigneeId != null) {
            userRepository.getUserById(task.assigneeId)?.firebaseUid
        } else {
            task.assigneeUid
        }

        val taskToSave = task.copy(
            remoteId = remoteId, 
            creatorUid = uid,
            assigneeUid = assigneeUid
        )

        if (editingTaskId != null && editingTaskId != 0L) {
            // Обновляем существующую
            val updatedSubTasks = subTaskTitles.map { SubTaskEntity(parentTaskId = editingTaskId, title = it, isCompleted = false) }
            taskDao.updateTaskWithSubTasks(taskToSave, updatedSubTasks)
            if (uid != null) {
                updatingRemoteIds.add(remoteId)
                syncTaskWithSubTasksToFirestore(taskToSave, updatedSubTasks, remoteId)
            }
        } else {
            // Создаем абсолютно новую
            val parentId = taskDao.insertTask(taskToSave)
            val newSubTasks = subTaskTitles.map { SubTaskEntity(parentTaskId = parentId, title = it, isCompleted = false) }
            taskDao.insertSubTasks(newSubTasks)
            if (uid != null) {
                updatingRemoteIds.add(remoteId)
                syncTaskWithSubTasksToFirestore(taskToSave.copy(id = parentId), newSubTasks, remoteId)
            }
        }
    }

    /**
     * Отправляю данные задачи и подзадач в Firebase.
     * Если задача личная — летит в папку пользователя, если командная — в общую папку команд.
     */
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
            "assigneeUid" to task.assigneeUid,
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

    /**
     * Полное удаление задачи: стираю и из телефона, и из облака.
     */
    suspend fun deleteTask(task: TaskEntity) {
        val remoteId = task.remoteId
        remoteId?.let { updatingRemoteIds.add(it) }
        
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

    /**
     * Переключаю статус "Выполнено" у всей задачи.
     */
    suspend fun toggleTaskStatus(task: TaskEntity) {
        val remoteId = task.remoteId
        remoteId?.let { updatingRemoteIds.add(it) }

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
