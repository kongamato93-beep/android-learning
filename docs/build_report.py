# -*- coding: utf-8 -*-
"""用实验一那份 docx 作模板，生成本次实验的报告（docx + md）。

模板结构：封面段落 + 表1（前台实现 / 后台部分 / 调试及总结）+ 表2（问题 / 总结）。
本脚本只替换文字，保留模板的字体、表格边框和分页，截图位置用红色占位符标出。
"""
import os
import re
import zipfile
import shutil
import xml.etree.ElementTree as ET

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
SRC = r"D:\andriod\课件\第一次实验报告（学号姓名）（双面打印提交）.docx"
OUT_DOCX = r"D:\andriod\WeChat\实验报告.docx"
OUT_MD = r"D:\andriod\WeChat\实验报告.md"

# ---------------------------------------------------------------- 内容
# ("h", 标题) ("h2", 小标题) ("p", 正文) ("li", 列表) ("code", 代码)
# ("table", 表头行列表, 列宽列表) ("img", 占位文字, 图注)

SEC1 = [
    ("h", "一、界面设计介绍（给出具体界面截图及界面间跳转逻辑）"),
    ("p", "本程序的主界面是「标题栏 + 内容区 + 底部导航栏」的三段式结构（activity_main.xml 的根布局是 orientation=vertical 的 LinearLayout）。"
          "内容区是一个 FrameLayout，里面装着四个 Fragment；底部导航栏是四个等宽的 LinearLayout，每个里面一个 ImageView 加一个 TextView。"
          "顶部标题栏左边是当前页名字，右边是「+」号。"),
    ("h2", "（一）四个分页与命名"),
    ("p", "四个分页按微信的命名来：微信、通讯录、发现、我。切换用 Fragment 的 show/hide，不用 replace：Fragment 常驻内存，切回来列表的滚动位置还在；"
          "微信的 Tab 本来也不能左右滑，用 ViewPager 反而多一层。标题栏文字跟着当前 Tab 走，右上角「+」只在微信页和通讯录页出现。"),
    ("table", [
        ["位置", "控件 id", "分页名称", "主要内容"],
        ["第 1 页", "tab_chat", "微信", "搜索框 + 会话列表（ListView，10 条会话）"],
        ["第 2 页", "tab_contacts", "通讯录", "新的朋友 / 群聊 / 标签 / 公众号 + A-Z 分组联系人"],
        ["第 3 页", "tab_discover", "发现", "朋友圈 / 视频号 / 直播 / 扫一扫 / 摇一摇 / 看一看 / 搜一搜 / 小程序"],
        ["第 4 页", "tab_me", "我", "个人资料区（登录账号）+ 七项功能列表（ListView）"],
    ], [1000, 1500, 1200, 4600]),

    ("h2", "（二）界面之间的跳转逻辑"),
    ("li", "1. 微信页点会话行：先弹 Toast「你点击了：张伟」，再带 conversationId 进 ChatActivity 聊天窗口；从聊天页返回后，列表的最后一条消息和时间会刷新。"),
    ("li", "2. 微信页长按会话行：弹一个 ListPopupWindow（置顶 / [测试] 收到一条消息），不跳页。"),
    ("li", "3. 通讯录点联系人：进 ContactDetailActivity 详情页。"),
    ("li", "4. 我页点顶部资料区：Toast 显示当前登录用户（5120244700）；点「设置」进 SettingsActivity；其余六项统一 Toast「该功能暂未实现」。"),
    ("li", "5. 发现页的八个入口同样只给 Toast 反馈，不留死按钮。"),
    ("p", "跳转关系可以概括成一句：页面内的四个 Tab 之间是 Fragment 的 show/hide，跨页面才 startActivity（ChatActivity / ContactDetailActivity / SettingsActivity）。"),

    ("h2", "（三）界面截图"),
    ("img", "【此处插入 图 1：微信分页会话列表】", "图 1 微信分页：ListView 显示 10 条会话，每行包含头像、昵称、最近一条消息和时间，未读的还有红点"),
    ("p", "微信页是评分点里最主要的一页：列表是自定义 ListView（id=lv_conversations），一共 10 条会话，每行四要素齐全——左边头像、上排昵称、下排最后一条消息、右上角时间；"
          "置顶的那一条（大学室友群）用浅灰底区分，未读数量用红点角标，超过 99 显示 99+。标题栏的「微信(3)」是全部会话的未读总数。"),
    ("img", "【此处插入 图 2：通讯录分页】", "图 2 通讯录分页：搜索框 + 四个功能入口 + A-Z 分组联系人 + 右侧字母索引条"),
    ("img", "【此处插入 图 3：发现分页】", "图 3 发现分页：八项入口按组分段，图标用彩色"),
    ("img", "【此处插入 图 4：我分页】", "图 4 我分页：顶部是实验三的登录账号 5120244700 及其头像，下面是七项功能的自定义 ListView"),
    ("p", "「我」页顶部是实验三登录的那个账号（学号 5120244700，头像索引 0 对应的那张），不是随手编的假名字；下面的七项依次是服务、收藏、朋友圈、作品、小店与卡包、表情、设置，"
          "按微信的习惯分成三段，段与段之间留 8dp 灰底。"),
    ("img", "【此处插入 图 5：点击会话行后的 Toast】", "图 5 点击会话行弹出的 Toast「你点击了：张伟」（Toast 弹出后立刻跳转到聊天页，所以截图时聊天页已经打开）"),
    ("img", "【此处插入 图 6：我页点顶部资料区的 Toast】", "图 6 我页点顶部资料区弹出的 Toast「当前登录用户：5120244700」"),
    ("img", "【此处插入 图 7：聊天页】", "图 7 点会话行跳转到的聊天页（ChatActivity），用来体现界面之间的跳转关系"),
]

