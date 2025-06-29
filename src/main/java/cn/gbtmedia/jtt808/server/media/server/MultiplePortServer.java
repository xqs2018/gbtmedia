package cn.gbtmedia.jtt808.server.media.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.MediaManger;
import cn.gbtmedia.jtt808.server.media.codec.Jtt1078RtpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * @author xqs
 */
@Slf4j
public class MultiplePortServer extends MediaServer{

    private DatagramSocket datagramSocket;

    private ServerSocket serverSocket;

    private Socket client;

    @Override
    protected void doStart() throws Exception {
        // 启动udp服务端
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
                    Jtt1078RtpMessage message = Jtt1078RtpMessage.toJtt1078RtpMessage(data);
                    receiveJtt1078RtpMessage(message);
                }
            }catch (Exception ex){
                if("Socket closed".equals(ex.getMessage())){
                    log.info("Socket closed");
                }else {
                    // tcp没连接才关闭
                    if(ex instanceof SocketTimeoutException && client != null){
                        log.info("UdpServer Socket closed");
                        return;
                    }
                    log.error("UdpServer ex",ex);
                }
                stop();
            }
        };
        MediaManger.getInstance().run(task);

        // 启动tcp服务端
        serverSocket = new ServerSocket(freePort);
        log.info("TcpServer start port {} ",freePort);
        Runnable tcpTask = ()->{
            try {
                serverSocket.setSoTimeout(1000*30);
                client = serverSocket.accept();
                log.info("TcpServer accept {}", client.getInetAddress());
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] delimiter = new byte[] {0x30, 0x31, 0x63, 0x64};
                try (InputStream inputStream = client.getInputStream()) {
                    // 读取消息 分隔符 0x30316364
                    byte[] temp = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(temp)) != -1) {
                        buffer.write(temp, 0, bytesRead);
                        processBuffer(buffer, delimiter);
                    }
                }
                log.info("TcpServer inputStream close");
                stop();
            }catch (Exception ex){
                if("Socket closed".equals(ex.getMessage())){
                    log.info("Socket closed");
                }else {
                    log.error("TcpServer ex",ex);
                }
                stop();
            }
        };
        MediaManger.getInstance().run(tcpTask);
    }

    private void processBuffer(ByteArrayOutputStream buffer, byte[] delimiter) {
        byte[] data = buffer.toByteArray();
        int currentStart = 0;
        while (true) {
            int firstDelimiterPos = findDelimiter(data, currentStart, delimiter);
            if (firstDelimiterPos == -1) break;
            int nextDelimiterPos = findDelimiter(data, firstDelimiterPos + delimiter.length, delimiter);
            if (nextDelimiterPos == -1) break;
            // 提取消息（包含起始分隔符）
            byte[] message = Arrays.copyOfRange(data, firstDelimiterPos, nextDelimiterPos);
            Jtt1078RtpMessage jtt1078RtpMessage = Jtt1078RtpMessage.toJtt1078RtpMessage(message);
            receiveJtt1078RtpMessage(jtt1078RtpMessage);
            currentStart = nextDelimiterPos;
        }
        buffer.reset();
        if (currentStart < data.length) {
            buffer.write(data, currentStart, data.length - currentStart);
        }
    }

    private int findDelimiter(byte[] data, int startPos, byte[] delimiter) {
        for (int i = startPos; i <= data.length - delimiter.length; i++) {
            boolean match = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (data[i + j] != delimiter[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    @Override
    protected void doStop() {
        if(datagramSocket != null && !datagramSocket.isClosed()){
            log.info("UdpServer datagramSocket close");
            datagramSocket.close();
        }
        try {
            if(serverSocket != null && !serverSocket.isClosed()){
                log.info("TcpServer serverSocket close ");
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("close ex",e);
        }
        try {
            if(client != null && !client.isClosed()){
                log.info("TcpServer client close");
                client.close();
            }
        } catch (IOException e) {
            log.error("close ex",e);
        }
        try {
            if(outputStream != null){
                log.info("TcpServer outputStream close");
                outputStream.close();
            }
        } catch (IOException e) {
            log.error("close ex",e);
        }
    }

    private OutputStream outputStream;

    @Override
    public void doSend1078RtpMessage(Jtt1078RtpMessage message) throws Exception {
        if(client == null){
            return;
        }
        if(outputStream == null){
            outputStream = client.getOutputStream();
        }
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = message.getMessageBytes();
        byteBuf.writeBytes(bytes);
        outputStream.write(ByteBufUtil.getBytes(byteBuf));
        outputStream.flush();
    }
}
