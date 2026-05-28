package kz.kripto.studycompose1.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.ui.theme.KineticStyle
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kz.kripto.studycompose1.components.AddNavbar
import kz.kripto.studycompose1.components.KineticInput
import kz.kripto.studycompose1.components.AddSubtaskRow
import kz.kripto.studycompose1.components.DeadlinePickerField
import kz.kripto.studycompose1.components.KineticLoader
import kz.kripto.studycompose1.components.KineticPrimaryButton
import kz.kripto.studycompose1.components.SubtaskItem
import kz.kripto.studycompose1.viewModel.TaskViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    taskId: Long? = null,
    teamId: Long? = null,
    onBack: () -> Unit,
    viewModel: TaskViewModel = koinViewModel()
) {
    val isEditing = taskId != null && taskId != 0L

    // 1. Инициализируем стейты изначально пустыми
    var taskTitle by remember { mutableStateOf("") }
    val subTasks = remember { mutableStateListOf<String>() }
    var newSubTaskTitle by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    var editingSubTaskIndex by remember { mutableStateOf<Int?>(null) }
    var editingSubTaskText by remember { mutableStateOf("") }
    var activeSubTaskMenuIndex by remember { mutableStateOf<Int?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }

    // Ответственный за задачу
    var selectedAssigneeId by remember { mutableStateOf<Long?>(null) }
    val teamMembers by if (teamId != null) {
        viewModel.getTeamMembers(teamId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<kz.kripto.studycompose1.database.entities.UserEntity>()) }
    }
    var showAssigneeMenu by remember { mutableStateOf(false) }

    // Контроллеры загрузки: isLoading отвечает за показ спиннера, isDataLoaded — чтобы не качать из базы дважды
    var isLoading by remember { mutableStateOf(isEditing) }
    var isDataLoaded by remember { mutableStateOf(!isEditing) }

    val datePickerState = rememberDatePickerState()

    // 2. СЮДА СТАВИМ delay(1000): Асинхронный блок загрузки данных из Room
    LaunchedEffect(taskId) {
        if (isEditing && !isDataLoaded) {
            viewModel.getTaskById(taskId!!).collect { data ->
                if (data != null && !isDataLoaded) {

                    // АНАЛОГ wait(1) ИЗ LUAU: Ждем ровно 1 секунду, пока горит экран загрузки
                    delay(1)

                    // Только ПОСЛЕ задержки раскладываем данные по стейтам
                    taskTitle = data.task.title
                    selectedDateMillis = data.task.deadline
                    selectedAssigneeId = data.task.assigneeId
                    subTasks.clear()
                    subTasks.addAll(data.subTasks.map { it.title })

                    datePickerState.selectedDateMillis = data.task.deadline

                    // Выключаем загрузку и фиксируем, что данные импортированы
                    isDataLoaded = true
                    isLoading = false
                }
            }
        }
    }

    val dateText = selectedDateMillis?.let {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
    } ?: ""

    // 3. РАСПРЕДЕЛЕНИЕ ЭКРАНОВ: Если грузимся — показываем только крутилку, иначе — весь интерфейс
    if (isLoading) {
        KineticLoader()
    } else {
        Scaffold(
            topBar = {
                AddNavbar(
                    taskId = taskId,
                    onBack = onBack,
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Название задачи
                item {
                    KineticInput(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = "Что нужно сделать?"
                    )
                }

                // Выбор дедлайна
                item {
                    DeadlinePickerField(
                        dateText = dateText,
                        onClick = { showDatePicker = true }
                    )
                }

                // ВЫБОР ОТВЕТСТВЕННОГО (Только если это командная задача)
                if (teamId != null) {
                    item {
                        val selectedUser = teamMembers.find { it.id == selectedAssigneeId }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Ответственный за выполнение:",
                                style = KineticStyle.nunitoNormalStyle
                            )
                            Box {
                                OutlinedButton(
                                    onClick = { showAssigneeMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedUser?.username ?: "Не назначен",
                                            style = KineticStyle.rubikNormalStyle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_bar),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showAssigneeMenu,
                                    onDismissRequest = { showAssigneeMenu = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Не назначен", style = KineticStyle.rubikNormalStyle) },
                                        onClick = {
                                            selectedAssigneeId = null
                                            showAssigneeMenu = false
                                        }
                                    )
                                    teamMembers.forEach { member ->
                                        DropdownMenuItem(
                                            text = { Text(member.username, style = KineticStyle.rubikNormalStyle) },
                                            onClick = {
                                                selectedAssigneeId = member.id
                                                showAssigneeMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Подзадачи (Удерживай для меню):",
                        style = KineticStyle.nunitoNormalStyle
                    )
                }

                // Вывод списка подзадач
                itemsIndexed(subTasks) { index, subTask ->
                    SubtaskItem(
                        index = index,
                        title = subTask,
                        isEditing = editingSubTaskIndex == index,
                        editingText = editingSubTaskText,
                        onEditingTextChange = { editingSubTaskText = it },
                        onSaveEdit = {
                            if (editingSubTaskText.isNotBlank()) {
                                subTasks[index] = editingSubTaskText.trim()
                            }
                            editingSubTaskIndex = null
                        },
                        isMenuExpanded = activeSubTaskMenuIndex == index,
                        onMenuDismiss = { activeSubTaskMenuIndex = null },
                        onLongClick = { activeSubTaskMenuIndex = index },
                        onEditClick = {
                            activeSubTaskMenuIndex = null
                            editingSubTaskIndex = index
                            editingSubTaskText = subTask
                        },
                        onDeleteClick = {
                            activeSubTaskMenuIndex = null
                            subTasks.removeAt(index)
                        }
                    )
                }

                // Строка добавления новой подзадачи
                item {
                    AddSubtaskRow(
                        value = newSubTaskTitle,
                        onValueChange = { newSubTaskTitle = it },
                        onAddClick = {
                            if (newSubTaskTitle.isNotBlank()) {
                                subTasks.add(newSubTaskTitle.trim())
                                newSubTaskTitle = ""
                            }
                        }
                    )
                }

                // Кнопка сохранения всей группы
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    KineticPrimaryButton(
                        text = "Сохранить изменения",
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                viewModel.saveTask(
                                    title = taskTitle.trim(),
                                    subTaskTitles = subTasks.toList(),
                                    deadline = selectedDateMillis,
                                    editingTaskId = taskId,
                                    teamId = teamId,
                                    assigneeId = selectedAssigneeId
                                )
                                onBack()
                            }
                        }
                    )
                }
            }

            // Диалог календаря
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) {
                            Text("ОК")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Отмена")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}