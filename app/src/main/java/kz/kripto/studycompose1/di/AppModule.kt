package kz.kripto.studycompose1.di

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.viewModel.AuthViewModel
import kz.kripto.studycompose1.database.KineticDatabase
import kz.kripto.studycompose1.repository.SubTaskRepository
import kz.kripto.studycompose1.repository.TaskRepository
import kz.kripto.studycompose1.repository.TeamRepository
import kz.kripto.studycompose1.repository.UserRepository
import kz.kripto.studycompose1.viewModel.TaskViewModel
import kz.kripto.studycompose1.viewModel.TeamViewModel // <- ДОБАВИЛИ ИМПОРТ
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // 1. Инициализация базы данных Room
    single<KineticDatabase> {
        Room.databaseBuilder(
            androidContext(),
            KineticDatabase::class.java,
            "kinetic_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 2. Менеджер сессий
    single { SessionManager(androidContext()) }

    // 3. Регистрация DAO (явно указываем тип базы через get<KineticDatabase>())
    single { get<KineticDatabase>().taskDao() }
    single { get<KineticDatabase>().userDao() }
    single { get<KineticDatabase>().teamDao() }

    // 4. Firebase
    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }

    // 5. Repository
    single { TaskRepository(get(), get(), get(), get()) }
    single { TeamRepository(get(), get(), get()) }
    single { UserRepository(get(), get(), get()) }
    single { SubTaskRepository(get(), get(), get(), get()) }

    // 6. Регистрация ViewModel
    viewModel { TaskViewModel(repository = get(), subTaskRepository = get(), taskDao = get(), teamDao = get(), sessionManager = get()) }
    viewModel { AuthViewModel(userDao = get(), sessionManager = get(), auth = get(), userRepository = get()) }
    viewModel { TeamViewModel(teamDao = get(), sessionManager = get(), teamRepository = get()) }
    // Если твоя TeamViewModel принимает что-то еще (например, taskDao),
    // Koin сам разберется, главное просто дописать еще один get(), вот так: TeamViewModel(get(), get())
}