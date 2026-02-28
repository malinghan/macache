package com.malinghan.macache.command;

import com.malinghan.macache.command.list.*;
import com.malinghan.macache.command.set.*;
import com.malinghan.macache.command.hash.*;
import com.malinghan.macache.command.zset.*;

import java.util.LinkedHashMap;

public class Commands {

    public static final LinkedHashMap<String, Command> map = new LinkedHashMap<>();

    static {
        // v1.0
        register(new PingCommand());
        register(new HelloCommand());
        register(new InfoCommand());
        register(new CommandCommand());
        register(new SetCommand());
        register(new GetCommand());
        register(new StrlenCommand());
        register(new DelCommand());
        register(new ExistsCommand());
        register(new IncrCommand());
        register(new DecrCommand());
        register(new MsetCommand());
        register(new MgetCommand());

        // v2.0 List
        register(new LpushCommand());
        register(new RpushCommand());
        register(new LpopCommand());
        register(new RpopCommand());
        register(new LlenCommand());
        register(new LindexCommand());
        register(new LrangeCommand());

        // v2.0 Set
        register(new SaddCommand());
        register(new SmembersCommand());
        register(new SremCommand());
        register(new ScardCommand());
        register(new SpopCommand());
        register(new SismemberCommand());

        // v2.0 Hash
        register(new HsetCommand());
        register(new HgetCommand());
        register(new HgetallCommand());
        register(new HlenCommand());
        register(new HdelCommand());
        register(new HexistsCommand());
        register(new HmgetCommand());

        // v2.0 ZSet
        register(new ZaddCommand());
        register(new ZcardCommand());
        register(new ZscoreCommand());
        register(new ZremCommand());
        register(new ZrankCommand());
        register(new ZcountCommand());

        // v3.0 TTL
        register(new ExpireCommand());
        register(new PexpireCommand());
        register(new TtlCommand());
        register(new PttlCommand());
        register(new PersistCommand());
    }

    private static void register(Command cmd) {
        map.put(cmd.name().toUpperCase(), cmd);
    }

    public static Command get(String name) {
        return map.get(name.toUpperCase());
    }
}
