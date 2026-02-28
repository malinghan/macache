package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public interface Command {
    String name();
    Reply<?> exec(MaCache cache, String[] args);

    // args is now a clean array: [cmd, key, value, ...]
    default String getKey(String[] args) {
        return args.length > 1 ? args[1] : null;
    }

    default String getVal(String[] args) {
        return args.length > 2 ? args[2] : null;
    }

    // Returns parameters after the command name
    default String[] getParams(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] params = new String[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);
        return params;
    }
}
