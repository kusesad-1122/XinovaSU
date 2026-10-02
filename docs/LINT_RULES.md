# Android Lint 规则 ID 核实清单

> 核实日期：2026-10-02 · 方法：下载 `com.android.tools.lint:lint-checks` 的 jar，
> 在所有 class 的常量池里搜字面量。**不要凭记忆写 issue id** ——
> 写错会在构建时报 `Unknown issue id` 直接失败（本次就踩了）。

## 已验证存在（可用于 `error +=`）

| ID | 作用 | 后果 |
|---|---|---|
| `MissingPermission` | 调用需权限的 API 但没标 `@RequiresPermission` | SecurityException |
| `UnspecifiedImmutableFlag` | PendingIntent 没指定 mutability | **Android 12+ 直接崩** |
| `UnspecifiedRegisterReceiverFlag` | 注册 receiver 没指定 exported | Android 13+ 崩 |
| `WrongThread` | 跨线程访问 UI 元素 | 偶发崩溃 / ANR |
| `Recycle` | 忘记回收 | 内存泄漏 |
| `StaticFieldLeak` | 静态字段持有 Context | 内存泄漏 |
| `ObsoleteSdkInt` | 用了已废弃的 SDK 判断 | 逻辑错误 |
| `InlinedApi` | 用了不该 inline 的 API | 兼容性 |
| `MissingTranslation` | 字符串未翻译（本项目**主动禁用**，见下） | UI 显示 key |

## 已验证不存在（写进去会报 UnknownIssueId）

| 我曾误写的 ID | 说明 |
|---|---|
| `UnsafeOptInUsageError` | Compose 实验性 API 的正确 ID 是 `UnsafeOptInUsageError` 在部分版本存在，**但 32.5.0-alpha08 的 jar 里没有**。若要用需先在 jar 里确认。 |
| `ComposableNaming` | 该规则在 `lint-checks` 里不存在（可能在独立的 compose-lint 模块） |
| `ModifierMissing` | 同上，AGP 内置 lint 无此 ID |

## 本项目的配置决定

```kotlin
lint {
    abortOnError = true
    checkReleaseBuilds = false
    warningsAsErrors = true
    error += listOf( /* 上表 8 条崩溃/泄漏类 */ )
    disable += "MissingTranslation"   // 20+ values-xx，翻译不全属内容工作
}
```

`MissingTranslation` 禁用的理由：`res/` 下有 46 个语言目录，覆盖率从 0.9% 到 53.8%
不等（用 `.workbuddy/verify_i18n.py` 测的）。简中 427/449（95%），
繁中 TW 仅 166/449（37%）。翻译缺失是内容工作，不该让代码 CI 红。

## 当前代码的 Lint 状态

首次成功运行 lintDebug：**0 error、0 warning**（`UnknownIssueId` 那个配置错误不算）。
即这 8 条崩溃类规则在本项目当前代码上都干净 —— 这本身是个有价值的结论：
说明反射调用的都是系统类（`SystemProperties` / `AppOpsManager` / 三星浮窗），
不受 R8 影响。

## 复现核实方法

```bash
# 注意版本号是 32.x 而非 9.x（AGP 8.0 之后 lint 工具独立 versioning）
curl -sSL -o lc.jar \
  https://dl.google.com/dl/android/maven2/com/android/tools/lint/lint-checks/32.5.0-alpha08/lint-checks-32.5.0-alpha08.jar
# 然后在 jar 内所有 .class 的常量池里搜目标 ID 的字面量
```

若要确认当前 AGP 对应的 lint 版本，查 `~/.gradle/caches/modules-2/files-2.1/com.android.tools.lint/lint-checks/`
或跑一次 `./gradlew :app:lintDebug` 后看 `manager/app/build/reports/lint-results-debug.html`。
