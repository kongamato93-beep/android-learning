# 要求像更像微信界面的方向改进

## 1.把顶部的“微信（）”“通讯录”“发现”“我”从顶部的左边改到中间

思路：在标题左边和右边各放一个 权重相同（weight 都是 1）的空占位 。左右占位一样宽，标题自然被挤到正中；右边的占位容器再用`gravity="end"` 把加号钉在最右。
改动：activity_main.xml

### 第一步：在`<TextView android:id="@+id/tv_title">` 之前 加一个左占位
```xml
<View
    android:layout_width="0dp"
    android:layout_height="1dp"
    android:layout_weight="1" />
```

### 第二步：把标题的宽度改成内容自适应
把固定宽度
```xml
android:layout_width="0dp"
android:layout_weight="1"
android:paddingStart="@dimen/space_16"
android:paddingEnd="@dimen/space_16"
```
改成
```xml
android:layout_width="wrap_content"
android:gravity="center"
```

### 第三步：把`<ImageView android:id="@+id/iv_action">` 包进一个右侧占位容器
```xml
<LinearLayout
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:gravity="end|center_vertical">

    <ImageView
        android:id="@+id/iv_action"
        ... 里面内容保持原样不动 ...
        />

</LinearLayout>
```

## 2.把置顶的会话背景变成微信那样的灰色以作区分
已经实现，但是颜色不够深

### 颜色定义
在color.xml
```xml
 <!-- 置顶会话的行背景：比普通会话略灰，用来区分是否置顶 -->
    <color name="conversation_pinned_bg">#FFF0F0F0</color>
```
改色号为FFE4E4E4

### 适配器里根据`isPinned()` 切换背景
在适配器ConversationListAdapter
```java
// 置顶会话用浅灰底区分；根布局保留点击效果，背景设在内容区这一层
        holder.llContent.setBackgroundColor(ContextCompat.getColor(
                convertView.getContext(),
                conversation.isPinned() ? R.color.conversation_pinned_bg : R.color.card_bg));
```

### 判断依据： Conversation.java
```java
//置顶功能的判断依据
    public boolean isPinned() {
        return pinned;
    }
```
