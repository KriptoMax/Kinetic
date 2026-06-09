package kz.kripto.studycompose1.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kz.kripto.studycompose1.screens.MainScreen
import kz.kripto.studycompose1.screens.CreateTeamScreen
import kz.kripto.studycompose1.screens.JoinTeamScreen
import kz.kripto.studycompose1.screens.AddTaskScreen
import kz.kripto.studycompose1.screens.TaskDetailScreen
import kz.kripto.studycompose1.screens.TeamDetailsScreen // Наш исправленный экран деталей
import kz.kripto.studycompose1.screens.AuthScreen

@Serializable object AuthRoute
@Serializable object MainRoute
@Serializable object CreateTeamRoute
@Serializable data class EditTeamRoute(val teamId: Long)
@Serializable object JoinTeamRoute
@Serializable data class TeamTasksRoute(val teamId: Long)
@Serializable data class AddTaskRoute(val taskId: Long? = null, val teamId: Long? = null)
@Serializable data class TaskDetailRoute(val taskId: Long)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Начинаем с MainRoute, но внутри него кнопки будут проверять авторизацию
    NavHost(navController = navController, startDestination = MainRoute) {

        composable<AuthRoute> {
            AuthScreen(
                onAuthSuccess = {
                    // Когда вошли успешно — открываем Главный и стираем экран авторизации,
                    // чтобы кнопка назад из Главного не вела снова на логин
                    navController.navigate(MainRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                },
                onBackClick = {
                    // Теперь сзади лежит MainRoute, и этот вызов успешно на него вернет! ✅
                    navController.popBackStack()
                }
            )
        }

        // 2. Главный экран (Список команд)
        // Внутри NavHost в файле AppNavigation.kt:
        composable<MainRoute> {
            MainScreen(
                onNavigateToCreateTeam = { navController.navigate(CreateTeamRoute) },
                onNavigateToJoinTeam = { navController.navigate(JoinTeamRoute) },
                onTeamClick = { teamId -> navController.navigate(TeamTasksRoute(teamId = teamId)) },
                onNavigateToEditTeam = { teamId -> navController.navigate(EditTeamRoute(teamId = teamId)) },
                onNavigateToAuth = {
                    navController.navigate(AuthRoute)
                }
            )
        }

        // 3. Экран создания новой команды
        composable<CreateTeamRoute> {
            CreateTeamScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<EditTeamRoute> { backStackEntry ->
            val route: EditTeamRoute = backStackEntry.toRoute()
            kz.kripto.studycompose1.screens.EditTeamScreen(
                teamId = route.teamId,
                onBack = { navController.popBackStack() }
            )
        }

        // 4. Экран вступления в команду по коду
        composable<JoinTeamRoute> {
            JoinTeamScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 5. Экран внутри команды
        composable<TeamTasksRoute> { backStackEntry ->
            // Извлекаем teamId безопасным Type-Safe способом
            val route: TeamTasksRoute = backStackEntry.toRoute()

            TeamDetailsScreen(
                teamId = route.teamId,
                onBackClick = { navController.popBackStack() },
                onNavigateToAuth = { navController.navigate(AuthRoute) },
                onAddTask = { teamId -> navController.navigate(AddTaskRoute(teamId = teamId)) },
                onTaskClick = { taskId -> navController.navigate(TaskDetailRoute(taskId = taskId)) },
                onEditTask = { taskId -> navController.navigate(AddTaskRoute(taskId = taskId, teamId = route.teamId)) }
            )
        }

        // Старые роуты для задач
        composable<AddTaskRoute> { backStackEntry ->
            val route: AddTaskRoute = backStackEntry.toRoute()

            AddTaskScreen(
                taskId = route.taskId,
                teamId = route.teamId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<TaskDetailRoute> { backStackEntry ->
            val route: TaskDetailRoute = backStackEntry.toRoute()
            TaskDetailScreen(
                taskId = route.taskId,
                onBack = { navController.popBackStack() },
                onEditTask = { taskId ->
                    navController.navigate(AddTaskRoute(taskId = taskId))
                },
                onDeleteTask = {
                    navController.popBackStack()
                }
            )
        }
    }
}