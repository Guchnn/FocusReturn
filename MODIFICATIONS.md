# 修改说明（GPL-3.0-only 要求的改动声明）

本目录中的代码是基于上游 HyperOS3FocusRestore 的**独立衍生作品（derivative work）**，
已作为全新独立开源仓库发布，不再依附上游 Fork。

- **上游项目**：HyperOS3FocusRestore
- **上游作者**：ImKani（酷安 <https://www.coolapk.com/u/1205658> · GitHub <https://github.com/ImKani/HyperOS3FocusRestore>）
- **上游许可证**：GNU General Public License v3.0 only（GPL-3.0-only，SPDX: `GPL-3.0-only`）
- **本衍生作品基线版本**：0.13.6（versionCode 60，即 1.0.0 之前对外可获得的版本）
- **本衍生作品版本**：`1.0`（versionCode 122）
- **修改者 / 二次创作者**：**Guchnn**（GitHub <https://github.com/Guchnn>）
- **修改日期**：2026-09-18 ~ 2026-09-20

依据 GPL-3.0 第 5 条，本衍生作品整体**同样以 GPL-3.0-only 授权**发布，不包含任何
"or later" 选择，也不附加其他限制。任何人依据本许可证获得的权利，均适用于完整作品及
其所有部分，无论以整体还是部分形式分发。完整的许可证正文见同目录的 `COPYING` 文件
（原件：<https://www.gnu.org/licenses/gpl-3.0.txt>）。

## 版权与署名（Copyright & Attribution）

- 上游原有代码的版权归原作者 **ImKani** 所有，其版权声明在本文件与所有相关源文件
  头部均完整保留，未被删除或改动。
- 本次修改、新增的代码与资源，版权归 **Guchnn** 所有：

  > Copyright (C) 2026 Guchnn
  > Modifications Copyright (C) 2026 Guchnn <https://github.com/Guchnn>

- 上述修改部分**同样以 GPL-3.0-only 授权**发布（GPL-3.0 第 5(c) 条：衍生作品整体
  必须按同一许可证授权）。Guchnn 未对本作品附加任何超出 GPL-3.0-only 的额外限制。
- 本衍生作品的署名在以下位置向最终用户可见：应用内「更多 → 关于」页面、
  `NOTICE.md`、`README.md`、各被修改源文件头部注释、以及本文件。
- `LICENSE` 与 `COPYING` 均为 GNU GPL v3.0 全文；本作品整体为 **GPL-3.0-only**
  （SPDX: `GPL-3.0-only`），不含 "or later" 选项。

## 改动清单

**本衍生作品的第一个公开发布版本为 `1.0.0`（versionCode 104）。**
在它之前对外可获得的版本是上游原版 `0.13.6`（versionCode 60）；开发过程中曾产出若干
仅用于自测的中间构建，未对外发布，其编号已不再追溯，全部改动统一归入下面这一份清单。

基于上游 0.13.6 的改动，均已在对应源文件的头部注释中标注。
以下条目按开发顺序排列，靠后的条目可能覆盖靠前的条目（例如第 11 条的三栏底部 Tab
在第 15 条被精简为两栏），**最终形态以最后一条为准**。


1. **新增「自定义焦点通知字体大小」**
   - 新增设置项 `focus_font_size_sp`（0 = 跟随系统，8–32 sp）
   - 新增工具类 `FocusTextStyle`，负责把字号应用到焦点文本
   - HyperOS 3：作用于 `FocusedNotifPromptView` 的 `mContentText`
   - HyperOS 4：作用于焦点内容视图树内的全部 `TextView`


2. **新增「亮色 / 暗色场景文字颜色」**
   - 新增设置项 `focus_text_color_light`、`focus_text_color_dark`
     （`0xAARRGGBB`，0 = 跟随系统）
   - 场景由系统深浅色模式自动切换（`Configuration.uiMode`）
   - 提供 13 种预置色与 R/G/B（0–255）三通道自定义

3. **新增「字体 / 字重 / 斜体」**
   - 新增设置项 `focus_font_family`（10 种系统内置字族）、`focus_font_weight`
     （100–900 共 7 档）、`focus_font_italic`
   - 字重使用 `Typeface.create(family, weight, italic)`，Android 9（API 28）
     及以上精确生效，更低版本回退为粗体近似

4. **配套改动**
   - `SettingsProvider` 的配置列由 17 列扩展到 22 列（新列追加在末尾）
   - `HookSettings.fromCursor` 对所有新列做 `columnCount` 保护，保证
     旧版模块 APK 与新版本设置共存时不会崩溃
   - 设置界面新增「字体」面板与亮 / 暗两个颜色面板

5. **构建脚本**
   - 新增 `build-apk.bat`（一键构建 release / debug），仅用于本地构建，
     不属于上游项目的一部分


6. **修复自定义文字颜色偶发失效**
   - HyperOS 4：`applyTint` 每次状态栏 tint 变化都会把所有焦点文本重新着色，
     会静默覆盖自定义颜色；现改为经 `resolveTextColor` 解析后再着色
   - HyperOS 3：`setData` 之后的 RemoteViews 二次 apply 会覆盖颜色；现改为
     在下一帧（post）与 150ms 后各重新应用一次样式，同时可弥补设置读取晚到的情况

7. **字体改为 5 种经典字体**
   - 黑体 Sans / 宋体 Serif / 楷体 Kai / 仿宋 FangSong / 等宽 Mono
   - 楷体、仿宋等会扫描 `/system/fonts`、`/product/fonts`、`/vendor/fonts`
     查找匹配字体文件，找不到时回退到对应通用字族
   - 每个源文件头部保留 GPL 修改声明

8. **设置界面重构为 MIUI 风格卡片列表**
   - 分组卡片（系统界面 / 显示 / 文字样式 / 文字颜色 / 滚动 / 超级岛 / 调试）
   - 颜色项改为可折叠行，点击展开预置色与 RGB 滑块

9. **增加桌面启动器图标**
   - `AndroidManifest.xml` 的 intent-filter 原来只有 `category.INFO`
     （不会在桌面显示），现补充 `category.LAUNCHER`


10. **设置界面整体重新设计**
    - 修复顶栏标题被状态栏 insets 裁切的显示异常（原实现把内边距加进了
      固定 56dp 高度的栏里，内容区被挤没）
    - 移除底部三页 Tab，改为单页滚动 + 分组卡片的信息架构，避免同组功能
      跨页冗余排布；所有功能重新归为：通用 / 焦点通知 / 文字样式 / 文字颜色 /
      滚动 / 超级岛 / 高级 / 关于
    - 条件显示：宽度滑块仅在开启宽度限制时出现；超级岛连接符与白名单仅在
      开启转换时出现，减少无效占位
    - 底部保存状态条（未保存提醒 / 保存结果），替代原先混在正文里的提示文字
    - 新增深色主题支持，跟随系统深浅色模式切换配色与系统栏
    - 关于区块收拢为单卡片（版本 / 作者 / 许可证 / 链接），末尾统一放置风险说明


11. **改为「底部三大类 + 二级页面」的信息架构**
    - 底部三个大类 Tab：显示 / 文字 / 更多
    - 每个 Tab 下只放 2–3 个入口卡片，具体设置移入二级页面：
      焦点通知、滚动、文字样式、文字颜色、超级岛、高级、关于
    - 二级页顶部标题栏带返回箭头，并接管系统返回键
    - 页面切换时统一重建内容区，控件值全部从 `pending*` 回填，避免状态错乱

12. **补齐二次创作者署名（GPL-3.0 第 5 条合规）**
    - 填写此前留空的「修改者」字段为 **Guchnn**（GitHub <https://github.com/Guchnn>）
    - 新增「版权与署名」小节，明确上游版权归 ImKani、修改部分版权归 Guchnn，
      且修改部分同样以 GPL-3.0-only 授权
    - 所有被修改源文件头部追加 `Modifications Copyright (C) 2026 Guchnn` 声明
    - 应用内「更多 → 关于」页面新增「二创修改」行与 Guchnn 的 GitHub 链接，
      并补充 GPL-3.0 要求的无担保（no warranty）声明
    - `README.md` 与 `LICENSE` 补充二次创作者署名
      （`LICENSE` 后来改为 GPL 全文，该署名声明迁至 `NOTICE.md`，见第 21 条）


