import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProp(key: String): String =
    localProps.getProperty(key, "").trim().replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.btday4"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.btday4"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PAYOS_CLIENT_ID", "\"${localProp("PAYOS_CLIENT_ID")}\"")
        buildConfigField("String", "PAYOS_API_KEY", "\"${localProp("PAYOS_API_KEY")}\"")
        buildConfigField("String", "PAYOS_CHECKSUM_KEY", "\"${localProp("PAYOS_CHECKSUM_KEY")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.glide)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
