package kz.kripto.studycompose1.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.database.dao.TaskDao
import kz.kripto.studycompose1.database.entities.TaskEntity
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.entities.TaskWithSubTasks
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.UserEntity

import kz.kripto.studycompose1.repository.TaskRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val taskDao: TaskDao,
    private val teamDao: TeamDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    // При инициализации можно запустить синхронизацию
    init {
        repository.startRealtimeSync()
    }

    // Текущий залогиненный пользователь
    val currentUserId: Long
        get() = sessionManager.getUserId()

    // Идентификатор выбранной команды. Если null -> смотрим личные задачи. Если число -> задачи этой команды
    val selectedTeamId = MutableStateFlow<Long?>(null)

    // Переменная для хранения текста поиска
    var searchQuery = mutableStateOf("")
        private set

    // Реактивный поток для отслеживания текста поиска внутри Flow
    private val _searchFlow = MutableStateFlow("")

    // --- ДОБАВЛЕНО ДЛЯ ЭКРАНА КОМАНДЫ (TeamTasksScreen) ---
    private val _teamTasksState = MutableStateFlow<List<TaskWithProgress>>(emptyList())
    val teamTasksState: StateFlow<List<TaskWithProgress>> = _teamTasksState.asStateFlow()

    // Метод загружает задачи конкретной команды и сразу считает по ним прогресс (для чекбоксов)
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
    // -----------------------------------------------------

    // УМНЫЙ ПОТОК: Автоматически переключает базу данных в зависимости от того, выбрана ли команда
    val taskListState: StateFlow<List<TaskWithProgress>> = combine(
        selectedTeamId,
        _searchFlow
    ) { teamId, query -> Pair(teamId, query) }
        .flatMapLatest { (teamId, query) ->
            // Если команда не выбрана — тянем только личные приватные задачи пользователя
            val dbFlow = if (teamId == null) {
                taskDao.getUserPrivateTasks(currentUserId)
            } else {
                // Если команда выбрана — тянем задачи этой конкретной команды
                taskDao.getTeamTasks(teamId)
            }

            // Внутри фильтруем по поисковой строке
            dbFlow.map { list ->
                if (query.isBlank()) list
                else list.filter { it.task.title.contains(query, ignoreCase = true) }
            }
        }
        .map { list ->
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
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // МОДИФИЦИРОВАННЫЙ МЕТОД СОХРАНЕНИЯ: Теперь принимает teamId напрямую из экрана
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
                // Пытаемся найти существующую задачу, чтобы сохранить её remoteId
                val existing = taskDao.getTaskByIdOnce(editingTaskId)
                TaskEntity(
                    id = editingTaskId,
                    remoteId = existing?.task?.remoteId, // ВАЖНО: сохраняем старый ID из Firestore
                    title = title,
                    deadline = deadline,
                    creatorId = currentUserId,
                    teamId = finalTeamId,
                    assigneeId = assigneeId
                )
            } else {
                TaskEntity(
                    title = title,
                    deadline = deadline,
                    creatorId = currentUserId,
                    teamId = finalTeamId,
                    assigneeId = assigneeId
                )
            }
            
            repository.saveTask(task, subTaskTitles, editingTaskId)
        }
    }

    fun toggleSubTaskStatus(subTaskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            taskDao.updateSubTaskStatus(subTaskId, isCompleted)
            // Добавить синхронизацию подзадач в репозиторий при необходимости
        }
    }

    fun toggleTaskStatus(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskStatus(task)
        }
    }

    // Дополнительный хелпер для работы экрана TeamTasksScreen
    fun toggleTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            if (task.isCompleted != isCompleted) {
                repository.toggleTaskStatus(task)
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun getTaskById(taskId: Long): StateFlow<TaskWithSubTasks?> {
        return taskDao.getTaskById(taskId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    // Метод для переключения вкладок (Личные задачи / Командные задачи)
    fun changeTeamContext(teamId: Long?) {
        selectedTeamId.value = teamId
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
        _searchFlow.value = newQuery
    }

    fun getTeamMembers(teamId: Long): Flow<List<UserEntity>> {
        return teamDao.getTeamMembersEntities(teamId)
    }
}

data class TaskWithProgress(
    val data: TaskWithSubTasks,
    val progress: Int
)