SEC2 = [
    ("h", "二、界面控件介绍（描述程序用到的各种界面控件及其关键属性）"),
    ("table", [
        ["控件 id", "类型", "作用与关键属性"],
        ["lv_conversations", "ListView", "微信页会话列表。divider=@null、dividerHeight=0dp 关掉系统分割线（行布局里自己画不通栏的线）；overScrollMode=never 去掉边缘拖影"],
        ["iv_avatar", "ImageView", "会话行头像，40dp，scaleType=centerCrop，比例不对时按短边裁切不变形"],
        ["tv_name", "TextView", "会话行昵称，单行，primary 色（#181818），17sp"],
        ["tv_last_message", "TextView", "最后一条消息，maxLines=1 + ellipsize=end，太长显示省略号"],
        ["tv_time", "TextView", "右上角时间，TimeUtil 按「今天/昨天/星期/日期」四档格式化"],
        ["tv_unread", "TextView", "未读红点：>99 显示 99+，为 0 时 setVisibility(GONE) 不占位"],
        ["lv_entries", "ListView", "我页七项功能列表，divider=@null，行高 56dp（common_item_height）"],
        ["iv_icon / tv_title / tv_extra", "ImageView + TextView", "我页行内：26dp 图标（运行时用 ImageViewCompat.setImageTintList 上色）、标题、右侧说明文字（没内容时 GONE）"],
        ["view_divider", "View", "0.5dp 分割线，左侧留 64dp 缩进（和文字对齐），每组最后一项隐藏"],
        ["view_group_space", "View", "8dp 灰底分组间隔，条目带 startGroup 标记时 VISIBLE"],
        ["iv_avatar / tv_name / tv_wechat_id", "ImageView + TextView ×2", "我页顶部资料区：64dp 头像、加粗姓名（17sp）、灰色微信号"],
        ["tab 图标", "ImageView ×4", "srcCompat=@drawable/tab_icon_chat 等，tint=@color/tab_icon_tint，选中态由 selector 切成 filled 版本"],
    ], [2200, 1900, 4200]),
    ("h2", "关键属性说明"),
    ("li", "1. ListView 的 divider / dividerHeight：ListView 默认自带通栏分割线，本项目行布局里已经画了不通栏的线，如果不把 divider 设成 @null 就会出现两条线、行高也会多出几像素。"),
    ("li", "2. BaseAdapter.getView 的三个参数：position 是第几行，convertView 是可复用的旧行（第一次显示时为 null），parent 用来给 inflate 提供 LayoutParams。自定义适配器的核心逻辑都在这里。"),
    ("li", "3. ImageView.scaleType=centerCrop：头像素材尺寸不统一，用 centerCrop 保证圆形区域填满又不拉伸。"),
    ("li", "4. selector + state_selected：底部图标是 <selector>，选中时换成 filled 版；MainActivity 里对 Tab 容器调 setSelected(true)，选中状态会向下传给子 View，图标和文字颜色一起变，不用手动逐个改颜色。"),
    ("li", "5. <vector> 矢量图：四个 Tab 图标都是 24dp viewport、stroke 1.8 的矢量路径，只放一份就适配所有屏幕密度，比切 5 套 png 省事。"),
]

