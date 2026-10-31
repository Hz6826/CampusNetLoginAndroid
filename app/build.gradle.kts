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

// 版本号从 git tag 读取：CI 用 GITHUB_REF_NAME，本地回退到最近的 v* tag；都没有时用 1.0.0。
fun versionNameFromTag(): String {
    val ciTag = System.getenv("GITHUB_REF_NAME")
    if (!ciTag.isNullOrBlank() && ciTag.startsWith("v")) return ciTag.removePrefix("v")
    val tag = runCatching {
        val process = ProcessBuilder("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        process.waitFor()
        output
    }.getOrDefault("")
    return if (tag.startsWith("v")) tag.removePrefix("v") else "1.0.0"
}

// 语义化 tag（vX.Y.Z 或 vX.Y.Z-beta.N）→ 单调递增的 versionCode。
fun versionCodeFrom(name: String): Int {
    val numbers = name.substringBefore('-').removePrefix("v").split('.')
        .map { it.toIntOrNull() ?: 0 }
    val major = numbers.getOrElse(0) { 0 }
    val minor = numbers.getOrElse(1) { 0 }
    val patch = numbers.getOrElse(2) { 0 }
    return (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}

val appVersionName = versionNameFromTag()
val appVersionCode = versionCodeFrom(appVersionName)

android {
    namespace = "com.hz6826.xjtunetlogin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hz6826.xjtunetlogin"
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
