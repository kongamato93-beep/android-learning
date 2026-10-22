package com.example.wechat.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * AI 调用失败的统一描述。
 *
 * 为什么要区分 code：
 * 「网络不通」「密钥没配」「接口报错」「回复是空的」对用户的意义完全不同，
 * 混成一句「发送失败」会让人不知道该检查什么。
 * 具体文案由界面层按 code 决定，这里只负责把事实带上去。
 */
public final class AiError {

    public enum Code {
        /** 没填 API Key，请求根本没发出去 */
        NOT_CONFIGURED,
        /** 连不上 / 读写超时 */
        TIMEOUT,
        /** 网络不可用、DNS 失败、连接被拒等 */
        NETWORK,
        /** 服务端返回了非 2xx，httpStatus 里有状态码 */
        HTTP,
        /** 返回了 200 但结构解析不出来 */
        PARSE,
        /** 解析成功但内容是空的 */
        EMPTY,
        /** 拿到了回复，但写进本地消息列表时失败（和「请求失败」是两回事） */
        SAVE_FAILED,
        /** 离开页面时主动取消，不算错误，不给用户提示 */
        CANCELLED,
        /** 兜底 */
        UNKNOWN
    }

    private final Code code;
    private final String message;
    private final int httpStatus;

    public AiError(@NonNull Code code, @NonNull String message) {
        this(code, message, 0);
    }

    public AiError(@NonNull Code code, @NonNull String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @NonNull
    public Code getCode() {
        return code;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    /** 只有 Code.HTTP 时才有意义，其余为 0 */
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    @NonNull
    public String toString() {
        return "AiError{" + code + ", http=" + httpStatus + ", " + message + "}";
    }

    /** 日志用。刻意不带任何可能含密钥的内容。 */
    @NonNull
    public static String describe(@Nullable AiError error) {
        return error == null ? "null" : error.toString();
    }
}