13. **更名与署名校正**
    - 版本命名为 `1.0.0`（首个正式发布版；为便于覆盖安装自测构建，versionCode 取 104）
    - 应用中文名改为「焦点回归」；仓库/产物名改为 `FocusReturn`
      （APK 输出名与源码包根目录），包名保持 `com.guchnn.focusreturn` 不变
    - 二次创作者 GitHub 用户名由 `Goochan` 更正为 **`Guchnn`**
      （源文件头、`LICENSE`、`MODIFICATIONS.md`、`README.md`、应用关于页与链接；
      `LICENSE` 已改为 GPL 全文，署名声明现位于 `NOTICE.md`）
    - 上游作者 ImKani 的署名在所有位置继续完整保留


14. **修复「亮/暗场景文字颜色」自动切换失效**
    - 根因：`FocusTextStyle.isDarkScene()` 使用系统深色/浅色模式
      （`Configuration.UI_MODE_NIGHT_*`）判断场景，而该场景实际指
      **焦点通知所在状态栏的真实底色**，与系统主题无关，导致应用程序在
      深色主题下使用亮色背景（或反之）时选错颜色分支，表现为自定义颜色「不生效」
    - 改为以系统状态栏内容色（`DarkIconDispatcher` / `MiuiClock` 的 tint）判定场景：
      内容色偏亮 ⇒ 底色为暗（暗场景）；内容色偏暗 ⇒ 底色为亮（亮场景）
    - 新增 `FocusTextStyle.publishStatusBarTint()` / `isDarkSceneFromTint()`，
      并加入亮度迟滞区间（0.38–0.62）以忽略 ROM 的颜色渐变动画帧
    - HyperOS 4：所有着色点（`applyTint` / `resolveTextColor` / 兜底 TextView /
      `showContent` 视图树）统一按状态栏 tint 选色，tint 变化时实时切换
    - HyperOS 3：`installOS3Hooks()` 新增 `hookSceneTint()`，注册
      `DarkIconDispatcher.DarkReceiver` 跟踪底色并在变化时重新应用样式；
      取不到派发器时回退为读取状态栏时钟颜色
    - 设置页「文字颜色」的说明文案更正为“按状态栏实际底色自动切换，
      与系统深色/浅色模式无关”，并提示两个场景都需设置


15. **界面精简 + 新增「重启系统界面」入口**
    - 顶栏由原来 56dp 高、带应用图标与 19sp 大标题、右侧「保存」文字按钮的栏位，
      改为 44dp 紧凑栏：左侧在主页面显示软件名「焦点回归」，进入二级页面时
      显示该页名称（并在其左侧保留返回箭头）；右侧为「保存」与「重启系统界面」
      两个图标按钮，原「保存」文字按钮与顶栏应用图标一并移除
    - 新增资源 `res/drawable/ic_restart.xml`（重启/刷新图标），
      运行时按当前主题着色（`headerIconAction`），与深/浅色主题一致
    - 新增「重启系统界面」对话框：说明影响后由用户选择「取消 / 重启」。
      确认后在**工作线程**通过 `su -c` 执行重启指令，依次尝试
      `pkill -f "[c]om.android.systemui"` → `killall com.android.systemui`
      → `kill $(pidof com.android.systemui)`，任一成功即结束 SystemUI 进程
      并由系统自动拉起（无需重启手机）
      - root 探测：`su -c id`，输出需含 `uid=0`；否则提示
        「请为该应用授予 root 权限，然后再试一次。」
      - 设备完全没有 `su` 时可执行文件时，提示改用手动重启
      - 所有 `su` 调用均带超时（探测 60 秒、执行 15 秒），避免无响应时卡死
    - 底部 Tab 由「显示 / 文字 / 更多」精简为「显示 / 更多」；原「文字」Tab 的
      「文字样式」「文字颜色」两个入口并入「显示」一级页面，二级页面内容不变
    - 删除「超级岛」一级页面（含其中的「通用连接符」「左右连接符」两项输入），
      将「转换超级岛内容」与「强制转换白名单」移入「更多 → 高级」
    - 连接符取值仍保留在配置项 `island_general_separator` /
      `island_side_separator` 中，保存时原样回写，仅不再提供界面入口，
      因此已有用户的连接符设置不会被重置


16. **修复设置界面与状态栏之间的割裂（沉浸式适配）**
    - 根因：状态栏高度被重复计算了一次——根布局先按整个窗口的 insets 加了内边距，
      固定顶栏又按状态栏 insets 再加了一次，导致顶栏被整体下推，与状态栏之间
      出现一条可见的空隙；同时状态栏底色为不透明色，形成"两截"的观感
    - 改为真正的 edge-to-edge：新增 `values/styles.xml` 的 `AppTheme`
      （`windowDrawsSystemBarBackgrounds=true`、状态栏与导航栏透明），
      `configureSystemBars()` 在 API 30+ 调用 `setDecorFitsSystemWindows(false)`、
      API 35 以下设置透明栏色、并按当前主题切换系统栏图标明暗
    - 系统栏 insets 的归属重新划分：根布局不再消费 insets，改由**固定顶栏**承担
      状态栏 insets、**固定底栏**承担导航栏/手势条 insets，使卡片背景一直铺到
      屏幕边缘，顶栏与状态栏之间不再有缝隙
    - 新增 `values-night/`（`colors.xml` / `bools.xml`），启动首帧即使用深色背景，
      避免深色模式下的白屏闪一下

17. **改为「改完即存」的静默自动保存**
    - 所有设置项（开关、滑块、输入框、颜色、白名单）变动后统一进入 `markPending()`，
      经 350ms 防抖合并为一次静默写入，避免拖动滑块时频繁落盘
    - 离开页面（`onPause`）时立即补写，确保不丢改动
    - 移除顶栏的「保存」图标按钮与底部保存状态提示条（`statusSnack`），
      保存成功/失败不再打扰用户，仅保留 `Log.i` 便于排查
    - 删除已无用的 `res/drawable/ic_save_floppy.xml`

18. **重启系统界面的结果改为弹窗提示**
    - 原「未检测到 su 命令……」「请为该应用授予 root 权限……」等提示只显示在
      底部提示条里，容易被忽略；现统一通过 `showMessageDialog()` 以卡片弹窗呈现，
      点击「知道了」关闭
    - 请求 root 期间改用 Toast 轻提示，不阻塞操作

19. **重做启动器图标**
    - 新图标（靛蓝→紫色渐变底 + 白色圆形"恢复"箭头环抱白色焦点胶囊）替换原先的
      灰底同心圆，交由 `res/drawable/ic_launcher_background.xml`（渐变背景）、
      `ic_launcher_foreground.xml`（前景标记）、`ic_launcher_monochrome.xml`
      （Android 13+ 主题图标单色层）三个矢量资源组成
    - 新增 `mipmap-anydpi-v33/`（带 `<monochrome>` 的自适应图标），
      `mipmap-anydpi-v26/` 保持不带该字段，以兼容旧版本解析
    - `mipmap-anydpi/`（自适应图标出现前的旧图标位）同步替换，其图形按 1.5 倍
      放大以铺满整块图标；删除仅用于旧图标的 `values-v31/colors.xml` 与
      `ic_launcher_background` 颜色项

20. **修复启动闪退（1.0.0 发布前最后一项修复）**
    - 现象：带沉浸式适配的那次自测构建在 HyperOS 4 上点击图标后应用立刻闪退
      （`data_app_crash` 记录 6 次相同堆栈），设置界面完全打不开
    - 根因：`SettingsActivity.onCreate()` 中 `configureSystemBars(getWindow())`
      在 `setContentView()` **之前**被调用。MIUI 定制的
      `PhoneWindow#getInsetsController()`（`PhoneWindow.java:4235`）会直接对
      `mDecor` 解引用而不先创建它，此时 DecorView 仍为 `null`，于是抛出
      `NullPointerException: Attempt to invoke virtual method
      'android.view.WindowInsetsController com.android.internal.policy.DecorView.getWindowInsetsController()'
      on a null object reference`，Activity 启动即失败
    - 修复：
      - 将 `configureSystemBars()` 移到 `setContentView()` **之后**调用，保证
        DecorView 已随内容一起创建
      - 方法内先取 `Window#getDecorView()` 显式创建 DecorView，并把
        `WindowInsetsController` 的来源由 `Window#getInsetsController()`
        改为 `View#getWindowInsetsController()`（拆出
        `applySystemBarIconAppearance(View decor)`），彻底绕开 MIUI 的
        `PhoneWindow` 实现
      - 整个系统栏配置包上 `try/catch (Throwable)`：edge-to-edge 只是外观，
        任何 ROM 差异都不应导致设置页打不开
    - 该缺陷由「沉浸式适配」那次改动引入（上游 0.13.6 及更早的构建只在 `onCreate`
      内调用不依赖 DecorView 的 `setStatusBarColor()` 等方法，因此未触发）；
      已在本版修复，`1.0.0` 是修复后的版本

