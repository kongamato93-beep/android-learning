package com.example.wechat.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 通讯录右侧的 A-Z 字母索引条。
 *
 * 纯自定义 View：onDraw 画字母，onTouchEvent 按手指位置算出第几个字母回调出去。
 * 按下时整条背景变灰（跟微信一致），松手恢复。
 */
public class SideIndexView extends View {

    public interface OnLetterChangeListener {
        void onLetterChanged(String letter);
    }

    private static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float letterHeight;
    private boolean pressed;
    private OnLetterChangeListener listener;

    public SideIndexView(Context context) {
        super(context);
    }

    public SideIndexView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SideIndexView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnLetterChangeListener(OnLetterChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        letterHeight = (float) h / LETTERS.length;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pressed) {
            canvas.drawColor(Color.parseColor("#1A000000"));
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(getResources().getDisplayMetrics().density * 11);
        paint.setColor(Color.parseColor(pressed ? "#07C160" : "#7F7F7F"));

        Paint.FontMetrics fm = paint.getFontMetrics();
        float x = getWidth() / 2f;
        for (int i = 0; i < LETTERS.length; i++) {
            float centerY = letterHeight * i + letterHeight / 2f;
            float baseline = centerY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(LETTERS[i], x, baseline, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                notifyLetter(event.getY());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                notifyLetter(event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void notifyLetter(float y) {
        if (listener == null || letterHeight <= 0) {
            return;
        }
        int index = (int) (y / letterHeight);
        if (index < 0) {
            index = 0;
        }
        if (index >= LETTERS.length) {
            index = LETTERS.length - 1;
        }
        listener.onLetterChanged(LETTERS[index]);
    }
}
