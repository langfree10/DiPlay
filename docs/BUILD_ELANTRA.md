# 构建伊兰特适配版

源码位于 [elantra 分支](https://github.com/langfree10/DiPlay/tree/elantra)，基于上游 0.2.21 的已测试快照。`main` 保留上游跟踪代码，不能据其源码判断伊兰特适配已包含。

## 环境

使用 JDK 25、Android SDK Platform 37、Build-Tools 36.0.0、Platform-Tools 和 NDK 28.2.13676358。使用仓库中的 Gradle wrapper，并设置 `JAVA_HOME`、`ANDROID_HOME` 或本地 `local.properties`。详细环境说明见[上游构建指南](BUILD.md)。

```sh
git clone --branch elantra git@github.com:langfree10/DiPlay.git
cd DiPlay
./gradlew :mobile:assembleDebug
```

主应用输出为 `mobile/build/outputs/apk/debug/mobile-debug.apk`，开发包名为 `com.shihab.diplay.hudtest`，开发版本为 `1.0.0-dev`。

## 认证与签名资料

普通源码／CI 构建不包含配件认证身份。独立连接 iPhone 的实车构建需要按[上游指南](BUILD.md#build-a-standalone-car-test-apk)准备外部运行资料，并显式设置 `DIPLAY_AUTH_ASSETS_DIR`。相关私钥、证书容器和 Android 签名资料保持在仓库外。

```sh
DIPLAY_AUTH_ASSETS_DIR=/absolute/path/to/runtime-assets ./gradlew :mobile:assembleStandaloneDebug
```

不要把本地密钥、运行目录、日志或实车 APK 放入源码提交。没有配置外部身份的源码 APK 与本地实车测试 APK 的运行输入不同；构建成功不等于已具有独立连接所需的身份。

## 完整检查

GitHub Actions 对 `main`、`elantra` 的推送和 PR 使用上游完整检查，**不排除网络用例**：

```sh
python3 scripts/check_public_tree.py
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug
```

车主本机存在三个已知网络测试问题，车主明确要求后续本地检查跳过它们。仅在复现这套本地验证时使用下面两个 init 脚本；其他环境应先运行完整检查：

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :home:testDebugUnitTest \
  :mobile:lintDebug :home:lintDebug :maphost:lintDebug \
  :mobile:assembleDebug :home:assembleDebug :maphost:assembleDebug \
  --init-script scripts/elantra-local-network-exclusions.init.gradle \
  --init-script scripts/elantra-test-workers.init.gradle --max-workers=2
```

第一个脚本仅排除[验证记录](ELANTRA_AUDIO_TEST.md)列出的三个方法；第二个每 40 个测试类重启 Robolectric worker，控制多 SDK 测试的堆积内存，不筛选用例。

本地 Kotlin/Java 调试还可通过 `DIPLAY_PREBUILT_NATIVE_DIR` 指向已有的未修改 JNI 库目录。未指定时仍由 NDK 从仓库源码构建；预编译库不随本仓库分发。test12 本地验证使用了外部运行身份和这项 JNI 复用，因此不把本地结果当作所有机器或 GitHub CI 的已通过声明。

## 覆盖安装

覆盖安装需要使用与已安装 APK 相同的 Android 签名，并保持版本代码不低于现有版本。另一台机器新生成的默认 debug keystore 通常不同，不能据此保证覆盖已有车测 APK；签名密钥由维护者本地保存。

## 1.0.0 正式安装包

正式构建保持 `com.shihab.diplay.hudtest`，以兼容车上已安装版本；名称为“DiPlay 伊兰特”，versionCode 为 52，versionName 为 1.0.0。发布变体关闭调试，不包含 debug 目录下的测试组件。上游普通 release 包名说明不适用于本 fork 的伊兰特发布变体。

显式设置上述运行资料，以及 `ANDROID_KEYSTORE_PATH`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD` 后运行：

```sh
./gradlew :mobile:assembleStandaloneRelease :mobile:lintRelease
```

输出 `mobile/build/outputs/apk/release/mobile-release.apk`。为了覆盖旧测试包，必须继续使用旧包的签名密钥；仅修改名称或重新生成密钥不能实现覆盖。首次正式版继续使用本机测试阶段的原签名，以保留升级兼容性；“正式版”不表示更换了签名身份。运行资料及签名文件不进入 Git 或公开附件。
