# 用 System.Drawing 画实验报告用的两张图：类图 / getView 流程图
# 输出 PNG（插入 Word 用），逻辑坐标 * SCALE 得到实际像素
Add-Type -AssemblyName System.Drawing

$OUT = "D:\andriod\WeChat\docs\figures"
$SCALE = 3
$FONT = "Microsoft YaHei"
$C_LINE = [System.Drawing.Color]::FromArgb(43, 43, 43)
$C_HEAD = [System.Drawing.Color]::FromArgb(236, 239, 243)
$C_DEC = [System.Drawing.Color]::FromArgb(255, 248, 230)
$C_RED = [System.Drawing.Color]::FromArgb(192, 57, 43)
$C_GRAY = [System.Drawing.Color]::FromArgb(85, 85, 85)
$C_TEXT = [System.Drawing.Color]::FromArgb(17, 17, 17)

function New-Font {
    param([float]$size, [string]$style = "regular")
    $s = if ($style -eq "bold") { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
    New-Object System.Drawing.Font -ArgumentList @($FONT, $size, $s, [System.Drawing.GraphicsUnit]::Pixel)
}

function New-Canvas {
    param([int]$cw, [int]$ch)
    # 画布按 SCALE 放大：后面所有坐标都用逻辑坐标（和 SVG 版一致），缩放由 ScaleTransform 负责
    $b = New-Object System.Drawing.Bitmap -ArgumentList @([int]($cw * $SCALE), [int]($ch * $SCALE))
    $gg = [System.Drawing.Graphics]::FromImage($b)
    $gg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $gg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $gg.Clear([System.Drawing.Color]::White)
    $gg.ScaleTransform([float]$SCALE, [float]$SCALE)
    $script:CUR_BMP = $b
    $script:CUR_G = $gg
}

function New-Pen {
    param([float]$width = 2, [string]$dash = "solid")
    $p = New-Object System.Drawing.Pen -ArgumentList @($C_LINE, $width)
    if ($dash -eq "dash") { $p.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash }
    $p.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    return $p
}

function Arrow-Cap {
    param([float]$w = 6, [float]$h = 9)
    New-Object System.Drawing.Drawing2D.AdjustableArrowCap($w, $h, $true)
}

function Draw-Text {
    param($g, [string]$text, [float]$x, [float]$y, $font, $brush, [string]$align = "center")
    $sf = New-Object System.Drawing.StringFormat
    if ($align -eq "center") { $sf.Alignment = [System.Drawing.StringAlignment]::Center }
    else { $sf.Alignment = [System.Drawing.StringAlignment]::Near }
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, $brush, $x, $y, $sf)
    $sf.Dispose()
}

function RoundRect-Path {
    param([float]$x, [float]$y, [float]$w, [float]$h, [float]$r)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddLine($x + $r, $y, $x + $w - $r, $y)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddLine($x + $w, $y + $r, $x + $w, $y + $h - $r)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddLine($x + $w - $r, $y + $h, $x + $r, $y + $h)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.AddLine($x, $y + $h - $r, $x, $y + $r)
    $p.CloseFigure()
    return $p
}

