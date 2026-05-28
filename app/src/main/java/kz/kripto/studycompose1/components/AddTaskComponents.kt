package kz.kripto.studycompose1.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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

@Composable
fun DeadlinePickerField(
    dateText: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        KineticInput(
            value = dateText,
            onValueChange = {},
            label = "Дедлайн",
            readOnly = true,
            enabled = false,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar),
                    contentDescription = "Выбрать дату"
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubtaskItem(
    index: Int,
    title: String,
    isEditing: Boolean,
    editingText: String,
    onEditingTextChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuDismiss: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KineticInput(
                value = editingText,
                onValueChange = onEditingTextChange,
                label = "",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSaveEdit) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_galka),
                    contentDescription = "Сохранить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onLongClick,
                        onLongClick = onLongClick
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. $title",
                    style = KineticStyle.rubikNormalStyle,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar),
                    contentDescription = "Опции подзадачи",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = onMenuDismiss,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Редактировать текст",
                            style = KineticStyle.rubikNormalStyle
                        )
                    },
                    onClick = onEditClick
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Удалить подзадачу",
                            style = KineticStyle.rubikNormalStyle,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun AddSubtaskRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KineticInput(
            value = value,
            onValueChange = onValueChange,
            label = "Добавить подзадачу",
            modifier = Modifier.weight(1f)
        )
        KineticSmallButton(
            text = "Добавить",
            onClick = onAddClick
        )
    }
}

@Composable
fun KineticLoader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
