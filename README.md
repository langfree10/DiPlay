# DiPlay · 第七代伊兰特适配 fork

本仓库 fork 自 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay)，为 **2021 款第七代伊兰特 / Humax BHCN-T3103C / Android 9** 维护有线 CarPlay 适配。

> **伊兰特的源码和完整说明在 [`elantra` 分支](https://github.com/langfree10/DiPlay/tree/elantra)。** `main` 保留上游跟踪代码；本首页用于指引到适配版本。伊兰特实现以已测试的上游 0.2.21 快照为基线，当前候选版本为 `0.2.21-elantra-test12`。

## 我们做了哪些适配

- **方向盘切歌**：适配 274/275 按键，合并窗口和媒体会话的重复转发，修复单按切两首与吞键。
- **蓝牙音乐与 Siri**：配合原车 MODE 的蓝牙音频，增加助手临时焦点、结束后音源恢复和播放诊断；Siri 回复无声仍在验证。
- **导航音质**：“兼容／清晰优先／48 kHz PCM”三档协商，实际收到的采样率单独显示，保留上次连接记录并自动刷新。
- **显示与车型标识**：直角全屏，默认伊兰特名称与现代车辆按钮图标。
- **USB**：0 mA 数据模式请求、电流档位升序、插线重连配置与连接阶段诊断优化。
- **无 Wi-Fi 车机**：USB 为主要连接入口，隐藏不适用的无线和 BYD 专属界面，保留 USB 必需的本地网络能力。
- **离线诊断**：可导出到 U 盘／文件夹，适合车机无法联网的情况。
- **视频参数**：帧率、编码偏好与实际接收格式诊断。

[完整适配清单与使用说明](https://github.com/langfree10/DiPlay/blob/elantra/README.md) · [构建指南](https://github.com/langfree10/DiPlay/blob/elantra/docs/BUILD_ELANTRA.md) · [验证记录](https://github.com/langfree10/DiPlay/blob/elantra/docs/ELANTRA_AUDIO_TEST.md)

## 验证状态

车主反馈 test10 的蓝牙音乐、音量、方向盘单次切歌、导航声道及 Siri 后恢复蓝牙音源正常；导航日志为 **24 kHz 双声道 PCM**。

**test11/12 的 Siri 回复播放方案和 test12 的 48 kHz PCM 档位仍待实车确认。** 无线转接盒和其他车机不视为已支持。本地 test12 检查为 3192 项通过、1 项原有跳过，另三项已知本机网络用例按车主要求排除；不据此声明所有远端 CI 或实车功能均通过。

源码／CI 默认不含配件认证身份。本地运行私钥、Android 签名密钥、个人日志、第三方 APK、固件和实车安装包不随这次源码提交上传。

## 来源与许可

直接上游为 [DiPlay](https://github.com/shihabal3amri/DiPlay)，接收器基于 [xcertplay](https://github.com/shilapi/xcertplay)，上游界面参考 [DiAuto](https://github.com/shihabal3amri/DiAuto)。保留 [GPL-3.0](LICENSE)、原有 AGPL 标记及[第三方说明](docs/THIRD_PARTY_NOTICES.md)。这是独立维护的适配，现代及 Apple 的名称、图标不代表官方授权或背书。

English: Hyundai Elantra adaptations live on the [elantra branch](https://github.com/langfree10/DiPlay/tree/elantra); main retains upstream tracking code. See that branch for the tested 0.2.21-based source, build instructions, credits and explicit physical-test limitations.
