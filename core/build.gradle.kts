plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = AndroidConfig.compileSdk

    defaultConfig {
        minSdk = AndroidConfig.minSdk
    }

    namespace = "keiyoushi.core"

    buildFeatures {
        resValues = false
        shaders = false
    }

    compileOptions {
        // Enable core library desugaring for newer Java APIs
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        // Enable incremental compilation and other optimizations
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xexplicit-api=strict"
        )
    }
}

dependencies {
    compileOnly(versionCatalogs.named("libs").findBundle("common").get())

    // Add desugar library if needed for Java 8+ APIs
    // coreLibraryDesugaring(libs.desugar.jdk.libs)  // Uncomment if available in libs.versions.toml
}
