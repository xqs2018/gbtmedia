package cn.gbtmedia.gbt28181.server.media.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.MediaManger;
import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import lombok.extern.slf4j.Slf4j;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @author xqs
 */
@Slf4j
public class MultiplePortUdpServer extends MediaServer {

    private DatagramSocket datagramSocket;

    @Override
    public void doStart() throws Exception{
        Integer freePort = MediaManger.getInstance().getFreePort();
        datagramSocket = new DatagramSocket(freePort);
        // 10s没有收到任何消息，停止服务器
        datagramSocket.setSoTimeout(1000*10);
        mediaIp = ServerConfig.getInstance().getAccessIp();
        mediaPort = freePort;
        log.info("UdpServer start port {} ",freePort);
        Runnable task = ()->{
            try {
                byte[] buffer = new byte[1024*1024];
                while (!isStop){
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, data.length);
                    RtpMessage message = RtpMessage.toRtpMessage(data);
                    receiveRtpMessage(message);
                }
            }catch (Exception ex){
                if("Socket closed".equals(ex.getMessage())){
                    log.info("Socket closed");
                }else {
                    log.error("UdpServer ex",ex);
                }
                stop();
            }
        };
        MediaManger.getInstance().run(task);
    }

    @Override
    public void doStop() {
        if(datagramSocket != null && !datagramSocket.isClosed()){
            log.info("UdpServer datagramSocket close");
            datagramSocket.close();
        }
    }

    @Override
    public void doSendRtpMessage(RtpMessage message) throws Exception{

    }

}
