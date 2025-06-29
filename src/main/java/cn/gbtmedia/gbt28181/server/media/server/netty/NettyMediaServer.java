package cn.gbtmedia.gbt28181.server.media.server.netty;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.codec.RtpDecoder;
import cn.gbtmedia.gbt28181.server.media.codec.RtpEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * @author xqs
 */
@Order(3)
@Slf4j
@Component("gbt28181MediaServer")
public class NettyMediaServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int mediaPort = ServerConfig.getInstance().getGbt28181().getMediaSinglePort();
        log.info("gbt28181MediaServer start port {} ",mediaPort);
        new Thread(()->{
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new IdleStateHandler(180, 0, 0));
                            // RFC4571标准格式: 长度(2字节) + RTP头+数据
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(1024*1024,0,2,0,2));
                            pipeline.addLast(new RtpDecoder.Tcp());
                            pipeline.addLast(new RtpEncoder.Tcp());
                            pipeline.addLast(new NettyMediaHandler.Tcp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(mediaPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",mediaPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("gbt28181MediaServer start error port {}",mediaPort,ex);
                int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"gbt28181MediaServer-tcp").start();
        new Thread(()->{
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_RCVBUF,1024*1024)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new IdleStateHandler(180, 0, 0));
                            pipeline.addLast(new RtpDecoder.Udp());
                            pipeline.addLast(new RtpEncoder.Udp());
                            pipeline.addLast(new NettyMediaHandler.Udp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(mediaPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",mediaPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("gbt28181MediaServer start error port {}",mediaPort,ex);
                int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"gbt28181MediaServer-udp").start();
    }

}
