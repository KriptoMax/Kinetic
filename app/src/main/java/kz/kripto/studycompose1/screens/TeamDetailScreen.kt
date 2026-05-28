package kz.kripto.studycompose1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kz.kripto.studycompose1.components.KineticAddFAB
import kz.kripto.studycompose1.components.KineticReturnFAB
import kz.kripto.studycompose1.components.MainNavbar
import kz.kripto.studycompose1.components.TaskCard
import kz.kripto.studycompose1.components.TeamCard
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.ui.theme.StudyCompose1Theme
import kz.kripto.studycompose1.viewModel.TaskViewModel
import kz.kripto.studycompose1.viewModel.TaskWithProgress
import kz.kripto.studycompose1.viewModel.TeamViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun TeamDetailsScreen(
    teamId: Long,
    onBackClick: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onAddTask: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    sessionManager: SessionManager = koinInject(),
    teamViewModel: TeamViewModel = koinViewModel(),
    taskViewModel: TaskViewModel = koinViewModel()
) {
    val team by teamViewModel.getTeamById(teamId).collectAsState(initial = null)
    val tasks by taskViewModel.teamTasksState.collectAsState()
    val members by teamViewModel.getTeamMembers(teamId).collectAsState(initial = emptyList())
    val isAuthorized = sessionManager.getUserId() != -1L

    var showMembersSheet by remember { mutableStateOf(false) }

    if (!isAuthorized) {
        LaunchedEffect(Unit) {
            onNavigateToAuth()
        }
        return
    }
    
    LaunchedEffect(teamId) {
        taskViewModel.loadTeamTasks(teamId)
        taskViewModel.changeTeamContext(teamId)
    }

    TeamDetailsContent(
        team = team,
        tasks = tasks,
        isOwner = { t -> teamViewModel.isOwner(t) },
        onBackClick = onBackClick,
        onLogout = onNavigateToAuth,
        onAddTask = { onAddTask(teamId) },
        onTaskClick = onTaskClick,
        onEditTask = onEditTask,
        onDeleteTask = { taskViewModel.deleteTask(it.data.task) },
        sessionManager = sessionManager,
        onShowMembersClick = { showMembersSheet = true }
    )

    if (showMembersSheet) {
        MembersBottomSheet(
            members = members,
            isOwner = team?.let { teamViewModel.isOwner(it) } ?: false,
            currentUsername = sessionManager.getUsername() ?: "",
            onRemoveMember = { username -> teamViewModel.removeMemberByUsername(teamId, username) },
            onDismiss = { showMembersSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersBottomSheet(
    members: List<String>,
    isOwner: Boolean,
    currentUsername: String,
    onRemoveMember: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Участники команды",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (members.isEmpty()) {
                Text(
                    text = "Список пуст",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(members) { member ->
                        var showMemberMenu by remember { mutableStateOf(false) }
                        val isMe = member == currentUsername

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = kz.kripto.studycompose1.R.drawable.ic_bar),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isMe) "$member (Вы)" else member,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (isOwner && !isMe) {
                                Box {
                                    IconButton(onClick = { showMemberMenu = true }) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = kz.kripto.studycompose1.R.drawable.ic_bar),
                                            contentDescription = "Меню участника",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMemberMenu,
                                        onDismissRequest = { showMemberMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    "Удалить из команды", 
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error
                                                ) 
                                            },
                                            onClick = {
                                                onRemoveMember(member)
                                                showMemberMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailsContent(
    team: TeamEntity?,
    tasks: List<TaskWithProgress>,
    isOwner: (TeamEntity) -> Boolean,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onDeleteTask: (TaskWithProgress) -> Unit,
    sessionManager: SessionManager? = null,
    onShowMembersClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) tasks
        else tasks.filter { it.data.task.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (sessionManager != null) {
                MainNavbar(
                    sessionManager = sessionManager,
                    onLogout = onLogout
                )
            }
        },
        floatingActionButton = {
            KineticAddFAB(onClick = onAddTask)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(45.dp))
                    team?.let { currentTeam ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TeamCard(
                                team = currentTeam,
                                isOwner = isOwner(currentTeam),
                                onTeamClick = { },
                                onEditClick = { },
                                onDeleteClick = { },
                                onShowMembersClick = onShowMembersClick
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        kz.kripto.studycompose1.components.KineticInput(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "Поиск в этой команде...",
                            leadingIcon = androidx.compose.ui.res.painterResource(id = kz.kripto.studycompose1.R.drawable.ic_search)
                        )
                    }
                }

                item {
                    Text(
                        text = "Задачи команды:",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(filteredTasks) { taskWithProgress ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TaskCard(
                            taskWithProgress = taskWithProgress,
                            onTaskClick = { onTaskClick(taskWithProgress.data.task.id) },
                            onEditClick = { onEditTask(taskWithProgress.data.task.id) },
                            onDeleteClick = { onDeleteTask(taskWithProgress) }
                        )
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyText = if (searchQuery.isEmpty()) "В этой команде пока нет задач" else "Задачи не найдены"
                            Text(
                                text = emptyText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            KineticReturnFAB(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(16.dp)
                    .size(50.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Preview(showBackground = true,
    device = "spec:width=412dp,height=915dp,dpi=450",
    showSystemUi = true)
@Composable
fun TeamDetailPreview() {
    StudyCompose1Theme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            TeamDetailsContent(
                team = TeamEntity(id = 1, teamName = "Моя Команда", creatorId = 1, inviteCode = "ABC-123"),
                tasks = emptyList(),
                isOwner = { true },
                onBackClick = {},
                onLogout = {},
                onAddTask = {},
                onTaskClick = {},
                onEditTask = {},
                onDeleteTask = {}
            )
        }
    }
}
