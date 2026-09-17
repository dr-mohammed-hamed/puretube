import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("key.properties").takeIf { it.exists() }
    ?: rootProject.file("PureTube_Master_Secrets/01_android_keys/key.properties").takeIf { it.exists() }
val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.reader(Charsets.UTF_8)?.use { load(it) }
}

fun getKeystoreProp(key: String): String? {
    return keystoreProperties.getProperty(key)
        ?: keystoreProperties.entries.firstOrNull { it.key.toString().trim() == key }?.value?.toString()?.trim()
}

android {
    namespace = "com.dr.tech.puretube"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dr.tech.puretube"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile != null && keystorePropertiesFile.exists()) {
                val storeFilePath = getKeystoreProp("storeFile")
                val resolvedStoreFile = if (storeFilePath != null) {
                    val candidate = rootProject.file(storeFilePath)
                    if (candidate.exists()) candidate else file(storeFilePath)
                } else null

                storeFile = resolvedStoreFile
                storePassword = getKeystoreProp("storePassword")
                keyAlias = getKeystoreProp("keyAlias")
                keyPassword = getKeystoreProp("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (keystorePropertiesFile != null && keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3 ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // NewPipeExtractor & Network
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)

    // Koin Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Coil Image Loading
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}