SEC3 = [
    ("h", "三、类设计（介绍程序用到的各种组件类、工具类、自定义类，建议绘制类图描述）"),
    ("table", [
        ["类名", "职责"],
        ["MainActivity", "主界面容器：四个 Fragment 的 show/hide、标题栏文字、Tab 选中态、未读角标"],
        ["ChatListFragment", "微信页：装配 ListView 与适配器，处理点击 / 长按 / 搜索过滤"],
        ["MeFragment", "我页：读登录用户填资料区，构造七项 EntryItem 列表"],
        ["ConversationListAdapter", "会话列表适配器，继承 BaseAdapter，负责行复用与控件绑定"],
        ["MeEntryListAdapter", "我页列表适配器，额外控制分割线与分组间隔"],
        ["LoginUserStore", "登录用户读取（SharedPreferences），默认值是实验三那个账号"],
        ["EntryItem / Conversation / Contact", "行数据模型，只放数据不带逻辑"],
        ["DataRepository / DataProvider", "数据仓库接口与唯一出口，UI 只认接口，不直接 new 数据源"],
        ["ChatActivity / SettingsActivity", "聊天页、设置页"],
    ], [3000, 5300]),
    ("h2", "为什么这么分"),
    ("li", "适配器单独成类：行绑定和页面逻辑是两件事，塞在 Fragment 里页面会越写越长；而且会话列表和我页列表的绑定规则完全不同（一个有未读角标，一个有分组间隔），硬凑一个适配器反而要写一堆 if。"),
    ("li", "LoginUserStore 单独成类：它回答的是「谁登录了」，跟界面无关。以后加登录页只要调它的 save()，MeFragment 一行都不用改。"),
    ("li", "数据只从 DataProvider.get() 取：Fragment 不直接 new 数据源，从内存数据换成 Room 数据库时 UI 层没有改动。"),
    ("img", "【此处插入 图 8：类图（可直接插图 docs/figures/fig_class.svg 的截图）】", "图 8 与本次实验相关的类：实线实心菱形是组合，虚线空心箭头是依赖"),
]

SEC4 = [
    ("h", "四、数据结构及算法（介绍程序中用到的各种重要数据结构及算法，算法建议绘制流程图）"),
    ("h2", "（一）数据结构"),
    ("li", "1. List<Conversation>：会话列表的数据源，先按「置顶优先 + 最后消息时间倒序」排好序再交给适配器；适配器内部再维护一份过滤后的列表，搜索时不动原始数据。"),
    ("li", "2. List<EntryItem>：我页七项。EntryItem 除了图标、标题、颜色，还带 startGroup 和 showDivider 两个布尔量——用数据描述版式，getView 里就不用写 position==2 这种硬编码。"),
    ("li", "3. 没有用 Map：这一版只有十几条数据，按 id 线性查一次就够；如果联系人上千，应该在 Repository 里加一个 id → Contact 的 HashMap 缓存。"),
    ("h2", "（二）算法一：ListView 的行复用（getView）"),
    ("code", "getView(position, convertView, parent)\n"
             "    if (convertView == null)                     // 第一次显示，没有可复用的行\n"
             "        convertView = inflate(item_conversation)\n"
             "        holder = new ViewHolder(convertView)     // 一次性 findViewById 六个控件\n"
             "        convertView.setTag(holder)               // 存进 tag，下次直接取\n"
             "    else\n"
             "        holder = (ViewHolder) convertView.getTag()\n"
             "    conversation = data.get(position)\n"
             "    peer = repository.getContact(conversation.getPeerId())\n"
             "    绑定头像、昵称、最后一条消息、时间、未读角标\n"
             "    return convertView"),
    ("p", "滑动十行时 inflate 只发生屏幕可见的那几次，之后都是复用；ViewHolder 把 findViewById 从「每次滑动都做」变成「每次创建行才做」。"
          "单次绑定是 O(1)，整体 O(n)（n 是可见行数，不是数据条数）。"),
    ("h2", "（三）算法二：关键字过滤"),
    ("p", "遍历会话列表，对每条的「昵称 + 最后一条消息」做 contains 判断（两边都转小写），命中就加入新列表，最后 notifyDataSetChanged。"
          "复杂度 O(n·m)，n 是会话数、m 是关键字长度，十几条数据完全够用；数据量大时应该交给数据库的 LIKE 查询或者做分词索引。"),
    ("img", "【此处插入 图 9：流程图（可直接插图 docs/figures/fig_flow.svg 的截图）】", "图 9 getView 的行复用流程：convertView 为 null 才 inflate 并建 ViewHolder"),
]

