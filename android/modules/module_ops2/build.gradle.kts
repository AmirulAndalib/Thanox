import tornaco.project.android.thanox.Configs.resPrefix

plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "github.tornaco.thanos.android.ops2"

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
        renderScript = false
        resValues = false
        shaders = false
        viewBinding = true
        dataBinding = true
    }

    resourcePrefix = resPrefix
}
dependencies {
    implementation(libs.accompanist.swiperefresh)

    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.navigation.compose)
    kapt(libs.dagger.hilt.android.compiler)
    implementation(libs.dagger.hilt.android)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference)
    implementation(libs.constraint.layout)

    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.guava.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.glide.landscapist)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.navigation.compose)
    kapt(libs.dagger.hilt.android.compiler)
    implementation(libs.dagger.hilt.android)

    implementation(project(":modules:module_common"))
    implementation(project(":android_framework:res"))
    implementation(project(":android_framework:base"))
    implementation(project(":third_party:remix"))
}