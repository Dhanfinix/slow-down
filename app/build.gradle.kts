import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.slowdown.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.slowdown.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    // Release signing reads a properties file outside the repo. Path: -PslowdownKeystoreProps=... or
    // env SLOWDOWN_KEYSTORE_PROPS, default ~/.slowdown-release/keystore.properties.
    // shortcut: when the file is absent the release APK is built unsigned, so a clean clone still builds.
    val ksProps = Properties()
    val ksFile = File(
        (project.findProperty("slowdownKeystoreProps") as String?)
            ?: System.getenv("SLOWDOWN_KEYSTORE_PROPS")
            ?: "${System.getProperty("user.home")}/.slowdown-release/keystore.properties"
    )
    if (ksFile.isFile) ksFile.inputStream().use { ksProps.load(it) }
    signingConfigs {
        if (ksProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(ksProps.getProperty("storeFile"))
                storePassword = ksProps.getProperty("storePassword")
                keyAlias = ksProps.getProperty("keyAlias")
                keyPassword = ksProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }
    lint { abortOnError = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(project(":throttle"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.nav)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
