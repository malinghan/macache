package com.malinghan.macache.plugin;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    private final List<MaPlugin> plugins;

    public MaApplicationListener(List<MaPlugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (MaPlugin plugin : plugins) {
            plugin.init();
            plugin.startup();
        }
    }
}
