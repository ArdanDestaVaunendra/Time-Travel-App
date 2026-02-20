plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Plugin compose ini opsional kalau kamu mau pake Jetpack Compose juga, kalau error bisa dihapus
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.magictime"
    compileSdk = 35 // KITA PAKAI 35 (Android 14) AGAR STABIL

    defaultConfig {
        applicationId = "com.example.magictime"
        minSdk = 24
        targetSdk = 35 // SAMAKAN DENGAN COMPILE SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // SETTING JAVA VERSION (Kita kunci di Java 11 biar aman)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // FITUR WAJIB
    buildFeatures {
        compose = true      // Biar template bawaan gak error
        viewBinding = true  // WAJIB: Biar kita bisa koding logic Fake Lock Screen pakai XML
    }
}

dependencies {
    // --- LIBRARY BAWAAN (JANGAN DIHAPUS) ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- TAMBAHAN KHUSUS MAGIC APP (WAJIB ADA) ---
    // Library ini supaya Activity 'Fake Lock Screen' bisa pakai XML biasa (ImageView/TextView)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("androidx.gridlayout:gridlayout:1.1.0")
    implementation("com.google.android.gms:play-services-ads:24.9.0")

    // --- TESTING (BIARKAN SAJA) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}