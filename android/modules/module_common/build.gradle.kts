plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "github.tornaco.android.thanos.module.common"
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
}

dependencies {
    implementation(libs.rxjava)
    implementation(libs.guava.android)
    
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.extensions)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference)
    implementation(libs.constraint.layout)
    implementation(libs.browser)
    implementation(libs.datastore.preferences)

    implementation(libs.tinypinyin)

    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.navigation.compose)
    kapt(libs.dagger.hilt.android.compiler)
    implementation(libs.dagger.hilt.android)

    implementation(libs.glide)
    implementation(libs.glide.landscapist)
    kapt(libs.glide.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.viewbinding)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.androidx.adaptive)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material.icons.core)
    implementation(libs.navigation.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.navigation.animation)

    implementation(libs.compose.markdown)

    implementation(libs.lottie.compose)
    implementation(libs.lottie)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.storage)

    compileOnly(files(project.rootProject.file("android_sdk/xposed-api-82.jar")))

    implementation(project(":android_framework:base"))
    implementation(project(":android_framework:res"))
    implementation(project(":third_party:recyclerview-fastscroll"))
    implementation(project(":third_party:search"))
    implementation(project(":third_party:dateformatter"))
    implementation(project(":third_party:remix"))
    implementation(project(":third_party:reorderable"))


    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}