plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.yusufaf.duckviewer"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.yusufaf.duckviewer"
        // RoleManager.ROLE_BROWSER needs API 29.
        minSdk = 29
        targetSdk = 37
        versionName = "0.1.0" // x-release-please-version

        // release-please bumps versionName; versionCode follows it so a
        // semver bump is always an upgrade on the phone. Only the numeric core
        // counts, so a prerelease suffix like -rc.1 doesn't break the build.
        val (major, minor, patch) = Regex("""(\d+)\.(\d+)\.(\d+)""")
            .find(versionName!!)!!.destructured.toList().map(String::toInt)
        // 0.0.0 is the unreleased placeholder; Android rejects versionCode 0.
        versionCode = maxOf(major * 10_000 + minor * 100 + patch, 1)

        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    // The release key comes from CI secrets (see README). Without it the release
    // APK is left unsigned rather than falling back to a debug key, which would
    // produce an APK that can't update the installed one.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    if (keystoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Installs next to the release app, so testing a debug build never
        // replaces the signed install or its default-browser role.
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Duck Viewer (debug)"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
}
