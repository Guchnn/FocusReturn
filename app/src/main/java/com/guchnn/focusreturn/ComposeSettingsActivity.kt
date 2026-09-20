package com.guchnn.focusreturn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** Miuix 版设置界面。设置仍然读写原 Java 的存储，Hook 侧完全不受影响。 */
class ComposeSettingsActivity : ComponentActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var hookPrefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(FocusRestoreSettings.PREFS_NAME, Context.MODE_PRIVATE)
        hookPrefs = FocusRestoreSettings.hookPreferences(this)
        if (!FocusRestoreSettings.hasHookSettings(hookPrefs)) {
            FocusRestoreSettings.fromPreferences(prefs).save(hookPrefs)
        }
        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                var draft by remember {
                    mutableStateOf(SettingsDraft.from(FocusRestoreSettings.fromPreferences(prefs)))
                }
                val scope = rememberCoroutineScope()
                // 防抖保存：滑块连续拖动只落盘一次，且两个存储都写。
                LaunchedEffect(draft) {
                    delay(400)
                    withContext(Dispatchers.IO) {
                        val s = draft.toSettings()
                        s.save(prefs)
                        s.save(hookPrefs)
                    }
                }
                SettingsScreen(
                    draft = draft,
                    onChange = { draft = it },
                    onRestartSystemUi = { restartSystemUi(this) { msg -> Toast.makeText(this@ComposeSettingsActivity, msg, Toast.LENGTH_SHORT).show() } },
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ 状态桥接 */

/** FocusRestoreSettings 的 39 个字段的可变副本，负责与 Java 侧互转。 */
data class SettingsDraft(
    var hookMode: Int,
    var limitWidth: Boolean,
    var widthDp: Int,
    var marqueeDelayMs: Int,
    var compatRetry: Boolean,
    var marqueeBounce: Boolean,
    var islandCompat: Boolean,
    var disableIslandProperty: Boolean,
    var disableIslandFeatureCache: Boolean,
    var hideIconsMode: Int,
    var focusVerticalOffsetDp: Int,
    var focusIconVerticalOffsetDp: Int,
    var focusTextVerticalOffsetDp: Int,
    var focusFontSizeSp: Int,
    var focusTextColorLight: Int,
    var focusTextColorDark: Int,
    var focusFontFamily: Int,
    var focusFontWeight: Int,
    var focusFontItalic: Boolean,
    var showAppIcon: Boolean,
    var appIconSizeDp: Int,
    var focusMarginStartDp: Int,
    var focusMarginEndDp: Int,
    var iconTextGapDp: Int,
    var clickOpenMode: Int,
    var clickDoubleTap: Boolean,
    var freeformFallbackDirect: Boolean,
    var freeformSizeMode: Int,
    var hideOnLockscreen: Boolean,
    var hideWhenAppOpen: Boolean,
    var clockDividerSymbol: String,
    var clockDividerMarginStartDp: Int,
    var clockDividerMarginEndDp: Int,
    var clockDividerColorLight: Int,
    var clockDividerColorDark: Int,
    var clockDividerVerticalOffsetDp: Int,
    var focusFadeEnabled: Boolean,
    var focusFadeDurationMs: Int,
    var islandGeneralSeparator: String,
    var islandSideSeparator: String,
    var islandForcePackages: MutableSet<String>,
) {
    fun toSettings(): FocusRestoreSettings = FocusRestoreSettings.withValues(
        hookMode, limitWidth, widthDp, marqueeDelayMs, compatRetry, marqueeBounce, islandCompat,
        disableIslandProperty, disableIslandFeatureCache, hideIconsMode, focusVerticalOffsetDp,
        focusFontSizeSp, focusTextColorLight, focusTextColorDark,
        focusFontFamily, focusFontWeight, focusFontItalic,
        showAppIcon, appIconSizeDp,
        focusMarginStartDp, focusMarginEndDp, iconTextGapDp,
        clickOpenMode, clickDoubleTap, freeformFallbackDirect, freeformSizeMode,
        hideOnLockscreen, hideWhenAppOpen, clockDividerSymbol,
        clockDividerMarginStartDp, clockDividerMarginEndDp,
        clockDividerColorLight, clockDividerColorDark, clockDividerVerticalOffsetDp,
        focusFadeEnabled, focusFadeDurationMs,
        islandGeneralSeparator, islandSideSeparator, islandForcePackages,
        focusIconVerticalOffsetDp, focusTextVerticalOffsetDp,
    )

    companion object {
        fun from(s: FocusRestoreSettings) = SettingsDraft(
            s.hookMode, s.limitWidth, s.widthDp, s.marqueeDelayMs, s.compatRetry, s.marqueeBounce,
            s.islandCompat, s.disableIslandProperty, s.disableIslandFeatureCache,
            s.hideIconsMode, s.focusVerticalOffsetDp,
            s.focusIconVerticalOffsetDp, s.focusTextVerticalOffsetDp, s.focusFontSizeSp,
            s.focusTextColorLight, s.focusTextColorDark,
            s.focusFontFamily, s.focusFontWeight, s.focusFontItalic,
            s.showAppIcon, s.appIconSizeDp,
            s.focusMarginStartDp, s.focusMarginEndDp, s.iconTextGapDp,
            s.clickOpenMode, s.clickDoubleTap, s.freeformFallbackDirect, s.freeformSizeMode,
            s.hideOnLockscreen, s.hideWhenAppOpen, s.clockDividerSymbol,
            s.clockDividerMarginStartDp, s.clockDividerMarginEndDp,
            s.clockDividerColorLight, s.clockDividerColorDark, s.clockDividerVerticalOffsetDp,
            s.focusFadeEnabled, s.focusFadeDurationMs,
            s.islandGeneralSeparator, s.islandSideSeparator,
            HashSet(s.islandForcePackages),
        )
    }
}

/* ------------------------------------------------------------------ 主界面 */

private const val TAB_FOCUS = 0
private const val TAB_STYLE = 1
private const val TAB_ABOUT = 2

@Composable
private fun SettingsScreen(
    draft: SettingsDraft,
    onChange: (SettingsDraft) -> Unit,
    onRestartSystemUi: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(TAB_FOCUS) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            // SmallTopAppBar 内部把 title 强制居中，没有对齐参数；
            // 因此把标题放到 navigationIcon 槽（靠左），title 留空。
            SmallTopAppBar(
                title = "",
                navigationIcon = {
                    Text(
                        text = "焦点回归",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { showRestartConfirm = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = rememberVectorPainter(TabIcons.Restart),
                            contentDescription = "重启系统界面",
                            // 用不透明的 onSurface 而非 40% 的 onSurfaceVariantActions：
                            // 后者太淡，重启按钮看起来像个没有功能的装饰圆圈。
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == TAB_FOCUS, onClick = { tab = TAB_FOCUS },
                    icon = TabIcons.Focus, label = "焦点",
                )
                NavigationBarItem(
                    selected = tab == TAB_STYLE, onClick = { tab = TAB_STYLE },
                    icon = TabIcons.Style, label = "样式",
                )
                NavigationBarItem(
                    selected = tab == TAB_ABOUT, onClick = { tab = TAB_ABOUT },
                    icon = TabIcons.About, label = "关于",
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
        ) {
            when (tab) {
                TAB_STYLE -> StyleTab(draft, onChange)
                TAB_ABOUT -> AboutTab()
                else -> FocusTab(draft, onChange)
            }

            // 必须放在 Scaffold 内部！
            // Miuix 的 OverlayDialog 通过 CompositionLocal 把自身注册进「当前/根 Scaffold」
            // 的弹层列表，再由 Scaffold 内置的 MiuixPopupHost 真正渲染出来；而
            // LocalRootDialogStates / LocalDialogStates 在 Scaffold 之外只解析到一个
            // 没人消费的默认列表 → 弹窗永远不会显示。
            // 旧代码把它写在 Scaffold 之后（同级），因此点击右上角重启图标后
            // 弹窗从未出现，看起来就像"按钮没反应"。取色器/白名单弹窗因为在 Tab 内容里
            // （也就是 Scaffold 内部）所以一直正常。
            if (showRestartConfirm) {
                OverlayDialog(title = "重启系统界面", show = true, onDismissRequest = { showRestartConfirm = false }) {
                    Card {
                        Text(
                            text = "确定要重启系统界面吗？重启后状态栏会短暂重载，期间屏幕可能闪一下。",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { showRestartConfirm = false },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = "确认重启",
                            onClick = {
                                showRestartConfirm = false
                                onRestartSystemUi()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 一张卡片：可选的 SmallTitle 分组标题 + 内容（用于「关于」等保持平铺的分组）。 */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    SmallTitle(text = title)
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        content()
    }
}

/** 一级菜单：标题作为可点击的表头，点击展开/收起其下的功能。 */
@Composable
private fun CollapsibleGroup(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        BasicComponent(
            title = title,
            onClick = { expanded = !expanded },
            endActions = {
                Image(
                    painter = rememberVectorPainter(TabIcons.Chevron),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceVariantActions),
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
                )
            },
        )
        if (expanded) {
            HorizontalDivider()
            content()
        }
    }
}

/* ------------------------------------------------------------------ Tab 1 焦点 */

@Composable
private fun FocusTab(draft: SettingsDraft, onChange: (SettingsDraft) -> Unit) {
    var showWhitelist by remember { mutableStateOf(false) }
    CollapsibleGroup("焦点通知基础设置") {
        SwitchPreference(
            title = "渐隐渐显动画",
            checked = draft.focusFadeEnabled,
            onCheckedChange = { onChange(draft.copy(focusFadeEnabled = it)) },
        )
        if (draft.focusFadeEnabled) {
            HorizontalDivider()
            SliderPreference(
                title = "动画时长",
                value = draft.focusFadeDurationMs.toFloat(),
                onValueChange = { onChange(draft.copy(focusFadeDurationMs = it.toInt())) },
                valueText = "${draft.focusFadeDurationMs} ms",
                valueRange = FocusRestoreSettings.MIN_FOCUS_FADE_DURATION_MS.toFloat()..
                    FocusRestoreSettings.MAX_FOCUS_FADE_DURATION_MS.toFloat(),
            )
        }
        HorizontalDivider()
        SliderPreference(
            title = "图标上下位置",
            value = draft.focusIconVerticalOffsetDp.toFloat(),
            onValueChange = { onChange(draft.copy(focusIconVerticalOffsetDp = it.toInt())) },
            valueText = "${draft.focusIconVerticalOffsetDp} dp",
            valueRange = FocusRestoreSettings.MIN_FOCUS_ICON_VERTICAL_OFFSET_DP.toFloat()..
                FocusRestoreSettings.MAX_FOCUS_ICON_VERTICAL_OFFSET_DP.toFloat(),
        )
        HorizontalDivider()
        SliderPreference(
            title = "文字上下位置",
            value = draft.focusTextVerticalOffsetDp.toFloat(),
            onValueChange = { onChange(draft.copy(focusTextVerticalOffsetDp = it.toInt())) },
            valueText = "${draft.focusTextVerticalOffsetDp} dp",
            valueRange = FocusRestoreSettings.MIN_FOCUS_TEXT_VERTICAL_OFFSET_DP.toFloat()..
                FocusRestoreSettings.MAX_FOCUS_TEXT_VERTICAL_OFFSET_DP.toFloat(),
        )
    }

    CollapsibleGroup("通知滚动设置") {
        SwitchPreference(
            title = "限制焦点通知宽度",
            checked = draft.limitWidth,
            onCheckedChange = { onChange(draft.copy(limitWidth = it)) },
        )
        if (draft.limitWidth) {
            HorizontalDivider()
            SliderPreference(
                title = "最大宽度",
                value = draft.widthDp.toFloat(),
                onValueChange = { onChange(draft.copy(widthDp = it.toInt())) },
                valueText = "${draft.widthDp} dp",
                valueRange = FocusRestoreSettings.MIN_WIDTH_DP.toFloat()..
                    FocusRestoreSettings.MAX_WIDTH_DP.toFloat(),
            )
        }
        HorizontalDivider()
        SliderPreference(
            title = "滚动启动延迟",
            value = draft.marqueeDelayMs.toFloat(),
            onValueChange = { onChange(draft.copy(marqueeDelayMs = it.toInt())) },
            valueText = String.format(java.util.Locale.US, "%.1f 秒", draft.marqueeDelayMs / 1000f),
            valueRange = 0f..5000f,
        )
        HorizontalDivider()
        SwitchPreference(
            title = "往返滚动",
            checked = draft.marqueeBounce,
            onCheckedChange = { onChange(draft.copy(marqueeBounce = it)) },
        )
        HorizontalDivider()
        SwitchPreference(
            title = "兼容重试",
            checked = draft.compatRetry,
            onCheckedChange = { onChange(draft.copy(compatRetry = it)) },
        )
    }

    CollapsibleGroup("状态栏图标") {
        RadioButtonPreference(
            title = "不隐藏",
            selected = draft.hideIconsMode == FocusRestoreSettings.HIDE_ICONS_NONE,
            onClick = { onChange(draft.copy(hideIconsMode = FocusRestoreSettings.HIDE_ICONS_NONE)) },
        )
        HorizontalDivider()
        RadioButtonPreference(
            title = "隐藏其他",
            selected = draft.hideIconsMode == FocusRestoreSettings.HIDE_ICONS_OTHERS,
            onClick = { onChange(draft.copy(hideIconsMode = FocusRestoreSettings.HIDE_ICONS_OTHERS)) },
        )
        HorizontalDivider()
        RadioButtonPreference(
            title = "仅隐藏本应用",
            selected = draft.hideIconsMode == FocusRestoreSettings.HIDE_ICONS_FOCUS_APP,
            onClick = {
                onChange(draft.copy(hideIconsMode = FocusRestoreSettings.HIDE_ICONS_FOCUS_APP))
            },
        )
    }

    CollapsibleGroup("显示时机") {
        SwitchPreference(
            title = "锁屏时隐藏",
            checked = draft.hideOnLockscreen,
            onCheckedChange = { onChange(draft.copy(hideOnLockscreen = it)) },
        )
        HorizontalDivider()
        SwitchPreference(
            title = "应用打开时隐藏",
            checked = draft.hideWhenAppOpen,
            onCheckedChange = { onChange(draft.copy(hideWhenAppOpen = it)) },
        )
    }

    CollapsibleGroup("小窗打开") {
        RadioButtonPreference(
            title = "以小窗打开",
            selected = draft.clickOpenMode == FocusRestoreSettings.CLICK_OPEN_FREEFORM,
            onClick = {
                onChange(draft.copy(clickOpenMode = FocusRestoreSettings.CLICK_OPEN_FREEFORM))
            },
        )
        HorizontalDivider()
        RadioButtonPreference(
            title = "直接打开应用",
            selected = draft.clickOpenMode == FocusRestoreSettings.CLICK_OPEN_DIRECT,
            onClick = {
                onChange(draft.copy(clickOpenMode = FocusRestoreSettings.CLICK_OPEN_DIRECT))
            },
        )
        HorizontalDivider()
        SwitchPreference(
            title = "双击呼出",
            checked = draft.clickDoubleTap,
            onCheckedChange = { onChange(draft.copy(clickDoubleTap = it)) },
        )
        HorizontalDivider()
        SwitchPreference(
            title = "小窗失败时直接打开应用",
            checked = draft.freeformFallbackDirect,
            onCheckedChange = { onChange(draft.copy(freeformFallbackDirect = it)) },
        )
        HorizontalDivider()
        RadioButtonPreference(
            title = "系统默认尺寸",
            selected = draft.freeformSizeMode == FocusRestoreSettings.FREEFORM_SIZE_SYSTEM,
            onClick = {
                onChange(draft.copy(freeformSizeMode = FocusRestoreSettings.FREEFORM_SIZE_SYSTEM))
            },
        )
        HorizontalDivider()
        RadioButtonPreference(
            title = "小米默认比例",
            selected = draft.freeformSizeMode == FocusRestoreSettings.FREEFORM_SIZE_MEASURED,
            onClick = {
                onChange(draft.copy(freeformSizeMode = FocusRestoreSettings.FREEFORM_SIZE_MEASURED))
            },
        )
    }

    CollapsibleGroup("超级内容转换") {
        SwitchPreference(
            title = "转换超级岛内容",
            checked = draft.islandCompat,
            onCheckedChange = { onChange(draft.copy(islandCompat = it)) },
        )
        HorizontalDivider()
        ArrowPreference(
            title = "强制转换白名单",
            summary = if (draft.islandForcePackages.isEmpty()) "未选择"
            else "已选 ${draft.islandForcePackages.size} 个",
            enabled = draft.islandCompat,
            onClick = { if (draft.islandCompat) showWhitelist = true },
        )
        HorizontalDivider()
        TextField(
            value = draft.islandGeneralSeparator,
            onValueChange = { onChange(draft.copy(islandGeneralSeparator = it)) },
            label = "内容连接符号（留空用·）",
            singleLine = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (showWhitelist) {
        WhitelistDialog(
            selected = draft.islandForcePackages,
            onDismiss = { showWhitelist = false },
            onConfirm = { picked ->
                onChange(draft.copy(islandForcePackages = picked))
                showWhitelist = false
            },
        )
    }
}

/* ------------------------------------------------------------------ Tab 2 样式 */

@Composable
private fun StyleTab(draft: SettingsDraft, onChange: (SettingsDraft) -> Unit) {
    var colorTarget by remember { mutableStateOf(0) }

    CollapsibleGroup("文字基础样式") {
        SliderPreference(
            title = "字体大小",
            value = draft.focusFontSizeSp.toFloat(),
            onValueChange = { onChange(draft.copy(focusFontSizeSp = it.toInt())) },
            valueText = if (draft.focusFontSizeSp == 0) "跟随系统" else "${draft.focusFontSizeSp} sp",
            valueRange = 0f..FocusRestoreSettings.MAX_FOCUS_FONT_SIZE_SP.toFloat(),
        )
        HorizontalDivider()
        for (i in 0 until FocusTextStyle.FONT_FAMILY_COUNT) {
            if (i > 0) HorizontalDivider()
            RadioButtonPreference(
                title = FocusTextStyle.fontFamilyName(i),
                selected = draft.focusFontFamily == i,
                onClick = { onChange(draft.copy(focusFontFamily = i)) },
            )
        }
        HorizontalDivider()
        SliderPreference(
            title = "字重",
            value = draft.focusFontWeight.toFloat(),
            onValueChange = { onChange(draft.copy(focusFontWeight = it.toInt())) },
            valueText = if (draft.focusFontWeight == 0) "跟随系统"
            else FocusTextStyle.fontWeightName(draft.focusFontWeight),
            valueRange = 0f..(FocusTextStyle.FONT_WEIGHT_COUNT - 1).toFloat(),
        )
        HorizontalDivider()
        SwitchPreference(
            title = "斜体",
            checked = draft.focusFontItalic,
            onCheckedChange = { onChange(draft.copy(focusFontItalic = it)) },
        )
    }

    CollapsibleGroup("文字颜色") {
        ArrowPreference(
            title = "亮色场景",
            summary = colorLabel(draft.focusTextColorLight),
            onClick = { colorTarget = 1 },
        )
        HorizontalDivider()
        ArrowPreference(
            title = "暗色场景",
            summary = colorLabel(draft.focusTextColorDark),
            onClick = { colorTarget = 2 },
        )
    }

    CollapsibleGroup("应用图标") {
        SwitchPreference(
            title = "显示应用图标",
            checked = draft.showAppIcon,
            onCheckedChange = { onChange(draft.copy(showAppIcon = it)) },
        )
        if (draft.showAppIcon) {
            HorizontalDivider()
            SliderPreference(
                title = "图标大小",
                value = draft.appIconSizeDp.toFloat(),
                onValueChange = { onChange(draft.copy(appIconSizeDp = it.toInt())) },
                valueText = "${draft.appIconSizeDp} dp",
                valueRange = FocusRestoreSettings.MIN_APP_ICON_SIZE_DP.toFloat()..
                    FocusRestoreSettings.MAX_APP_ICON_SIZE_DP.toFloat(),
            )
            HorizontalDivider()
            SliderPreference(
                title = "图标与文字间距",
                value = draft.iconTextGapDp.toFloat(),
                onValueChange = { onChange(draft.copy(iconTextGapDp = it.toInt())) },
                valueText = "${draft.iconTextGapDp} dp",
                valueRange = FocusRestoreSettings.MIN_ICON_TEXT_GAP_DP.toFloat()..
                    FocusRestoreSettings.MAX_ICON_TEXT_GAP_DP.toFloat(),
            )
        }
    }

    CollapsibleGroup("分隔符设置") {
        TextField(
            value = draft.clockDividerSymbol,
            onValueChange = { onChange(draft.copy(clockDividerSymbol = it.trim())) },
            label = "分隔符内容（留空不显示）",
            singleLine = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()
        SliderPreference(
            title = "分隔符与时钟间距",
            value = draft.clockDividerMarginStartDp.toFloat(),
            onValueChange = { onChange(draft.copy(clockDividerMarginStartDp = it.toInt())) },
            valueText = "${draft.clockDividerMarginStartDp} dp",
            valueRange = FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_START_DP.toFloat()..
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP.toFloat(),
        )
        HorizontalDivider()
        SliderPreference(
            title = "分隔符与焦点通知间距",
            value = draft.clockDividerMarginEndDp.toFloat(),
            onValueChange = { onChange(draft.copy(clockDividerMarginEndDp = it.toInt())) },
            valueText = "${draft.clockDividerMarginEndDp} dp",
            valueRange = FocusRestoreSettings.MIN_CLOCK_DIVIDER_MARGIN_DP.toFloat()..
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_MARGIN_DP.toFloat(),
        )
        HorizontalDivider()
        SliderPreference(
            title = "分隔符上下位置",
            value = draft.clockDividerVerticalOffsetDp.toFloat(),
            onValueChange = { onChange(draft.copy(clockDividerVerticalOffsetDp = it.toInt())) },
            valueText = "${draft.clockDividerVerticalOffsetDp} dp",
            valueRange = FocusRestoreSettings.MIN_CLOCK_DIVIDER_VERTICAL_OFFSET_DP.toFloat()..
                FocusRestoreSettings.MAX_CLOCK_DIVIDER_VERTICAL_OFFSET_DP.toFloat(),
        )
        HorizontalDivider()
        ArrowPreference(
            title = "分隔符颜色·亮色场景",
            summary = colorLabel(draft.clockDividerColorLight),
            onClick = { colorTarget = 3 },
        )
        HorizontalDivider()
        ArrowPreference(
            title = "分隔符颜色·暗色场景",
            summary = colorLabel(draft.clockDividerColorDark),
            onClick = { colorTarget = 4 },
        )
    }

    if (colorTarget != 0) {
        val current = when (colorTarget) {
            1 -> draft.focusTextColorLight
            2 -> draft.focusTextColorDark
            3 -> draft.clockDividerColorLight
            else -> draft.clockDividerColorDark
        }
        ColorPickerDialog(
            title = when (colorTarget) {
                1 -> "文字颜色·亮色场景"
                2 -> "文字颜色·暗色场景"
                3 -> "分隔符颜色·亮色场景"
                else -> "分隔符颜色·暗色场景"
            },
            current = current,
            onDismiss = { colorTarget = 0 },
            onPick = { picked ->
                onChange(
                    when (colorTarget) {
                        1 -> draft.copy(focusTextColorLight = picked)
                        2 -> draft.copy(focusTextColorDark = picked)
                        3 -> draft.copy(clockDividerColorLight = picked)
                        else -> draft.copy(clockDividerColorDark = picked)
                    },
                )
                colorTarget = 0
            },
        )
    }
}

/* ------------------------------------------------------------------ Tab 3 关于 */

@Composable
private fun AboutTab() {
    val context = LocalContext.current
    Group("模块信息") {
        InfoRow("模块名称", "FocusReturn")
        HorizontalDivider()
        // 只展示 versionName（1.0）；内部 versionCode 保持单调递增以支持覆盖安装，
        // 不对用户暴露，避免"版本号不是 1.0"的困惑。
        InfoRow("当前版本", BuildConfig.VERSION_NAME)
        HorizontalDivider()
        InfoRow("适配系统", "HyperOS 4")
    }
    Group("开源信息") {
        InfoRow("作者", "Guchnn")
        HorizontalDivider()
        InfoRow("底层设置基于", "ImKani")
        HorizontalDivider()
        InfoRow("开源许可证", "GPL-3.0-only")
    }
    Group("外部链接") {
        ArrowPreference(title = "模块作者 GitHub", onClick = { openUrl(context, "https://github.com/Guchnn") })
        HorizontalDivider()
        ArrowPreference(
            title = "上游项目主页",
            onClick = { openUrl(context, "https://github.com/ImKani/HyperOS3FocusRestore") },
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    BasicComponent(
        title = title,
        endActions = { Text(text = value, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
    )
}

/* ------------------------------------------------------------------ 小部件 */

private fun colorLabel(color: Int): String =
    if (color == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) "跟随系统"
    else String.format("#%06X", color and 0x00FFFFFF)

@Composable
private fun ColorPickerDialog(
    title: String,
    current: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val custom = current != FocusRestoreSettings.NO_FOCUS_TEXT_COLOR
    var r by remember { mutableIntStateOf(if (custom) (current shr 16) and 0xFF else 0xFF) }
    var g by remember { mutableIntStateOf(if (custom) (current shr 8) and 0xFF else 0xFF) }
    var b by remember { mutableIntStateOf(if (custom) current and 0xFF else 0xFF) }
    val customColor = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

    OverlayDialog(title = title, show = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Card {
                BasicComponent(
                    title = "跟随系统",
                    endActions = {
                        Text(
                            text = if (current == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) "✓" else "",
                            color = MiuixTheme.colorScheme.primary,
                        )
                    },
                    onClick = { onPick(FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) },
                )
                HorizontalDivider()
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "预置颜色", color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    for (start in 0 until FocusTextStyle.presetColorCount() step 7) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (i in start until minOf(start + 7, FocusTextStyle.presetColorCount())) {
                                val c = FocusTextStyle.presetColor(i)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (c == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR)
                                                MiuixTheme.colorScheme.surfaceVariant else Color(c),
                                        )
                                        .clickable { onPick(c) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (c == FocusRestoreSettings.NO_FOCUS_TEXT_COLOR) {
                                        Text(
                                            text = "系统",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    } else if (current == c) {
                                        Text(
                                            text = "✓",
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                SliderPreference(
                    title = "红 R",
                    value = r.toFloat(),
                    onValueChange = { r = it.toInt() },
                    valueText = r.toString(),
                    valueRange = 0f..255f,
                )
                HorizontalDivider()
                SliderPreference(
                    title = "绿 G",
                    value = g.toFloat(),
                    onValueChange = { g = it.toInt() },
                    valueText = g.toString(),
                    valueRange = 0f..255f,
                )
                HorizontalDivider()
                SliderPreference(
                    title = "蓝 B",
                    value = b.toFloat(),
                    onValueChange = { b = it.toInt() },
                    valueText = b.toString(),
                    valueRange = 0f..255f,
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(customColor)),
                    )
                    Text(
                        text = "自定义：${colorLabel(customColor)}",
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "应用",
                    onClick = { onPick(customColor) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WhitelistDialog(
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (MutableSet<String>) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val picked = remember { HashSet(selected) }
    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .map { it.packageName to it.loadLabel(context.packageManager).toString() }
            .sortedBy { it.second }
    }
    OverlayDialog(title = "强制转换白名单", show = true, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Card {
                apps.forEachIndexed { index, (pkg, label) ->
                    if (index > 0) HorizontalDivider()
                    SwitchPreference(
                        title = label,
                        summary = pkg,
                        checked = picked.contains(pkg),
                        onCheckedChange = { on ->
                            if (on) picked.add(pkg) else picked.remove(pkg)
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "确认",
                    onClick = { onConfirm(picked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ 工具 */

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Throwable) {
        // 没有可处理该链接的应用时静默忽略。
    }
}

/* ------------------------------------------------------------------ 重启系统界面 */

private const val ROOT_PROBE_TIMEOUT_MS = 60_000L
private const val ROOT_COMMAND_TIMEOUT_MS = 15_000L

private val RESTART_SYSTEMUI_COMMANDS = arrayOf(
    "pkill -f \"[c]om.android.systemui\"",
    "killall com.android.systemui",
    "kill \$(pidof com.android.systemui)",
)

/**
 * 重启 SystemUI：与旧 View 版实现保持一致（先探 root，再依次尝试三条命令）。
 * 结果通过 [notify] 回传，避免在 Compose 里直接持有 Activity 引用。
 */
private fun restartSystemUi(context: Context, notify: (String) -> Unit) {
    Toast.makeText(context, "正在重启系统界面…", Toast.LENGTH_SHORT).show()
    Thread {
        val message = runCatching { performRestart() }.getOrElse { "重启失败：${it.message}" }
        android.os.Handler(android.os.Looper.getMainLooper()).post { notify(message) }
    }.start()
}

private fun performRestart(): String {
    val probe = runSu("id", ROOT_PROBE_TIMEOUT_MS)
    if (probe.launchFailed) return "未检测到 su 命令，设备可能没有 root 权限，请手动重启系统界面或手机。"
    if (probe.exitCode != 0 || !probe.output.contains("uid=0")) return "请为该应用授予 root 权限后再试一次。"
    for (command in RESTART_SYSTEMUI_COMMANDS) {
        val result = runSu(command, ROOT_COMMAND_TIMEOUT_MS)
        if (!result.launchFailed && result.exitCode == 0) return "已发送重启指令，系统界面正在重启。"
    }
    return "重启失败：已获得 root 权限，但仍无法结束系统界面进程，请手动重启。"
}

private class ShellResult(val launchFailed: Boolean, val exitCode: Int, val output: String)

private fun runSu(command: String, timeoutMs: Long): ShellResult {
    val process = try {
        ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
    } catch (t: Throwable) {
        return ShellResult(true, -1, "")
    }
    return try {
        if (!process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            process.destroy()
            ShellResult(false, -1, "")
        } else {
            ShellResult(false, process.exitValue(), process.inputStream.bufferedReader().readText())
        }
    } catch (t: Throwable) {
        process.destroy()
        ShellResult(false, -1, "")
    }
}
