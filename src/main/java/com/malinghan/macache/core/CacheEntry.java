package com.malinghan.macache.core;

public class CacheEntry<T> {
    private T value;

    public CacheEntry(T value) {
        this.value = value;
    }

    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}
