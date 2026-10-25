# 🎮 俄罗斯方块 (Tetris) - Android 开发大作业

这是一个基于 Android 平台开发的经典俄罗斯方块小游戏，使用 Java 原生开发。项目实现了完整的游戏交互逻辑，并使用 Android 四大组件（Activity、Service、BroadcastReceiver、ContentProvider）配合 SQLite 数据库完成登录、对局、排行榜与历史记录功能。

---

## 📌 目录
- [游戏规则](#-游戏规则)
- [按键操作说明](#-按键操作说明)
- [数据与持久化](#-数据与持久化)
- [核心技术点实现](#-核心技术点实现)
- [ContentProvider 接口](#-contentprovider-接口)
- [数据库结构](#-数据库结构)
- [项目结构说明](#-项目结构说明)
- [环境要求与运行指南](#-环境要求与运行指南)
- [设计说明与当前限制](#-设计说明与当前限制)

---

## 🕹️ 游戏规则

### 1. 棋盘与方块
- 游戏区为 **10 列 × 20 行** 的网格（`GameBoard.COLS = 10`，`GameBoard.ROWS = 20`），坐标原点在左上角，`grid[row][col]` 中 `0` 表示空格，大于 0 的值表示该格是第 `值 - 1` 种颜色的方块。
- 共 7 种方块（I、O、T、S、Z、J、L），每种由 4 个格子组成，形状在 4×4 的相对坐标内定义。
- 新方块由 `Random` 在 0~6 中等概率随机生成（不是 7-bag 随机），初始旋转为 0，初始位置为 `x = 3, y = 0`。
- 方块的碰撞判定：4 个格子中任意一个超出左右边界或上下边界，或与已固定的方块重叠，则视为不能放置。

### 2. 下落与速度
- 方块由 `GameService` 中的主线程 `Handler` 定时驱动自动下落，下落间隔公式为：

  `间隔(ms) = max(120, 600 - (等级 - 1) × 60)`

  - 1 级：600 ms
  - 2 级：540 ms
  - 每升 1 级加快 60 ms
  - 最低 120 ms（从 9 级起不再加快）
- 暂停时移除定时任务，继续时重新调度，不会出现切屏后仍继续下落的情况。

### 3. 移动、旋转与速降
- 左移、右移每次移动 1 格，若移动后碰撞则回退，位置不变。
- 旋转为顺时针 90°，可在 0~3 四种旋转状态间循环；旋转后若发生碰撞或越界则回退到原旋转状态（没有墙踢/踢墙修正）。
- 速降会让方块一直下落到最底部才停止，并立即固定。

### 4. 得分机制
- 每固定（落地）一个方块：基础分 `+1`。
- 每消除 1 行：额外获得 `100 × 当前等级` 分。
- 一次消除多行时按行数线性叠加（例如一次消 4 行 = `4 × 100 × 当前等级`），没有额外的多行奖励倍率。
- 消行奖励使用的是消行前的等级。

### 5. 等级机制
- 初始等级为 1。
- 每累计消除 5 行提升 1 级：`等级 = 1 + 总消行数 / 5`（整数除法）。
- 等级越高自动下落越快，同时消行奖励越高。

### 6. 消行规则
- 从棋盘底部向上逐行检查，某一行 10 格全部被填满即为满行。
- 满行整行删除，其上所有行整体下移一行，顶部补空行，然后重新检查当前行，支持连续多行消除。

### 7. 游戏结束
- 当新生成的方块在出生位置无法放置（越界或与已有方块重叠）时游戏结束。
- 结束后 `GameService` 发送 `ACTION_GAME_OVER` 广播（携带分数、消行、等级、本局时长、固定方块数），由 `GameActivity` 动态注册的广播接收器接收并写入数据库，随后弹出结算弹窗。

---

## 🎮 按键操作说明

### 主界面
| 区域 | 功能说明 |
| :--- | :--- |
| 昵称输入框 | 输入玩家昵称，未登录时必须填写，留空会弹出 Toast 提示。 |
| 开始游戏 / 继续游戏 | 保存昵称到 SharedPreferences，若玩家不存在则写入 `player` 表，然后进入游戏页。已登录时按钮文字变为“继续游戏”，输入框锁定。 |
| 查看排行榜 | 进入排行榜页面，展示所有玩家的最高分排名。 |
| 历史记录 | 进入历史记录页面，展示当前登录玩家的每局战绩与汇总统计。 |
| 退出登录 | 清除本地保存的昵称，回到未登录状态（按钮默认隐藏，仅登录后显示）。 |

### 游戏页顶部状态栏
| 按键 | 功能说明 |
| :--- | :--- |
| 分数 / 消行 / 等级 | 实时显示当前对局数据。 |
| 查看排行榜 | 若游戏正在运行则先自动暂停，再跳转到排行榜页面，避免回来时方块已经落地。 |
| 重开 | 弹出确认弹窗，确认后重新开始一局；弹窗出现时自动暂停，取消或点击弹窗外会恢复游戏。 |
| 退出 | 弹出确认弹窗，确认后结束当前 Activity 返回主页；本局进度不保存。 |

### 游戏页底部控制栏
| 按键 | 功能说明 |
| :--- | :--- |
| 向左 | 当前方块向左移动 1 格。 |
| 旋转 | 方块顺时针旋转 90°，碰撞时自动回退。 |
| 向右 | 当前方块向右移动 1 格。 |
| 速降 | 方块瞬间下落到最底部并固定。 |
| 暂停 / 继续 | 暂停或恢复游戏，按钮文字随状态切换。只有在游戏运行中，方向、旋转、速降按钮才会响应。 |

### 游戏结束弹窗
| 按键 | 功能说明 |
| :--- | :--- |
| 继续 | 关闭弹窗并立即开始新一局。 |
| 查看排行榜 | 跳转到排行榜页面。 |
| 退出 | 关闭弹窗并返回主页。 |

弹窗不可点击外部或返回键关闭；若因跳转页面等原因未显示，回到游戏页时（`onResume`）会自动补弹。

---

## 📊 数据与持久化

### 登录状态
- 使用 `SharedPreferences`（文件名 `user_info`）保存键 `player_name`，实现“记住登录”和“退出登录”。

### 对局保存
游戏结束时同时写入两张表：

- `score` 表：保存 `player_id`、`score`、`level`、`lines`、`played_at`，供排行榜使用。
- `game_history` 表：保存 `player_id`、`score`、`level`、`lines`、`duration`（本局秒数）、`pieces`（固定方块数）、`played_at`，供历史记录使用。

所有读写都通过 `ContentResolver` 访问 `ScoreProvider`，不直接操作 SQLite。

### 排行榜规则
- 查询 `score` 表，按 `score DESC, level DESC, lines DESC, played_at DESC` 排序。
- 同一玩家只保留排序后的第一条（即个人最高分），不会重复占榜。
- 显示名通过 `player` 表按 `_id` 查询，前三名徽章分别使用金、灰、绿配色。
- 无记录时列表显示“暂无记录，快去玩一局吧！”。

### 历史记录规则
- 读取当前登录玩家（SharedPreferences 中的昵称）对应的 `player_id`。
- 按 `played_at DESC` 倒序列出每局数据：时间（yyyy-MM-dd HH:mm）、分数、等级、消行、时长（mm:ss）、方块数。
- 顶部汇总显示总局数、最高分、总消行。
- 未登录或无记录时显示“暂无历史记录，快去玩一局吧！”。

---

## 🛠️ 核心技术点实现（对应大作业要求）

| 技术组件 | 对应类 / 文件 | 功能说明 |
| :--- | :--- | :--- |
| **Activity** | `MainActivity`<br>`GameActivity`<br>`RankActivity`<br>`HistoryActivity` | 负责 UI 展示与页面跳转（主页、游戏页、排行榜、历史记录）。 |
| **Service** | `GameService` | **绑定式 Service**。负责核心游戏逻辑（定时下落、碰撞检测、消行计分、结束判定），通过 `GameBinder` + `GameListener` 与 UI 通信，实现逻辑与界面分离。 |
| **BroadcastReceiver** | `GameActivity` 内部匿名类 | **动态注册**接收 `ACTION_GAME_OVER` 广播，触发分数与历史入库、弹出结算弹窗；`onDestroy` 中注销。当前项目没有静态注册的广播接收器。 |
| **ContentProvider** | `ScoreProvider` | 对外暴露 `player`、`score`、`game_history` 的增删改查 URI，所有数据读写均通过 `ContentResolver` 完成。 |
| **SQLite 数据库** | `DBHelper` | 建立 `tetris.db`（版本 2），包含 `player`、`score`、`game_history` 三张表。 |
| **自定义 View** | `GameView` | 按可用宽高自适应计算格子大小，绘制棋盘边框、网格线、已固定方块和当前下落方块，莫兰迪配色并带白色描边。 |
| **Intent** | 全局 | 页面跳转、传递玩家昵称、绑定 Service、发送游戏结束广播。 |
| **SharedPreferences** | `MainActivity` / `HistoryActivity` | 保存登录昵称，供进入游戏和历史记录查询使用。 |

---

## 🔗 ContentProvider 接口

Authority：`com.example.tetris.provider`

| URI | 匹配码 | 支持操作 |
| :--- | :--- | :--- |
| `content://com.example.tetris.provider/player` | `PLAYER` (1) | query、insert |
| `content://com.example.tetris.provider/score` | `SCORE` (2) | query、insert、update、delete |
| `content://com.example.tetris.provider/score/#` | `SCORE_ID` (3) | query、update、delete |
| `content://com.example.tetris.provider/game_history` | `HISTORY` (4) | query、insert、update、delete |
| `content://com.example.tetris.provider/game_history/#` | `HISTORY_ID` (5) | query、update、delete |

插入、更新、删除后会调用 `notifyChange`；查询结果会设置 `setNotificationUri`，便于监听数据变化。

---

## 🗄️ 数据库结构

数据库名：`tetris.db`，版本：`2`（升级时删除三张表并重建）。

| 表名 | 字段 | 说明 |
| :--- | :--- | :--- |
| `player` | `_id` INTEGER PK AUTOINCREMENT<br>`name` TEXT NOT NULL<br>`created_at` INTEGER | 玩家表，昵称唯一性由应用逻辑保证。 |
| `score` | `_id` INTEGER PK AUTOINCREMENT<br>`player_id` INTEGER<br>`score` INTEGER<br>`level` INTEGER<br>`lines` INTEGER<br>`played_at` INTEGER<br>FOREIGN KEY(player_id) → player(_id) | 每局分数记录，用于排行榜。 |
| `game_history` | `_id` INTEGER PK AUTOINCREMENT<br>`player_id` INTEGER<br>`score` INTEGER<br>`level` INTEGER<br>`lines` INTEGER<br>`duration` INTEGER<br>`pieces` INTEGER<br>`played_at` INTEGER<br>FOREIGN KEY(player_id) → player(_id) | 每局完整战绩，用于历史记录页面。 |

---

## 📂 项目结构说明

```text
com.example.tetris
├── db/
│   └── DBHelper.java          # SQLiteOpenHelper，建表与升级
├── game/
│   ├── GameBoard.java         # 棋盘矩阵、放置判定、锁定、消行
│   └── Tetromino.java         # 7 种方块形状、颜色、旋转坐标计算
├── provider/
│   └── ScoreProvider.java     # ContentProvider，URI 匹配与数据库操作
├── service/
│   └── GameService.java       # 绑定式 Service，游戏主循环与计分
├── ui/
│   ├── MainActivity.java      # 主页：登录、排行榜/历史入口
│   ├── GameActivity.java      # 游戏页：控制按钮、绑定 Service、广播、弹窗、入库
│   ├── GameView.java          # 自定义 View：绘制棋盘与方块
│   ├── RankActivity.java      # 排行榜列表
│   └── HistoryActivity.java   # 个人历史记录与汇总
└── util/
    └── Constants.java         # Authority、表名、匹配码、广播 Action 与 Extra 键
```

布局文件位于 `app/src/main/res/layout/`：`activity_main.xml`、`activity_game.xml`、`activity_rank.xml`、`activity_history.xml`、`dialog_confirm.xml`、`dialog_game_over.xml`、`item_rank.xml`、`item_history.xml`。

---

## ⚙️ 环境要求与运行指南

- 开发语言：Java 11
- `compileSdk` / `targetSdk`：36；`minSdk`：24
- 构建工具：Gradle（Kotlin DSL，`build.gradle.kts`）
- 主要依赖：AndroidX AppCompat 1.8.0、Material 1.10.0、Activity 1.8.0、ConstraintLayout 2.2.2

运行方式：

1. 使用 Android Studio 打开项目根目录，等待 Gradle 同步完成。
2. 安装 Android SDK 36 并准备一个 API 24 及以上的模拟器或真机。
3. 运行 `app` 配置，或在命令行执行 `gradlew assembleDebug` 后安装 `app/build/outputs/apk/debug/app-debug.apk`。
4. 首次启动输入昵称即可开始游戏；对局结束后的分数与战绩会自动写入本地数据库。

---

## 📝 设计说明与当前限制

- 方块采用纯随机生成，没有 7-bag 算法，也没有“下一块预览”和“暂存（Hold）”功能。
- 旋转只有碰撞回退，没有墙踢、地面踢等高级旋转修正。
- 没有幽灵块（落点提示）。
- 游戏页退出或返回主页时本局进度不保存，只有正常“游戏结束”才会写入 `score` 与 `game_history`。
- 排行榜对每位玩家只展示最高分那一局，不展示同一玩家的多条记录。
- 数据库版本升级采用删表重建策略，升级后历史数据会清空。