# ---------------------------------------------------------------- 类图
function Draw-Class {
    param($g, [float]$x, [float]$y, [float]$w, [string]$name, [string]$stereo,
        [string[]]$fields, [string[]]$methods)
    $row = 28
    $titleH = if ($stereo) { 56 } else { 40 }
    $h = $titleH + $row * ($fields.Count + $methods.Count) + 22
    $pen = New-Pen -width 2
    $white = [System.Drawing.Brushes]::White
    $head = New-Object System.Drawing.SolidBrush($C_HEAD)
    $text = [System.Drawing.Brushes]::Black
    $gray = New-Object System.Drawing.SolidBrush($C_GRAY)

    $p1 = RoundRect-Path $x $y $w $h 5
    $g.FillPath($white, $p1); $g.DrawPath($pen, $p1)
    $p2 = RoundRect-Path $x $y $w $titleH 5
    $g.FillPath($head, $p2); $g.DrawPath($pen, $p2)

    $fName = New-Font 20 "bold"
    $fSmall = New-Font 15
    $fBody = New-Font 17
    $cy = $y + 19
    if ($stereo) {
        Draw-Text $g ("«" + $stereo + "»") ($x + $w / 2) ($y + 17) $fSmall $gray
        $cy = $y + 40
    }
    Draw-Text $g $name ($x + $w / 2) $cy $fName $text
    $ty = $y + $titleH
    for ($i = 0; $i -lt $fields.Count; $i++) {
        Draw-Text $g $fields[$i] ($x + 14) ($ty + 18 + $i * $row) $fBody $text "left"
        $tw = $g.MeasureString($fields[$i], $fBody).Width
        if ($tw -gt $w - 28) { Write-Output ("WARN long field: " + $fields[$i] + " " + [int]$tw) }
    }
    $my = $ty + $row * $fields.Count + 8
    $g.DrawLine((New-Pen -width 1.2), $x, $my, $x + $w, $my)
    for ($i = 0; $i -lt $methods.Count; $i++) {
        Draw-Text $g $methods[$i] ($x + 14) ($my + 18 + $i * $row) $fBody $text "left"
        $tw = $g.MeasureString($methods[$i], $fBody).Width
        if ($tw -gt $w - 28) { Write-Output ("WARN long method: " + $methods[$i] + " " + [int]$tw) }
    }
    $pen.Dispose(); $head.Dispose(); $gray.Dispose()
    return $h
}

function Draw-DiamondFilled {
    param($g, [float]$cx, [float]$cy)
    $pts = @(
        (New-Object System.Drawing.PointF($cx, $cy - 10)),
        (New-Object System.Drawing.PointF($cx + 9, $cy)),
        (New-Object System.Drawing.PointF($cx, $cy + 10)),
        (New-Object System.Drawing.PointF($cx - 9, $cy))
    )
    $g.FillPolygon([System.Drawing.Brushes]::Black, $pts)
    $g.DrawPolygon((New-Pen -width 2), $pts)
}

function Draw-OpenArrow {
    param($g, [float]$x, [float]$y, [string]$dir)
    $pen = New-Pen -width 1.8
    $L = 11; $Wd = 5.5
    switch ($dir) {
        "down" {
            $g.DrawLine($pen, $x - $Wd, $y - $L, $x, $y)
            $g.DrawLine($pen, $x + $Wd, $y - $L, $x, $y)
        }
        "right" {
            $g.DrawLine($pen, $x - $L, $y - $Wd, $x, $y)
            $g.DrawLine($pen, $x - $L, $y + $Wd, $x, $y)
        }
        "up" {
            $g.DrawLine($pen, $x - $Wd, $y + $L, $x, $y)
            $g.DrawLine($pen, $x + $Wd, $y + $L, $x, $y)
        }
        "left" {
            $g.DrawLine($pen, $x + $L, $y - $Wd, $x, $y)
            $g.DrawLine($pen, $x + $L, $y + $Wd, $x, $y)
        }
    }
    $pen.Dispose()
}

