package com.malinghan.macache.persistence;

import com.malinghan.macache.command.Command;
import com.malinghan.macache.command.Commands;
import com.malinghan.macache.core.MaCache;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class AofLoader {

    public void load(MaCache cache) {
        File file = new File(AofWriter.AOF_FILE);
        if (!file.exists()) return;

        System.out.println("Loading AOF file...");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("*")) continue;
                int count = Integer.parseInt(line.substring(1));
                List<String> parts = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    reader.readLine(); // skip $len
                    parts.add(reader.readLine());
                }
                if (parts.isEmpty()) continue;
                String cmdName = parts.get(0);
                // skip TTL commands — absolute timestamps are meaningless after restart
                if (cmdName.equalsIgnoreCase("EXPIRE") || cmdName.equalsIgnoreCase("PEXPIRE")) continue;
                Command cmd = Commands.get(cmdName);
                if (cmd != null) {
                    cmd.exec(cache, parts.toArray(new String[0]));
                }
            }
        } catch (IOException e) {
            System.err.println("AOF load failed: " + e.getMessage());
        }
        System.out.println("AOF load complete.");
    }
}
