plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ciBuild = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 3

android {
    namespace = "com.buddy.jizhang"
    compileSdk = 34

    signingConfigs {
        create("fixed") {
            storeFile = file("keystore.p12")
            storeType = "PKCS12"
            storePassword = "jizhang2026"
            keyAlias = "jizhangbuddy"
            keyPassword = "jizhang2026"
        }
    }

    defaultConfig {
        applicationId = "com.buddy.jizhang"
        minSdk = 26
        targetSdk = 34
        versionCode = ciBuild
        versionName = "1.2.0"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            signingConfig = signingConfigs.getByName("fixed")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
}
