# -*- coding: utf-8 -*-
"""生成实验报告用的两张示意图（类图 / 流程图），纯 SVG，用 Edge 无头渲染成 PNG。"""
import os

FONT = "Microsoft YaHei, SimSun, sans-serif"
OUT = os.path.dirname(os.path.abspath(__file__))


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def uml_box(x, y, w, name, fields, methods, stereo=None):
    """返回 (svg片段, 高度)。标题栏 + 属性区 + 方法区。"""
    row = 26
    title_h = 40 if not stereo else 56
    h = title_h + row * (len(fields) + len(methods)) + 22
    parts = []
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4" fill="#ffffff" stroke="#2b2b2b" stroke-width="2"/>')
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{title_h}" rx="4" fill="#eceff3" stroke="#2b2b2b" stroke-width="2"/>')
    cy = y + (18 if not stereo else 16)
    if stereo:
        parts.append(f'<text x="{x + w/2}" y="{cy}" font-family="{FONT}" font-size="15" fill="#555" text-anchor="middle">«{esc(stereo)}»</text>')
        cy += 22
    parts.append(f'<text x="{x + w/2}" y="{cy}" font-family="{FONT}" font-size="20" font-weight="bold" fill="#111" text-anchor="middle">{esc(name)}</text>')
    ty = y + title_h
    for i, f in enumerate(fields):
        parts.append(f'<text x="{x + 14}" y="{ty + 20 + i*row}" font-family="{FONT}" font-size="15" fill="#222">{esc(f)}</text>')
    if fields:
        ty2 = ty + row * len(fields) + 8
        parts.append(f'<line x1="{x}" y1="{ty2}" x2="{x+w}" y2="{ty2}" stroke="#2b2b2b" stroke-width="1.2"/>')
        my = ty2
    else:
        my = ty
        parts.append(f'<line x1="{x}" y1="{my}" x2="{x+w}" y2="{my}" stroke="#2b2b2b" stroke-width="1.2"/>')
    for i, m in enumerate(methods):
        parts.append(f'<text x="{x + 14}" y="{my + 20 + i*row}" font-family="{FONT}" font-size="15" fill="#222">{esc(m)}</text>')
    return "\n".join(parts), h


def arrow_defs():
    return f'''<defs>
  <marker id="arrow" markerWidth="12" markerHeight="12" refX="10" refY="5" orient="auto">
    <path d="M0,0 L10,5 L0,10 z" fill="#2b2b2b"/>
  </marker>
  <marker id="openArrow" markerWidth="14" markerHeight="14" refX="12" refY="6" orient="auto">
    <path d="M0,1 L12,6 L0,11" fill="none" stroke="#2b2b2b" stroke-width="1.6"/>
  </marker>
  <marker id="diamond" markerWidth="18" markerHeight="10" refX="1" refY="5" orient="auto">
    <path d="M0,5 L8,0 L16,5 L8,10 z" fill="#2b2b2b" stroke="#2b2b2b"/>
  </marker>
</defs>'''


