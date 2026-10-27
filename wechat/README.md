# 微信界面复刻（Android 课程作业）

用 **Java + XML** 复刻微信 Android 版主要界面，并在数据层为「把联系人换成 AI Agent」预留扩展能力。
本期**不接入任何真实 AI**，不调用大模型 API，不实现推理 / 记忆 / 工具调用。

## 一、技术选型（沿用本机已验证的组合）

| 项 | 版本 | 说明 |
|---|---|---|
| Gradle | 9.3.1 | 本机 `.gradle` 里已有的发行版，不额外下载新版本 |
| Android Gradle Plugin | 9.1.1 | 与 Gradle 9.x 官方配对，本机已缓存 |
| JDK | 21（`C:\Users\ASUS\tools\jdk-21`） | PATH 上的 `java` 是 1.8，所以 `gradle.properties` 里显式钉了 `org.gradle.java.home` |
| compileSdk / targetSdk | 36 | 本机 platform 是 android-36.1，AGP 会自动补齐 android-36 |
| minSdk | 24 | Android 7.0，可以放心用 vector drawable、自适应布局属性 |
| 依赖 | appcompat / recyclerview / activity / fragment | 故意不引入 material、constraintlayout，减少不必要依赖 |

## 二、目录结构

```
app/src/main/java/com/example/wechat/
├── model/      Contact / ContactType / Conversation / Message / EntryItem（纯业务对象，无数据库注解）
├── data/       DataRepository（接口，UI 只认它）· DataProvider（唯一出口）· WriteFailureListener
├── db/
│   ├── entity/   ContactEntity / ConversationEntity / MessageEntity / MetaEntity（表结构）
│   ├── mapper/   EntityMapper（Entity ↔ Model 手写转换）
│   ├── AppDatabase / *Dao / Converters / DatabaseInitializer / DefaultDataSeed / InitResult
│   └── RoomDataRepository（DataRepository 的 SQLite 实现）
├── ui/
│   ├── launch/   LauncherActivity（后台初始化，就绪后才进主界面）
│   ├── main/     MainActivity（四个 Tab）+ 四个 Fragment
│   ├── chat/     ChatActivity（聊天窗口）
│   ├── contact/  ContactDetailActivity（联系人详情）
│   └── me/       SettingsActivity（设置）
├── adapter/    ConversationAdapter / MessageAdapter / ContactsAdapter / EntryAdapter
├── widget/     SideIndexView（通讯录右侧字母索引，自定义 View）
└── util/       AvatarLoader / TimeUtil / PinyinIndex / InsetsHelper
```

## 三、已经做出来的页面

| 页面 | 内容 | 怎么验 |
|---|---|---|
| 主界面 | 底部 4 Tab（微信 / 通讯录 / 发现 / 我），show/hide 切换，选中图标变绿填充态，标题栏跟着变 | 四个 Tab 来回切，切回来列表滚动位置还在 |
| 会话列表 | 头像、昵称、最后一条消息、时间、未读红点（>99 显示 99+）、分割线不通栏、搜索框实时过滤 | 搜索框输入「张」只剩张伟；发完消息返回后最后一条消息已更新 |
| 聊天窗口 | 左右气泡 + 尖角 + 头像、5 分钟内不重复显示时间、输入区语音/表情/加号、有字时「发送」和「+」互换、键盘弹起保持贴底 | 连发 5 条不串行；输入时右下角出现绿色「发送」 |
| 通讯录 | 搜索框、新的朋友/群聊/标签/公众号四个入口、字母分组、右侧 A-Z 索引条、点人进详情 | 点右侧字母 M 跳到 M 分组；搜索「李」只剩李娜 |
| 联系人详情 | 头像/昵称/微信号/地区、备注（显示签名）、朋友圈、更多信息、发消息 / 语音通话 / 视频通话 | 「发消息」会新建会话并进聊天页 |
| 发现页 | 朋友圈 / 视频号 / 直播 · 扫一扫 / 摇一摇 · 看一看 / 搜一搜 · 小程序，彩色图标 + 分组间距 | 每行都能点（未实现的给 Toast 反馈） |
| 我 | 资料区（头像/昵称/微信号）、服务/收藏 · 朋友圈/相册/卡包/表情 · 设置 | 点头像区进自己的资料详情；点「设置」进设置页 |
| 设置 | 账号与安全 / 消息通知 / 隐私 / 通用 · 关于微信（带版本号）/ 帮助与反馈 · 切换账号 · 退出登录（红色 + 确认弹窗） | 退出登录会弹确认框 |

按需求约定，**本期不做**：扫一扫、添加朋友、搜索结果页、朋友圈内容页、个人资料编辑、支付相关。
这些入口都保留了点击反馈（Toast），不是死图标。

## 四、数据模型与 Agent 扩展点

```
Contact        id(唯一) · 昵称 · 头像 · 简介 · type(NORMAL/AGENT) · 微信号 · 地区
Conversation   id · peerId → Contact.id · 最后一条消息 · 时间 · 未读数 · 免打扰
Message        id · conversationId · senderId · 类型 · 内容 · 时间戳
```

会话和消息都通过 **id** 关联联系人，不缓存联系人对象——联系人改名/换头像/换成 Agent，列表自动跟着变。

预留了三条「缝隙」，将来加 Agent 时只往缝里塞东西，不用推翻界面：

