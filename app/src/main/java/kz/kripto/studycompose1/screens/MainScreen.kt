package kz.kripto.studycompose1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.components.KineticAddFAB
import kz.kripto.studycompose1.components.KineticInput
import kz.kripto.studycompose1.components.MainNavbar
import kz.kripto.studycompose1.components.TeamActionBottomSheet
import kz.kripto.studycompose1.components.TeamCard
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.TeamViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCreateTeam: () -> Unit,
    onNavigateToJoinTeam: () -> Unit,
    onTeamClick: (Long) -> Unit,
    onNavigateToEditTeam: (Long) -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: TeamViewModel = koinViewModel(),
    sessionManager: kz.kripto.studycompose1.database.data.SessionManager = koinInject()
) {
    val teams by viewModel.teamsState.collectAsState()
    val currentUserId = sessionManager.getUserId()
    val isAuthorized = currentUserId != -1L && currentUserId != 0L
    var searchQuery by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<kz.kripto.studycompose1.database.entities.TeamEntity?>(null) }

    val filteredTeams = remember(teams, searchQuery, isAuthorized) {
        if (!isAuthorized) emptyList()
        else if (searchQuery.isBlank()) teams
        else teams.filter { it.teamName.contains(searchQuery, ignoreCase = true) }
    }

    if (teamToDelete != null) {
        val isOwner = viewModel.isOwner(teamToDelete!!)
        AlertDialog(
            onDismissRequest = { teamToDelete = null },
            title = {
                Text(
                    text = if (isOwner) "Удалить команду?" else "Выйти из команды?",
                    style = KineticStyle.rubikNormalStyle
                )
            },
            text = {
                Text(
                    text = if (isOwner) 
                        "Вы являетесь владельцем. При выходе команда \"${teamToDelete!!.teamName}\" будет удалена навсегда для всех участников." 
                        else "Вы действительно хотите выйти из команды \"${teamToDelete!!.teamName}\"?",
                    style = KineticStyle.rubikNormalStyle
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val team = teamToDelete!!
                        if (viewModel.isOwner(team)) {
                            viewModel.deleteTeam(team)
                        } else {
                            viewModel.leaveTeam(team.id)
                        }
                        teamToDelete = null
                    }
                ) {
                    Text("Да", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            MainNavbar(
                sessionManager = sessionManager,
                onLogout = {
                    onNavigateToAuth()
                }
            )
        },
        floatingActionButton = {
            KineticAddFAB(
                onClick = {
                    if (!isAuthorized) {
                        onNavigateToAuth()
                    } else {
                        showBottomSheet = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (!isAuthorized) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Для просмотра команд\nнеобходимо авторизоваться",
                            style = KineticStyle.rubikNormalStyle,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToAuth) {
                            Text("Войти", style = KineticStyle.rubikNormalStyle)
                        }
                    }
                }
            } else {
                KineticInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Поиск команд...",
                    leadingIcon = painterResource(id = R.drawable.ic_search)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredTeams.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyText = if (searchQuery.isEmpty()) {
                            "Вы пока не состоите в командах\nНажми +, чтобы создать или войти"
                        } else {
                            "Команда не найдена\nПопробуйте изменить запрос"
                        }
                        Text(
                            text = emptyText,
                            style = KineticStyle.rubikNormalStyle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = filteredTeams, key = { it.id }) { team ->
                            val isMember by viewModel.isUserInTeam(team.id).collectAsState(initial = true)

                            TeamCard(
                                team = team,
                                isOwner = viewModel.isOwner(team),
                                isMember = isMember,
                                onTeamClick = { onTeamClick(team.id) },
                                onJoinClick = { viewModel.joinPublicTeam(team.id) },
                                onEditClick = { onNavigateToEditTeam(team.id) },
                                onDeleteClick = { teamToDelete = team }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        TeamActionBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCreateTeamClick = onNavigateToCreateTeam,
            onJoinTeamClick = onNavigateToJoinTeam
        )
    }
}
