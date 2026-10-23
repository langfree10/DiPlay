# DiPlay Elantra · 第七代伊兰特适配版

基于 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay) 的个人维护 fork，为 **2021 款第七代伊兰特的安卓车机**适配有线 CarPlay。

- 本仓库：[langfree10/DiPlay](https://github.com/langfree10/DiPlay)
- 伊兰特源码分支：[elantra](https://github.com/langfree10/DiPlay/tree/elantra)；`main` 保留上游跟踪代码
- 适配基线：上游 **0.2.21**，提交 [`6d3b8a8ce927cb4b115b8c3881f17619433e93b1`](https://github.com/shihabal3amri/DiPlay/commit/6d3b8a8ce927cb4b115b8c3881f17619433e93b1)
- 当前正式版本：**1.0.1**（2026-10-23，基于实车验证通过的 test12）
- 应用安装在车机；包名沿用测试版以支持覆盖升级：`com.shihab.diplay.hudtest`

这是独立的伊兰特适配工作，**不代表上游 DiPlay 官方支持现代车型**。上游原始说明保存在[英文原文](docs/DIPLAY_UPSTREAM_README.md)与[中文原文](docs/DIPLAY_UPSTREAM_README.zh-CN.md)。

## 适配对象

实际测试车辆为 2021 款第七代伊兰特，后加装 360，车机没有 Wi-Fi 模块。车型专属默认配置按以下信息匹配：

| 项目 | 当前验证对象 |
| --- | --- |
| Manufacturer | `humaxdigital` |
| Model | `BHCN-T3103C` |
| Android | Android 9 / API 28 |
| 连接方式 | iPhone USB 有线 CarPlay |
| 音乐工作方式 | 原车 MODE 选择“蓝牙音频” |

其他年款、车机型号和固件仍需单独验证。没有新增 360、空调、仪表或车身控制协议。

## 相对上游做了哪些改动

| 方面 | 本 fork 的改动 |
| --- | --- |
| 伊兰特默认配置 | 按准确车机型号初始化有线、全屏、音频焦点等默认值；保留已有音频传输偏好与后续手动设置。 |
| 方向盘切歌 | 适配第三方 CarLife 使用的 274/275 按键，支持松开触发与仅收到松开事件。窗口与媒体会话共享去重，修复单按切两首；通话和未连接状态保留原车处理。 |
| 原车蓝牙音乐 | 移除依赖蓝牙音源／焦点状态的按键封锁，避免吞键；提供 A2DP Sink、HFP Client 等接收端诊断。 |
| Siri 与音源恢复 | 独立管理 USB 助手的临时音频焦点，结束后恢复之前的原厂音源；使用导航播放用途输出 Siri 回复，并提供下行音频电平诊断。此车型的 Siri 播报及恢复蓝牙已实车验证。 |
| 导航语音音质 | 增加“兼容／清晰优先／48 kHz PCM”三档协商；此伊兰特车型未保存音质选择时默认 **48 kHz PCM**，已有手动选择保持不变。48 kHz 档位只调整导航／提示音输出格式，保留 Siri、音乐、电话和麦克风的格式。 |
| 实际采样率 | 显示真正收到的 USB 提示音采样率、编码与声道数，保留历史连接记录并标明来源，页面自动刷新；设置值与接收值独立。 |
| 全屏和外观 | 伊兰特默认直角、铺满屏幕，提供直角全屏开关；默认车辆名称为“伊兰特”，车辆按钮采用现代图标。 |
| USB 充电 | 增加 0 mA“只传数据”请求，电流列表升序排列。该请求不等同于物理切断 USB 5V 供电，实际行为取决于手机。 |
| USB 连接 | 改进插线时的重连配置判断、重新枚举轮询及阶段日志；此车型跳过可选手机 syslog 采集，保留必要认证、配对和 USB 网络流程。 |
| 无 Wi-Fi 车机界面 | 此车型以 USB 为主要入口，隐藏不适用的无线连接和 BYD 专属卡片；保留 USB 所需的本地网络能力。 |
| 视频参数 | 提供帧率和 H.264／HEVC 偏好，显示实际收到的编码；HEVC 使用取决于解码能力及手机协商。 |
| 离线日志 | 可选择 U 盘／文件夹导出诊断，适合车机无法联网或扫码导出不可用的情况。 |

实现参考了车主使用的**第三方 CarLife 的按键与公开 Android 播放行为**。本仓库不包含该应用的 APK、反编译工程或车机固件。

## 已验证与待验证

实车反馈与本地自动化检查分开记录：

| 功能 | 当前状态 |
| --- | --- |
| 蓝牙音乐、音量、方向盘单次切歌 | **实车验证正常**；此前重复切歌已修正。 |
| 导航声道与 48 kHz PCM | **实车验证正常**；2026-10-23 晚间日志中 77 次导航音轨初始化全部为 **48 kHz、双声道 LPCM**，实际播放音轨同为 48 kHz。 |
| Siri 回复出声 | **实车确认可听到回复**；日志记录有效非静音回复及正常播放进度，使用 24／48 kHz 单声道。 |
| Siri 结束后恢复蓝牙音频 | **实车验证正常**；当晚 4 次恢复请求均收到原厂音频焦点确认，均一次成功。 |
| 0 mA 请求、直角全屏 | **实车验证生效**。 |
| 实际采样率显示 | 显示接收到的格式，与设置档位分别记录；历史连接会标注来源。 |
| USB 重连速度、无线转接盒、其他车型 | 尚无足够实车结果，不作兼容保证。 |

1.0.1 沿用 test12 的连接、音频和按键功能，将伊兰特未设置过的导航音质默认值改为 48 kHz PCM，保留已有选择及所有档位。test12 本地检查为 **3192 项通过、1 项原有跳过**；三项已知本机网络用例按车主要求排除，不计为通过。正式版发布检查见[验证记录](docs/ELANTRA_AUDIO_TEST.md)。


## 当前正式版

版本 **1.0.1**（1.0.0 的默认音质更新），上游基线仍为 DiPlay 0.2.21。保留原有包名与本地签名，支持从 test12 覆盖升级并保留设置。应用显示名为“DiPlay 伊兰特”，发布构建关闭 Android 调试标志，不再包含测试专用入口。详见[发布说明](docs/RELEASE-NOTES-ELANTRA-1.0.1.md)。

## 使用方式

1. 将 `DiPlay-Elantra-1.0.1.apk` 安装在车机；本地交付包可直接覆盖 test12，无需卸载，使用支持数据传输的 USB 线连接 iPhone。
2. 沿用已正常工作的蓝牙配对及音频设置，实际播放音乐时用原车 **MODE 选择“蓝牙音频”**。
3. “我的音乐＋静音文件”仅是车主用于屏蔽收音机的办法；实际音乐音源仍按上一项选择。静音文件不包含在仓库中。
4. 导航音质在 **设置 → 高级 → 视频与音频 → USB 提示音质（实验性）**，选择后需重新连接 CarPlay。
5. 选择 48 kHz 后播放一次导航语音，查看下方实际接收格式。只有显示本次连接的 **48 kHz · PCM**，才能确认手机确实采用；若无播报，改回“清晰优先”或“兼容”再重连。

切换音质选项不自动中断运行中的连接。排查时结合接收日志及设置，不仅依赖原车音源名称推断蓝牙或 USB 传输。

## 构建与反馈

- [伊兰特构建说明](docs/BUILD_ELANTRA.md)：环境、源码构建与本地检查。
- [伊兰特音频与实车验证](docs/ELANTRA_AUDIO_TEST.md)：功能边界、验证状态和日志要点。
- [上游构建说明](docs/BUILD.md)、[原始验证记录](docs/VALIDATION.md)。

源码与 CI 构建默认不含配件认证身份。本地独立实车 APK 使用显式指定的外部运行资料；**配件私钥、Android 签名密钥、个人诊断日志及本地安装包不提交到本仓库**。正式实车安装包在本地交付目录提供，不将带运行身份的 APK 上传到公开仓库。

## 来源与致谢

- [DiPlay / shihabal3amri](https://github.com/shihabal3amri/DiPlay)：本 fork 的直接上游。
- [xcertplay / shilapi](https://github.com/shilapi/xcertplay)：DiPlay 的接收器基础，保留 [GPL-3.0](LICENSE) 与原有归属。
- [DiAuto](https://github.com/shihabal3amri/DiAuto)：上游首页／设置界面来源，保留原有 AGPL-3.0 标记及[许可文本](docs/licenses/DiAuto-AGPL-3.0.txt)。
- 其他依赖、资源及商标说明见[第三方说明](docs/THIRD_PARTY_NOTICES.md)。现代与 Apple 的名称、图标不代表官方授权或背书。

### English summary

This is an independent Hyundai Elantra adaptation of [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay), based on upstream 0.2.21. The exact Humax `BHCN-T3103C` / Android 9 profile focuses on wired USB CarPlay, steering-wheel key deduplication, native Bluetooth music integration, fullscreen defaults, offline diagnostics and selectable USB prompt formats. Version 1.0.1 defaults previously unconfigured Elantra navigation to 48 kHz PCM and preserves all saved quality choices. In the first stable release, audible Siri replies, Bluetooth source restoration and 48 kHz stereo LPCM navigation were confirmed in the owner’s car on 2026-10-23. Other vehicles and wireless adapters remain unverified. Upstream credits and license notices are retained; runtime identities, signing keys and personal logs are excluded.