21. **仓库整理：让源码可以被别人直接编译（发布前准备）**
    - **新增 Gradle Wrapper**（`gradlew`、`gradlew.bat`、`gradle/wrapper/`，
      Gradle 8.9）。此前仓库只有裸的 `build.gradle`，且在 `app/build.gradle` 里把
      `release` 的签名指向 `.android/debug.keystore` —— 该目录位于 `.gitignore` 中，
      因此任何人 clone 后执行 `assembleRelease` 都会因为找不到密钥库而**直接构建失败**，
      CI 同理
    - **签名改为可选**（`app/build.gradle`）：密钥库存在时才接线，不存在则产出未签名包，
      保证源码在任何环境下都能编译。同时支持通过 Gradle 属性注入正式发布密钥
      （`FOCUSRETURN_KEYSTORE` / `FOCUSRETURN_STORE_PASSWORD` /
      `FOCUSRETURN_KEY_ALIAS` / `FOCUSRETURN_KEY_PASSWORD`），
      这样维护者换用正式密钥时**无需改动任何源码**，且密钥不会进入版本库
    - **`LICENSE` 由摘要改为 GPL-3.0 全文**：原先该文件只有一段简短声明，
      GitHub 依靠许可证全文指纹识别协议，摘要会导致仓库被显示为「无许可证」；
      原声明内容迁至新增的 `NOTICE.md`，`COPYING` 保持全文不变
    - **`build-apk.bat` 改为调用 `gradlew`**：原先硬编码了本机的 JDK / SDK / Gradle
      绝对路径，对外无用；现在改为优先读取环境变量 `JAVA_HOME` / `ANDROID_HOME`，
      仅在未设置时回退到默认路径，且不再需要单独安装 Gradle
    - **`settings.gradle`**：`rootProject.name` 由 `HyperOS3FocusRestore` 改为
      `FocusReturn`，与本衍生作品的命名一致（包名不变）
    - **`.gitignore`**：补充 `*.jks` / `*.keystore` / `keystore.properties` /
      `signing.properties`，防止发布密钥被误提交

22. **接入正式发布密钥 + 持续集成（发布前准备，续）**
    - **新增独立的正式签名密钥**，取代此前的调试密钥。密钥库为
      `focusreturn-release.jks`（PKCS12，RSA 4096 / SHA256withRSA，
      别名 `focusreturn`，有效期 10950 天），**存放于版本库之外**，
      以 `keystore.properties` 接入；该文件与密钥库本身均已被 `.gitignore` 覆盖。
      本节提交的 `1.0.0` 构建即由该密钥签名
      （证书 `CN=Guchnn, OU=FocusReturn, O=Guchnn, C=CN`，
      证书 SHA-1 `d5:11:21:6b:c6:d4:9f:5e:1d:b7:84:57:73:61:12:f4:73:96:31:cb`）
    - **签名配置支持三种来源**（`app/build.gradle`，按优先级）：
      仓库根目录的 `keystore.properties`（已忽略，供本地使用）→
      Gradle 属性 `-PFOCUSRETURN_*` / `~/.gradle/gradle.properties`（供 CI 使用）→
      本地调试密钥库回退。三者都不可用时产出**未签名**包而不是构建失败，
      以保证 fork 与 PR 的 CI 始终为绿色
    - **新增 `keystore.properties.example`** 作为对外模板（该文件本身不含任何真实凭据，
      可安全提交）
    - **签名方案**：显式启用 APK Signature Scheme **v2 + v3**。注意 AGP 的默认值其实
      是「v1（仅当 `minSdk < 24`）+ v2」，**v3 并不在默认里**，必须显式声明
      （实测：不写 `enableV3Signing true` 时产物为 v2-only）。
      v1（JAR 签名）未索取：`minSdk` 为 27，Android 8.1 及以上一律验 v2，
      且 AGP 在 `minSdk >= 24` 时会忽略 `enableV1Signing true`（实测确认）
    - **新增 GitHub Actions 工作流** `.github/workflows/build.yml`：
      检出 → JDK 17 → Android SDK（platform 35 + build-tools 35.0.0）→
      Gradle 缓存 → 还原密钥（存在 `SIGNING_*` Secrets 时）→ `assembleRelease` →
      `testReleaseUnitTest` → 校验签名方案（有密钥时必须验签通过，
      无密钥时必须确认确实未签名）→ 打包 GPL 对应源码 zip → 上传构件。
      推送 `v*` 标签时另起一个任务，通过 `gh release create`
      发布 Release 并附带 APK 与源码包
    - **`.gitignore`**：补充 `*.p12` / `*.bks`

23. **新增三大焦点通知功能（1.1.0）**
    - **显示应用图标**：在焦点通知左侧显示「发出该通知的应用」的小图标。
      - 新增 `FocusAppIcon`：优先取自适应图标单色层（API 33+ `AdaptiveIconDrawable#getMonochrome()`），按状态栏内容色 tint 着色，**随亮/暗场景反色**；无单色层时回退使用彩色启动图标
      - 大小可调（`app_icon_size_dp`，8–32 dp），并由「显示应用图标」开关控制
      - HyperOS 4：在自建 `FocusHostView` 中干净渲染
      - HyperOS 3：在 `FocusedNotifPromptView` 内尽力注入 `ImageView`（best-effort，
        因 ROM 布局差异可能表现不同；全程 try/catch，注入失败不会破坏原有焦点通知）
    - **自定义分隔符与间距**
      - 分隔符号 `focus_divider_symbol` 可自定义（默认延续原观感的 `丨`，可改为
        `| │ · —` 等）。HyperOS 4 用 `TextView` 显示该符号替代原先的固定细竖线；
        HyperOS 3 同样尽力在内容左侧注入符号 `TextView` 并把 ROM 原生内容右移留出间距
      - 与左侧时钟的间距 `focus_margin_start_dp`、与右侧的间距 `focus_margin_end_dp`
        （均 0–24 dp）通过给焦点视图设置 `MarginLayoutParams` 实现
      - **内容连接符号** `island_general_separator` 此前已在数据通路中存在（用于拼接
        「小爱陪伴：陪伴中」中间的冒号等），本次在「高级」页新增输入框把它暴露给用户，
        留空时默认使用 `·`
    - **点击以小窗打开应用**：新增「点击以小窗打开应用」开关（`click_open_freeform`）
      - 新增 `FocusFreeformLauncher`：点击焦点通知时以小米小窗（freeform，
        `WINDOWING_MODE_FREEFORM = 5`）模式打开对应应用；经反射调用
        `ActivityOptions#setLaunchWindowingMode(int)`，失败回退普通启动，
        任何异常都被吞掉并记录日志，**绝不导致 SystemUI 崩溃**
      - HyperOS 4：给焦点宿主设 `OnClickListener` 走 `FocusFreeformLauncher`
      - HyperOS 3：在 `onFocusNotifPromptClicked` 钩子里拦截并启动小窗，再 `setResult(null)`
        阻止 ROM 原生的点击行为
    - **设置管线扩展**：`FocusRestoreSettings` 新增 6 个键；`SettingsProvider` 游标列由
      22 列扩展到 28 列；`HookSettings.fromCursor` 同步读取新列（带 `columnCount` 保护）
    - 版本提升为 `1.1.0`（versionCode 105）

    > 说明：此处的说明已被第 24 条取代——HyperOS 3 支持已整体移除，上述三项功能现在
    > 只由 HyperOS 4 的自建渲染路径提供。


