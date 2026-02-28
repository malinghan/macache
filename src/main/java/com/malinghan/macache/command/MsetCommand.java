package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class MsetCommand implements Command {
    @Override
    public String name() { return "MSET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // params are interleaved: k1, v1, k2, v2, ...
        String[] params = getParams(args);
        String[] keys = new String[params.length / 2];
        String[] values = new String[params.length / 2];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = params[i * 2];
            values[i] = params[i * 2 + 1];
        }
        cache.mset(keys, values);
        return Reply.ok();
    }
}
