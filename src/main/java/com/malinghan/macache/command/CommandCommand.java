package com.malinghan.macache.command;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.reply.Reply;

import java.util.ArrayList;
import java.util.List;

public class CommandCommand implements Command {
    @Override
    public String name() { return "COMMAND"; }

    @Override
    public Reply<?> exec(MaCache cache, String[] args) {
        // COMMAND DOCS / COMMAND INFO / COMMAND COUNT etc. — return empty array
        if (args.length > 1) {
            return Reply.array(new ArrayList<>());
        }
        List<String> cmds = new ArrayList<>(Commands.map.keySet());
        return Reply.array(cmds);
    }
}
