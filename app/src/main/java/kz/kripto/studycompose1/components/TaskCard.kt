package kz.kripto.studycompose1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.TaskWithProgress
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    taskWithProgress: TaskWithProgress,
    onTaskClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val task = taskWithProgress.data.task

    val strokeColor = MaterialTheme.colorScheme.onPrimary
    val cardBgColor = MaterialTheme.colorScheme.primary

    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) { configuration.locales[0] }

    val deadlineText = task.deadline?.let {
        SimpleDateFormat("dd.MM.yyyy", currentLocale).format(Date(it))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, strokeColor, RoundedCornerShape(12.dp))
            .background(cardBgColor, RoundedCornerShape(12.dp))
            .clickable { onTaskClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = KineticStyle.minecraftStyle,
                    fontSize = 22.sp,
                    color = strokeColor
                )

                // ОТОБРАЖЕНИЕ ДАТЫ: Показываем дедлайн, если он задан
                if (deadlineText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "До: $deadlineText",
                        style = KineticStyle.rubikNormalStyle,
                        fontSize = 13.sp,
                        color = strokeColor.copy(alpha = 0.8f)
                    )
                }

                // ОТОБРАЖЕНИЕ ОТВЕТСТВЕННОГО (Если задача командная)
                if (task.teamId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bar),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = strokeColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ответственный: ${taskWithProgress.data.assignee?.username ?: "Не назначен"}",
                            style = KineticStyle.rubikNormalStyle,
                            fontSize = 13.sp,
                            color = strokeColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { taskWithProgress.progress / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .border(1.dp, strokeColor, RoundedCornerShape(4.dp)),
                        color = strokeColor,
                        trackColor = cardBgColor,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${taskWithProgress.progress}%",
                        style = KineticStyle.rubikNormalStyle,
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = strokeColor
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bar),
                        contentDescription = "Опции",
                        modifier = Modifier.size(32.dp),
                        tint = strokeColor
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Открыть детали", style = KineticStyle.rubikNormalStyle) },
                        onClick = {
                            menuExpanded = false
                            onTaskClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Редактировать", style = KineticStyle.rubikNormalStyle) },
                        onClick = {
                            menuExpanded = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить группу задач", style = KineticStyle.rubikNormalStyle, color = Color.Red) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}