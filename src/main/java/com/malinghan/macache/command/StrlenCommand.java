package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class StrlenCommand implements Command {
    @Override
    public String name() { return "STRLEN"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.strlen(getKey(args)));
    }
}
