package cn.gbtmedia.jtt808.server.media.server.netty;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpDecoder;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpEncoder;
import cn.hutool.extra.spring.SpringUtil;
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
@Order(6)
@Slf4j
@Component("jtt808MediaServer")
public class NettyMediaServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int playPort = ServerConfig.getInstance().getJtt808().getMediaSinglePlayPort();
        log.info("jtt808MediaServer start playPort {} ",playPort);
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
                            pipeline.addLast(new IdleStateHandler(180, 0, 0));
                            pipeline.addLast(new Jtt1078RtpDecoder.Tcp());
                            pipeline.addLast(new Jtt1078RtpEncoder.Tcp());
                            pipeline.addLast(new NettyMediaHandler.Tcp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(playPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",playPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }catch (Exception ex) {
                log.error("jtt808MediaServer start error port {}",playPort,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808MediaServer-play-tcp").start();
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
                            pipeline.addLast(new Jtt1078RtpDecoder.Udp());
                            pipeline.addLast(new Jtt1078RtpEncoder.Udp());
                            pipeline.addLast(new NettyMediaHandler.Udp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(playPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",playPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("jtt808MediaServer Start Error Port {}",playPort,ex);
                int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808MediaServer-play-udp").start();

        // 回放端口

        int playbackPort = ServerConfig.getInstance().getJtt808().getMediaSinglePlaybackPort();
        log.info("jtt808MediaServer start playbackPort {} ",playbackPort);
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
                            pipeline.addLast(new IdleStateHandler(180, 0, 0));
                            pipeline.addLast(new Jtt1078RtpDecoder.Tcp());
                            pipeline.addLast(new Jtt1078RtpEncoder.Tcp());
                            pipeline.addLast(new NettyMediaHandler.Tcp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(playbackPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",playbackPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }catch (Exception ex) {
                log.error("jtt808MediaServer start error port {}",playbackPort,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808MediaServer-playbackPort-tcp").start();
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
                            pipeline.addLast(new Jtt1078RtpDecoder.Udp());
                            pipeline.addLast(new Jtt1078RtpEncoder.Udp());
                            pipeline.addLast(new NettyMediaHandler.Udp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(playbackPort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",playbackPort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("jtt808MediaServer start error port {}",playbackPort,ex);
                int exitCode = SpringApplication.exit(cn.hutool.extra.spring.SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808MediaServer-playbackPort-udp").start();
    }

}
