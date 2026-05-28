package kz.kripto.studycompose1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.components.TaskDetailNavbar
import kz.kripto.studycompose1.database.entities.SubTaskEntity
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.TaskViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskViewModel = koinViewModel()
) {
    val taskWithSubTasks by viewModel.getTaskById(taskId).collectAsState(initial = null)

    val strokeColor = MaterialTheme.colorScheme.onPrimary
    val cardBgColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = { TaskDetailNavbar(onBack = onBack) }
    ) { innerPadding ->
        taskWithSubTasks?.let { currentData ->
            val mainTask = currentData.task
            val subTaskList = currentData.subTasks

            // Форматируем дедлайн для деталей
            val deadlineText = mainTask.deadline?.let {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
            } ?: "Не установлен"

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Шапка задачи
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, strokeColor, RoundedCornerShape(12.dp))
                            .background(cardBgColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mainTask.title,
                            style = KineticStyle.minecraftStyle,
                            fontSize = 24.sp,
                            color = strokeColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (mainTask.isCompleted) "Статус: Выполнено" else "Статус: В процессе",
                            style = KineticStyle.rubikNormalStyle,
                            fontSize = 14.sp,
                            color = strokeColor.copy(alpha = 0.7f)
                        )

                        // ОТОБРАЖЕНИЕ ДАТЫ НА ПЛАШКЕ ДЕТАЛЕЙ
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Крайний срок: $deadlineText",
                            style = KineticStyle.rubikNormalStyle,
                            fontSize = 14.sp,
                            color = strokeColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // Список подзадач
                if (subTaskList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Подзадачи:",
                            style = KineticStyle.nunitoNormalStyle,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(subTaskList) { subTask ->
                        DetailSubTaskRow(
                            subTask = subTask,
                            strokeColor = MaterialTheme.colorScheme.onBackground,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleSubTaskStatus(subTask.id, isCompleted = isChecked)
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "У этой задачи нет подзадач",
                                style = KineticStyle.rubikNormalStyle,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun DetailSubTaskRow(
    subTask: SubTaskEntity,
    strokeColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, strokeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, strokeColor, RoundedCornerShape(6.dp))
                .background(
                    if (subTask.isCompleted) MaterialTheme.colorScheme.secondary else Color.Transparent
                )
                .clickable { onCheckedChange(!subTask.isCompleted) },
            contentAlignment = Alignment.Center
        ) {
            if (subTask.isCompleted) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_galka),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = subTask.title,
            style = KineticStyle.rubikNormalStyle,
            fontSize = 16.sp,
            color = if (subTask.isCompleted) strokeColor.copy(alpha = 0.5f) else strokeColor,
            modifier = Modifier.clickable { onCheckedChange(!subTask.isCompleted) }
        )
    }
}