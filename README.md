# FloatingClock - 悬浮时钟自动点击工具

一款 Android 悬浮窗工具，在屏幕上常驻显示时钟，并提供自动点击/滑动的脚本录制与执行功能。

## 功能概览

### 悬浮时钟
- 在所有应用上层显示实时时钟（HH:mm:ss）
- 悬浮窗可自由拖动到屏幕任意位置
- 半透明圆角背景，不遮挡主要内容

### 自动点击 / 滑动
- 添加单个点击：倒计时 3 秒后，点击屏幕任意位置记录坐标
- 添加滑动：依次选择滑动起点和终点，记录为滑动步骤
- 多步骤顺序执行，每步可独立设置间隔时间（毫秒级精度）
- 支持点击与滑动混合编排

### 步骤管理
- 查看所有已录制步骤（坐标、类型、间隔时间）
- 编辑单个步骤的间隔时间（±100ms / ±1s 快速调整）
- 清空全部步骤
- 保存步骤为命名分组，持久化存储到本地
- 加载 / 删除 / 重命名已保存的分组

### 定时启动
- 设置目标时间（时/分/秒），到达时自动开始执行点击序列
- 若目标时间已过则顺延至次日
- 支持随时清除定时

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI（主界面） | Jetpack Compose + Material3 |
| 悬浮窗 | WindowManager + 原生 View |
| 自动操作 | AccessibilityService + GestureDescription |
| 数据持久化 | SharedPreferences + Gson |
| 最低 SDK | Android 7.0（API 24） |
| 目标 SDK | Android 16（API 36） |

## 所需权限

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 显示悬浮窗 |
| `BIND_ACCESSIBILITY_SERVICE` | 模拟点击和滑动手势 |

## 使用步骤

1. 安装 APK 后打开应用
2. 点击「启动悬浮时钟」，按提示授予悬浮窗权限
3. 点击「检查无障碍服务」，跳转设置页开启 FloatingClock 无障碍服务
4. 在悬浮窗中点击「添加点击」或「添加滑动」录制操作步骤
5. 点击「步骤(N)」可查看、编辑或保存当前步骤分组
6. 点击「开始点击」循环执行所有步骤；再次点击停止
7. 可选：点击「设置定时」指定自动启动时间

## 项目结构

```
app/src/main/java/com/ijackey/floatingclock/
├── MainActivity.kt          # 主界面，权限引导，服务启停
├── FloatingClockService.kt  # 悬浮窗核心，时钟显示，步骤录制，定时逻辑
├── AutoClickService.kt      # 无障碍服务，手势执行，步骤队列管理
├── StepGroupManager.kt      # 步骤分组的保存/加载/删除（SharedPreferences）
├── ClickAreaSelector.kt     # 独立点击位置选择服务（辅助）
└── ComboSelector.kt         # 组合操作录制服务（点击+滑动+点击）
```

## 构建 & 安装

```bash
# 调试包
./gradlew assembleDebug

# 正式包
./gradlew assembleRelease
```

或直接安装 `app/release/app-release.apk`（版本 1.1）。
