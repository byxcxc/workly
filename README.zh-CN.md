# Workly

[English](README.md) | **中文**

[![Android CI](https://github.com/byxcxc/workly/actions/workflows/android.yml/badge.svg)](https://github.com/byxcxc/workly/actions/workflows/android.yml)

**个人工时与收入记录工具。**

Workly 是一个安静、离线的小时工时记录 App。打开它，点 **开始工作**，去做你的事，
结束时点 **结束工作**——工作时间与收入自动算好。

没有账号、没有云端、没有广告、没有统计上报。所有数据都保存在你手机本地的
数据库里，核心功能完全离线可用。

---

## 功能

**记录**

- 一键 **开始工作** / **结束工作**，实时计时并实时显示收入
- 进行中的记录会立刻写入数据库，因此关闭 App、锁屏、进程被系统回收甚至
  手机重启，工作状态都不会丢失
- 已用时间始终按 `当前时间 - 开始时间` 计算，即使 App 没在运行也照样准确
- **结束** 时会打开可编辑的汇总：结束时间、休息、时薪、工作类型、备注，
  保存前都能改
- 手动 **添加记录**，补记忘记计时的工时
- 编辑和删除任意记录，删除前会二次确认

**统计，图表会说话**

- 每张图表都标注数值：数字会画在柱子上，同时每个数据点还会在图表下方
  以可横向滚动的标签逐条列出
- 工作安排：每周目标工时 + 休息日。首页和统计会按所选区间显示目标完成进度，
  日历上会标出休息日
- 统计里的 **自定义区间** 可以单独填写总工时，预计收入随输入实时更新
- 长按日历上的任意日期，可以 **直接记录当天工时**、把它设为休息日，或单独
  指定当天的目标工时。每次自定义都会被记住

**个性化**

- **背景**：主题默认、纯色，或从相册里选一张自己的图片
- 图片支持 **高斯模糊**，程度 0%–100% 可调
- **玻璃质感**：设置自定义背景后，所有卡片都会变成半透明，透出模糊后的图片
- **适配图片配色**：按钮和图表的颜色取自图片的主色调

> **说明：** 高斯模糊使用系统渲染效果，需要 Android 12（API 31）及以上。更早的
> 设备依然会显示图片，只是没有模糊效果，其余功能完全一致。


**配置**

- 默认时薪与货币（按各自的货币小数规则显示：¥12,480 或 $12,480.50）
- 多个工作类型，各自带默认时薪；选中类型会自动填入时薪
- 浅色 / 深色 / 跟随系统，外加六种主题色（靛蓝、青碧、森林、日落、玫瑰、石墨）
- 时长可以显示成 `6小时30分` 或 `6.5小时`
- 每周起始日、每周目标工时、休息日
- **导出** CSV（适合表格软件）或 JSON（完整备份），**导入** JSON 备份并自动去重
- 首页可直接 **手动记录工时**，补记忘记开始计时的工作

**质量**

- 支持英文、日文、简体中文，所有文案都在资源文件里
- 应用内语言切换（Android 13+ 也会出现在系统的「应用语言」设置中），
  选择会被记住
- Material 3、深色主题、足够大的点击区域、内容描述、对读屏友好的数值，
  并支持系统字体放大
- 金额以整数「最小货币单位」存储，不会有浮点误差
- Compose 状态类标注 `@Immutable`，每秒跳动的计时器只影响它要更新的两个数字，
  列表和卡片可以跳过重组
- 时间以 `Instant`（UTC）存储，只在显示时转换为设备时区，跨时区或改时区
  都不会让历史记录漂移
- 正确处理跨午夜（`23:00 → 02:00 = 3小时`）
- 每条记录都会保存当时使用的时薪与工作类型名称快照，之后修改默认时薪
  不会改写过去的收入

---

## 截图

截图由 `app/src/main/java/com/workly/app/ui/preview/DesignPreviews.kt` 里的
`@Preview` 生成。用 Android Studio 打开该文件，在预览面板中渲染并导出到
`docs/screenshots/` 即可。

| 界面 | 内容 |
| --- | --- |
| 首页 | 今日工时、今日收入、本周、最近记录 |
| 首页（工作中） | 实时计时、实时收入、结束工作 |
| 记录 | 按天分组的列表 |
| 日历 | 月视图、工作标记、当日合计 |
| 统计 | 合计、平均值与趋势图 |
| 设置 | 时薪、货币、工作类型、主题、背景、备份 |

---

## 技术栈

| 层次 | 选择 |
| --- | --- |
| 语言 | Kotlin |
| 界面 | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 状态 | ViewModel + `StateFlow`，单向数据流 |
| 持久化 | Room（记录、工作类型）+ DataStore（设置） |
| 异步 | Kotlin Coroutines |
| 序列化 | kotlinx.serialization（JSON 备份） |
| 构建 | Android Gradle Plugin 9.4（**内置 Kotlin**）、Gradle 9.7、KSP |
| SDK | `minSdk 26`、`targetSdk 37`、`compileSdk 37`（Android 17） |
| 测试 | JUnit4（JVM）+ Compose UI test / Espresso（仪器化） |

项目刻意不使用依赖注入框架，也没有做完整的 Clean Architecture 分层：
Workly 是单模块应用，希望刚学 Android 的人也能读懂。

---

## 架构

```
UI（Compose 界面）
      ↓  事件
ViewModel（StateFlow<UiState>）
      ↓
Repository（WorkRepository、WorkTypeRepository、SettingsRepository）
      ↓
Room 数据库（work_sessions、work_types）  +  DataStore（设置）
```

- **单向数据流。** 界面渲染不可变的 `UiState`，把事件交回 ViewModel，
  从不直接访问数据库。
- **纯领域层。** `domain/` 里是时间、金额、校验、统计和日历的计算逻辑，
  不依赖 Android，且有完整单测——包括 `23:00 → 02:00` 和 `8小时 − 1小时 = 7小时`。
- **用 `Result` 代替抛异常。** 仓库返回 `Result<T>`，失败时带一个 `WorklyError`，
  由界面映射成本地化文案。用户永远看不到堆栈。
- **与原数据模型的两处有意偏离**，在此说明：
  1. 设置存放在 **DataStore**，而不是 Room 的 `AppSettings` 表。它们只是几个
     基础值，DataStore 提供了可观察的原子更新，且不需要 schema 和 DAO 样板。
  2. Room 实体直接当模型用，没有再做一份 `domain/model` 加映射器。派生值
     （`workedMinutes`、`incomeMinor`）是委托给纯领域函数的扩展属性，
     规则依然只有一处。

### 项目结构

```
app/src/main/java/com/workly/app/
├── AppGraph.kt               # 手写的极简依赖容器
├── MainActivity.kt           # 单 Activity，edge-to-edge Compose
├── WorklyApplication.kt
├── data/
│   ├── backup/               # CSV + JSON 导出、JSON 导入、去重
│   ├── local/                # Room 数据库、DAO、实体、类型转换
│   ├── prefs/                # DataStore 设置
│   └── repository/           # WorkRepository、WorkTypeRepository
├── domain/                   # Money、WorkTime、SessionValidator、统计、区间
└── ui/
    ├── background/           # 背景图片解码、主色调提取、背景渲染
    ├── components/           # 卡片、行、空状态、对话框、图标
    ├── home/                 # 首页 + 实时计时
    ├── navigation/           # 路由与 NavHost
    ├── preview/              # @Preview 设计预览
    ├── records/              # 列表、日历、详情、新增/编辑/结束编辑页
    ├── settings/             # 设置、工作类型、关于
    ├── statistics/           # 区间统计 + 趋势图
    ├── theme/                # 配色、字号、形状、强调色
    └── util/                 # 格式化与错误文案
```

`app/src/main/res/values/` 是英文字符串，`values-ja/` 是日文，`values-zh/`
是简体中文。Kotlin 里没有硬编码的用户可见文案。

---

## 构建

### 环境要求

- Android Studio（当前稳定版）**或** 命令行工具链
- JDK 17 或更高（推荐 JDK 21）
- Android SDK：**platform 37.0** 与 **build-tools 36.0.0**

### 用 Android Studio

1. `File → Open…`，选择项目根目录。
2. 等待 Gradle 同步（会自动下载 wrapper 和依赖）。
3. 按 **Run ▶** 安装到设备或模拟器。

### 用命令行

```bash
git clone https://github.com/byxcxc/workly.git
cd workly

# 如果 SDK 不在 ANDROID_HOME，指定一下
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew assembleDebug          # 构建 debug APK
./gradlew installDebug           # 安装到已连接设备
./gradlew assembleRelease        # 未签名的 release 构建（已开启 R8）
```

debug APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

### Release 签名

仓库里不含任何 keystore，未提供签名文件时 release 构建保持未签名状态。
本地签名请在项目根目录创建 `keystore.properties`（该文件已被 git 忽略）：

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

CI 签名请添加 `WORKLY_KEYSTORE_BASE64`、`WORKLY_KEYSTORE_PASSWORD`、
`WORKLY_KEY_ALIAS`、`WORKLY_KEY_PASSWORD` 四个仓库 Secret，并按
`.github/workflows/android.yml` 底部注释掉的模板配置。

---

## 测试

```bash
./gradlew testDebugUnitTest          # JVM 单测（不需要设备）
./gradlew connectedDebugAndroidTest  # 仪器化测试（需要设备或模拟器）
```

JVM 单测 90 个、仪器化测试 40 个，CI 上全部通过。

**JVM 单测**（`app/src/test/`）覆盖容易写错的规则：

- 时间计算，包括 `23:00 → 02:00 = 3小时` 和整 24 小时班次
- `8小时工作 − 1小时休息 = 7小时`
- 收入计算与四舍五入，JPY（0 位小数）与 USD（2 位小数）的差异
- 校验：起止相同、休息超过工时、时薪为负
- 按日 / 周 / 月 / 年的汇总、工作天数与平均值
- 工作安排：计划工作日、休息日、区间目标与进度
- 按天覆盖：强制休息、强制上班、单日工时
- 小数工时格式化（`6.5`、`8`、`7.33`）
- 可配置每周起始日的周区间
- 月历网格的形状与补位
- CSV 转义、排序与金额格式
- JSON 备份往返、未知字段、损坏输入、版本拒绝

**仪器化测试**（`app/src/androidTest/`）在模拟器上跑真实 Room 数据库与真实界面：

- DAO 与 schema 行为（类型转换、唯一索引、外键 `SET_NULL`）
- 仓库规则：开始、结束、放弃、编辑、删除、导入去重
- CSV/JSON 导出与 JSON 导入往返
- Compose 流程：开始工作、结束工作、新增、编辑、删除、首页手动记录、
  语言切换、长按日历自定义、底部导航，以及离开首页后仍能恢复进行中的记录

仪器化测试需要真机或模拟器，因为 Room 依赖 Android 的 SQLite 实现。

---

## GitHub Actions

| 工作流 | 触发 | 内容 |
| --- | --- | --- |
| `Android CI`（`.github/workflows/android.yml`） | push / PR 到 `main`、手动 | JDK 21 + Android SDK，`testDebugUnitTest`、`lintDebug`、`assembleDebug`，并把 debug APK 与报告作为产物上传 |
| `Instrumented tests`（`.github/workflows/android-instrumented-tests.yml`） | 手动、每周 | 启动模拟器并运行 `connectedDebugAndroidTest` |

两个工作流都不需要任何 Secret，仓库里也不存放任何敏感信息。

---

## 后续计划

- [ ] 桌面小组件与快捷设置磁贴，用于快速开始/结束
- [ ] 单次工时异常偏长时的可选提醒
- [ ] 加班倍率与按天覆盖时薪
- [ ] 记录页的标签与搜索
- [ ] 更多语言（资源结构已就绪）
- [ ] 可选的加密云同步——核心功能依然不需要账号

---

## 仓库地址

<https://github.com/byxcxc/workly>

## 许可证

[MIT](LICENSE) © 2026 Workly contributors
