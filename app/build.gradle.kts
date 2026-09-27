plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseUrl = providers.gradleProperty("CERITARIA_SUPABASE_URL")
    .orElse(providers.environmentVariable("CERITARIA_SUPABASE_URL"))
    .orElse("")
val supabaseKey = providers.gradleProperty("CERITARIA_SUPABASE_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("CERITARIA_SUPABASE_PUBLISHABLE_KEY"))
    .orElse("")

android {
    namespace = "com.flyonz.ceritaria.studio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.flyonz.ceritaria.studio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.get().asBuildConfigString())
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", supabaseKey.get().asBuildConfigString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.viewmodel.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.androidx)
    implementation(libs.work.runtime.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.media3.common)
    implementation(libs.media3.inspector)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
