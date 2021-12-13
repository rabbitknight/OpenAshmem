package net.rabbitknight.open.ashmem;

public class ErrorCode {
    private ErrorCode() {

    }

    public static final int SUCCESS = 0;

    public static final int ERROR = -1;

    /**
     * 客户端未设置客户端
     */
    public static final int ERROR_CLIENT_CLIENT_LOSS = -10001;
    /**
     * 客户端尺寸不匹配
     */
    public static final int ERROR_CLIENT_SIZE_NOT_MATCH = -10002;
    /**
     * 客户端未设置回调
     */
    public static final int ERROR_CLIENT_CALLBACK_LOSS = -10003;

    /**
     * 服务端 未找到回调
     */
    public static final int ERROR_SERVER_CALLBACK_LOSS = -20001;


    /**
     * Sender服务未连接
     */
    public static final int ERROR_SENDER_SERVICE_NOT_CONNECT = -30001;
    /**
     * Sender API丢失
     */
    public static final int ERROR_SENDER_SERVICE_API_LOSS = -30002;
    /**
     * Sender 文件丢失
     */
    public static final int ERROR_SENDER_FILE_LOSS = -30003;
    /**
     * Sender 写入文件失败
     */
    public static final int ERROR_SENDER_FILE_WRITE = -30004;
    /**
     * Sender 回调失败
     */
    public static final int ERROR_SENDER_FILE_CALL = -30005;
}
