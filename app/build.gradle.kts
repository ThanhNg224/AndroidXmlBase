plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.example.androidxmlbase"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.androidxmlbase"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://example.com/\"")
        buildConfigField("boolean", "API_ENABLE_LOGGING", "false")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.shimmer)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ktlint {
    android = true
    outputToConsole = true
    filter {
        exclude("**/generated/**")
    }
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "*.core.architecture.result.ResultState*",
                    "*.core.architecture.result.DomainResult*",
                    "*.core.architecture.StateViewModel*",
                    "*.core.architecture.DefaultAppDispatchers",
                    "*.core.localization.LocaleManager*",
                    "*.core.localization.LocaleTagMapper*",
                    "*.core.navigation.NavigationOptions*",
                    "*.core.network.ApiResult*",
                    "*.core.network.RetrofitApiClient*",
                    "*.core.network.auth.SecureStoreAuthTokenProvider*",
                    "*.core.network.transfer.TransferResult*",
                    "*.core.network.transfer.ProgressRequestBody*",
                    "*.core.network.transfer.OkHttpFileTransferClient*",
                    "*.core.network.auth.*",
                    "*.core.network.connectivity.*",
                    "*.core.network.transfer.*",
                    "*.core.storage.settings.DataStoreSettingsStore*",
                    "*.core.storage.settings.SettingsKey*",
                    "*.core.ui.base.Debouncer*",
                    "*.core.ui.base.ResultRenderState*",
                    "*.core.ui.util.ShapeUtils*",
                    "*.feature.demo.data.mapper.*",
                    "*.feature.demo.domain.usecase.FetchDemoMessageUseCase",
                    "*.feature.demo.domain.usecase.IncrementCounterUseCase",
                    "*.feature.demo.domain.usecase.ObserveDemoCountUseCase",
                    "*.feature.demo.domain.usecase.SaveDemoCountUseCase",
                    "*.feature.demo.presentation.viewmodel.DemoViewModel",
                    "*.feature.designsystem.presentation.viewmodel.DesignSystemViewModel",
                )
            }
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*.databinding.*",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_MembersInjector*",
                    "*Hilt_*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    "*.AndroidXmlBaseApplication",
                    "*.MainActivity",
                    "*Activity",
                    "*Fragment",
                    "*DialogFragment",
                    "*.core.ui.components.*",
                )
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
