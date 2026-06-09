package kz.kripto.studycompose1.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.repository.TeamRepository

// Моя ViewModel для управления командами
class TeamViewModel(
    private val teamDao: TeamDao,
    private val sessionManager: SessionManager,
    private val teamRepository: TeamRepository
) : ViewModel() {

    init {
        // Подключаю синхронизацию команд из облака
        teamRepository.startRealtimeSync()
    }

    val currentUserId: Long
        get() = sessionManager.getUserId()

    // Наблюдаю за списком команд, в которых я состою
    val teamsState: StateFlow<List<TeamEntity>> = teamDao.getTeamsForUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Временные поля для форм создания/вступления
    var teamNameInput = mutableStateOf("")
    var inviteCodeInput = mutableStateOf("")
    var isPrivateInput = mutableStateOf(false)
    var teamError = mutableStateOf<String?>(null)

    // Получаю данные о команде по ID
    fun getTeamById(teamId: Long) = teamDao.getTeamById(teamId)

    // Получаю список участников конкретной команды с ролями
    fun getTeamMembers(teamId: Long) = teamDao.getTeamMembersWithRoles(teamId)

    // Проверяю, состою ли я в этой команде
    fun isUserInTeam(teamId: Long): Flow<Boolean> {
        return teamDao.isUserInTeam(teamId, currentUserId)
    }

    // Проверяю роль пользователя в команде
    fun getUserRoleInTeam(teamId: Long): Flow<String?> {
        return teamDao.getTeamMembersWithRoles(teamId).map { members ->
            val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            members.find { it.firebaseUid == myUid }?.role
        }
    }

    // Проверяю, я ли создал эту команду (по глобальному ID)
    fun isOwner(team: TeamEntity): Boolean {
        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        return if (team.creatorUid != null) {
            team.creatorUid == myUid
        } else {
            team.creatorId == currentUserId
        }
    }

    // Метод для смены роли участника (только для владельца)
    fun changeMemberRole(teamId: Long, memberUid: String, newRole: String) {
        viewModelScope.launch {
            val team = teamDao.getTeamByIdOnce(teamId)
            if (team != null && isOwner(team)) {
                teamRepository.updateMemberRole(team.inviteCode, memberUid, newRole)
                teamDao.updateMemberRole(teamId, memberUid, newRole)
            }
        }
    }

    // Заполняю форму данными команды для редактирования
    fun setupEditTeam(team: TeamEntity) {
        teamNameInput.value = team.teamName
        isPrivateInput.value = team.isPrivate
        inviteCodeInput.value = team.inviteCode
    }

    // Создаю новую команду
    fun createTeam(onSuccess: () -> Unit) {
        if (teamNameInput.value.isBlank()) {
            teamError.value = "Введите название команды"
            return
        }
        viewModelScope.launch {
            val code = inviteCodeInput.value.ifBlank { null }
            teamRepository.createTeam(
                teamName = teamNameInput.value,
                inviteCode = code,
                creatorId = currentUserId,
                isPrivate = isPrivateInput.value
            )
            clearInputs()
            onSuccess()
        }
    }

    // Обновляю название или настройки приватности моей команды
    fun updateTeam(teamId: Long, onSuccess: () -> Unit) {
        if (teamNameInput.value.isBlank()) {
            teamError.value = "Введите название команды"
            return
        }
        viewModelScope.launch {
            val team = teamDao.getTeamById(teamId).first()
            if (team != null && isOwner(team)) {
                teamDao.updateTeam(team.copy(
                    teamName = teamNameInput.value,
                    isPrivate = isPrivateInput.value,
                    inviteCode = inviteCodeInput.value
                ))
                // Тут в будущем можно добавить синхронизацию изменений в Firebase
                clearInputs()
                onSuccess()
            }
        }
    }

    // Удаляю команду полностью (только если я владелец)
    fun deleteTeam(team: TeamEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (isOwner(team)) {
                teamRepository.deleteTeam(team)
                onSuccess()
            }
        }
    }

    // Выхожу из команды или удаляю её, если я владелец
    fun leaveTeam(teamId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val team = teamDao.getTeamByIdOnce(teamId)
            if (team != null && isOwner(team)) {
                teamRepository.deleteTeam(team)
            } else {
                teamDao.removeMember(teamId, currentUserId)
            }
            onSuccess()
        }
    }

    // Выгоняю участника из моей команды по его логину
    fun removeMemberByUsername(teamId: Long, username: String) {
        viewModelScope.launch {
            teamDao.removeMemberByUsername(teamId, username)
        }
    }

    // Просто вступаю в публичную команду (без кода)
    fun joinPublicTeam(teamId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            teamRepository.joinPublicTeam(teamId, currentUserId)
            onSuccess()
        }
    }

    // Вступаю в закрытую команду по пригласительному коду
    fun joinTeam(onSuccess: () -> Unit) {
        val code = inviteCodeInput.value
        if (code.isBlank()) {
            teamError.value = "Введите код команды"
            return
        }
        viewModelScope.launch {
            val success = teamRepository.joinTeamByCode(code, currentUserId)
            if (success) {
                clearInputs()
                onSuccess()
            } else {
                teamError.value = "Команда не найдена"
            }
        }
    }

    // Очищаю все поля ввода
    private fun clearInputs() {
        teamNameInput.value = ""
        inviteCodeInput.value = ""
        isPrivateInput.value = false
        teamError.value = null
    }
}
