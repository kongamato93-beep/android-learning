# -*- coding: utf-8 -*-
"""生成默认头像矢量图：圆角色块 + 白色人物剪影。

用途：在 mato 自己导入真实头像图片之前，先用这些占位图让列表有区分度。
生成的是 vector drawable，任意密度都不会糊。

运行：python tools/gen_avatars.py
"""
import os

OUT_DIR = os.path.join("app", "src", "main", "res", "drawable")

# 占位头像配色（微信默认头像偏灰，这里给一点柔和色系用于区分）
COLORS = {
    "avatar_default": "#FF9AA0A6",
    "avatar_1": "#FF6E8BC4",
    "avatar_2": "#FF7FB08A",
    "avatar_3": "#FFC9A46B",
    "avatar_4": "#FF9A8FC4",
    "avatar_5": "#FFC97F94",
    "avatar_6": "#FF6FA8A6",
}

ROUNDED_RECT = ("M2.5,0 L21.5,0 A2.5,2.5 0 0 1 24,2.5 L24,21.5 "
                "A2.5,2.5 0 0 1 21.5,24 L2.5,24 A2.5,2.5 0 0 1 0,21.5 "
                "L0,2.5 A2.5,2.5 0 0 1 2.5,0 Z")

HEAD = "M16.5,9 A4.5,4.5 0 1,1 7.5,9 A4.5,4.5 0 1,1 16.5,9 Z"
BODY = "M12,14.8 C7.8,14.8 4.4,17.4 4.4,20.4 L4.4,22 L19.6,22 L19.6,20.4 C19.6,17.4 16.2,14.8 12,14.8 Z"

TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="{color}"
        android:pathData="{rect}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{head}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{body}" />
</vector>
"""


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, color in COLORS.items():
        content = TEMPLATE.format(color=color, rect=ROUNDED_RECT, head=HEAD, body=BODY)
        path = os.path.join(OUT_DIR, name + ".xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print("wrote", path)


if __name__ == "__main__":
    main()
