package com.malinghan.macache.persistence;

import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Set;

@Component
public class AofWriter {

    static final String AOF_FILE = "macache.aof";

    private static final Set<String> WRITE_COMMANDS = Set.of(
        "SET", "DEL", "INCR", "DECR", "MSET",
        "LPUSH", "RPUSH", "LPOP", "RPOP",
        "SADD", "SREM", "SPOP",
        "HSET", "HDEL",
        "ZADD", "ZREM",
        "EXPIRE", "PEXPIRE", "PERSIST"
    );

    private BufferedWriter writer;

    public AofWriter() {
        try {
            writer = new BufferedWriter(new FileWriter(AOF_FILE, true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open AOF file", e);
        }
    }

    public boolean isWriteCommand(String cmdName) {
        return WRITE_COMMANDS.contains(cmdName.toUpperCase());
    }

    public synchronized void append(String respMessage) {
        try {
            writer.write(respMessage);
            writer.flush();
        } catch (IOException e) {
            System.err.println("AOF write failed: " + e.getMessage());
        }
    }

    public synchronized void rewrite(String content) {
        try {
            writer.close();
            writer = new BufferedWriter(new FileWriter(AOF_FILE, false));
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            System.err.println("AOF rewrite failed: " + e.getMessage());
        }
    }
}
