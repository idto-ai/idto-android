import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val demoProperties: Properties = Properties().apply {
    val file = project.file("demo.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun demoCredential(key: String): String =
    System.getenv(key)
        ?: (project.findProperty(key) as String?)
        ?: demoProperties.getProperty(key)
        ?: ""

android {
    namespace = "ai.idto.sdk.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.idto.sdk.example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "IDTO_DEMO_CLIENT_ID", "\"${demoCredential("IDTO_DEMO_CLIENT_ID")}\"")
        buildConfigField("String", "IDTO_DEMO_CLIENT_SECRET", "\"${demoCredential("IDTO_DEMO_CLIENT_SECRET")}\"")
        buildConfigField("String", "IDTO_DEMO_WORKFLOW_ID", "\"${demoCredential("IDTO_DEMO_WORKFLOW_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(project(":sdk"))

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