SEC5 = [
    ("h", "五、程序运行、调试过程遇到的问题及解决方法"),
    ("p", "问题 1：RecyclerView 换成 ListView，适配器要整个重写。"),
    ("p", "原来写的是 RecyclerView.Adapter，有 onCreateViewHolder / onBindViewHolder；换成 BaseAdapter 之后得自己实现 getCount / getItem / getItemId / getView 四个方法，"
          "刷新也从 notifyItemChanged 变成 notifyDataSetChanged。踩的坑是 setData 之后忘了调 notifyDataSetChanged，列表一直显示旧数据——ListView 不会自己感知集合变化。"),
    ("p", "问题 2：ListView 的行设 margin 完全没效果。"),
    ("p", "想在我页两个功能区之间留 8dp 灰底间隔，在行根布局上写 layout_marginTop，跑起来一点距离都没有。查资料才知道，ListView 给行设置的是 AbsListView.LayoutParams，"
          "它只有宽高两个字段，没有 margin，所以 margin 被直接忽略了。最后在行布局顶部加了一个 8dp 的占位 View（view_group_space），需要分组时设成 VISIBLE，效果等价。"),
    ("p", "问题 3：分割线出现了两条。"),
    ("p", "ListView 自带 divider，行布局里又画了一条，视觉上变成双线，行高也对不上。把 ListView 的 divider 设为 @null、dividerHeight 设 0dp 之后解决。"),
    ("p", "问题 4：读不到实验三的登录用户。"),
    ("p", "本来想直接读 MyApplication2 的 SharedPreferences（文件名 users），结果读出来永远是默认值。原因是 Android 7 以后每个 App 的私有目录互相隔离，"
          "跨包读不到对方的 SP。最后新建 LoginUserStore，把实验三的账号（5120244700、头像索引 0）作为默认值带过来，头像图片也复制了一份并改名 login_avatar_1~3，避免覆盖本项目已有的 avatar_1.jpg。"),
    ("p", "问题 5：切到「我」页，标题栏还显示「微信(3)」。"),
    ("p", "原因是 selectTab() 里先调了 updateUnread()、之后才给 currentIndex 赋值，而 updateUnread() 正是拿 currentIndex 去取标题文字的，等于用了上一个 Tab 的下标。"
          "把赋值提到更新之前就正常了。这个 bug 是四个 Tab 来回切才发现的，只盯着第一页看不出来。"),
    ("p", "问题 6：真机装不上，adb install 报 INSTALL_FAILED_ABORTED: User rejected permissions。"),
    ("p", "不是命令写错，是手机上弹了安装确认框没人点。后面统一改用模拟器（Pixel 6，Android 17）验证。"),
    ("p", "问题 7：截图抓不到 Toast。"),
    ("p", "Toast 是系统窗口（WindowManager 添加的），uiautomator dump 出来的树里没有它，只能 dump 到应用自己的窗口。做法是点击后 sleep 0.25 秒再 screencap，"
          "或者先 dumpsys window | grep -i toast 确认窗口存在再截图。"),
]

