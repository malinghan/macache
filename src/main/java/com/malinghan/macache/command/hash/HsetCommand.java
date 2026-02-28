package com.malinghan.macache.command.hash;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class HsetCommand implements Command {
    @Override public String name() { return "HSET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // args = ["HSET", key, f1, v1, f2, v2, ...]
        String key = args[1];
        int pairs = (args.length - 2) / 2;
        String[] fields = new String[pairs];
        String[] values = new String[pairs];
        for (int i = 0; i < pairs; i++) {
            fields[i] = args[2 + i * 2];
            values[i] = args[3 + i * 2];
        }
        return Reply.integer(cache.hset(key, fields, values));
    }
}
