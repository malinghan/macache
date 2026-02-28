package com.malinghan.macache.core;

public class CacheEntry<T> {
    private T value;
    private long expireAt = -1; // -1 = never expires, otherwise millis timestamp

    public CacheEntry(T value) {
        this.value = value;
    }

    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }

    public long getExpireAt() { return expireAt; }
    public void setExpireAt(long expireAt) { this.expireAt = expireAt; }

    public boolean isExpired() {
        return expireAt > 0 && System.currentTimeMillis() > expireAt;
    }
}
