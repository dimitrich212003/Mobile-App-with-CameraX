plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.cameraproj"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cameraproj"
        minSdk = 21
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout.v220)

    // базовая библиотека CameraX
    implementation(libs.androidx.camera.core.v140)
    // поддержка Camera2 API.
    implementation(libs.androidx.camera.camera2.v140)
    // интеграция с жизненным циклом приложения.
    implementation(libs.androidx.camera.lifecycle.v140)
    // предоставляет компоненты для отображения предпросмотра камеры.
    implementation(libs.androidx.camera.view.v140)

    // для тестов
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Для корректной работы с компонентами Android (Activity)
    implementation(libs.androidx.activity)

    // Для загрузки и отображения изображений в андройде
    implementation(libs.glide)

    // Зависимости для видеоплеера exoplayer-core
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.smoothstreaming)
}