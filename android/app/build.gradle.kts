import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

fun Properties.loadRawKeyValueFile(file: File) {
    file.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank() || line.startsWith("#")) return@forEachLine
        val separator = line.indexOf('=')
        if (separator <= 0) return@forEachLine
        val key = line.substring(0, separator).trim().removePrefix("\uFEFF")
        setProperty(key, line.substring(separator + 1).trim())
    }
}

val uploadSigningProps = Properties().also { props ->
    val configuredPath = System.getenv("UPLOAD_KEYSTORE_PROPERTIES")
    val candidates = listOfNotNull(
        configuredPath?.takeIf { it.isNotBlank() }?.let { file(it) },
        rootProject.file("upload-keystore.properties"),
        project.file("../upload-keystore.properties")
    )
    candidates.firstOrNull { it.exists() }?.let { props.loadRawKeyValueFile(it) }
}

fun String.asBuildConfigString(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

val geminiApiKeys: String =
    localProps.getProperty("gemini.api.keys")
        ?: localProps.getProperty("gemini.api.key", "")
val isDeviceBenchmark = providers.gradleProperty("deviceBenchmark").orNull == "true"

fun signingValue(name: String, envName: String): String? =
    uploadSigningProps.getProperty(name)
        ?: localProps.getProperty("upload.$name")
        ?: localProps.getProperty(name)
        ?: System.getenv(envName)

val uploadStoreFile = signingValue("storeFile", "UPLOAD_STORE_FILE")
val uploadStorePassword = signingValue("storePassword", "UPLOAD_STORE_PASSWORD")
val uploadKeyAlias = signingValue("keyAlias", "UPLOAD_KEY_ALIAS")
val uploadKeyPassword = signingValue("keyPassword", "UPLOAD_KEY_PASSWORD")
val hasUploadSigning = listOf(
    uploadStoreFile,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.phuongnn14.tuithantai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stevejobvnAIstudio.tuithantai"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "0.17.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://api.your-domain.com\"")
        buildConfigField(
            "String", "GEMINI_API_KEY",
            "\"${geminiApiKeys.asBuildConfigString()}\""
        )
    }

    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (isDeviceBenchmark) {
                applicationIdSuffix = ".dev"
                versionNameSuffix = "-dev"
                resValue("string", "google_app_id", "benchmark")
                resValue("string", "gcm_defaultSenderId", "benchmark")
                resValue("string", "default_web_client_id", "benchmark")
                resValue("string", "google_api_key", "benchmark")
                resValue("string", "google_crash_reporting_api_key", "benchmark")
                resValue("string", "google_storage_bucket", "benchmark")
                resValue("string", "project_id", "benchmark")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasUploadSigning) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    androidResources {
        noCompress += "onnx"
    }
}

if (isDeviceBenchmark) {
    tasks.configureEach {
        if (name == "processDebugGoogleServices") enabled = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // Quantized multilingual semantic model and its fully offline tokenizer.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")
    implementation("ai.djl.huggingface:tokenizers:0.33.0")
    runtimeOnly("ai.djl.android:tokenizer-native:0.33.0")

    // Google Sign-In + Drive backup
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
