package com.example.wechat.db;

/**
 * 后台初始化的结果。
 *
 * 成功 / 失败都显式带在对象里，不用「返回空数据」这种含糊的方式表达失败。
 * 启动页据此决定进主界面还是弹错误+重试，见 ui/launch/LauncherActivity。
 */
public final class InitResult {

    private final Throwable error;

    private InitResult(Throwable error) {
        this.error = error;
    }

    public static InitResult success() {
        return new InitResult(null);
    }

    public static InitResult failure(Throwable error) {
        return new InitResult(error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Throwable getError() {
        return error;
    }

    /** 给用户看的一句话描述：异常类名 + message */
    public String describe() {
        if (error == null) {
            return "";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : "：" + message);
    }
}
