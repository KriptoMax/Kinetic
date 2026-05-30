plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // 1. ПОДКЛЮЧИЛИ ПЛАГИН KSP
    // ДОПГРЕЙД
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "kz.kripto.studycompose1"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "kz.kripto.studycompose1"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    //навигация
    implementation(libs.androidx.navigation.compose)
    // Допгрейд ядра сериализации json, без которого навигация не поймет объекты маршрутов
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Золотой стандарт:
    // Корутины
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    // Room (База данных)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler) // KSP процессор вместо старого kapt
    // Retrofit (Сеть)
    implementation(libs.retrofit.main)
    implementation(libs.retrofit.gson)
    // Koin (Внедрение зависимостей)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // WorkManager (Фоновые задачи)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //Внедрение Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Платформа управления версиями
    implementation("com.google.firebase:firebase-auth-ktx")           // Аутентификация
    implementation("com.google.firebase:firebase-firestore-ktx")      // Облачная БД
    implementation("com.google.firebase:firebase-messaging-ktx")      // Push-уведомления
}