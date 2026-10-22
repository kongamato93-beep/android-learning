package com.example.wechat.util;

/**
 * 取汉字拼音首字母（用于通讯录的 A-Z 字母索引）。
 *
 * 原理：GB2312 编码把同拼音的汉字排在一起，所以只要拿字符的 GBK 编码值
 * 去查一张“拼音分界表”，就能得到它属于哪个字母段。这是一张广泛使用的静态表，
 * 比引入整包拼音库轻得多。
 *
 * 局限性：多音字只能取常见读音，对课程作业足够；真要精准可以换 pinyin4j。
 */
public final class PinyinIndex {

    private static final char[] LETTERS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'W', 'X', 'Y', 'Z'
    };

    private static final int[] BOUNDARIES = {
            -20319, -20283, -19775, -19422, -18710, -18526, -18239, -17922,
            -17732, -17521, -17056, -16474, -16212, -15635, -15289, -14710,
            -14204, -13657, -13318, -12607, -12122, -11440, -11004
    };

    private PinyinIndex() {
    }

    public static String firstLetter(char ch) {
        try {
            byte[] bytes = String.valueOf(ch).getBytes("GBK");
            if (bytes.length < 2) {
                return String.valueOf(ch).toUpperCase();
            }
            int code = (bytes[0] << 8) | (bytes[1] & 0xFF);
            for (int i = BOUNDARIES.length - 1; i >= 0; i--) {
                if (code >= BOUNDARIES[i]) {
                    return String.valueOf(LETTERS[i]);
                }
            }
        } catch (Exception e) {
            // 编码不支持时退化为井号分组
        }
        return "#";
    }
}
