package kz.kripto.studycompose1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.ui.theme.KineticStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavbar(
    sessionManager: kz.kripto.studycompose1.database.data.SessionManager,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val username = sessionManager.getUsername() ?: "Гость"

    TopAppBar(
        modifier = Modifier
            .statusBarsPadding()
            .height(54.dp),
        title = {
            Text(
                text = "Kinetic",
                style = KineticStyle.minecraftStyle,
                modifier = Modifier.padding(start = 10.dp),
            )
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar),
                    contentDescription = "menu",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .width(200.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "Вы вошли как:",
                                style = KineticStyle.rubikNormalStyle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = username,
                                style = KineticStyle.rubikNormalStyle,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = { },
                    enabled = false
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                DropdownMenuItem(
                    text = { Text(text = "Настройки", style = KineticStyle.rubikNormalStyle) },
                    onClick = { expanded = false }
                )
                DropdownMenuItem(
                    text = { Text(text = "О приложении", style = KineticStyle.rubikNormalStyle) },
                    onClick = { expanded = false }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Выйти",
                            style = KineticStyle.rubikNormalStyle,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        expanded = false
                        sessionManager.logout()
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bar),
                            contentDescription = "Выйти",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthNavbar(
    isRegisterMode: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isRegisterMode) "Регистрация" else "Авторизация",
                style = KineticStyle.minecraftStyle
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Назад",
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailNavbar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Детали задачи", style = KineticStyle.minecraftStyle) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamNavbar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Новая команда", style = KineticStyle.minecraftStyle) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamNavbar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text("Вступление в команду", style = KineticStyle.minecraftStyle) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNavbar(
    taskId: Long? = null,
    onBack: () -> Unit,
){
    val isEditing = taskId != null && taskId != 0L

    TopAppBar(
        title = {
            Text(
                if (isEditing) "Редактирование" else "Новая задача",
                style = KineticStyle.minecraftStyle
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}