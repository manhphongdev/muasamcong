package com.muasamcong.enums;

public enum RecordStatus {
    ACTIVE(1),
    INACTIVE(0),
    DELETED(-1);

    private final int code;

    RecordStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RecordStatus fromCode(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (RecordStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown record status code: " + code);
    }
}
