package kz.kripto.studycompose1.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.entities.TaskEntity
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.entities.TaskWithSubTasks
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.UserEntity
import kz.kripto.studycompose1.repository.SubTaskRepository
import kz.kripto.studycompose1.repository.TaskRepository

// Моя ViewModel для работы со списком задач
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val subTaskRepository: SubTaskRepository,
    private val taskDao: TaskDao,
    private val teamDao: TeamDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    init {
        // Сразу при создании запускаю синхронизацию с Firebase
        repository.startRealtimeSync()
    }

    // Достаю ID текущего пользователя из сессии
    val currentUserId: Long
        get() = sessionManager.getUserId()

    // Храню ID выбранной команды (если null — значит смотрю личные задачи)
    val selectedTeamId = MutableStateFlow<Long?>(null)

    // Роль текущего пользователя в выбранной команде
    val currentUserRole = selectedTeamId.flatMapLatest { teamId ->
        if (teamId == null) {
            MutableStateFlow<String?>(null)
        } else {
            teamDao.getTeamMembersWithRoles(teamId).map { members ->
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                members.find { it.firebaseUid == myUid }?.role
            }
        }
    }
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Список задач для экрана команды
    private val _teamTasksState = MutableStateFlow<List<TaskWithProgress>>(emptyList())
    val teamTasksState: StateFlow<List<TaskWithProgress>> = _teamTasksState.asStateFlow()

    // Загружаю задачи конкретной команды и считаю прогресс по подзадачам
    fun loadTeamTasks(teamId: Long) {
        viewModelScope.launch {
            taskDao.getTeamTasks(teamId).map { list ->
                list.map { taskWithSubTasks ->
                    val total = taskWithSubTasks.subTasks.size
                    val completed = taskWithSubTasks.subTasks.count { it.isCompleted }
                    val progress = if (total == 0) {
                        if (taskWithSubTasks.task.isCompleted) 100 else 0
                    } else {
                        ((completed.toFloat() / total.toFloat()) * 100).toInt()
                    }
                    TaskWithProgress(data = taskWithSubTasks, progress = progress)
                }
            }.collect { tasksWithProgress ->
                _teamTasksState.value = tasksWithProgress
            }
        }
    }

    // Сохраняю новую задачу или обновляю существующую
    fun saveTask(
        title: String,
        subTaskTitles: List<String>,
        deadline: Long?,
        editingTaskId: Long? = null,
        teamId: Long? = null, 
        assigneeId: Long? = null
    ) {
        viewModelScope.launch {
            val finalTeamId = teamId ?: selectedTeamId.value
            
            val task = if (editingTaskId != null && editingTaskId != 0L) {
                // Если редактирую, подтягиваю remoteId из базы
                val existing = taskDao.getTaskByIdOnce(editingTaskId)
                
                // ПРОВЕРКА: Редактировать может только создатель или админы команды
                if (existing != null && !canManageTask(existing.task)) {
                    return@launch
                }

                TaskEntity(
                    id = editingTaskId,
                    remoteId = existing?.task?.remoteId,
                    title = title,
                    deadline = deadline,
                    creatorId = currentUserId,
                    creatorUid = existing?.task?.creatorUid,
                    teamId = finalTeamId,
                    assigneeId = assigneeId
                )
            } else {
                // Создаю новую структуру задачи
                TaskEntity(
                    title = title,
                    deadline = deadline,
                    creatorId = currentUserId,
                    teamId = finalTeamId,
                    assigneeId = assigneeId
                )
            }
            
            // Отдаю в репозиторий для сохранения локально и в облаке
            repository.saveTask(task, subTaskTitles, editingTaskId)
        }
    }

    // Переключаю галочку у подзадачи через специальный репозиторий
    fun toggleSubTaskStatus(taskId: Long, subTaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            subTaskRepository.toggleSubTaskStatus(taskId, subTaskId, isCompleted)
        }
    }

    // Инвертирую статус выполнения всей задачи
    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskStatus(task)
        }
    }

    // Проверяю, я ли создал эту задачу (по глобальному ID)
    fun isOwner(task: TaskEntity): Boolean {
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        return if (task.creatorUid != null) {
            task.creatorUid == myUid
        } else {
            task.creatorId == currentUserId
        }
    }

    // Может ли текущий пользователь управлять этой задачей (редактировать/удалять)
    fun canManageTask(task: TaskEntity): Boolean {
        if (isOwner(task)) return true
        val role = currentUserRole.value
        return role == "admin" || role == "junior_admin"
    }

    // Удаляю задачу полностью (только если я владелец или админ)
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            if (canManageTask(task)) {
                repository.deleteTask(task)
            }
        }
    }

    // Получаю детали конкретной задачи
    fun getTaskById(taskId: Long): StateFlow<TaskWithSubTasks?> {
        return taskDao.getTaskById(taskId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    // Меняю контекст (личные задачи / задачи команды)
    fun changeTeamContext(teamId: Long?) {
        selectedTeamId.value = teamId
    }

    // Достаю список участников команды
    fun getTeamMembers(teamId: Long): Flow<List<UserEntity>> {
        return teamDao.getTeamMembersEntities(teamId)
    }
}

// Вспомогательный класс для хранения задачи и её прогресса в процентах
data class TaskWithProgress(
    val data: TaskWithSubTasks,
    val progress: Int
)
