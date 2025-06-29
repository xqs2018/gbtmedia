package cn.gbtmedia.gbt28181.server.flv;

import cn.gbtmedia.common.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
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
@Order(9)
@Slf4j
@Component("gbt28181FlvServer")
public class FlvServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int flvPort = ServerConfig.getInstance().getGbt28181().getFlvPort();
        log.info("gbt28181FlvServer start port {} ",flvPort);
        new Thread(()->{
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpResponseEncoder());
                            pipeline.addLast(new HttpRequestDecoder());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();
                            pipeline.addLast(new CorsHandler(corsConfig));
                            pipeline.addLast(new FlvHandler());
                        }
                    });
            try {
                ChannelFuture cf = bootstrap.bind(flvPort).sync();
                cf.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",flvPort,future.cause());
                    }
                });
                cf.channel().closeFuture().sync();
            } catch (Exception ex){
                log.error("gbt28181FlvServer start error port {}",flvPort,ex);
                int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"gbt28181MediaFlv").start();
    }
}
