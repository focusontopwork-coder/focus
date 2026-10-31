import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Push on Android needs the app's google-services.json from Firebase
// (Project settings → add Android app "de.focusontop.client").
// Without the file the app still builds and runs — just without push.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Visual-QA only: a desktop (JVM) target of the SAME shared Compose UI
    // used on Android/iOS, so screens can be rendered and screenshotted
    // headlessly (no Mac, no device, no simulator) to compare pixel-for-
    // pixel against Figma. Never shipped to users; see :composeApp:run.
    jvm("desktop")

    // Real client target: the shared UI compiled to Kotlin/Wasm and run in
    // the browser, served to customers at focusontop.de/mobile-web. This
    // replaces nothing on Android/iOS/desktop above — those stay untouched.
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "focusOnTop"
        browser {
            commonWebpackConfig {
                outputFileName = "focusOnTop.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8090
                }
            }
        }
        binaries.executable()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.ui)
            implementation(libs.media3.transformer)
            implementation(libs.media3.effect)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.video)
            implementation(libs.camerax.view)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play)
            implementation(libs.googleid)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }
        val wasmJsMain by getting
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "de.focusontop.client.res"
    generateResClass = always
}

// Runs the desktop visual-QA harness (see composeApp/src/desktopMain/.../Main.kt):
// `SCREEN=Login LANG_CODE=ar ./gradlew :composeApp:desktopRun`. Headless CI uses
// this + Xvfb + ImageMagick to screenshot every screen without a device/simulator.
tasks.register<JavaExec>("desktopRun") {
    group = "qa"
    description = "Run the desktop screenshot-harness entry point (SCREEN/LANG_CODE env vars)."
    val compilation = kotlin.targets.getByName("desktop").compilations
        .getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
    dependsOn(compilation.compileTaskProvider)
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles!!
    mainClass.set("de.focusontop.client.MainKt")
}

android {
    namespace = "de.focusontop.client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.focusontop.client"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Fixed test signing key (kept in the project on purpose: it is only for
    // test builds). Its SHA-1 is registered in Firebase for Google sign-in,
    // so every Mac that builds the app gets a working Google button.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
