plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Read configuration from gradle.properties
val appPackageName: String by project
val appVersionCode: String by project
val appVersionName: String by project

android {
    namespace = "com.adapter.logreader"  // Keep static for R class
    compileSdk = 34

    defaultConfig {
        applicationId = appPackageName
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode.toInt()
        versionName = appVersionName

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "/Users/zeno/Documents/store.jks"
            val keystorePass = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAliasName = System.getenv("KEY_ALIAS") ?: "lvalen91"
            val keyPass = System.getenv("KEY_PASSWORD") ?: keystorePass

            storeFile = file(keystorePath)
            storePassword = keystorePass
            keyAlias = keyAliasName
            keyPassword = keyPass
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // SSH - JSch maintained fork
    implementation("com.github.mwiede:jsch:0.2.13")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DocumentFile for SAF
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
