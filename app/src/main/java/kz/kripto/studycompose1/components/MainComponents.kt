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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.components.KineticPrimaryButton
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.ui.theme.KineticStyle

@Composable
fun TeamCard(
    team: TeamEntity,
    isOwner: Boolean,
    isMember: Boolean = true,
    onTeamClick: () -> Unit,
    onJoinClick: () -> Unit = {},
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShowMembersClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(12.dp))
            .background(
                if (isMember) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant, 
                RoundedCornerShape(12.dp)
            )
            .clickable { if (isMember) onTeamClick() else onJoinClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = team.teamName,
                        style = KineticStyle.rubikNormalStyle,
                        fontSize = 18.sp,
                        color = if (isMember) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isMember) {
                        Text(
                            text = "Публичная команда",
                            style = KineticStyle.rubikNormalStyle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (isMember) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bar),
                                contentDescription = "Опции команды",
                                modifier = Modifier.size(70.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Открыть детали", style = KineticStyle.rubikNormalStyle) },
                                onClick = { onTeamClick(); menuExpanded = false }
                            )
                            if (onShowMembersClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Участники", style = KineticStyle.rubikNormalStyle) },
                                    onClick = { onShowMembersClick(); menuExpanded = false }
                                )
                            }
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Редактировать", style = KineticStyle.rubikNormalStyle) },
                                    onClick = { onEditClick(); menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Удалить",
                                            style = KineticStyle.rubikNormalStyle,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = { onDeleteClick(); menuExpanded = false }
                                )
                            } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Выйти из команды",
                                                style = KineticStyle.rubikNormalStyle,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = { onDeleteClick(); menuExpanded = false }
                                    )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Вступить", style = KineticStyle.rubikNormalStyle)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamActionBottomSheet(
    onDismiss: () -> Unit,
    onCreateTeamClick: () -> Unit,
    onJoinTeamClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KineticPrimaryButton(
                text = "Создать команду",
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        onCreateTeamClick()
                    }
                }
            )

            KineticPrimaryButton(
                text = "Присоединиться к команде",
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        onJoinTeamClick()
                    }
                }
            )
        }
    }
}
