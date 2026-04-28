plugins {
    alias(libs.plugins.android.application)
}

val hasGoogleServicesJson = file("google-services.json").exists()

// Load .env file for Hugging Face token
fun loadEnvFile(): Map<String, String> {
    val envMap = mutableMapOf<String, String>()
    val envFile = file("../.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    envMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }
    return envMap
}

val env = loadEnvFile()
val huggingFaceToken = env["HUGGING_FACE_TOKEN"] ?: "PLACEHOLDER_TOKEN"

android {
    namespace = "com.example.smartscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartscan"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasGoogleServicesJson.toString())
        buildConfigField("String", "HUGGING_FACE_TOKEN", "\"$huggingFaceToken\"")
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
        buildConfig = true
    }
}

dependencies {
    // Firebase BoM and libraries from catalog
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // ML Kit
    implementation(libs.mlkit.text.recognition)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    
    // Google Auth
    implementation(libs.play.services.auth)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)
}

if (hasGoogleServicesJson) {
    apply(plugin = "com.google.gms.google-services")
}