24. **移除 HyperOS 3 支持，并按新布局重做焦点通知渲染（1.2.0）**
    - **彻底删除 HyperOS 3 代码**：本衍生作品此前同时对澎湃 OS3（改 ROM 原生
      `FocusedNotifPromptView`）与 OS4（模块自建 `FocusHostView`）两套路径提供支持。
      本版删除全部 OS3 专有实现（含 `installOS3Hooks()` 及其全部辅助方法：
      OS3 样式注入/边距/分隔符/跑马灯/ marshmallow 兼容、Bean 改写与还原、
      预标记集合 `preMarkedIslands` 等），**仅保留 HyperOS 4 一条渲染路径**。
      入口类 `HyperOS3FocusRestoreHook` 名称保持不变以免改动 Xposed 入口配置（该入口类已于 1.7.2 更名为 `FocusReturnHook`，见本文 #39），
      但其 `installConfiguredModeHooks()` 现已固定只安装 OS4 钩子。
      - 共享的 Bean 解析与超级岛转换（`createOS4DisplayItem`、`inspectExpanded`、
        `extractIslandContent`、`IslandPayloadParser`）不受影响，继续复用
    - **焦点通知的新布局定义**：`图标 + 文字-文字`，即
      `时钟 ┃ 分隔符 ┃ 图标 ─ 间距 ─ 文字 A - 文字 B`。
      四处可配置的位置本次全部拆开管理：
      1. **图标 ↔ 文字**：只提供间距，不再显示任何间隔符。
         新增滑钮 `icon_text_gap_dp`（0–32 dp，默认 6），渲染时换算为像素后
         加在图标右侧；图标本身仍由 `FocusAppIcon` 按状态栏内容色 tint 反色
      2. **时钟 ↔ 图标**：这才是需要间隔符的位置。由开关
         `show_clock_icon_divider`（默认开）控制，符号
         `clock_icon_divider_symbol` 可自定义（默认延续原观感的 `丨`，可改为
         `| │ · —` 等）；颜色直接使用场景 tint（`currentTint`，alpha 0.55），
         **随亮/暗场景自动反色**，表现与文字颜色一致
      3. **文字 ↔ 文字**：沿用的 `island_general_separator` / `island_side_separator`
         （如「小爱陪伴：陪伴中」中间的冒号），可在「高级」页自定义，同样经
         `FocusTextStyle.applyTree` 按场景着色，**跟随亮/暗模式**
      4. **点击行为改为二重选项** `click_open_mode`：
         - `1` 以小窗打开（默认）
         - `2` 直接打开应用
         - `0` 不响应点击
         选中「以小窗打开」时提供附加开关 `freeform_fallback_direct`（默认开）：
         **小窗启动失败时自动改为直接打开应用**，不再无声无息什么都不做
    - **修复小窗比例**：原先只设置 `WINDOWING_MODE_FREEFORM`，窗口大小由 ROM 决定，
      比例不对。现按小米 15 实测的默认小窗尺寸 **847×1350 px**（约 0.628:1）
      作为参考比例，用 `ActivityOptions#setLaunchBounds(Rect)` 显式下发区域：
      高度取屏幕高度的 `1350/1440`（约 94%），宽度按同一比例换算，再垂直居中、
      水平靠右（留 1% 边距），因此在任意分辨率上都维持同一观感。
      仍经反射调用 `ActivityOptions#setLaunchWindowingMode(5)`，失败则按附加开关回退
    - **设置管线调整**：`FocusRestoreSettings` 移除 `HOOK_MODE_OS3`、
      `allowFocusClick`、`clickOpenFreeform`、`showFocusDivider`、`focusDividerSymbol`
      五个字段（对应键一并删除），新增
      `show_clock_icon_divider`、`clock_icon_divider_symbol`、`icon_text_gap_dp`、
      `click_open_mode`、`freeform_fallback_direct`；`hook_mode` 恒为 OS4。
      `SettingsProvider` 游标仍是 28 列，但去掉了 `hook_mode` 与
      `allow_focus_click`，其余列在末尾重排；`HookSettings.fromCursor`
      仍对所有列做 `columnCount` 保护，保证与旧版模块混装时不崩溃
    - 版本提升为 `1.2.0`（versionCode 106）


25. **`1.3.0`：按实测数据重做小窗尺寸、位置与通知图标隐藏**
    - **删除「系统界面版本」卡片**：App 内不再出现「仅支持 HyperOS 4」之类的
      版本提示（`buildModeCard` 及其调用点整体删除）；LSPosed 模块描述里的
      「恢复 HyperOS 3/4 的焦点通知状态栏显示」也去掉了版本措辞。因为 OS3
      代码在第 24 条已全部删除，这条说明已无存在必要
    - **新增「焦点通知上下位置」**：新增设置项 `focus_vertical_offset_dp`
      （−10 ~ +10 dp，默认 0 = 居中）。渲染时对整个 `FocusHostView` 做
      `setTranslationY(offset × density)`，不改变原有布局基准，拖动滑钮即可
      把焦点通知整体上移或下移
    - **删除时钟 ↔ 焦点通知的分隔符及其全部选项**：`show_clock_icon_divider`、
      `clock_icon_divider_symbol` 两个设置项、Provider 列、渲染分支与
      `dividerSymbolView` 字段全部移除。现在焦点通知自左向右就是
      `图标 ─ 间距 ─ 文字 A - 文字 B`，不再有任何自定义分隔符号
    - **「距左侧时钟间距」最小值即 0**：`MIN_FOCUS_MARGIN_DP` 仍为 0，默认值
      由 4 dp 改为 **0 dp**，即设置成 0 时焦点通知几乎紧贴时钟；滑钮左端标签
      直接写明「0 dp（紧贴时钟）」
    - **「隐藏其他通知图标」扩展为三态** `hide_icons_mode`：
      - `0` 不隐藏
      - `1` 隐藏其他（隐藏状态栏上全部通知图标，默认，等同 1.2.0 的开关打开）
      - `2` 仅隐藏本应用（只隐藏**发出焦点通知那个应用自己**的通知图标）
      实现上，模式 2 不再对整个 `notificationIcons` 容器做 `GONE`，而是遍历
      容器子视图，通过反射读 `StatusBarIconView.mNotification`
      （回退 `mIcon.pkg`）取到发包名，与本条焦点通知的包名比对，命中的图标单独
      `GONE` 并记录原可见性；同时给容器挂 `OnHierarchyChangeListener`，
      焦点通知期间新到的同应用通知图标会被立即隐藏，焦点消失时统一还原
    - **小窗尺寸重做（关键修复）**：上一版用「847×1350 参考比例的 94% 屏高」
      计算，尺寸仍不对。本次改为两种模式 `freeform_size_mode`：
      - `0` **系统默认**（默认）：只设置 `WINDOWING_MODE_FREEFORM`，
        **完全不下发 `setLaunchBounds`**，由系统（AOSP 的
        `TaskLaunchParamsModifier` / `LaunchParamsUtil#getDefaultFreeformSize`，
        小米会再叠加自己的策略）按**当前横竖屏**计算默认自由窗口尺寸。
        这是唯一能保证「与系统默认小窗一致」的做法
      - `1` **小米默认比例**：按 ADB 实测数据换算。实测（1200×2670 屏幕、
        520 dpi、ROTATION_90）微信小窗
        `Task{... mode=freeform}` 的 `bounds=[140,84][1340,1884]`、
        `Requested w=1200 h=1800`，即窗口一边取屏幕短边、另一边取短边的 1.5 倍。
        实现为：`shortSide = min(屏宽, 屏高)`，
        `longSpan = round(shortSide × 1.5)`，竖屏取 `shortSide × longSpan`、
        横屏取 `longSpan × shortSide`，再在屏幕内居中，两向都做了不超过屏幕的
        截断，因此横竖屏都会得到「形状方向正确、尺寸贴合屏幕」的小窗
    - **设置管线调整**：`FocusRestoreSettings` 删除 `showClockIconDivider`、
      `clockIconDividerSymbol`、`hideNotificationIcons` 三个字段，新增
      `hideIconsMode`、`focusVerticalOffsetDp`、`freeformSizeMode`（共 28 个字段）；
      `SettingsProvider` 游标仍为 28 列，列序与
      `HookSettings.fromCursor` 逐位对齐（第 12 列由布尔改为三态整数、第 13 列改为
      垂直偏移）。旧版 `hide_notification_icons` 布尔键会被读取一次并迁移为
      `hide_icons_mode` 的 0/1，保证升级后行为不变
    - 版本提升为 `1.3.0`（versionCode 107）


