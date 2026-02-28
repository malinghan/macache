package com.malinghan.macache.reply;

import java.util.List;

public class Reply<T> {
    private final ReplyType type;
    private final T data;

    private Reply(ReplyType type, T data) {
        this.type = type;
        this.data = data;
    }

    public ReplyType getType() { return type; }
    public T getData() { return data; }

    public static Reply<String> ok() {
        return new Reply<>(ReplyType.SIMPLE_STRING, "OK");
    }

    public static Reply<String> error(String msg) {
        return new Reply<>(ReplyType.ERROR, msg);
    }

    public static Reply<Long> integer(long val) {
        return new Reply<>(ReplyType.INT, val);
    }

    public static Reply<String> bulkString(String val) {
        return new Reply<>(ReplyType.BULK_STRING, val);
    }

    public static Reply<List<String>> array(List<String> val) {
        return new Reply<>(ReplyType.ARRAY, val);
    }

    public static Reply<String> nil() {
        return new Reply<>(ReplyType.BULK_STRING, null);
    }
}
