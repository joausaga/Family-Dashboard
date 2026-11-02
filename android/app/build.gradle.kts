import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Production signing material is stored locally outside the repository and is not committed.
val productionSigningPropertiesFile = File(System.getProperty("user.home"), ".android/family-dashboard-release.properties")
val productionSigningProperties = Properties().apply {
    if (productionSigningPropertiesFile.isFile) {
        productionSigningPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.joausaga.familydashboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.joausaga.familydashboard"
        minSdk = 22 // Fire OS 5.1
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        // El RTC del marco no conserva la hora: cualquier fecha anterior a la compilación es falsa.
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    }

    signingConfigs {
        if (productionSigningPropertiesFile.isFile) {
            create("production") {
                storeFile = file(productionSigningProperties.getProperty("storeFile"))
                storeType = productionSigningProperties.getProperty("storeType")
                keyAlias = productionSigningProperties.getProperty("keyAlias")
                storePassword = productionSigningProperties.getProperty("storePassword")
                keyPassword = productionSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"https://api.open-meteo.com/\"")
            manifestPlaceholders["cleartext"] = "false"
            if (productionSigningPropertiesFile.isFile) {
                signingConfig = signingConfigs.getByName("production")
            }
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api.open-meteo.com/\"")
            manifestPlaceholders["cleartext"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("com.airbnb.android:lottie:6.7.1")
    // Identity Authorization API: permisos OAuth para Calendar/Tasks desde Google Play Services.
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    testImplementation("junit:junit:4.13.2")
}
