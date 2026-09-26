plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android { namespace = "com.archidoc.cartonlocator"; compileSdk = 35
    defaultConfig { applicationId = "com.archidoc.cartonlocator"; minSdk = 23; targetSdk = 35; versionCode = 21; versionName = "2.1" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}
