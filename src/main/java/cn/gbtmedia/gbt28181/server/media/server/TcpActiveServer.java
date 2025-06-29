package cn.gbtmedia.gbt28181.server.media.server;

import cn.gbtmedia.gbt28181.server.media.MediaManger;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author xqs
 */
@Slf4j
public class TcpActiveServer extends MediaServer {

    private Socket client;

    @Override
    public void doStart() throws Exception{
        if(mediaIp  == null || mediaPort == 0){
            return;
        }
        client = new Socket(mediaIp, mediaPort);
        log.info("TcpActiveServer start client {} ",client.getInetAddress());
        Runnable task = ()->{
            try {
                try (InputStream inputStream = client.getInputStream()) {
                    // 读取RTP消息 RFC4571标准格式: 长度(2字节) + (RTP头 + 数据)
                    while (!isStop){
                        int length = inputStream.read() << 8 | inputStream.read();
                        if (length == -1) {
                            break;
                        }
                        byte[] data = new byte[length];
                        int dataRead = 0;
                        while (dataRead < length) {
                            int read = inputStream.read(data, dataRead, length - dataRead);
                            if (read == -1) {
                                break;
                            }
                            dataRead += read;
                        }
                        RtpMessage message = RtpMessage.toRtpMessage(data);
                        receiveRtpMessage(message);
                    }
                }
                log.info("TcpActiveServer inputStream close");
                stop();
            }catch (Exception ex){
                if("Socket closed".equals(ex.getMessage())){
                    log.info("Socket closed");
                }else {
                    log.error("TcpActiveServer ex",ex);
                }
                stop();
            }
        };
        MediaManger.getInstance().run(task);
    }

    @Override
    public void doStop() {
        try {
            if(client != null && !client.isClosed()){
                log.info("TcpActiveServer client close");
                client.close();
            }
        } catch (IOException e) {
            log.error("close ex",e);
        }
        try {
            if(outputStream != null){
                log.info("TcpActiveServer outputStream close");
                outputStream.close();
            }
        } catch (IOException e) {
            log.error("close ex",e);
        }
    }

    private OutputStream outputStream;

    @Override
    public void doSendRtpMessage(RtpMessage message) throws Exception {
        if(client == null){
            return;
        }
        if(outputStream == null){
            outputStream = client.getOutputStream();
        }
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = message.getMessageBytes();
        byteBuf.writeShort(bytes.length);
        byteBuf.writeBytes(bytes);
        outputStream.write(ByteBufUtil.getBytes(byteBuf));
        outputStream.flush();
    }
}
