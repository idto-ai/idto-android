plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val idtoGroupId = "ai.idto"
val idtoArtifactId = "idto-android"
val idtoVersion = "0.1.0"

android {
    namespace = "ai.idto.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testOptions.targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.webkit:webkit:1.12.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.6.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = idtoGroupId
            artifactId = idtoArtifactId
            version = idtoVersion

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("IDto Android SDK")
                description.set("Native Android SDK for IDto KYC verification — hosts the IDto web SDK in a WebView with a Java-first API.")
                url.set("https://github.com/idto-ai/idto-android")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("idto")
                        name.set("IDto")
                        email.set("developer@idto.ai")
                    }
                }
                scm {
                    url.set("https://github.com/idto-ai/idto-android")
                    connection.set("scm:git:https://github.com/idto-ai/idto-android.git")
                    developerConnection.set("scm:git:ssh://git@github.com/idto-ai/idto-android.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/idto-ai/idto-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
