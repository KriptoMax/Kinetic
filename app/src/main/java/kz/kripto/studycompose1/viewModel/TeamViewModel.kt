package kz.kripto.studycompose1.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.TeamEntity

class TeamViewModel(
    private val teamDao: TeamDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserId: Long
        get() = sessionManager.getUserId()

    val teamsState: StateFlow<List<TeamEntity>> = teamDao.getTeamsForUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var teamNameInput = mutableStateOf("")
    var inviteCodeInput = mutableStateOf("")
    var isPrivateInput = mutableStateOf(false)
    var teamError = mutableStateOf<String?>(null)

    fun getTeamById(teamId: Long) = teamDao.getTeamById(teamId)

    fun getTeamMembers(teamId: Long) = teamDao.getTeamMembers(teamId)

    fun isUserInTeam(teamId: Long): Flow<Boolean> {
        return teamDao.isUserInTeam(teamId, currentUserId)
    }

    fun isOwner(team: TeamEntity): Boolean {
        return team.creatorId == currentUserId
    }

    fun setupEditTeam(team: TeamEntity) {
        teamNameInput.value = team.teamName
        isPrivateInput.value = team.isPrivate
        inviteCodeInput.value = team.inviteCode
    }

    fun createTeam(onSuccess: () -> Unit) {
        if (teamNameInput.value.isBlank()) {
            teamError.value = "Введите название команды"
            return
        }
        viewModelScope.launch {
            val code = inviteCodeInput.value.ifBlank { null }
            teamDao.createTeam(
                teamName = teamNameInput.value,
                inviteCode = code,
                creatorId = currentUserId,
                isPrivate = isPrivateInput.value
            )
            clearInputs()
            onSuccess()
        }
    }

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
                clearInputs()
                onSuccess()
            }
        }
    }

    fun deleteTeam(team: TeamEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (isOwner(team)) {
                teamDao.deleteTeam(team)
                onSuccess()
            }
        }
    }

    fun leaveTeam(teamId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            teamDao.removeMember(teamId, currentUserId)
            onSuccess()
        }
    }

    fun removeMemberByUsername(teamId: Long, username: String) {
        viewModelScope.launch {
            teamDao.removeMemberByUsername(teamId, username)
        }
    }

    fun joinPublicTeam(teamId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            teamDao.joinTeamById(teamId, currentUserId)
            onSuccess()
        }
    }

    fun joinTeam(onSuccess: () -> Unit) {
        val code = inviteCodeInput.value
        if (code.isBlank()) {
            teamError.value = "Введите код команды"
            return
        }
        viewModelScope.launch {
            val success = teamDao.joinTeamByCode(code, currentUserId)
            if (success) {
                clearInputs()
                onSuccess()
            } else {
                teamError.value = "Команда не найдена"
            }
        }
    }

    private fun clearInputs() {
        teamNameInput.value = ""
        inviteCodeInput.value = ""
        isPrivateInput.value = false
        teamError.value = null
    }
}
