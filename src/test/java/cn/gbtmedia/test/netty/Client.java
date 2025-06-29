package cn.gbtmedia.test.netty;

import cn.hutool.core.util.RandomUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xqs
 */
@Slf4j
public class Client {

    public static void main(String[] args) throws InterruptedException {
        for(int i=0;i<100;i++){
            new Thread(()->{
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    ChannelPipeline p = ch.pipeline();
                                    p.addLast(new StringDecoder());
                                    p.addLast(new StringEncoder());
                                    p.addLast(new ClientHandler());
                                }
                            });

                    // Start the client.
                    ChannelFuture f = b.connect("127.0.0.1", 32100).sync();

                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                }catch (Exception ex){
                    log.error("ex",ex);
                }
                finally {
                    group.shutdownGracefully();
                }
            },"c-"+i).start();
        }
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            log.debug("channelRead0 {}",msg);
            Thread.sleep(RandomUtil.randomInt(1,2));
            ctx.writeAndFlush(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
