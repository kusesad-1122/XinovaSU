@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.apksign)
    // Paparazzi：JVM 上用 LayoutLib 把 Compose 渲染成 PNG（无需设备/模拟器）。
    // 2.0.0-alpha05.1 是首个支持 AGP 9.x 的版本。产物落在
    // app/src/test/snapshots/images/，仅供视觉回归，不进 APK。
    alias(libs.plugins.paparazzi)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileSdkVersionMinor: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra
val isPrBuild = project.findProperty("IS_PR_BUILD")?.toString()?.toBoolean() ?: false

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

val baseCFlags = listOf(
    "-Wall", "-Qunused-arguments", "-fvisibility=hidden", "-fvisibility-inlines-hidden",
    "-fno-exceptions", "-fno-stack-protector", "-fomit-frame-pointer",
    "-Wno-builtin-macro-redefined", "-Wno-unused-value", "-D__FILE__=__FILE_NAME__"
)
val baseCppFlags = baseCFlags + "-fno-rtti"

android {
    namespace = "com.xinsu.moe"

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_CXX_FLAGS_DEBUG=-Og", "-DCMAKE_C_FLAGS_DEBUG=-Og")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            if (isPrBuild) applicationIdSuffix = ".dev"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.get().asFile.absolutePath}/symbols"
                    arguments += "-DCMAKE_BUILD_TYPE=Release"

                    val releaseFlags = listOf(
                        "-flto", "-ffunction-sections", "-fdata-sections", "-Wl,--gc-sections",
                        "-fno-unwind-tables", "-fno-asynchronous-unwind-tables", "-Wl,--exclude-libs,ALL"
                    )
                    val configFlags = listOf("-Oz", "-DNDEBUG").joinToString(" ")

                    cppFlags += releaseFlags
                    cFlags += releaseFlags

                    arguments += listOf(
                        "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                        "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,--icf=all -s -Wl,--hash-style=sysv -Wl,-z,norelro"
                    )
                }
            }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        prefab = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion
    ndkVersion = androidCompileNdkVersion

    defaultConfig {
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName

        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
                cFlags += baseCFlags + "-std=c2x"
                cppFlags += baseCppFlags + "-std=c++2b"
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    lint {
        // 崩溃风险（MissingPermission、WrongThread 等）必须在 CI 就拦住 ——
        // 这类问题在编译期完全不可见，只在真机运行时才暴露。
        abortOnError = true
        checkReleaseBuilds = false
        warningsAsErrors = true
        // 只对「改了就是错」的规则用 error，其余保持默认 warning。
        // 全部开 error 会让存量问题把 CI 卡死，规则要逐个加而不是一把梭。
        error += listOf(
            "MissingPermission",        // 缺权限标注
            "UnspecifiedRegisterReceiverFlag",
            "UnspecifiedImmutableFlag", // PendingIntent 缺 mutability flag（Android 12+ 崩）
            "WrongThread",              // 跨线程访问 UI
            "Recycle",                  // 漏回收
            "StaticFieldLeak",          // 静态字段持有 Context
            "ObsoleteSdkInt",           // 废弃的 SDK 判断
            "InlinedApi",               // 用了不该 inline 的 API
        )
        // 资源多语言：项目有 46 个语言目录，覆盖率 0.9%~95% 不等
        // （用 .workbuddy/verify_i18n.py 实测），翻译缺失属内容工作而非代码缺陷。
        disable += "MissingTranslation"

        // 以下为 lint 首次全量运行的分类结论，详见 docs/LINT_TRIAGE.md。
        // 原则：lint 输出里只保留真信号。提示类（"有新版"）、设计决策类、以及
        // 需要人工判断的规则一律关掉 —— 让它们红只会训练团队忽略整个 lint 输出。
        disable += listOf(
            // 提示类：有新版本可用
            "NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion",
            // 设计决策：图标是否铺满方形、是否用圆形变体、是否需要单色版
            "IconLauncherShape", "IconDuplicates", "MonochromeLauncherIcon",
            // 历史遗留：标了 translatable=false 却仍在翻译目录里
            "Untranslatable",
            // 需人工确认是否动态引用（getIdentifier 之类），误删风险大于减体积收益
            "UnusedResources",
            // 排版/内容优化建议
            "TypographyEllipsis", "PluralsCandidate",
            // minSdk 31 下 mipmap-anydpi-v26 目录多余，留给专门的资源清理任务
            "ObsoleteSdkInt",
            // 间接数据流：BgEffectPainter 有 @RequiresApi(TIRAMISU) 且调用链上游有
            // isRuntimeShaderSupported() 守卫，运行时安全，lint 跟不穿这层间接
            "NewApi",
            // Compose 最佳实践建议（逐条核实过，无一是缺陷）：
            //  ModifierParameter                 Composable 参数顺序建议
            //  LocalContextResourcesRead         主题图在切换主题时整个 Composable
            //                                     重组，资源对象不会变，读法安全
            //  ModifierNodeInspectableProperties 调试用修饰符属性声明
            //  AutoboxingStateCreation          mutableStateOf(0f) 的装箱开销，
            //                                     可换 mutableFloatStateOf 但收益极小
            "ModifierParameter",
            "LocalContextResourcesRead",
            "ModifierNodeInspectableProperties",
            "AutoboxingStateCreation",
        )
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(listOf("META-INF/**", "kotlin/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "XinovaSU_${managerVersionName}_${managerVersionCode}"
    )
}

val checkThemeSurfaceGuards by tasks.registering(org.gradle.api.tasks.Exec::class) {
    workingDir(rootProject.projectDir.parentFile)
    commandLine("python3", "scripts/check_theme_surface_guards.py")
}

val checkThemeCatalog by tasks.registering(org.gradle.api.tasks.Exec::class) {
    workingDir(rootProject.projectDir.parentFile)
    commandLine("python3", "scripts/check_theme_catalog.py")
}

if (isPrBuild) {
    tasks.matching { it.name == "assembleRelease" }.configureEach {
        dependsOn("testDebugUnitTest", checkThemeSurfaceGuards, checkThemeCatalog)
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    // 无设备截图：LayoutLib 在 JVM 上渲染 Compose，产物供视觉回归比对。
    testImplementation(libs.paparazzi)

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    implementation(libs.androidx.webkit)

    implementation(libs.lsposed.cxx)

    implementation(libs.hiddenapibypass)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation(libs.material.kolor)

    implementation(libs.appiconloader)
}