# ---------------------------------------------------------------- 类图
def class_diagram():
    W, H = 1520, 980
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
         f'<rect width="{W}" height="{H}" fill="#ffffff"/>', arrow_defs()]

    boxes = {}
    x, y, w = 560, 30, 400
    svg, h = uml_box(x, y, w, "MainActivity",
                     ["- fragments: List<Fragment>", "- tabViews: List<View>", "- currentIndex: int"],
                     ["+ selectTab(int index)", "+ updateUnread()"])
    boxes["main"] = (x, y, w, h)
    s.append(svg)

    svg, h = uml_box(150, 250, 380, "ChatListFragment",
                     ["- adapter: ConversationListAdapter", "- listView: ListView"],
                     ["+ refreshList()", "- refresh(String keyword)"])
    boxes["chat"] = (150, 250, 380, h)
    s.append(svg)

    svg, h = uml_box(900, 250, 380, "MeFragment",
                     ["- loginUser: LoginUserStore"],
                     ["- buildEntries(): List<EntryItem>"])
    boxes["me"] = (900, 250, 380, h)
    s.append(svg)

    svg, h = uml_box(110, 500, 420, "ConversationListAdapter",
                     ["«extends BaseAdapter»", "- data: List<Conversation>"],
                     ["+ setData(List<Conversation>, String)", "+ getView(int, View, ViewGroup)"])
    boxes["conv"] = (110, 500, 420, h)
    s.append(svg)

    svg, h = uml_box(820, 500, 420, "MeEntryListAdapter",
                     ["«extends BaseAdapter»", "- data: List<EntryItem>"],
                     ["+ setData(List<EntryItem>)", "+ getView(int, View, ViewGroup)"])
    boxes["entry"] = (820, 500, 420, h)
    s.append(svg)

    svg, h = uml_box(1290, 500, 210, "LoginUserStore",
                     ["- sp: SharedPreferences"],
                     ["+ getUsername()", "+ getAvatarRes()"])
    boxes["login"] = (1290, 500, 210, h)
    s.append(svg)

    svg, h = uml_box(110, 760, 420, "DataRepository", ["（接口，UI 只认它）"],
                     ["+ getConversations()", "+ getContact(String id)"], stereo="interface")
    boxes["repo"] = (110, 760, 420, h)
    s.append(svg)

    svg, h = uml_box(820, 760, 420, "EntryItem / Conversation",
                     ["- iconRes / title", "- isStartGroup / isPinned"],
                     ["+ setStartGroup(boolean)"])
    boxes["model"] = (820, 760, 420, h)
    s.append(svg)

    s.append(f'<text x="{W/2}" y="{H-14}" font-family="{FONT}" font-size="16" fill="#555" text-anchor="middle">'
             f'实线实心菱形＝组合（生命周期由整体控制）；虚线空心箭头＝依赖；图中只画了与本次实验相关的成员</text>')

    def top(k):
        x, y, w, h = boxes[k]
        return x + w / 2, y

    def bottom(k):
        x, y, w, h = boxes[k]
        return x + w / 2, y + h

    def left(k):
        x, y, w, h = boxes[k]
        return x, y + h / 2

    def right(k):
        x, y, w, h = boxes[k]
        return x + w, y + h / 2

    # MainActivity 组合 ChatListFragment / MeFragment
    m = bottom("main")
    s.append(f'<path d="M{m[0]},{m[1]} L{m[0]},215 L{top("chat")[0]},215 L{top("chat")[0]},{top("chat")[1]}" fill="none" stroke="#2b2b2b" stroke-width="2" marker-end="url(#diamond)" transform="translate(0,0)"/>')
    s.append(f'<path d="M{top("chat")[0]},205 L{top("chat")[0]},{top("chat")[1]}" fill="none" stroke="#2b2b2b" stroke-width="2"/>')
    s.append(f'<path d="M{m[0]},215 L{top("me")[0]},215 L{top("me")[0]},{top("me")[1]}" fill="none" stroke="#2b2b2b" stroke-width="2"/>')
    # 菱形画在 MainActivity 下方
    s.append(f'<path d="M{m[0]},{m[1]+2} L{m[0]-9},{m[1]+12} L{m[0]},{m[1]+22} L{m[0]+9},{m[1]+12} z" fill="#2b2b2b"/>')

    # Fragment 组合适配器
    for frag, ad in (("chat", "conv"), ("me", "entry")):
        a, b = bottom(frag), top(ad)
        s.append(f'<path d="M{a[0]},{a[1]} L{b[0]},{b[1]-14}" fill="none" stroke="#2b2b2b" stroke-width="2"/>')
        s.append(f'<path d="M{b[0]},{b[1]-2} L{b[0]-9},{b[1]-14} L{b[0]},{b[1]-24} L{b[0]+9},{b[1]-14} z" fill="#2b2b2b"/>')

    # MeFragment 依赖 LoginUserStore
    a, b = right("me"), left("login")
    s.append(f'<path d="M{a[0]},{a[1]} L{b[0]-30},{a[1]} L{b[0]-30},{b[1]}" fill="none" stroke="#2b2b2b" stroke-width="1.6" stroke-dasharray="7,5" marker-end="url(#openArrow)"/>')

    # 适配器读数据
    a = bottom("conv")
    s.append(f'<path d="M{a[0]},{a[1]} L{a[0]},{top("repo")[1]-10}" fill="none" stroke="#2b2b2b" stroke-width="1.6" stroke-dasharray="7,5" marker-end="url(#openArrow)"/>')
    a = bottom("entry")
    s.append(f'<path d="M{a[0]},{a[1]} L{a[0]},{top("model")[1]-10}" fill="none" stroke="#2b2b2b" stroke-width="1.6" stroke-dasharray="7,5" marker-end="url(#openArrow)"/>')

    # DataRepository 实现关系（用 DataProvider 标注文字代替额外类框）
    a = right("repo")
    s.append(f'<path d="M{a[0]},{a[1]} L{left("model")[0]-20},{a[1]} L{left("model")[0]-20},{left("model")[1]}" fill="none" stroke="#2b2b2b" stroke-width="1.6" stroke-dasharray="7,5"/>')
    s.append(f'<text x="{(a[0]+left("model")[0])/2}" y="{a[1]-8}" font-family="{FONT}" font-size="14" fill="#555" text-anchor="middle">由 DataProvider.get() 提供实例</text>')

    s.append('</svg>')
    return "\n".join(s)


