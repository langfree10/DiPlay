# DiPlay · 第七代伊兰特适配 fork

本仓库 fork 自 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay)，为 **2021 款第七代伊兰特 / Humax BHCN-T3103C / Android 9** 维护有线 CarPlay 适配。

## 分支约定

- [`main`](https://github.com/langfree10/DiPlay/tree/main)：稳定主分支，包含已经验证的伊兰特适配，当前正式版本为 **1.0.1**。
- [`elantra`](https://github.com/langfree10/DiPlay/tree/elantra)：开发分支，后续改动在此开发，验证通过后再合并到 `main`。

适配基线为上游 DiPlay 0.2.21；仓库中的 `upstream` 远程指向原始 DiPlay 项目。

## 我们做了哪些适配

- **方向盘切歌**：适配 274/275 按键，合并窗口和媒体会话的重复转发，修复单按切两首与吞键。
- **蓝牙音乐与 Siri**：配合原车 MODE 的蓝牙音频，增加助手临时焦点、结束后音源恢复和播放诊断；Siri 回复及蓝牙音源恢复已实车验证。
- **导航音质**：“兼容／清晰优先／48 kHz PCM”三档协商；伊兰特未保存过音质选择时默认 48 kHz PCM，已有选择保留。实际收到的采样率单独显示，保留上次连接记录并自动刷新。
- **显示与车型标识**：直角全屏，默认伊兰特名称与现代车辆按钮图标。
- **USB**：0 mA 数据模式请求、电流档位升序、插线重连配置与连接阶段诊断优化。
- **无 Wi-Fi 车机**：USB 为主要连接入口，隐藏不适用的无线和 BYD 专属界面，保留 USB 必需的本地网络能力。
- **离线诊断**：可导出到 U 盘／文件夹，适合车机无法联网的情况。
- **视频参数**：帧率、编码偏好与实际接收格式诊断。

[稳定版源码](https://github.com/langfree10/DiPlay/tree/main) · [构建指南](https://github.com/langfree10/DiPlay/blob/main/docs/BUILD_ELANTRA.md) · [验证记录](https://github.com/langfree10/DiPlay/blob/main/docs/ELANTRA_AUDIO_TEST.md)

## 验证状态

2026-10-23 完成 test12 实车验证，作为 1.0.0 正式版的验证基础；当前版本为 **DiPlay 伊兰特 1.0.1**：

- 蓝牙音乐、音量、方向盘单次切歌、导航声道及 Siri 结束后恢复蓝牙音源正常。
- **48 kHz 导航实车生效**：晚间 77 次导航音轨初始化均为 48 kHz 双声道 LPCM，实际播放器采样率一致。
- **Siri 回复可听到**：日志同时记录有效非静音数据、正常播放进度；4 次蓝牙音源恢复均收到原厂焦点确认。
- 0 mA 请求、直角全屏已实车验证。

## 来源与许可

直接上游为 [DiPlay](https://github.com/shihabal3amri/DiPlay)，接收器基于 [xcertplay](https://github.com/shilapi/xcertplay)，上游界面参考 [DiAuto](https://github.com/shihabal3amri/DiAuto)。保留 [GPL-3.0](LICENSE)、原有 AGPL 标记及[第三方说明](docs/THIRD_PARTY_NOTICES.md)。这是独立维护的适配，现代及 Apple 的名称、图标不代表官方授权或背书。

English: `main` contains the verified stable Hyundai Elantra adaptation (currently 1.0.1); `elantra` is the development branch. Changes are developed on `elantra` and merged into `main` after validation. The adaptation is based on upstream DiPlay 0.2.21.
