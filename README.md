# DiPlay · 第七代伊兰特适配 fork

本仓库 fork 自 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay)，为 **2021 款第七代伊兰特 / Humax BHCN-T3103C / Android 9** 维护有线 CarPlay 适配。

> **伊兰特的源码和完整说明在 [`elantra` 分支](https://github.com/langfree10/DiPlay/tree/elantra)。** `main` 保留上游跟踪代码；本首页用于指引到适配版本。伊兰特实现以已测试的上游 0.2.21 快照为基线，首个正式版本为 **1.0.0**（2026-10-23）。

## 我们做了哪些适配

- **方向盘切歌**：适配 274/275 按键，合并窗口和媒体会话的重复转发，修复单按切两首与吞键。
- **蓝牙音乐与 Siri**：配合原车 MODE 的蓝牙音频，增加助手临时焦点、结束后音源恢复和播放诊断；Siri 回复及蓝牙音源恢复已实车验证。
- **导航音质**：“兼容／清晰优先／48 kHz PCM”三档协商，实际收到的采样率单独显示，保留上次连接记录并自动刷新。
- **显示与车型标识**：直角全屏，默认伊兰特名称与现代车辆按钮图标。
- **USB**：0 mA 数据模式请求、电流档位升序、插线重连配置与连接阶段诊断优化。
- **无 Wi-Fi 车机**：USB 为主要连接入口，隐藏不适用的无线和 BYD 专属界面，保留 USB 必需的本地网络能力。
- **离线诊断**：可导出到 U 盘／文件夹，适合车机无法联网的情况。
- **视频参数**：帧率、编码偏好与实际接收格式诊断。

[完整适配清单与使用说明](https://github.com/langfree10/DiPlay/blob/elantra/README.md) · [构建指南](https://github.com/langfree10/DiPlay/blob/elantra/docs/BUILD_ELANTRA.md) · [验证记录](https://github.com/langfree10/DiPlay/blob/elantra/docs/ELANTRA_AUDIO_TEST.md)

## 验证状态

2026-10-23 完成 test12 实车验证，发布首个正式版本 **DiPlay 伊兰特 1.0.0**：

- 蓝牙音乐、音量、方向盘单次切歌、导航声道及 Siri 结束后恢复蓝牙音源正常。
- **48 kHz 导航实车生效**：晚间 77 次导航音轨初始化均为 48 kHz 双声道 LPCM，实际播放器采样率一致。
- **Siri 回复可听到**：日志同时记录有效非静音数据、正常播放进度；4 次蓝牙音源恢复均收到原厂焦点确认。
- 0 mA 请求、直角全屏已实车验证。

正式包名称不再带 test，支持覆盖 test12 并保留设置。本次保持连接与音频逻辑不变，使用关闭调试的发布构建。[1.0.0 发布说明](https://github.com/langfree10/DiPlay/blob/elantra/docs/RELEASE-NOTES-ELANTRA-1.0.0.md)。

无线转接盒、其他车机和所有 USB 重连时序尚无足够实车验证。test12 本地检查为 3192 项通过、1 项原有跳过，另三项已知本机网络用例按车主要求排除；正式发布检查见 elantra 分支验证记录，不据此声明远端 CI 已通过。

源码／CI 默认不含配件认证身份。本地运行私钥、Android 签名密钥、个人日志、第三方 APK、固件和实车安装包不随这次源码提交上传。

## 来源与许可

直接上游为 [DiPlay](https://github.com/shihabal3amri/DiPlay)，接收器基于 [xcertplay](https://github.com/shilapi/xcertplay)，上游界面参考 [DiAuto](https://github.com/shihabal3amri/DiAuto)。保留 [GPL-3.0](LICENSE)、原有 AGPL 标记及[第三方说明](docs/THIRD_PARTY_NOTICES.md)。这是独立维护的适配，现代及 Apple 的名称、图标不代表官方授权或背书。

English: Hyundai Elantra adaptations live on the [elantra branch](https://github.com/langfree10/DiPlay/tree/elantra); main retains upstream tracking code. See that branch for the tested 0.2.21-based source, build instructions, credits and explicit physical-test limitations.
