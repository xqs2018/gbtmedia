package cn.gbtmedia.jtt808.server.cmd;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Decoder;
import cn.gbtmedia.jtt808.server.cmd.codec.Jtt808Encoder;
import cn.hutool.extra.spring.SpringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
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
@Order(4)
@Slf4j
@Component
public class Jtt808Server implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int jtt808Port = ServerConfig.getInstance().getJtt808().getCmdPort();
        log.info("jtt808Server start port {} ",jtt808Port);
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
                            pipeline.addLast(new DelimiterBasedFrameDecoder(1024 * 2, Unpooled.wrappedBuffer(new byte[]{0x7e})));
                            pipeline.addLast(new Jtt808Decoder.Tcp());
                            pipeline.addLast(new Jtt808Encoder.Tcp());
                            pipeline.addLast(new Jtt808Handler.Tcp());
                        }
                    });
            try {
                ChannelFuture cf = bootstrap.bind(jtt808Port).sync();
                cf.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",jtt808Port,future.cause());
                    }
                });
                cf.channel().closeFuture().sync();
            }catch (Exception ex) {
                log.error("jtt808Server start error port {}",jtt808Port,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808Server-tcp").start();
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
                            pipeline.addLast(new Jtt808Decoder.Udp());
                            pipeline.addLast(new Jtt808Encoder.Udp());
                            pipeline.addLast(new Jtt808Handler.Udp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(jtt808Port).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}",jtt808Port,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("jtt808Server start error port {}",jtt808Port,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808Server-udp").start();
    }
}
