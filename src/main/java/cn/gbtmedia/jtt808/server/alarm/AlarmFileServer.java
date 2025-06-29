package cn.gbtmedia.jtt808.server.alarm;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileDecoder;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileEncoder;
import cn.gbtmedia.jtt808.server.alarm.codec.LengthFieldBasedFrameDecoder;
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
@Order(5)
@Slf4j
@Component
public class AlarmFileServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int alarmFilePort = ServerConfig.getInstance().getJtt808().getAlarmFilePort();
        log.info("jtt808AlarmFileServer start port {}", alarmFilePort);
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
                            pipeline.addLast(new LengthFieldBasedFrameDecoder());
                            pipeline.addLast(new AlarmFileDecoder.Tcp());
                            pipeline.addLast(new AlarmFileEncoder.Tcp());
                            pipeline.addLast(new AlarmFileHandler.Tcp());
                        }
                    });
            try {
                ChannelFuture cf = bootstrap.bind(alarmFilePort).sync();
                cf.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}", alarmFilePort,future.cause());
                    }
                });
                cf.channel().closeFuture().sync();
            }catch (Exception ex) {
                log.error("jtt808AlarmFileServer start error port {}", alarmFilePort,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808AlarmFileServer-tcp").start();
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
                            pipeline.addLast(new AlarmFileDecoder.Udp());
                            pipeline.addLast(new AlarmFileEncoder.Udp());
                            pipeline.addLast(new AlarmFileHandler.Udp());
                        }
                    });
            try {
                ChannelFuture cf1 = bootstrap.bind(alarmFilePort).sync();
                cf1.addListener(future -> {
                    if(!future.isSuccess()){
                        log.error("start error port {}", alarmFilePort,future.cause());
                    }
                });
                cf1.channel().closeFuture().sync();
            }  catch (Exception ex){
                log.error("jtt808AlarmFileServer start error port {}", alarmFilePort,ex);
                int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 0);
                System.exit(exitCode);
            }
        },"jtt808AlarmFileServer-udp").start();
    }
}