# ---------------------------------------------------------------- 流程图
def flowchart():
    W, H = 1080, 1420
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
         f'<rect width="{W}" height="{H}" fill="#ffffff"/>', arrow_defs()]

    def box(cx, cy, w, h, text, kind="proc", size=18):
        out = []
        if kind == "term":
            out.append(f'<rect x="{cx-w/2}" y="{cy-h/2}" width="{w}" height="{h}" rx="{h/2}" fill="#eceff3" stroke="#2b2b2b" stroke-width="2"/>')
        elif kind == "dec":
            out.append(f'<path d="M{cx},{cy-h/2} L{cx+w/2},{cy} L{cx},{cy+h/2} L{cx-w/2},{cy} z" fill="#fff8e6" stroke="#2b2b2b" stroke-width="2"/>')
        else:
            out.append(f'<rect x="{cx-w/2}" y="{cy-h/2}" width="{w}" height="{h}" rx="4" fill="#ffffff" stroke="#2b2b2b" stroke-width="2"/>')
        lines = text.split("\n")
        for i, ln in enumerate(lines):
            yy = cy + size * 0.36 + (i - (len(lines) - 1) / 2) * (size + 6)
            out.append(f'<text x="{cx}" y="{yy}" font-family="{FONT}" font-size="{size}" fill="#111" text-anchor="middle">{esc(ln)}</text>')
        return "\n".join(out)

    def vline(x1, y1, y2, label=None, lx=12):
        out = [f'<path d="M{x1},{y1} L{x1},{y2}" stroke="#2b2b2b" stroke-width="2" fill="none" marker-end="url(#arrow)"/>']
        if label:
            out.append(f'<text x="{x1+lx}" y="{(y1+y2)/2+6}" font-family="{FONT}" font-size="16" fill="#c0392b">{esc(label)}</text>')
        return "\n".join(out)

    CX = 400
    s.append(box(CX, 50, 240, 56, "开始", "term"))
    s.append(vline(CX, 78, 122))

    steps = [
        (170, "getView(position, convertView, parent)", 460, 56),
    ]
    s.append(box(CX, 170, 470, 56, "getView(position, convertView, parent)"))
    s.append(vline(CX, 198, 250))

    s.append(box(CX, 310, 300, 110, "convertView == null ?", "dec", 20))
    s.append(vline(CX, 365, 420, "是（第一次显示，没有可复用的行）", 14))

    # 右侧：复用分支（否）
    RX = 720
    s.append(f'<path d="M{CX+150},{310} L{RX},{310}" stroke="#2b2b2b" stroke-width="2" fill="none" marker-end="url(#arrow)"/>')
    s.append(f'<text x="{CX+160}" y="300" font-family="{FONT}" font-size="16" fill="#c0392b">否（有缓存行）</text>')
    s.append(box(RX, 420, 300, 90, "holder = (ViewHolder)\nconvertView.getTag()", "proc", 17))
    s.append(vline(RX, 310, 375))
    s.append(vline(RX, 465, 560))

    # 左侧/中间：新建分支
    s.append(box(CX, 490, 330, 110, "inflate(item_conversation.xml)\n→ new ViewHolder(convertView)", "proc", 17))
    s.append(vline(CX, 545, 600))
    s.append(box(CX, 650, 330, 80, "convertView.setTag(holder)", "proc", 17))
    s.append(vline(CX, 690, 760))
    s.append(f'<path d="M{CX},{760} L{RX},{760}" stroke="#2b2b2b" stroke-width="2" fill="none"/>')

    # 汇合点
    s.append(f'<circle cx="{RX}" cy="760" r="6" fill="#2b2b2b"/>')
    s.append(vline(RX, 760, 830))

    s.append(box(RX, 880, 520, 70, "取出 position 对应的 Conversation\npeer = repository.getContact(peerId)", "proc", 17))
    s.append(vline(RX, 915, 975))
    s.append(box(RX, 1030, 560, 90, "设置头像 / 昵称 / 最后一条消息 / 时间 / 未读角标\n置顶会话换浅灰底", "proc", 17))
    s.append(vline(RX, 1075, 1140))
    s.append(box(RX, 1180, 300, 70, "return convertView", "proc", 18))
    s.append(vline(RX, 1215, 1275))
    s.append(box(RX, 1310, 200, 56, "结束", "term"))

    s.append('</svg>')
    return "\n".join(s)


for name, svg in (("fig_class", class_diagram()), ("fig_flow", flowchart())):
    p = os.path.join(OUT, name + ".svg")
    with open(p, "w", encoding="utf-8") as f:
        f.write(svg)
    print("wrote", p)