26. **`1.4.0`：小窗尺寸以实测定为准、双击呼出、图标占位修复、负间距、锁屏隐藏**
    - **小窗尺寸（再次修正，依据实机日志）**：LSPosed 日志显示 1.3.0 的唯一一次
      点击走的是 `sizeMode=system`（不下发 bounds），而**平台对该种启动给出的默认
      尺寸只有约 412×732 px**（AOSP 的 412dp×732dp 常量被当作像素使用），与用户
      截图实测（约 405×780 px）互相印证——所谓「系统默认」本身就是那个过小的窗口。
      因此：
      - 「小米默认比例」改为按**用户实测的正常默认小窗** 847×1350 px
        （1200×2670 屏幕）换算：宽度取屏幕宽的 `847/1200`、高度取屏幕高的
        `1350/2670`，横屏时两个比例对调（宽取 1350/2670、高取 847/1200），
        再截断到屏幕内并居中，横竖屏均自动适配
      - **默认值改为「小米默认比例」**；存储值 0（1.3.0 的「系统默认」）在新版
        中读作「小米默认比例」，即升级后立即得到正确尺寸，「系统默认」保留为
        可选项（存储值 1）供对照
    - **新增「双击呼出」** `click_double_tap`（默认关）：开启后单击只"上膛"、
      400 ms 内第二击才触发点击行为（小窗/直接打开），避免误触；点击模式选
      「不响应点击」时该项禁用。实现于 `FocusHostView` 的点击监听内，
      记录 `SystemClock.uptimeMillis()` 时间戳
    - **修复「仅隐藏本应用」后图标仍占位**：原先只对命中的 `StatusBarIconView`
      做 `GONE`，但 `NotificationIconContainer` 布局时仍会为它保留宽度。现在隐藏时
      同时把该图标的 `LayoutParams` 尺寸清零（width/height=0、四边 margin=0），
      还原时按快照恢复（快照含可见性 + 尺寸 + 边距，存于
      `LinkedHashMap<View, int[]>`）
    - **「距左侧时钟间距」允许负值**：范围改为 **−24 ~ +24 dp**（默认 0）。0 dp
      只是去掉模块自己加的间距，ROM 在状态栏槽位上还有自带的留白，负值用负
      margin 把焦点通知继续向时钟方向压，直到几乎贴住。**删除「距右侧间距」
      选项**（`focus_margin_end_dp` 字段保留但不再对外暴露，固定 4 dp）
    - **新增「锁屏时隐藏焦点通知」** `hide_on_lockscreen`（默认关）：开启后
      `KeyguardManager.isKeyguardLocked()` 为真时不渲染焦点通知（等同无候选，
      Host 隐藏、通知图标还原），并在 `ACTION_SCREEN_OFF / ACTION_SCREEN_ON /
      ACTION_USER_PRESENT` 广播时重新评估，锁屏/解锁即时生效
    - **设置管线**：`FocusRestoreSettings` 新增 `clickDoubleTap`、`hideOnLockscreen`
      两个字段；`SettingsProvider` 游标扩为 **30 列**（第 28 列 `hide_on_lockscreen`、
      第 29 列 `click_double_tap`，追加在末尾，原有列序不变）；
      `HookSettings.fromCursor` 同步
    - 版本提升为 `1.4.0`（versionCode 108）


27. **`1.4.1`：小窗内应用按窗口尺寸原生重排、通知图标隐藏改走容器自有通道**
    - **修复小窗内应用渲染错乱（关键）**：用户对比截图显示，模块拉起的小窗中应用内容
      缩放/布局错误（标题与计时数字重叠），而 MIUI 手动小窗完全正常。原因是目标应用
      未声明 `resizeableActivity`，在 freeform 任务里进入 size-compat 模式：应用仍按
      全屏尺寸布局，再被缩放进窗口，Compose 布局与缩放不匹配导致重叠。现新增
      **system_server（android）作用域钩子**：hook `ActivityRecord#supportsSizeChanges`，
      当所属任务处于 freeform（windowingMode==5）时返回 true，使应用像 MIUI 手动小窗
      一样按窗口实际尺寸原生重排。整个钩子体全部 try/catch，异常只会降级为原行为，
      不会波及 system_server。**需要在 LSPosed 中为模块勾选「系统框架 (android)」
      作用域**（`xposed_scope` 数组已加入 `android`）
    - **小窗启动不再加 FLAG_ACTIVITY_MULTIPLE_TASK**：与 MIUI 手动小窗一致，优先把
      应用已存在的任务移入 freeform（普通 resize），而不是每次新建一个重复任务
      （重复任务更容易落入兼容缩放状态）
    - **通知图标「仅隐藏本应用」重构**：1.4.0 直接清零图标 LayoutParams 的做法破坏了
      MIUI `NotificationIconContainer` 的双行布局状态机（出现全部图标消失、图标换行
      叠到桌面上、切回「不隐藏」后仍异常）。现改为通过反射设置容器
      `mIconStates` 中该图标 `IconState.hidden = true`，由容器自己的布局逻辑收起并
      回收占位；同时移除了对容器的 `setOnHierarchyChangeListener`（避免覆盖 MIUI
      自身监听）与布局参数写入。还原时置回 `hidden=false` 并恢复可见性后
      `requestLayout()`
    - 版本提升为 `1.4.1`（versionCode 109）


28. **`1.4.2`：按 Android 15 的真实图标布局机制重做「仅隐藏本应用」**
    - **1.4.1 的做法在原理上不成立**。1.4.1 认为把 `NotificationIconContainer` 里该图标的
      `IconState.hidden` 置为 `true`，容器就会自己收起并回收占位。核对 AOSP 15 的
      `NotificationIconContainer` 源码后确认这不可能生效：
        - `resetViewStates()` 在**每次布局**里对每个子项执行 `iconState.hidden = false;`，
          外部写入的 `hidden` 立刻被清掉；
        - `calculateIconXTranslations()` 只在 `iconState.hidden` 为真时把可视为
          `STATE_HIDDEN`，而**预留宽度**用的是
          `translationX += iconState.iconAppearAmount * view.getWidth() * drawingScale;`
          —— 完全不看 `hidden`。
      所以结果是「图标看不见了，但槽位照旧被占」，正是用户复测到的现象；
      在图标区溢出时该图标还会被重新置为 `STATE_ICON` 而重新出现
    - **现改用 `IconState.iconAppearAmount` 作为唯一生效的开关**：置 `0f` 时
      `calculateIconXTranslations()` 推进的宽度为 0，槽位真正被释放、后续图标左移补齐；
      同时仍置 `hidden = true` 作为双保险，并保留 `setVisibility(GONE)`
    - **每次布局都重新施加**：新增 `ensureIconLayoutHook()`，按容器**类**只挂一次，对
      `resetViewStates`（前 + 后）、`calculateIconXTranslations` / 旧名
      `calculateIconTranslations`（前）、`applyIconStates`（后）挂钩：
      `resetViewStates` 之后立刻重算一次以抵消它的清零；`applyIconStates` 之后把仍在
      隐藏列表里的图标压回 `GONE`，处理「溢出时被改回 STATE_ICON」的边界情况。
      方法名在本 ROM 上缺失时只记日志、不抛异常（`hooked=`/`states=` 会写进模块日志，
      便于后续排障）
    - **还原路径同步修正**：`collapseContainerIcon(..., false)` 会把
      `iconAppearAmount` / `clampedAppearAmount` 复位为 `1f` 并清掉 `hidden`，否则图标会
      永久保持在塌陷状态。原来的 `setContainerIconHidden()` 已删除
    - 「隐藏整个通知图标容器」与「不隐藏」两种模式的行为不变
    - 版本提升为 `1.4.2`（versionCode 110）


29. **`1.4.3`：小窗尺寸按实测定准（此前把"显示尺寸"当成了"任务尺寸"）**
    - **定位方法**：用系统自己的路径（侧边栏 / 全局小窗手势）把**同一个 App** 变成小窗，
      与模块路径做 A/B 对比，抓 `dumpsys window` / `dumpsys activity activities` /
      带 WM ProtoLog 的 logcat。同屏同密度（1200×2670，520dpi）下两边差得很干净：
      ```
      系统: Requested w=1200 h=1920   应用配置 sw369dp w369dp h591dp 520dpi nrml
      模块: Requested w=847  h=1350   应用配置 sw261dp w261dp h415dp 520dpi smll
      ```
    - **根因**：1.4.0 起用的 847×1350 是从用户截图量出来的**显示尺寸**，而不是任务尺寸。
      MIUI 对 freeform 任务会再统一乘一次显示缩放——
      `MiuiMultiWindowUtils.getOriFreeformScale()` 实测恒为 **0.7**（`screenType:1`），
      日志里可见 leashe 动画的 `dest=(Rect(...), sx=0.7, sy=0.7)`。
      于是 1.4.2 的实际观感是 847×1350 × 0.7 ≈ **593×945**，比系统小窗（1200×1920 × 0.7
      ≈ 840×1344）明显偏小；而且任务尺寸只有 261dp 宽，应用会进入
      `SCREENLAYOUT_SIZE_SMALL`（配置里的 `smll`），按小屏资源与紧凑布局渲染，
      这就是"内部文字/缩放不对、不像默认小窗"的原因（系统那份是 `nrml`）
    - **修正**：`measuredBounds()` 改为「短边整宽 × 短边×1.6」，即竖屏 **1200×1920**、
      横屏 1920×1200，居中下发；**不再自行折算 0.7**，那个缩放由 MIUI 自己施加。
      两个旧的比例常量 `MEASURED_SHORT_SIDE_FRACTION` / `MEASURED_LONG_SIDE_FRACTION`
      已被 `MEASURED_ASPECT = 1.6f` 取代
    - 说明：MIUI 接受模块下发的 bounds 而不覆盖（A/B 对比中模块任务的 `mBounds`
      与我们请求的值逐像素一致），因此这里只需把数值改对
    - 版本提升为 `1.4.3`（versionCode 111）