SEC6 = [
    ("h", "六、实现功能上的不足以及完成这几次实验后的知识经验总结"),
    ("h2", "（一）实现功能上的不足"),
    ("li", "1. 我页七项里只有「设置」是真页面，其余六项都是 Toast；朋友圈、服务这些还没有内容页。"),
    ("li", "2. 会话列表每次刷新都是整表重建：改一条消息也 notifyDataSetChanged，行多了会闪。ListView 时代不明显，换成 RecyclerView 就得用 DiffUtil。"),
    ("li", "3. 适配器每次 getView 都调一次 repository.getContact()，联系人多了会有性能问题，应该在 Repository 里做 id → Contact 的缓存。"),
    ("li", "4. 头像没有内存/磁盘缓存，滚动时会重复 decode 图片（现在只有 10 条数据，看不出问题）。"),
    ("li", "5. 底部图标是自己用 vector 画的路径，跟微信官方图标比，线条粗细和圆角还有差距。"),
    ("h2", "（二）知识经验总结"),
    ("li", "1. 用 AI 生成三分页骨架很快，但它是按「能跑」写的，不是按「能验收」写的：默认给了 RecyclerView（实验要求是 ListView），Tab 名字也是 Tab1/Tab2。"
          "接受 AI 的结果之前得先把要求逐条对一遍，不然改起来比自己写还慢。"),
    ("li", "2. ListView 和 RecyclerView 的差别不只是新旧：ListView 的复用逻辑全压在 getView 里，写错了不报错，只会卡或者串数据；RecyclerView 把创建和绑定拆成两个方法，不容易写错。"
          "这次为了满足实验要求退回 ListView，反而把复用原理看明白了。"),
    ("li", "3. 版式需求要用数据表达，不要用 if 硬编码。EntryItem 里的 startGroup / showDivider 就是这么来的，加一项、调顺序都不用改适配器。"),
    ("li", "4. 界面验收不能只靠肉眼。控件类型是把 uiautomator dump 出来查到 class=\"android.widget.ListView\" 才确认的，Toast 也是先查窗口再截图；"
          "「看着像 ListView」在报告里站不住。"),
    ("h2", "（三）本次实验评分点对照"),
    ("table", [
        ["序号", "评分要求", "分值", "实现位置", "状态"],
        ["1", "用 AI 工具创建三分页界面程序", "20", "AI 生成骨架，本项目实际做了四分页", "达成（超额）"],
        ["2", "手动添加第四分页，参照微信命名四页", "10", "MainActivity + 四个 Fragment，Tab 命名微信/通讯录/发现/我", "达成"],
        ["3", "微信页插入 ListView 显示 10 个好友姓名", "20", "fragment_chat_list.xml 的 lv_conversations，10 条会话", "达成"],
        ["4", "自定义 ListView，每行显示头像/姓名/最近消息/时间", "10", "ConversationListAdapter（BaseAdapter + ViewHolder）绑定 item_conversation.xml", "达成"],
        ["5", "点击每行弹出 Toast", "10", "ChatListFragment 的 setOnItemClickListener，Toast「你点击了：X」", "达成"],
        ["6", "我页顶部用 ImageView + TextView 显示实验三登录用户", "10", "fragment_me.xml 的 ll_profile + LoginUserStore（5120244700）", "达成"],
        ["7", "我页自定义 ListView 显示七项功能", "10", "lv_entries + MeEntryListAdapter，服务/收藏/朋友圈/作品/小店与卡包/表情/设置", "达成"],
        ["8", "底部导航使用矢量图标", "10", "tab_icon_*.xml（selector）+ ic_tab_*（vector）+ tint", "达成"],
        ["合计", "—", "100", "—", "全部达成"],
    ], [600, 2900, 700, 3200, 900]),
]


def iter_all():
    for sec in (SEC1, SEC2, SEC3, SEC4, SEC5, SEC6):
        for b in sec:
            yield b


# ---------------------------------------------------------------- docx 生成
NS_DECL = ('xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
           'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"')


