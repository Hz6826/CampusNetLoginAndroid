import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release 签名通过环境变量注入（CI 用 GitHub Secrets）；本地没有这些变量时构建不签名。
val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("KEY_ALIAS")
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

// versionCode 由仓库根目录 version.properties 维护（CI 发版后由 GitHub Actions 自动 +1），
// versionName 仍从 git tag 读取：CI 用 GITHUB_REF_NAME，本地回退到最近的 v* tag。
// 必须用 providers.exec：ProcessBuilder 是 configuration time 的外部进程，会让 configuration cache 存储失败。
val gitTag: String? = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrNull()

fun versionNameFromTag(gitTag: String?): String {
    val ciTag = System.getenv("GITHUB_REF_NAME")
    if (!ciTag.isNullOrBlank() && ciTag.startsWith("v")) return ciTag.removePrefix("v")
    val tag = gitTag.orEmpty().trim()
    return if (tag.startsWith("v")) tag.removePrefix("v") else "2.0.0"
}

fun versionCodeFromFile(): Int {
    val file = rootProject.file("version.properties")
    return runCatching {
        val properties = Properties()
        file.inputStream().use { properties.load(it) }
        properties.getProperty("versionCode")?.trim()?.toIntOrNull()
    }.getOrNull()?.takeIf { it > 0 } ?: 1
}

val appVersionName = versionNameFromTag(gitTag)
val appVersionCode = versionCodeFromFile()

android {
    namespace = "com.hz6826.campusnetlogin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hz6826.campusnetlogin"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.kyant.backdrop)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
