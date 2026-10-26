plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val configuredApiBaseUrl = providers.gradleProperty("POSHAK_API_BASE_URL")
    .orElse(providers.environmentVariable("POSHAK_API_BASE_URL"))
    .orElse("https://poshak-attendance.onrender.com")
    .get()
val appVersionCode = 2
val appVersionName = "2.0.0"

    android {
        namespace = "com.stkrfx.poshak"
        compileSdk = 37

        signingConfigs {
            create("release") {
                storeFile = file("poshak.keystore")
                storePassword = "satyam"
                keyAlias = "poshak"
                keyPassword = "satyam"
            }
        }  

        defaultConfig {
        applicationId = "com.satyamfix.poshak"  // keep same as old app for in-app updates
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0.0"
        buildConfigField("String", "API_BASE_URL", "\"${configuredApiBaseUrl.trimEnd('/')}\"")
        buildConfigField("int", "APP_VERSION_CODE", "2")
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        resConfigs("en")
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

         
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }   

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose UI and Material 3
    implementation(libs.androidx.compose.ui)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Instrumented and local tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // JSON DTO support already configured by the project
    implementation(libs.kotlinx.serialization.core)

}
