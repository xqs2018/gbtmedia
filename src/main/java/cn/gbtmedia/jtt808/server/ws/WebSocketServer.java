package cn.gbtmedia.jtt808.server.ws;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.extra.spring.SpringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author xqs
 */
@Order(7)
@Slf4j
@Component("jtt808WsServer")
public class WebSocketServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int wsPort = ServerConfig.getInstance().getJtt808().getWsPort();
        log.info("jtt808WsServer start port {} ",wsPort);
        new Thread(()->{
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.TRACE))
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LoggingHandler(LogLevel.TRACE))
                                    .addLast(new HttpServerCodec())
                                    .addLast(new ChunkedWriteHandler())
                                    .addLast(new HttpObjectAggregator(10240))
                                    .addLast(new WebSocketServerCompressionHandler())
                                    .addLast(new WebSocketFrameAggregator(10*1024*1024))
                                    .addLast(new WebSocketServerProtocolHandler("/chat", null, true, 10485760,false,true))
                                    .addLast(new WebSocketHandler());
                        }
                    });
            try {
                ChannelFuture cf = bootstrap.bind(wsPort).sync();
                cf.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",wsPort,future.cause());
                    }
                });
                cf.channel().closeFuture().sync();
            } catch (Exception ex) {
                log.error("jtt808WsServer start error port {}",wsPort,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808WsServer").start();
    }
}
