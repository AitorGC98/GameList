plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //Dependencia para Google Services
    id("com.google.gms.google-services") version "4.4.2" apply true
    id("com.google.firebase.crashlytics") version "2.9.8" apply true
}

android {
    namespace = "com.agc.gamelist"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.agc.gamelist"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding= true
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

    // TODO: Add the dependencies for Firebase products you want to use
    // implementacion de firebase analitica
    implementation("com.google.firebase:firebase-analytics")
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    // Add the dependency for the Firebase Authentication library
    implementation("com.google.firebase:firebase-auth")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth-ktx")

    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    implementation ("androidx.activity:activity-ktx:1.9.2")

    // Dependencias de
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    //Reproductor de youtube
    implementation ("com.pierfrancescosoffritti.androidyoutubeplayer:core:11.1.0")

    implementation ("com.airbnb.android:lottie:5.0.3") // Asegúrate de usar la última versión

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    implementation("com.google.android.gms:play-services-auth:20.6.0") // Or latest version

    // Declare the dependency for the Cloud Firestore library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-firestore")
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    implementation ("com.squareup.picasso:picasso:2.8")

    //implementacion layout foros
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    // Add the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation ("com.google.firebase:firebase-crashlytics-ktx")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.jaredrummler:material-spinner:1.3.1")
    implementation ("androidx.preference:preference:1.2.1")



}