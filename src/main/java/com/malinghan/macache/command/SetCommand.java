package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class SetCommand implements Command {
    @Override
    public String name() { return "SET"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        cache.set(getKey(args), getVal(args));
        return Reply.ok();
    }
}
