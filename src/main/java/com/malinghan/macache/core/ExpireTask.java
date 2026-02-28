package com.malinghan.macache.core;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ExpireTask {

    @Autowired
    private MaCache cache;

    @PostConstruct
    public void start() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(cache::cleanExpired, 100, 100, TimeUnit.MILLISECONDS);
    }
}