30. **`1.4.4`：小窗水平居中（此前贴着屏幕左边）**
    - 1.4.3 修好尺寸后，用户反馈小窗**直接贴在屏幕左侧**（左边距为 0），需要改成居中的位置
    - **位置换算关系（由截图量测 + `dumpsys` 交叉推出）**：MIUI 把任务矩形**以左上角为锚点**
      乘上 `DISPLAY_SCALE = 0.7` 作为可视窗口，即
      `可视左上 = 任务左上`、`可视宽高 = 任务宽高 × 0.7`。
      所以 1.4.3 把任务矩形水平居中（1200 宽的任务 → `left = 0`）时，可视窗口的左边距也成了 0
    - **实测数据**（Xiaomi 15，1200×2670）：
      ```
      1.4.3（贴左）: 任务 Rect(0, 375 - 1200, 2295)  可视 左0 右838 上374
      ROM 自己的位置: 任务 Rect(203,486 - 1403,2406) 可视 左181 右1032 上406
      ```
    - **修正**：水平方向改为**按可视宽度居中**：
      `visibleWidth = round(width × 0.7)`，`left = (屏宽 − visibleWidth) / 2`
      （1200×2670 上即 `left = 180`，可视 180…1020，与 ROM 的 181…1032 基本重合）。
      **垂直方向保持原来的「任务矩形居中」不变**——实测该值在这台机上是 375px，
      而 ROM 自己落在 406px，本来就在正确位置，不动它以免反而偏下
    - 新增常量 `DISPLAY_SCALE = 0.7f`，并明确注释：它**只用于算位置，绝不用于算尺寸**
    - 版本提升为 `1.4.4`（versionCode 112）


31. **`1.5.0`：应用打开时隐藏焦点通知；加回时钟↔焦点通知分隔符**
    - **新增「应用打开时隐藏焦点通知」**（`hide_when_app_open`，默认关）：当发出焦点通知的
      应用正在前台运行时（**全屏或 freeform 小窗都算**）隐藏焦点通知，应用退到后台即恢复。
      判定方式：反射调用 `android.app.ActivityTaskManager.getService()` 的
      `getFocusedRootTaskInfo()`（失败时退回 `getTasks(1)`），取 `topActivity` /
      `baseActivity` 的包名与焦点通知所属包比对。因为焦点通知只在通知变化时重绘，
      另加一个 800ms 的轻量轮询（仅在「有焦点项 + 开关打开」时运行），只在前后台
      状态**翻转**时才触发重绘，避免无谓刷新
    - **该场景下不隐藏通知图标**：隐藏焦点通知时走的是与「锁屏隐藏」同一条分支，
      会 `restoreAllNotificationIconHiding()`，所以应用自己的通知图标照常留在状态栏，
      用户仍能看到"有个东西在跑"的线索（日志里区分 `reason=appOpen`）
    - **加回时钟与焦点通知之间的分隔符**（1.3.0 曾整体删除），四项都可调：
      `clock_divider_symbol`（分隔符内容，**留空＝不显示**，按原样绘制）、
      `clock_divider_margin_start_dp`（与时钟的间距，0–24dp，
      在原有「距左侧时钟间距」之上**额外**增加）、
      `clock_divider_margin_end_dp`（与焦点通知的间距，0–24dp）、
      `clock_divider_color_light` / `clock_divider_color_dark`（亮/暗场景颜色，
      留空跟随系统＝沿用状态栏原生反色，与「文字颜色」页同一套预置色 + R/G/B 取色器）
    - **渲染实现**：分隔符作为 `FocusHostView` 的**第一个子 view**，其 `marginStart` 即
      与时钟的间距；随后用 `measure(UNSPECIFIED)` 量出它的宽度，把后续元素（应用图标、
      焦点文字）的 `marginStart` 与走马灯的 `contentInsetPx` 整体后移
      `起始间距 + 分隔符宽度 + 结束间距`，因此分隔符不会被滚动内容压到。
      颜色在状态栏 tint 变化时经 `updateIconTint()` 重新解析
    - **取色器复用**：`addColorBlock` / `createPresetButton` / `installColorListeners`
      原先硬编码「亮/暗文字色」两个目标，现改为 `COLOR_TEXT_LIGHT/DARK` +
      `COLOR_DIVIDER_LIGHT/DARK` 四个目标（`colorValue`/`setColorValue`/`colorExpanded`/
      `setColorExpanded`），同一套色块实现服务两组颜色
    - 设置管线三处同步扩展：游标 30 → **36 列**（新列全部追加在末尾，原列序不变）
    - 版本提升为 `1.5.0`（versionCode 113）


32. **`1.5.1`：分隔符负间距与上下位置、应用图标不再反色、前台隐藏的响应延迟**
    - **分隔符起点间距支持负值**：新增 `MIN_CLOCK_DIVIDER_MARGIN_START_DP = -24`，
      与「距左侧时钟间距」一样可以用负边距把分隔符继续压向时钟（结束间距仍为 0–24，
      只有正值有意义）
    - **新增分隔符上下位置** `clock_divider_vertical_offset_dp`（−10 ~ +10 dp，默认 0），
      以 `divider.setTranslationY()` 实现，只影响分隔符自身、不动其它元素
    - **分隔符亮/暗颜色移到「焦点通知」页**：1.5.0 把它们放在「文字颜色」页，用户找不到，
      现改为紧跟分隔符的符号与间距设置之后，同一张卡片内可直接选预置色或 R/G/B 调色
    - **应用图标不再反色**：此前 `FocusAppIcon` 优先取自适应图标的 **单色层**并用状态栏
      内容色着色，导致所有应用都变成同一个反色字形，且随亮暗场景整体反转。现在直接返回
      应用自己的彩色启动器图标，并删除 `retint()`（`updateIconTint()` 只再处理分隔符）
    - **修复「应用打开时隐藏」恢复慢 3–6 秒**：`render()` 原先只在"要显示焦点通知"的分支里
      挂轮询，而应用打开时走的是提前 return 的隐藏分支，于是**没有任何东西在观察应用是否
      退回后台**，只能等下一次无关通知事件触发重绘。现在在分支判断之前就挂上轮询
      （`item != null` 即挂），并把轮询间隔从 800ms 收紧到 **400ms**
    - 游标 36 → **37 列**（新列仍追加在末尾）
    - 版本提升为 `1.5.1`（versionCode 114）


33. **`1.5.2`：分隔符距时钟间距改走 host margin，并加入布局坐标日志**
    - **问题**：用户反馈「分隔符与左侧时钟间距」怎么调都没反应
    - **改法**：该间距原先写成分隔符自己的 `marginStart`——也就是**一个子 view 在
      `WRAP_CONTENT` 的 FrameLayout 里的边距**。现改为折算进 **host 自身的 `marginStart`**：
      这是本模块里已经被验证有效的那条通道（「距左侧时钟间距」用的就是它），
      不再依赖子边距在 WRAP_CONTENT 父容器中的行为。分隔符自身的 `marginStart` 归 0，
      避免重复计入；`leftOffsetPx` 也相应只加「分隔符宽度 + 结束间距」
    - 语义不变：该值仍是「在『距左侧时钟间距』之上**额外**增加的间隔」，可负
    - **新增布局坐标日志**（`FocusHostView.onLayout` → `logLayoutPositions()`，
      内容变化才打印一次）：
      `OS4 layout slot=?+? host=?+? divider=?+? content=?+? inset=?`
      并把「实际生效的 divider 设置」并入原有那行 marquee 日志：
      `divider=? dividerStartDp=? dividerEndDp=? dividerVerticalDp=?
      hostMarginStartDp=? hostMarginStartPx=?`
      —— 前者回答「设置有没有送到 SystemUI」，后者回答「送到了但布局没动」
    - 提示：与「距左侧时钟间距」一样，改完需要重启系统界面才会重新渲染
    - 版本提升为 `1.5.2`（versionCode 115）


