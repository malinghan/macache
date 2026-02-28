package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class IncrCommand implements Command {
    @Override
    public String name() { return "INCR"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        try {
            return Reply.integer(cache.incr(getKey(args)));
        } catch (NumberFormatException e) {
            return Reply.error("ERR value is not an integer or out of range");
        }
    }
}
