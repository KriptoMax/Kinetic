package kz.kripto.studycompose1

import android.app.Application
import kz.kripto.studycompose1.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KineticApp : Application(){
    override fun onCreate(){
        super.onCreate()
        startKoin{
            // Передаем контекст Android, чтобы Room знал, где создавать файл БД
            androidContext(this@KineticApp)
            // наш модуль
            modules(appModule)
        }
    }
}