34. **`1.5.3`：修掉分隔符「与时钟间距」的负值被 SystemUI 侧夹掉**
    - **现象**：该滑块正值有效，往负方向拉却毫无反应。
    - **根因**：管线三层里只有中间那层没同步下限——
      `FocusRestoreSettings`（app 侧）构造时用 `MIN_CLOCK_DIVIDER_MARGIN_START_DP = -24`
      做下限、`SettingsProvider` 原样透传，但 **`HookSettings`（SystemUI 侧快照）
      仍用 `MIN_CLOCK_DIVIDER_MARGIN_DP = 0` 做下限**，于是任何负值都在进入 SystemUI
      时被静默夹回 0，渲染出来自然和 0 一模一样。
    - **修法**：`HookSettings` 中起点改用 `MIN_CLOCK_DIVIDER_MARGIN_START_DP`
      （终点仍是 0–24，只允许正值）。
    - **同时加固**：host 的总起点位移拆成两段——非负部分走 layout margin
      （已被验证有效的通道），负的部分走 `setTranslationX`。
      理由是部分 ROM 容器会丢弃负 margin，而 translation 一定会生效；
      `margin + translation == hostStartPx` 恒成立，所以不会出现重复偏移。
    - **排查日志**：marquee 行现在同时打印 `dividerStartPx` / `hostStartPx` /
      `hostMarginPx` / `hostTransXPx` 四个值。以后若某个方向又"调不动"，
      **先看 `hostStartPx` 有没有随滑块变化**：变了但画面没动＝布局层被夹；
      压根没变＝设置没送到 SystemUI。
    - 版本提升为 `1.5.3`（versionCode 116）


35. **`1.6.0`：设置界面重构为「顶部栏 + 四个 Tab + 底部固定导航」，新增渐隐渐显动画**
    - **界面结构**（信息分层、功能按类隔离）：
      - 顶部标题栏（固定）：左「返回箭头」、中「焦点回归」、右「重启系统界面」。
        标题栏左右两侧宽度做成一致，标题严格居中；`onBackPressed` 与返回箭头行为统一——
        **先回到「焦点」Tab，已在首个 Tab 时才退出设置**
      - 底部固定四 Tab：**焦点 / 样式 / 高级 / 关于**。原先是「显示 / 更多」两段 +
        主页面点进子页的两级结构，现已拍平为单层；切 Tab 只替换中间内容区并自动滚回顶部，
        顶栏与底栏不重建
      - 中间内容区是**唯一可滚动区域**，每张圆角卡片负责一类功能，卡片标题加粗
      - **没有任何设置项被删掉**：原有 27 项全部保留，只是重新分层；
        每张卡片用新的 `cardTitle()` 在卡内加粗体标题（此前卡片没有标题）
    - **卡片归属**：
      - 焦点：焦点通知基础设置（渐隐渐显 + 时长、上下位置）/ 通知滚动设置（限制宽度与最大宽度、
        滚动启动延迟、往返滚动、兼容重试）/ 状态栏图标（隐藏通知图标三选一）/ 显示时机
        （锁屏时隐藏、应用打开时隐藏）/ 小窗打开（点击方式、双击呼出、失败直接打开、小窗尺寸）
      - 样式：文字基础样式（字体大小、字体、字重、斜体）/ 文字颜色（亮暗两个场景）/
        应用图标（显示开关、图标大小、图标与文字间距）
      - 高级：分隔符设置（符号、与时钟间距、与通知间距、上下位置、亮暗颜色）/ 时钟关联
        （距左侧时钟间距）/ 超级内容转换（转换超级岛内容、强制转换白名单、内容连接符号）/
        调试选项（仅 DEBUG 构建）
      - 关于：模块信息 / 开源信息 / 外部链接 / 版权与许可证 / 说明
    - **新增「渐隐渐显动画」**：`focus_fade_enabled`（默认开）+ `focus_fade_duration_ms`
      （60–800 ms，默认 180），位于「焦点 → 焦点通知基础设置」。
      渲染侧在 `HyperOS4FocusController` 用 `ViewPropertyAnimator` 做 alpha 过渡：
      出现时**只在「从不可见变可见」的那一次**淡入（否则每条通知更新都会重放动画而闪烁）；
      消失时先淡出、动画结束后才真正 GONE 并还原通知图标（抽出 `applyHiddenState()`）。
      淡出回调带**代际校验**（`renderGeneration`）：动画期间来了新的焦点通知就放弃这次隐藏，
      不会把新通知吃掉
    - 设置管线三处同步扩展：游标 37 → **39 列**（新列仍只追加在末尾）
    - 版本提升为 `1.6.0`（versionCode 117）


36. **`1.6.1`：按 Miuix（HyperOS 组件库）官方令牌重做界面外观**
    - **背景**：用户指出「MIUI X 风格」实为 **Miuix** ——
      `compose-miuix-ui/miuix`，Compose Multiplatform 的 HyperOS 组件库（Apache-2.0）。
      其 `miuix-ui-android` 的 aar 元数据要求 **`minCompileSdk=37`**，本机 SDK 只有 35，
      真引入需连带升级 SDK / AGP / Gradle / Kotlin / Compose 并把构建改为联网，
      因此**决定照令牌用 View 复刻外观，不引入依赖**。
    - **令牌来源**（全部取自官方文档，逐项照抄，不再自行估值）：
      - 页面 surface `#F7F7F7` / `#000000`；卡片 surfaceVariant `#FFFFFF` / `#242424`
      - `CardDefaults.CornerRadius = 16.dp`；列表页左右内边距 **12dp**、卡间距 **12dp**
      - `BasicComponentDefaults.InsideMargin = 16.dp`（行内边距 16dp、最小高度 56dp）
      - `SmallTitleDefaults.InsideMargin = PaddingValues(28.dp, 8.dp)`，色值 `onBackgroundVariant`
        `#8C93B0` / `#787E96`（28dp = 页面 12 + 卡内 16，故与行文字左对齐）
      - `dividerLine` `#E0E0E0` / `#393939`，行分隔线**左缩进 16dp、右到边**
      - `primary` `#3482FF` / `#277AF7`；选中态 = `tertiaryContainer` `#EAF2FF` / `#2B3B54`
        ＋ `onTertiaryContainer` `#3482FF` / `#4788FF`
      - `onSurfaceVariantActions` 40%（次级文字/箭头）；`onSurfaceVariantSummary` 60% / 50%
      - 开关关态轨道 `secondary` `#E6E6E6` / `#505050`；滑块轨道 `sliderBackground` 6% / 15%
    - **界面调整**（对应用户四点要求）：
      - **不再渲染任何逐项小字说明**：`titleBox()` 保留 summary 形参但不再输出，
        说明既是设计决定也顺带把行高收敛到 Miuix 的 56dp
      - **顶栏去掉返回按钮**，标题「焦点回归」改为**左对齐 20sp**，顶栏与页面同底色、无投影；
        右侧仅保留重启系统界面（系统返回键仍保留"先回焦点 Tab"的行为）
      - **分组标题移到卡片上方**（`SmallTitle` 规格），原来的卡内加粗标题 `cardTitle()` 已删除；
        同时移除全部整页级 note（`addNote()` 调用），页面更干净
      - **关于页**：`开源信息` 改为「作者 Guchnn」「底层设置基于 ImKani」「开源许可证 GPL-3.0-only」；
        外部链接里「二次创作者 GitHub」→「**模块作者 GitHub**」
    - **控件实现**：开关改为**代码构造的 44×26dp 胶囊**（`GradientDrawable` 轨道 +
      `InsetDrawable` 内缩 2dp 的白色 22dp 圆点，用 `StateListDrawable` 切 on/off），
      不再依赖系统默认 thumb/track；滑块补上 `progressBackgroundTint`；
      段选按钮选中态改为浅蓝底＋蓝字、未选中为浅灰底＋40% 黑字、圆角 8dp、高 34dp
    - 版本提升为 `1.6.1`（versionCode 118）


