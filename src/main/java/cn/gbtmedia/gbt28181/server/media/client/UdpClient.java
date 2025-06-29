package cn.gbtmedia.gbt28181.server.media.client;

import cn.gbtmedia.gbt28181.server.media.codec.RtpMessage;
import lombok.extern.slf4j.Slf4j;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author xqs
 */
@Slf4j
public class UdpClient extends MediaClient{


    @Override
    public void doStart() throws Exception{

    }

    @Override
    public void doStop() {

    }

    @Override
    public void doSendRtpMessage(RtpMessage rtpMessage) throws Exception{
        try(DatagramSocket socket = new DatagramSocket()) {
            byte[] data = rtpMessage.getMessageBytes();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length,
                    InetAddress.getByName(mediaIp), mediaPort);
            socket.send(sendPacket);
        }
    }
}
