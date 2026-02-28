package com.malinghan.macache.command.set;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

public class SismemberCommand implements Command {
    @Override public String name() { return "SISMEMBER"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        return Reply.integer(cache.sismember(args[1], args[2]));
    }
}