1. **`Contact.type`**：现在只有 `NORMAL` / `AGENT`，界面不做任何区分展示。将来新建一份 `AgentProfile`（模型配置、系统提示词），用**同一个 id** 关联即可，UI 全程只认 `Contact`。
2. **`AvatarLoader`**：头像统一走这一个方法。现在是 drawable 名，换成真实图片或 URL 只改这个类的解析逻辑，所有页面自动生效。
3. **`ChatActivity.sendText()`**：唯一的发送入口。现在只做「追加到本地列表 + 刷新气泡 + 同步会话最后一句」；将来接 Agent 就在这里先把文本交给消息处理器，拿到回复再落库，界面一行不用改。

第 1 条里的 `AGENT` 在 mock 数据里已经有一个（`小助手`，id = `c_agent_helper`），可以拿来做扩展实验。

## 五、本地持久化（Room / SQLite）

Entity 与业务 Model 是分开的：`model/` 里的对象不含任何 Room 注解，界面只用它们；
`db/entity/` 才定义表名、列名、主键和索引，两边由 `db/mapper/EntityMapper` 手写转换。
这样将来接 AI Service 的 DTO 或改字段，都不会把数据库约束带进业务层。

| 表 | 写策略 | 说明 |
|---|---|---|
| `contacts` | `@Upsert` | 用 ON CONFLICT DO UPDATE，不是 REPLACE 的「先删后插」 |
| `conversations` | `@Upsert` | 同上；未读、置顶、最后一条消息都整行覆盖更新 |
| `messages` | `@Insert(ABORT)` | 主键是 UUID，撞号一定是 bug，宁可报错也不能静默覆盖历史聊天记录 |
| `db_meta` | `@Upsert` | 存「默认数据是否已灌过」的标记 |

**为什么不用 REPLACE**：`INSERT OR REPLACE` 的语义是先 DELETE 冲突行再 INSERT。
今天没有外键所以安全，但一旦将来给表加上带级联的外键，一次替换就会连带删掉关联数据。
`@Upsert` 不删行，语义也更贴合「有则改之、无则插入」。表之间暂不加外键：
目前没有删除会话的功能，不会有孤儿消息，一致性由事务和单一写线程保证。

### 初始化流程

1. `LauncherActivity` 是新的 LAUNCHER，它在**后台线程**执行 `DatabaseInitializer` + 全部表载入内存；
2. 成功 → 打开 `MainActivity` 并结束自己；失败 → **不进主界面**，弹对话框展示错误并提供「重试 / 退出」；
3. `RoomDataRepository` 内部有 `INITIALIZING / READY / FAILED` 状态机。
   读取前会检查 READY；非 READY 直接抛 `IllegalStateException`，**不降级返回空数据**——
   那会把「没准备好」和「真的没数据」混成同一种表现，问题会被掩盖很久。

**默认数据只灌一次**的判据是 `db_meta` 里的独立标记，**不是**「会话表是否为空」：
用户把会话删光之后表也是空的，用空表判断的话重启会重新灌默认数据，等于覆盖用户操作。
三步插入 + 标记写入在同一个事务里，只有全部成功标记才存在；中途失败整事务回滚，下次启动重新尝试。

### 事务边界

| 操作 | 事务 | 原因 |
|---|---|---|
| 首次默认数据 | 是（含标记写入） | 原子，避免「灌了一半」 |
| 插入消息 + 更新会话 | 是（`AppDatabase.appendMessage`） | 避免「消息存了但会话没更新」 |
| 新建会话 | 是（check-then-insert） | 避免竞态下重复插入 |
| 已读 / 置顶（单表单行） | Room 单条语句自带事务 | 再包一层没意义 |

所有落库都在**单个后台线程串行**执行，顺序即业务发生顺序；读操作全部走内存，不占主线程。

### 写失败怎么办

写库失败时（磁盘满、IO 异常）：先在**主线程**回滚这次的内存改动，再 Toast 提示，
最后通过 `WriteFailureListener` 回调让当前界面重新读数据。
这样「界面显示的」和「磁盘上的」始终一致，不会出现「看着发出去了其实没存」。
已知局限：回滚只针对当次改动，不重放后续已排队的写入（单机本地场景足够，更复杂的需要写队列）。

### 版本与迁移

- `version = 1`，**不开 `fallbackToDestructiveMigration`** —— 它会在版本变更时静默清库；
- `exportSchema = true`，schema JSON 落在 `app/schemas/com.example.wechat.db.AppDatabase/1.json`（已入库）；
- 将来改表结构：版本号 +1 + 写显式 `Migration(start, end)`，对着上面的 JSON 写 SQL。

## 六、构建方式

### 用 Android Studio（推荐）
直接用 Studio 打开这个目录，等 Gradle sync 完成，Run 'app' 即可。

### 用命令行
```bat
set JAVA_HOME=C:\Users\ASUS\tools\jdk-21
gradlew.bat :app:assembleDebug
```

> 注意：本机 `~/.gradle` 曾被一个空闲的 Gradle 守护进程占住锁文件（报 `拒绝访问`），
> 所以本项目构建时使用了工程内的独立缓存目录 `.gradle-home`（已在 `.gitignore` 里忽略）。
> Studio 用的是它自己的 `~/.gradle`，两边互不干扰。

## 七、已知的简化

- 底部 Tab 图标：选中态是「填充图形 + 绿色」，未选中是「描边 + 灰色」，细节与真机可能有差异。
- 头像是脚本生成的占位图（`tools/gen_avatars.py`，圆角色块 + 人形剪影）。导入真实头像后，
  把图片放进 `app/src/main/res/drawable/`，再把文件名写进 `DefaultDataSeed.contacts()` 的 `avatar` 即可
  （已经装过的 App 改这里不生效——默认数据只在首次创建数据库时灌一次，
  本地验证先卸载重装即可）。
- 默认数据只在首次安装灌一次；卸载重装的更早版本不清库的情况下不会重复插入。
