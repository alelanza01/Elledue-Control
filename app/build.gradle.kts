plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "it.elledue.control"
    compileSdk = 35
    defaultConfig {
        applicationId = "it.elledue.control"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.4.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    // The interface explicitly uses Italian, including Material date picker resources.
    // Keep language resources installed even when distributed as an App Bundle.
    bundle { language { enableSplit = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    val externalStore = providers.environmentVariable("ELLEDUE_RELEASE_STORE").orNull ?: providers.gradleProperty("android.injected.signing.store.file").orNull
    val externalAlias = providers.environmentVariable("ELLEDUE_RELEASE_ALIAS").orNull ?: providers.gradleProperty("android.injected.signing.key.alias").orNull
    val externalStorePassword = providers.environmentVariable("ELLEDUE_RELEASE_STORE_PASSWORD").orNull ?: providers.gradleProperty("android.injected.signing.store.password").orNull
    val externalKeyPassword = providers.environmentVariable("ELLEDUE_RELEASE_KEY_PASSWORD").orNull ?: providers.gradleProperty("android.injected.signing.key.password").orNull
    if (listOf(externalStore, externalAlias, externalStorePassword, externalKeyPassword).all { !it.isNullOrBlank() }) {
        val privateStore = file(externalStore!!).canonicalFile
        require(!privateStore.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) { "Il keystore release deve rimanere esterno al repository." }
        signingConfigs.create("externalRelease") {
            storeFile = privateStore
            storePassword = externalStorePassword
            keyAlias = externalAlias
            keyPassword = externalKeyPassword
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("externalRelease")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

// Maven resolver used by Robolectric is independent of Gradle's dependency resolver.
tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    systemProperty("user.home", System.getenv("ANDROID_USER_HOME") ?: System.getProperty("user.home"))
    maxHeapSize = "2g"
    listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort", "javax.net.ssl.trustStore").forEach { name ->
        System.getProperty(name)?.let { systemProperty(name, it) }
    }
}

val verifyReleaseSigning by tasks.registering {
    doLast {
        check(android.signingConfigs.findByName("externalRelease")?.storeFile?.isFile == true) {
            "Firma release non configurata: genera e conserva il keystore sul tuo PC, poi imposta le variabili ELLEDUE_RELEASE_*. Nessuna chiave viene creata nel cloud."
        }
    }
}
tasks.matching { it.name == "packageRelease" }.configureEach { dependsOn(verifyReleaseSigning) }
