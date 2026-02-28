package com.malinghan.macache.server;

import com.malinghan.macache.core.MaCache;
import com.malinghan.macache.persistence.AofLoader;
import com.malinghan.macache.plugin.MaPlugin;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MaCacheServer implements MaPlugin {

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    @Autowired
    private MaCacheHandler handler;

    @Autowired
    private MaCache cache;

    @Autowired
    private AofLoader aofLoader;

    @Override
    public void init() {
        aofLoader.load(cache);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(16);
    }

    @Override
    public void startup() {
        try {
            new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MaCacheDecoder());
                        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                        pipeline.addLast(handler);
                    }
                })
                .bind(6379)
                .sync();
            System.out.println("MaCache started on port 6379");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("MaCache server startup failed", e);
        }
    }
}