function Build-ClassDiagram {
    $W = 1120; $H = 1150
    New-Canvas $W $H
    $bmp = $script:CUR_BMP; $g = $script:CUR_G
    $pen = New-Pen -width 2
    $penArrow = New-Pen -width 2
    $penArrow.CustomEndCap = (Arrow-Cap 6 9)
    $penDash = New-Pen -width 1.8 "dash"
    $gray = New-Object System.Drawing.SolidBrush($C_GRAY)
    $red = New-Object System.Drawing.SolidBrush($C_RED)

    # ---- 类框：返回高度，供连线计算
    $hMain = Draw-Class $g 360 30 400 "MainActivity" "" `
    @("- fragments: List<Fragment>", "- tabViews: List<View>", "- currentIndex: int") `
        @("+ selectTab(int index)", "+ updateUnread()")
    Add-Content -Encoding utf8 "$OUT\diag.txt" ("hMain type=" + $hMain.GetType().Name + " val=" + [string]$hMain)
    $hChat = Draw-Class $g 60 290 440 "ChatListFragment" "" `
    @("- adapter: ConversationListAdapter", "- listView: ListView") `
        @("+ refreshList()", "- refresh(String keyword)")
    $hMe = Draw-Class $g 620 290 440 "MeFragment" "" `
    @("- loginUser: LoginUserStore") `
        @("- buildEntries(): List<EntryItem>")
    $hConv = Draw-Class $g 60 520 440 "ConversationListAdapter" "" `
    @("- extends BaseAdapter", "- data: List<Conversation>") `
        @("+ setData(List<Conversation>, String)", "+ getView(int, View, ViewGroup)")
    $hEntry = Draw-Class $g 620 520 440 "MeEntryListAdapter" "" `
    @("- extends BaseAdapter", "- data: List<EntryItem>") `
        @("+ setData(List<EntryItem>)", "+ getView(int, View, ViewGroup)")
    $hLogin = Draw-Class $g 620 740 440 "LoginUserStore" "" `
    @("- sp: SharedPreferences") `
        @("+ getUsername()", "+ getAvatarRes()")
    $hRepo = Draw-Class $g 60 740 440 "DataRepository" "interface" `
    @("（接口，UI 只认它）") `
        @("+ getConversations()", "+ getContact(String id)")
    $hModel = Draw-Class $g 340 960 440 "EntryItem / Conversation" "" `
    @("- iconRes / title / avatarRes", "- isStartGroup / isPinned") `
        @("+ setStartGroup(boolean)")

    # ---- MainActivity 组合两个 Fragment（实心菱形＝组合）
    $mx = 560; $my = 30 + $hMain
    Draw-DiamondFilled $g $mx ($my + 10)
    $g.DrawLine($pen, $mx, $my, $mx, $my + 2)
    $gy = $my + 34
    $g.DrawLine($pen, $mx, $my + 20, $mx, $gy)
    $g.DrawLine($pen, $mx, $gy, 280, $gy)
    $g.DrawLine($penArrow, 280, $gy, 280, 290)
    $g.DrawLine($pen, $mx, $gy, 840, $gy)
    $g.DrawLine($penArrow, 840, $gy, 840, 290)

    # ---- Fragment 组合各自的适配器
    Draw-DiamondFilled $g 280 (290 + $hChat + 10)
    $g.DrawLine($penArrow, 280, 290 + $hChat + 20, 280, 520)
    Draw-DiamondFilled $g 840 (290 + $hMe + 10)
    $g.DrawLine($penArrow, 840, 290 + $hMe + 20, 840, 520)

    # ---- 适配器 / MeFragment 的依赖（虚线 + 空心箭头）
    $g.DrawLine($penDash, 280, 520 + $hConv, 280, 740 - 11)
    Draw-OpenArrow $g 280 740 "down"
    $g.DrawLine($penDash, 840, 520 + $hEntry, 840, 740 - 11)
    Draw-OpenArrow $g 840 740 "down"
    $g.DrawLine($penDash, 280, 740 + $hRepo, 280, 930)
    $g.DrawLine($penDash, 280, 930, 340, 930)
    $g.DrawLine($penDash, 340, 930, 340, 960 - 11)
    Draw-OpenArrow $g 340 960 "down"

    # ---- 图例
    $fNote = New-Font 15
    Draw-Text $g "实线实心菱形＝组合（整体负责创建和销毁）；虚线空心箭头＝依赖（用到但不持有）；只画了与本次实验相关的成员" `
    ($W / 2) 1128 $fNote $gray

    $bmp.Save("$OUT\fig_class.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    Write-Output "fig_class.png ok"
}

# ---------------------------------------------------------------- getView 流程图
function Build-FlowChart {
    $W = 900; $H = 1120
    New-Canvas $W $H
    $bmp = $script:CUR_BMP; $g = $script:CUR_G
    $pen = New-Pen -width 2
    $penArrow = New-Pen -width 2
    $penArrow.CustomEndCap = (Arrow-Cap 6 9)
    $white = [System.Drawing.Brushes]::White
    $head = New-Object System.Drawing.SolidBrush($C_HEAD)
    $dec = New-Object System.Drawing.SolidBrush($C_DEC)
    $text = [System.Drawing.Brushes]::Black
    $red = New-Object System.Drawing.SolidBrush($C_RED)

    function FBox {
        param($g, [float]$cx, [float]$cy, [float]$w, [float]$h, [string]$t, [string]$kind = "proc", [float]$size = 17)
        $x = $cx - $w / 2; $y = $cy - $h / 2
        if ($kind -eq "term") {
            $p = RoundRect-Path $x $y $w $h ($h / 2)
            $g.FillPath($head, $p); $g.DrawPath((New-Pen -width 2), $p)
        }
        else {
            $p = RoundRect-Path $x $y $w $h 5
            $g.FillPath($white, $p); $g.DrawPath((New-Pen -width 2), $p)
        }
        $lines = $t -split "`n"
        $f = New-Font $size
        for ($i = 0; $i -lt $lines.Count; $i++) {
            $yy = $cy + ($i - ($lines.Count - 1) / 2) * ($size + 7)
            Draw-Text $g $lines[$i] $cx $yy $f $text
        }
    }
    function FDiamond {
        param($g, [float]$cx, [float]$cy, [float]$w, [float]$h, [string]$t, [float]$size = 19)
        $pts = @(
            (New-Object System.Drawing.PointF($cx, $cy - $h / 2)),
            (New-Object System.Drawing.PointF($cx + $w / 2, $cy)),
            (New-Object System.Drawing.PointF($cx, $cy + $h / 2)),
            (New-Object System.Drawing.PointF($cx - $w / 2, $cy))
        )
        $g.FillPolygon($dec, $pts); $g.DrawPolygon((New-Pen -width 2), $pts)
        Draw-Text $g $t $cx $cy (New-Font $size "bold") $text
    }
    function VArrow {
        param($g, [float]$x, [float]$y1, [float]$y2)
        $g.DrawLine($penArrow, $x, $y1, $x, $y2)
    }

    $CX = 380; $RX = 660
    FBox $g $CX 40 220 52 "开始" "term" 19
    VArrow $g $CX 66 105
    FBox $g $CX 138 440 56 "getView(position, convertView, parent)" "proc" 18
    VArrow $g $CX 166 200
    FDiamond $g $CX 258 340 116 "convertView == null ?"

    # 否：右侧复用分支
    $g.DrawLine($penArrow, $CX + 170, 258, $RX, 258)
    Draw-Text $g "否（有可复用的行）" ($CX + 180) 240 (New-Font 16) $red "left"
    FBox $g $RX 380 300 92 "holder = (ViewHolder)`nconvertView.getTag()" "proc" 17
    VArrow $g $RX 262 334
    VArrow $g $RX 426 600

    # 是：新建分支
    Draw-Text $g "是（第一次显示）" ($CX + 12) 340 (New-Font 16) $red "left"
    VArrow $g $CX 316 356
    FBox $g $CX 400 400 88 "inflate(item_conversation.xml)`n→ new ViewHolder(convertView)" "proc" 17
    VArrow $g $CX 444 492
    FBox $g $CX 528 400 60 "convertView.setTag(holder)" "proc" 17
    VArrow $g $CX 558 600

    # 汇合
    $g.DrawLine($pen, $CX, 600, $RX, 600)
    $g.FillEllipse([System.Drawing.Brushes]::Black, $RX - 6, 594, 12, 12)
    VArrow $g $RX 606 648
    FBox $g $RX 686 440 76 "取出 position 对应的 Conversation`npeer = repository.getContact(peerId)" "proc" 17
    VArrow $g $RX 724 772
    FBox $g $RX 812 440 88 "设置头像/昵称/最后一条消息/时间/未读角标`n置顶会话换浅灰底" "proc" 17
    VArrow $g $RX 856 908
    FBox $g $RX 948 300 64 "return convertView" "proc" 18
    VArrow $g $RX 980 1014
    FBox $g $RX 1048 200 52 "结束" "term" 19

    $bmp.Save("$OUT\fig_flow.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    Write-Output "fig_flow.png ok"
}

Build-ClassDiagram
Build-FlowChart
