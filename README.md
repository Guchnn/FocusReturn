# 焦点回归 · FocusReturn

> **把 HyperOS 4 的「焦点通知」找回来，并且让它听你的话。**  
> 状态栏上那个随音乐、导航、外卖实时滚动的胶囊，本模块让它重新出现，并支持字号、颜色、字体、  
> 图标、分隔符、上下位置、点击行为等深度定制。

![最新版本](https://img.shields.io/github/v/release/Guchnn/FocusReturn?label=%E4%B8%8B%E8%BD%BD\&color=3482FF)

![系统](https://img.shields.io/badge/%E9%80%82%E7%94%A8-HyperOS%204-3482FF)

![许可证](https://img.shields.io/badge/License-GPL--3.0--only-blue)

| 项目   | 说明                                      |
| ---- | --------------------------------------- |
| 应用名  | 焦点回归（FocusReturn）                       |
| 包名   | `com.guchnn.focusreturn`                |
| 适用系统 | **HyperOS 4**（澎湃 OS4）                   |
| 前置条件 | **root** + **LSPosed**                  |
| 作用域  | `com.android.systemui`、`android`（两个都要勾） |
| 许可证  | `GPL-3.0-only`                          |

> **⚠️ 已移除 HyperOS 3 支持。** 自 `1.2.0` 起模块只安装 HyperOS 4 的自绘渲染钩子；  
> 仍在澎湃 OS3 上的设备请停留在上游 `1.1.0`。

---

## 这是什么

一段让不少人怀念的体验：**焦点通知**（Focus Notification）——那条把「导航下一路口」  
「外卖还有几分钟」直接压进状态栏左侧的胶囊，文字还会自己滚动。

HyperOS 4 停用了这条显示路径。**FocusReturn** 通过 LSPosed 让它在 OS4 上重新工作，  
并把原本不可调的部分全部开放出来自定义。

本项目是上游 **[HyperOS3FocusRestore](https://github.com/ImKani/HyperOS3FocusRestore)**（作者 **ImKani**，  
基线 0.13.6）的**深度二次开发衍生作品**，以**全新独立仓库**发布，不再依附上游 Fork，**不是**官方版本。

- 模块创作者：**Guchnn** · GitHub [https://github.com/Guchnn](https://github.com/ImKani)
- 上游作者：**ImKani** · GitHub [https://github.com/ImKani](https://github.com/Guchnn)
- 界面已**完全重构**（由 Java/View 改为 Kotlin + Compose，采用 [Miuix](https://github.com/compose-miuix-ui/miuix) HyperOS 组件库）
- 功能较上游**大量新增**
- 保留上游全部版权声明，整体仍以 **`GPL-3.0-only`** 授权

---

## 功能亮点

- **恢复焦点通知**：HyperOS 4 下自建视图渲染，复用系统 Primary Chip 位置
- **超级岛内容转换**：把带超级岛协议的通知文字提取出来，补进焦点通知（支持强制转换白名单）
- **文字深度定制**：字号（8–32sp）、字体（黑体/宋体/楷体/仿宋/等宽）、字重（100–900）、斜体
- **颜色自定义**：文字与分隔符**各自**支持亮色 / 暗色两套场景，**13 种预置色 + R/G/B 三通道自定义**
- **配色随状态栏实时反色**：按状态栏**实际底色**（不是系统深色模式）自动切换亮/暗场景
- **应用图标**：开关 + 大小（8–32dp）+ 与文字间距（0–32dp），保留应用自身彩色图标
- **分隔符**：内容、与时钟间距、与焦点通知间距、上下位置、亮暗颜色
- **上下位置拆分**：**图标**与**文字**可分别微调上下位置（各 −20 ~ +20 dp）
- **状态栏图标隐藏**：三态（不隐藏 / 隐藏其他 / 仅隐藏本应用）
- **显示时机**：锁屏时隐藏、发出通知的应用在前台时隐藏
- **渐隐渐显动画**：淡入淡出，时长 60–800 ms
- **点击行为**：以小窗打开 / 直接打开应用 / 不响应；可选「双击呼出」与「小窗失败直接打开」
- **小窗尺寸**：小米默认比例（实测定准）或系统默认
- **宽度与滚动**：宽度上限（默认 160dp）、滚动启动延迟、往返滚动、兼容重试
- **一键重启系统界面**：顶栏图标按钮，二次确认后由本应用结束 SystemUI 进程使其自动重载

---

## 安装

1. **安装 APK**：从 [Releases](https://github.com/Guchnn/FocusReturn/releases/latest) 下载  
   `FocusReturn-1.0-release.apk` 安装（需允许安装未知来源应用）。
2. **在 LSPosed 中启用模块**，并**勾选两个作用域**（缺一不可）：
   ```text
   系统界面      com.android.systemui
   系统框架      android
   ```
   `android` 作用域用于修复小窗内应用按窗口尺寸原生重排。不勾选时小窗仍可打开，  
   但应用内容可能缩放错乱。
3. **重启设备**，确保 SystemUI 的静态功能字段与 Hook 在启动阶段完成初始化。
4. 从桌面图标或 LSPosed 模块详情进入设置页调整选项。**改完即自动保存**（约 350ms 防抖），  
   再点顶栏右上角的「重启系统界面」图标按钮（二次确认）让设置立即生效，无需重启手机。
5. 触发一条以前会显示超级岛 / 焦点通知的通知（例如播放音乐）。

> **这是全新的 Application ID。** 旧版模块**不会**自动升级，二者可共存；  
> 测试时建议先禁用旧模块，避免两个模块同时 Hook SystemUI。

---

## 设置界面

底部三个 Tab，顶部是居左标题「焦点回归」与右上角的重启图标按钮。

| Tab    | 分组                 | 涵盖的设置                           |
| ------ | ------------------ | ------------------------------- |
| **焦点** | 焦点通知基础设置           | 渐隐渐显动画与时长、**图标上下位置**、**文字上下位置** |
| **焦点** | 通知滚动设置             | 限制宽度与最大宽度、滚动启动延迟、往返滚动、兼容重试      |
| **焦点** | 状态栏图标              | 隐藏通知图标（不隐藏 / 隐藏其他 / 仅隐藏本应用）     |
| **焦点** | 显示时机               | 锁屏时隐藏、应用打开时隐藏                   |
| **焦点** | 小窗打开               | 点击焦点通知、双击呼出、小窗启动失败直接打开、小窗尺寸     |
| **焦点** | 超级内容转换             | 转换超级岛内容、强制转换白名单、内容连接符号          |
| **样式** | 文字基础样式             | 字体大小、字体、字重、斜体                   |
| **样式** | 文字颜色               | 亮色场景、暗色场景                       |
| **样式** | 应用图标               | 显示应用图标、图标大小、图标与文字间距             |
| **样式** | 分隔符设置              | 分隔符内容、与时钟间距、与焦点通知间距、上下位置、亮暗颜色   |
| **关于** | 模块信息 / 开源信息 / 外部链接 | 版本、作者、许可证与链接                    |

- **一级分组是可折叠菜单**：点击分组表头展开 / 收起其下设置项，默认收起。
- **所有修改即时生效，没有保存按钮**；涉及布局的项需重启系统界面后完全生效。
- 界面采用 **Miuix（HyperOS 官方风格组件库）**，随系统明暗模式自动适配。  
  APK 体积约 8.3 MB（Compose 运行时与 Miuix 依赖所致）。

---

## 主要设置项说明

### 焦点 Tab

- **渐隐渐显动画**（默认开启）：焦点通知出现时淡入、消失时淡出，时长 60–800 ms（默认 180）。  
  淡入只在「从不可见变可见」的那一次播放，避免通知刷新时闪烁。
- **图标上下位置** / **文字上下位置**（各 −20 ~ +20 dp，默认 0）：分别把应用图标、文字块在状态栏内  
  上下微调，互不影响。用于对齐时钟基线或微调观感。
- **焦点通知宽度限制**（默认开启，上限 160dp）：先测量完整内容，再把显示 Host 截断到上限并滚动。
- **滚动启动延迟**（0–5 秒，默认 0.2 秒）、**往返滚动**（默认开启）、**兼容重试**（默认关闭，某些  
  ROM 滚动被重置时开启）。
- **隐藏通知图标**（默认「隐藏其他」）：
  - **不隐藏**：保留状态栏上全部通知图标。
  - **隐藏其他**：焦点内容可见期间隐藏整个通知图标容器，焦点消失后恢复。
  - **仅隐藏本应用**：只隐藏发出焦点通知那个应用自己的图标，其余照常显示  
    （压 `IconState.iconAppearAmount`，容器会回收它占的宽度，后面的图标自动左移补齐）。  
    两种隐藏都只在焦点内容可见期间生效；右侧信号、电池等系统图标不受影响。
- **锁屏时隐藏**（默认关闭）：息屏 / 锁屏时不渲染焦点通知，解锁后恢复。
- **应用打开时隐藏**（默认关闭）：发出通知的应用在前台（**全屏或小窗都算**）时隐藏焦点通知，  
  退到后台后恢复。**此时不隐藏该应用的通知图标**，状态栏仍留着线索。
- **点击焦点通知**：三选一，默认**以小窗打开**（另有「直接打开应用」「不响应点击」）。
  - **双击呼出**（默认关闭）：改为双击触发，单击只「上膛」，400ms 内第二击才执行。
  - **小窗启动失败时直接打开应用**（默认开启）：小窗不可用时自动改为直接打开。
- **小窗尺寸**：默认**小米默认比例**。下发的是**任务尺寸**（短边整宽 × 1.6，竖屏 1200×1920），  
  MIUI 会再统一施加 0.7 的显示缩放，最终观感约 840×1344，与系统自带小窗一致；  
  窗口按可视宽度水平居中。（另一方「系统默认」只请求 freeform 模式，部分 ROM 上偏小，仅供对照。）
- **超级岛内容转焦点通知**（默认关闭）：从超级岛协议中提取**文字**内容补进焦点通知。  
  仅处理文本，不支持图片、按钮、动态计时器；对没有超级岛参数的普通通知不生成额外内容。  
  可设置**强制转换白名单**与**内容连接符号**（留空默认 `·`）。

### 样式 Tab

- **字体大小**：默认「跟随系统」，可选 8–32sp，作用于焦点内容视图树中全部文本。
- **字体**：黑体 Sans、宋体 Serif、楷体 Kai、仿宋 FangSong、等宽 Mono。  
  楷体 / 仿宋等会扫描 `/system/fonts`、`/product/fonts`、`/vendor/fonts` 找字体文件，  
  设备上不存在时回退到通用字族。
- **字重**：100–900 共 7 档（Android 9+ 精确生效），**斜体**独立开关。
- **文字颜色**：**亮色场景 / 暗色场景**各自独立设置。场景由**状态栏实际底色**决定，  
  与系统深浅色模式无关——底色亮用亮色场景色，底色暗用暗色场景色。  
  默认「跟随系统」保留 ROM 原色；可选 13 种预置色，或用 **R / G / B 三个滑块**自定义并实时预览。
- **应用图标**：默认开启，在焦点通知左侧显示发出应用的**彩色**启动器图标；大小 8–32dp；  
  **图标与文字间距** 0–32dp（该位置只有间距，不放任何符号）。
- **分隔符设置**（默认为空＝不显示）：位于**时钟与焦点通知之间**。
  - **分隔符内容**：文本框，按原样绘制，留空即关闭。
  - **分隔符与时钟间距**（−24 ~ +24 dp）：在「距左侧时钟间距」之上额外增加，负值继续压向时钟。
  - **分隔符与焦点通知间距**（0–24 dp）。
  - **分隔符上下位置**（−20 ~ +20 dp）：只调整分隔符自身高低。
  - **分隔符颜色**：亮色 / 暗色两套，同样支持预置色与 RGB 自定义。

> 涉及布局的间距 / 位置设置，**需要点右上角重启系统界面后生效**。

### 关于 Tab

模块信息（版本、适配系统）、开源信息（作者、上游、许可证）、外部链接。

---

## 日志判读

模块日志 TAG 为 `FocusReturn`。

```text
FocusReturn: capabilities installedMode=OS4 ...
FocusReturn: OS4 notifPipelineListener=registered
FocusReturn: OS4 statusBarPrimarySlot=attached ...
FocusReturn: OS4 focus shown ...
```

- 只有 `showOnStatusBar` 而没有 `setData` → 判断已放行，但通知没进入焦点 View。
- 只有 `DynamicIslandService` → 只走了动态岛路径。
- `updateRemoteViews` 报错 → RemoteViews 与当前 SystemUI 不兼容。

抓取（需 root 终端）：

```sh
/system/bin/logcat -c
/system/bin/logcat -v threadtime FocusReturn:I FocusedNotifPromptView:I PromptViewAnimState:D AndroidRuntime:E '*:S' > /sdcard/focusreturn.log
```

或电脑 ADB：

```sh
adb logcat -c
adb logcat -v threadtime FocusReturn:I FocusedNotifPromptView:I PromptViewAnimState:D AndroidRuntime:E '*:S' > focusreturn.log
```

---

## 构建

需要 **JDK 17** 与 **Android SDK Platform 37**（Miuix 的 aar 声明了 `minCompileSdk=37`，  
故 `compileSdk` 必须为 37）。Gradle 由仓库自带的 Wrapper 提供，**不需要单独安装 Gradle**：

```bash
./gradlew assembleRelease        # macOS / Linux
gradlew.bat assembleRelease      # Windows
```

Windows 也可直接双击根目录的 `build-apk.bat`（读取 `JAVA_HOME` / `ANDROID_HOME`）。

产物：

```text
app/build/outputs/apk/release/FocusReturn-1.0-release.apk
```

### 签名

本仓库**不包含任何密钥库**，也不应该包含。`keystore.properties`（已被 `.gitignore` 忽略）是唯一  
推荐方式：

```properties
storeFile=/absolute/path/to/focusreturn-release.jks
storePassword=...
keyAlias=focusreturn
keyPassword=...
```

找不到密钥时**产出未签名包而不会构建失败**，以保证 fork / PR 的 CI 常绿。  
CI 也可用 `-PFOCUSRETURN_KEYSTORE=... -PFOCUSRETURN_STORE_PASSWORD=... -PFOCUSRETURN_KEY_ALIAS=...
-PFOCUSRETURN_KEY_PASSWORD=...` 或 `~/.gradle/gradle.properties` 注入。

> **绝不要**把口令写进仓库根目录的 `gradle.properties`——那个文件是**被版本控制的**。

### 持续集成

`.github/workflows/build.yml` 在推送 `main` / `master`、PR、推送 `v*` 标签或手动触发时运行：  
装 JDK 17 与 Android SDK → `assembleRelease` + `testReleaseUnitTest` → 校验签名 → 打包 GPL 对应源码 zip → 上传构件。  
推 `v*` 标签时额外发布 GitHub Release；**但仅在产物已签名时才发布**，避免用未签名包覆盖正式包。

---

## 许可证与上游来源

本项目以 **GNU General Public License v3.0 only（`GPL-3.0-only`）** 发布，许可证全文见  
根目录 `LICENSE` / `COPYING`，版权与署名声明见 `NOTICE.md`，完整改动清单见 `MODIFICATIONS.md`。

- 上游项目：**[HyperOS3FocusRestore](https://github.com/ImKani/HyperOS3FocusRestore)**（作者 **ImKani**）
- 上游基线：`0.13.6`（versionCode 60）
- 二次创作者：**Guchnn**（<https://github.com/Guchnn>）
- 本仓库是**衍生作品**，不是官方版本，也不代表上游作者的立场
- 源码中每个被修改过的文件头部都带有修改标注，以满足 GPL-3.0 第 5 条要求

> **分发义务**：依据 GPL-3.0 第 6 条，分发二进制（APK）时必须同时提供**完整对应源码**。  
> 本仓库源码即构成该对应源码；每个 Release 也同时附带源码压缩包。

本项目的具体 Hook 方法、字段处理逻辑与内部判断流程不在 README 中展开  
（避免他人轻易绕过 GPL 重新实现）；如需了解实现细节请直接阅读源码。

---

## 风险提示

本模块通过 LSPosed Hook 介入 SystemUI，存在 **ROM 版本差异、系统崩溃、状态栏显示异常、功能失效、  
数据丢失** 等风险。使用前请自行备份，并自行承担使用风险。

- 焦点通知点击属于 ROM 原生路径之外的附加行为，不保证每个通知都能正确启动对应应用。
- 选择「不响应点击」时，模块会消费状态栏 Focus 区域的触摸事件。
- 「以小窗打开」在部分 ROM 或应用上可能不受支持，此时按附加选项回退。
- 本模块**不适配也不隐藏** MIUIStrongToast（灵动舞台），需要隐藏请使用其他专用工具。
- 模块仅作用于 `com.android.systemui`（+ `android`），**不要求 KernelSU**。
- 本项目由 AI 辅助分析与编写，请自行评估后使用。

---

## 致谢

- **[ImKani](https://github.com/ImKani/HyperOS3FocusRestore)** —— 上游作者，本项目的全部基础来自其工作
- **[Miuix](https://github.com/compose-miuix-ui/miuix)** —— HyperOS 风格 Compose 组件库（Apache-2.0）
