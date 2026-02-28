package com.malinghan.macache.core;

public class WrongTypeException extends RuntimeException {
    public WrongTypeException() {
        super("WRONGTYPE Operation against a key holding the wrong kind of value");
    }
}