37. **`1.7.0`：设置界面真正引入 Miuix（Compose Multiplatform）组件库重写**
    - **背景**：`1.6.1` 是"照 Miuix 令牌用纯 View 复刻外观、不引入依赖"；`1.7.0` 按用户决定
      **直接引入该组件库**（`compose-miuix-ui/miuix`，Apache-2.0），用真实控件重写整个设置界面。
      为此工具链连带升级：**compileSdk 37 / AGP 8.9.1 / Gradle 8.11.1 / Kotlin 2.4.20 /
      Compose 1.11.1**，`miuix-ui` + `miuix-preference` + `miuix-icons` 锁定 **0.9.3**；
      `miuix-ui-android` 的 aar 元数据要求 `minCompileSdk=37`，构建改为**联网解析依赖**。
    - **界面实现**（`ComposeSettingsActivity` + `TabIcons` + `SettingsScreen`）：四 Tab 结构沿用
      `1.6.0` 的设计（焦点 / 样式 / 高级 / 关于），外观全部用 Miuix 真实组件——
      `Scaffold` / `SmallTopAppBar`（右「重启系统界面」）/ `NavigationBar` + `NavigationBarItem`
      （底部四 Tab，图标为自绘 `ImageVector`，避免未确认的 `MiuixIcons` 包依赖）/
      `Card` / `SmallTitle` / `HorizontalDivider` / `SwitchPreference` / `SliderPreference` /
      `RadioButtonPreference` / `ArrowPreference` / `TextField` / `BasicComponent` /
      `OverlayDialog` / `TextButton`；颜色选择器用 `OverlayDialog` + 自绘预置色板实现，
      "跟随系统"以 `NO_FOCUS_TEXT_COLOR` 表示。
    - **设置管线零改动**：Compose 侧仅读写原有的 Java `FocusRestoreSettings`
      （`SharedPreferences` + `SettingsProvider` 39 列游标），SystemUI 的 Hook 完全不受影响；
      旧的纯 View `SettingsActivity` 保留为**未导出（exported=false）的回退入口**。
    - **关于页**：`作者 Guchnn` / `底层设置基于 ImKani` / `开源许可证 GPL-3.0-only`；
      外部链接「模块作者 GitHub」→ `github.com/Guchnn`，「上游项目主页」→ `github.com/ImKani/HyperOS3FocusRestore`。
    - **重启系统界面**：复用原有逻辑（先 `su` 探测、再依次尝试三条 kill 命令），结果以 Toast 提示，
      失败时引导用户手动重启或授予 root。
    - **签名不变**：仍用 `_keys/focusreturn-release.jks`（证书 SHA-256
      `1977345a…30a6`），V3 签名，可覆盖安装全部历史版本。
    - 版本提升为 `1.7.0`（versionCode 119）


38. **`1.7.1`：顶栏标题居左、重启改为图标且二次确认；焦点/样式/高级 三个 Tab 的分组改为可折叠一级菜单**
    - **顶栏标题居左**：`SmallTopAppBar` 内部把 `title` 强制水平居中且无对齐参数，因此把
      "焦点回归"标题移入 `navigationIcon` 槽（靠左渲染），`title` 留空，实现左对齐效果。
    - **重启系统界面改为图标控件**：原先右侧是文字按钮「重启系统界面」，现改为自绘的刷新
      箭头图标（`TabIcons.Restart`，Material refresh 字形）；点击后**先弹 `OverlayDialog`
      二次确认**，确认才真正执行重启，避免误触导致状态栏重载。
    - **三个 Tab 的分组改为可折叠一级菜单**：`焦点` / `样式` / `高级` 三个 Tab 内原先平铺的
      卡片分组（如「焦点通知基础设置」「通知滚动设置」「状态栏图标」「显示时机」「小窗打开」、
      「文字基础样式」「文字颜色」「应用图标」、「分隔符设置」「时钟关联」「超级内容转换」）
      改为可点击展开/收起的一级菜单（`CollapsibleGroup`）：表头是一个 `BasicComponent`，
      右侧带随展开状态旋转 180° 的箭头（`TabIcons.Chevron`），点击展开后在其下方显示该功能
      组下的全部设置项；默认收起（"收纳"状态）。`关于` Tab 保持平铺不变。
    - **签名不变**：仍用 `_keys/focusreturn-release.jks`（证书 SHA-256
      `1977345a…30a6`），V3 签名，可覆盖安装全部历史版本。
    - 版本提升为 `1.7.1`（versionCode 120）


39. **`1.7.2`：项目完全独立化改造（全新独立开源仓库）**
    - **全局包名更换**：`com.hyperos3.focusrestore` → `com.guchnn.focusreturn`，覆盖全部
      源码、配置、常量、日志 TAG、ContentProvider 注册信息（authority 改为
      `com.guchnn.focusreturn.settings`）、硬编码字符串；不留旧包名残留。
    - **LSPosed 入口类更名**：`HyperOS3FocusRestoreHook` → `FocusReturnHook`
      （`assets/xposed_init` 与 `proguard-rules.pro` 同步更新）；日志 TAG 由
      `HyperOS3FocusRestore` 统一改为 `FocusReturn`。
    - **项目正式更名**：英文名 `FocusReturn`、中文名 **焦点回归**；所有界面标题、关于页、
      字符串资源、日志标识同步更新（旧拼写一并修正）。
    - **独立仓库定位**：不再依附上游 Fork，作为基于 HyperOS3FocusRestore 0.13.6 深度二次开发、
      界面全重构、功能大量新增的**独立衍生作品**发布；上游作者 ImKani 的版权声明完整保留，
      仅追加 Guchnn 的修改标注，严格遵循 **GPL-3.0-only**。
    - **兼容与共存**：新包名 + 新模块 ID 使本模块与原版完全独立、互不覆盖、可共存安装，
      无包名冲突、无旧标识残留、无日志残留。
    - 签名不变：仍用 `_keys/focusreturn-release.jks`（证书 SHA-256 `1977345a…30a6`），V3 签名。
    - 版本提升为 `1.7.2`（versionCode 121）


40. **`1.0`：界面结构调整、重启按钮修复、颜色自定义回归、上下位置拆分**
    - **重启系统界面修复**：右上角图标原先是一个"看起来像空圆圈"的淡色字形，
      现改为清晰的**顺时针环形箭头**（`TabIcons.Restart`：整圆留 40° 缺口 + 实心箭头）；
      颜色由 40% 透明的 `onSurfaceVariantActions` 改为不透明的 `onSurface`，点击区 40→44dp。
      点击后仍然**先弹二次确认对话框**，确认才真正重启 SystemUI。
    - **颜色自定义回归**：取色器此前只剩 8 个硬编码色块，**丢失了完整自定义能力**；
      现恢复为 `FocusTextStyle` 的**全部 14 个预设色**（含"跟随系统"），并**加回 R / G / B
      三通道滑杆 + 实时色块与十六进制预览**，以"应用自定义颜色"提交 —— 即 View 版
      `addColorBlock` 的完整自定义能力。
    - **「焦点通知上下位置」拆分为两项**：删除整条偏移滑杆，改为**「图标上下位置」**与
      **「文字上下位置」**两枚独立滑杆（各 -20..20 dp）；渲染时分别作用于应用图标
      `ImageView` 与文本内容（各自 `setTranslationY`），宿主不再整体偏移。
      设置管线三处同步：`FocusRestoreSettings`（2 个新键）→ `SettingsProvider`
      （39 → **41 列**，**末尾追加**以保持既有列序）→ `HookSettings.fromCursor`（索引 39/40）。
    - **界面结构调整**：**删除「高级」Tab**（现只剩 焦点 / 样式 / 关于 三个）；
      「分隔符设置」（含分隔符颜色）整体迁入**样式** Tab；「超级内容转换」
      （含强制转换白名单、内容连接符号）迁入**焦点** Tab；**删除「时钟关联」**分组。
    - **版本号**：`versionName` 改为 **`1.0`**；`versionCode` 保持单调递增（121 → 122），
      以免已装 1.7.2 的用户无法覆盖安装。
    - **启动器图标改黑白配色**：自适应图标背景由蓝紫渐变改为**近黑单色**
      （`#141414 → #000000`），前景保持白色，Android 13+ 单色层（主题图标）保持纯黑。
    - 签名不变：仍用 `_keys/focusreturn-release.jks`（证书 SHA-256 `1977345a…30a6`），V3 签名。


## 分发时需要注意的义务

如果只是**自己安装、自己使用**，GPL-3.0 不要求你公开任何东西。
一旦你把 APK **分发给他人**（发布到论坛、酷安、GitHub Releases 等），GPL-3.0 第 6 条要求你同时：

1. 随附本许可证正文（本目录的 `COPYING`），或给出有效的书面报价；
2. 提供**完整对应源码**（Complete Corresponding Source）——即本目录中的全部源码，
   包含构建脚本；直接附上源码压缩包是最省事的做法；
3. 保留所有版权声明与这份修改说明；
4. 不得以任何方式限制接收者依据 GPL-3.0-only 享有的权利（例如不得追加
   "禁止二次分发"、"仅个人使用" 之类的条件）。

## 商标与署名

本衍生作品**不是**上游作者的官方版本，不得暗示其由上游作者背书或认可。
上游作者 ImKani 的版权声明保留在源文件头部与本文件中；二次创作者 Guchnn
的署名同时保留在上述位置，二者互不覆盖。
