import java.time.LocalDate
import java.time.ZoneOffset

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// Build identity: versionCode from the CI run number (monotonic), versionName = semver +
// "<run>-<shortSha>" so each build is traceable. Falls back to a local "dev" build off git.
private val versionBase = "1.0"
private val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
private val gitShortSha = (System.getenv("GITHUB_SHA")?.take(7))
    ?: runCatching {
        providers.exec { commandLine("git", "rev-parse", "--short=7", "HEAD") }
            .standardOutput.asText.get().trim()
    }.getOrNull()?.ifBlank { null } ?: "local"
private val buildId = if (ciRunNumber != null) "$ciRunNumber-$gitShortSha" else "dev-$gitShortSha"
private val buildDateUtc = LocalDate.now(ZoneOffset.UTC).toString()

android {
    namespace = "se.mindphaser.skytte"
    compileSdk = 36

    defaultConfig {
        applicationId = "se.mindphaser.skytte"
        minSdk = 36
        targetSdk = 36
        versionCode = ciRunNumber ?: 1
        versionName = "$versionBase ($buildId)"
        buildConfigField("String", "BUILD_DATE", "\"$buildDateUtc\"")
    }

    signingConfigs {
        create("release") {
            // Populated from environment variables in CI; absent for local/debug builds.
            System.getenv("RELEASE_KEYSTORE_PATH")?.let { storeFile = file(it) }
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only sign when the keystore is provided (CI); otherwise the APK is left unsigned.
            if (System.getenv("RELEASE_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
}
