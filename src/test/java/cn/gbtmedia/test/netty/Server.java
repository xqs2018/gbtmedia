package cn.gbtmedia.test.netty;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author xqs
 */
@Slf4j
public class Server {

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            p.addLast(new ServerHandler());
                        }
                    });

            ChannelFuture f = b.bind(32100).sync();
            System.out.println("Server started and listening on " + f.channel().localAddress());
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    private static class ServerHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            JSONObject data = JSONUtil.parseObj(msg);
            String id = data.getStr("id");
            log.debug("time {} 收到消息: {} ",System.currentTimeMillis(),data);
            Consumer<JSONObject> consumer = consumerMap.get(id);
            if(consumer == null){
                log.error("time {} 回调 null {}" ,System.currentTimeMillis(),data);
                System.exit(0);
            }
            consumer.accept(data);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 当客户端连接上时，发送字符串消息
            CompletableFuture.runAsync(()->{
                while (true){
                    try {
                        Thread.sleep(100);
                        JSONObject data = new JSONObject();
                        data.set("id", IdUtil.fastSimpleUUID());
                        data.set("time",System.currentTimeMillis());
                        ctx.channel().writeAndFlush(data.toString()).addListener((ChannelFutureListener) r -> {
                            if (!r.isSuccess()) {
                                log.error("send error", r.cause());
                            }
                            log.debug("time {} 发送成功: {}" ,System.currentTimeMillis(), data);
                            consumerMap.put(data.getStr("id"), json -> {
                                log.info("time {} 回调成功: {} mapSize {} " ,System.currentTimeMillis(), json,consumerMap.size());
                            });
                            log.debug("time {} 注册回调: {}" ,System.currentTimeMillis(), data);
                        });
                    }catch (Exception ex){
                        log.error("send ex",ex);
                    }
                }
            });
        }

        Map<String, Consumer<JSONObject>> consumerMap = new ConcurrentHashMap<>();
    }
}
