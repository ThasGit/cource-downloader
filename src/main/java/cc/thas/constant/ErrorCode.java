package cc.thas.constant;

public enum ErrorCode {
    /**
     * Internal error
     */
    INTERNAL_ERROR(500, "Internal error.");

    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    ErrorCode(int code, String messageTpl) {
        this.code = code;
        this.message = messageTpl;
    }

    public String format(String... args) {
        return String.format(message, args);
    }
}