def _run(text, bold=False, size=24, color=None, font="宋体"):
    rpr = f'<w:rFonts w:ascii="{font}" w:hAnsi="{font}" w:eastAsia="{font}" w:cs="{font}"/>'
    if bold:
        rpr += "<w:b/><w:bCs/>"
    if color:
        rpr += f'<w:color w:val="{color}"/>'
    rpr += f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>'
    t = (text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
    return f'<w:r><w:rPr>{rpr}</w:rPr><w:t xml:space="preserve">{t}</w:t></w:r>'


def para(text, bold=False, size=24, color=None, align=None, first_line=0,
         left=0, space_before=0, space_after=50, line=300, font="宋体"):
    # 注意：pPr 的子元素必须按 schema 顺序，shd 在 spacing 之前，否则 Word 报「文件可能已损坏」
    ppr = f'<w:spacing w:before="{space_before}" w:after="{space_after}" w:line="{line}" w:lineRule="auto"/>'
    ppr += f'<w:ind w:firstLine="{first_line}" w:left="{left}"/>'
    if align:
        ppr += f'<w:jc w:val="{align}"/>'
    return f'<w:p {NS_DECL}><w:pPr>{ppr}</w:pPr>{_run(text, bold, size, color, font)}</w:p>'


def para_multi(lines, bold=False, size=18, color=None, font="Consolas", shade=None):
    """代码块：同一个段落里多行（用 <w:br/> 换行），带浅灰底。"""
    rpr = (f'<w:rFonts w:ascii="{font}" w:hAnsi="{font}" w:eastAsia="宋体"/>'
           f'{("<w:b/>" if bold else "")}<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>')
    if color:
        rpr += f'<w:color w:val="{color}"/>'
    runs = []
    for i, ln in enumerate(lines):
        t = ln.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        runs.append(f'<w:r><w:rPr>{rpr}</w:rPr><w:t xml:space="preserve">{t}</w:t></w:r>')
        if i != len(lines) - 1:
            runs.append(f'<w:r><w:rPr>{rpr}</w:rPr><w:br/></w:r>')
    ppr = ('<w:shd w:val="clear" w:color="auto" w:fill="F2F3F5"/>'
           '<w:spacing w:before="80" w:after="80" w:line="280" w:lineRule="auto"/>'
           '<w:ind w:left="300"/>')
    return f'<w:p {NS_DECL}><w:pPr>{ppr}</w:pPr>{"".join(runs)}</w:p>'


def table_xml(rows, widths):
    border = ('<w:tblBorders>'
              + "".join(f'<w:{s} w:val="single" w:sz="4" w:space="0" w:color="808080"/>'
                        for s in ("top", "left", "bottom", "right", "insideH", "insideV"))
              + '</w:tblBorders>')
    total = sum(widths)
    x = [f'<w:tbl {NS_DECL}><w:tblPr><w:tblW w:w="{total}" w:type="dxa"/><w:jc w:val="center"/>{border}'
         '<w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>']
    x += [f'<w:gridCol w:w="{w}"/>' for w in widths]
    x.append('</w:tblGrid>')
    for ri, row in enumerate(rows):
        header = ri == 0
        x.append('<w:tr>')
        if header:
            x.append('<w:trPr><w:tblHeader/></w:trPr>')
        for ci, cell in enumerate(row):
            fill = '<w:shd w:val="clear" w:color="auto" w:fill="ECEFF3"/>' if header else ''
            x.append(f'<w:tc><w:tcPr><w:tcW w:w="{widths[ci]}" w:type="dxa"/>{fill}'
                     '<w:vAlign w:val="center"/></w:tcPr>')
            x.append(para(cell, bold=header, size=20, space_after=0, line=280))
            x.append('</w:tc>')
        x.append('</w:tr>')
    x.append('</w:tbl>')
    return "".join(x)


def blocks_to_xml(blocks):
    out = []
    for b in blocks:
        kind = b[0]
        if kind == "h":
            out.append(para(b[1], bold=True, size=26, space_before=40, space_after=100, line=320))
        elif kind == "h2":
            out.append(para(b[1], bold=True, size=24, space_before=60, space_after=60, line=320))
        elif kind == "p":
            out.append(para(b[1], first_line=480))
        elif kind == "li":
            out.append(para(b[1], left=240, first_line=0, space_after=40))
        elif kind == "code":
            out.append(para_multi(b[1].split("\n")))
        elif kind == "table":
            out.append(table_xml(b[1], b[2]))
            out.append(para("", space_after=40))
        elif kind == "img":
            out.append(para(b[1], bold=True, size=22, color="C00000", align="center",
                            space_before=120, space_after=40, line=280))
            out.append(para(b[2], size=20, color="404040", align="center", space_after=160, line=280))
    return out


def set_cell(cell, blocks):
    """清空单元格（保留 tcPr），写入新段落。"""
    for child in list(cell):
        if child.tag in (f"{{{W_NS}}}p", f"{{{W_NS}}}tbl"):
            cell.remove(child)
    for xml in blocks_to_xml(blocks):
        cell.append(ET.fromstring(xml))


def replace_para_text(p, new_text):
    """把段落里所有 run 去掉，只留一个 run，沿用第一个 run 的 rPr。"""
    runs = p.findall(f"{{{W_NS}}}r")
    rpr = None
    if runs:
        rpr = runs[0].find(f"{{{W_NS}}}rPr")
    for r in runs:
        p.remove(r)
    tpl = rpr if rpr is not None else ET.fromstring(f'<w:rPr {NS_DECL}><w:rFonts w:ascii="宋体" w:eastAsia="宋体"/></w:rPr>')
    r = ET.SubElement(p, f"{{{W_NS}}}r")
    r.append(tpl)
    t = ET.SubElement(r, f"{{{W_NS}}}t")
    t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    t.text = new_text


def build_docx():
    shutil.copyfile(SRC, OUT_DOCX)
    with zipfile.ZipFile(SRC) as z:
        doc_xml = z.read("word/document.xml").decode("utf-8")

    for prefix, uri in re.findall(r'xmlns:([a-zA-Z0-9]+)="([^"]+)"', doc_xml):
        try:
            ET.register_namespace(prefix, uri)
        except ValueError:
            pass
    # mc:Ignorable 里列了 w15 / wp14，而这两个前缀在正文里根本没用，ElementTree 会把它们的
    # 声明丢掉，留下一个指向未声明前缀的 mc:Ignorable —— Word 打开就报「文件可能已损坏」。
    doc_xml = re.sub(r'\smc:Ignorable="[^"]*"', '', doc_xml)
    root = ET.fromstring(doc_xml)
    body = root.find(f"{{{W_NS}}}body")

    # 封面
    for p in body.findall(f"{{{W_NS}}}p"):
        txt = "".join(t.text or "" for t in p.iter(f"{{{W_NS}}}t")).strip()
        if txt.startswith("实 验 报 告"):
            replace_para_text(p, "实 验 报 告 四")
        elif txt.startswith("实    验："):
            replace_para_text(p, "实    验：      四、ListView 与自定义适配器 —— 仿微信界面")
        elif txt.startswith("时    间："):
            replace_para_text(p, "时    间：      2026/10/21")

    tables = body.findall(f"{{{W_NS}}}tbl")
    t0, t1 = tables[0], tables[1]
    rows0 = t0.findall(f"{{{W_NS}}}tr")
    set_cell(rows0[1].find(f"{{{W_NS}}}tc"), SEC1)
    set_cell(rows0[2].find(f"{{{W_NS}}}tc"), SEC2)
    set_cell(rows0[4].find(f"{{{W_NS}}}tc"), SEC3)
    set_cell(rows0[5].find(f"{{{W_NS}}}tc"), SEC4)
    rows1 = t1.findall(f"{{{W_NS}}}tr")
    set_cell(rows1[0].find(f"{{{W_NS}}}tc"), SEC5)
    set_cell(rows1[1].find(f"{{{W_NS}}}tc"), SEC6)

    new_xml = ET.tostring(root, encoding="UTF-8", xml_declaration=True)

    tmp = OUT_DOCX + ".tmp"
    with zipfile.ZipFile(SRC) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/document.xml":
                data = new_xml
            zout.writestr(item, data)
    os.replace(tmp, OUT_DOCX)
    print("docx ok", OUT_DOCX, len(new_xml))


# ---------------------------------------------------------------- markdown
def build_md():
    out = ["# Android 应用开发实践 实验报告", "",
           "**实验**：四、ListView 与自定义适配器 —— 仿微信界面  ",
           "**学号**：5120244700　**姓名**：彭玢毓　**班级**：信安2402班　**时间**：2026/10/21", "",
           "> 说明：这份 md 和同目录的 `实验报告.docx` 内容一致。docx 是用实验一那份模板填的（封面、表格边框、字体都不动），"
           "红色占位符就是截图该放的位置；本文件方便你直接复制文字改。", ""]
    for b in iter_all():
        kind = b[0]
        if kind == "h":
            out += ["", f"## {b[1]}", ""]
        elif kind == "h2":
            out += ["", f"### {b[1]}", ""]
        elif kind == "p":
            out += [b[1], ""]
        elif kind == "li":
            out += [b[1], ""]
        elif kind == "code":
            out += ["```", b[1], "```", ""]
        elif kind == "table":
            rows = b[1]
            out.append("| " + " | ".join(rows[0]) + " |")
            out.append("|" + "|".join(["---"] * len(rows[0])) + "|")
            for r in rows[1:]:
                out.append("| " + " | ".join(r) + " |")
            out.append("")
        elif kind == "img":
            out += [f"> **{b[1]}**", f"> {b[2]}", ""]
    with open(OUT_MD, "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    print("md ok", OUT_MD)


if __name__ == "__main__":
    build_docx()
    build_md()
