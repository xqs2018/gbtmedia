package cn.gbtmedia.test;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * @author xqs
 */
@Slf4j
public class WebSocketTestgbt28181 {

    public static void main(String[] args) throws InterruptedException {
        WebSocketClient client =  new StandardWebSocketClient();
        String url = "ws://172.22.31.1:15800/chat/talk_0102000001";
        client.execute(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info("afterConnectionEstablished session {}",session);
                byte[] pcm = FileUtil.readBytes(ResourceUtils.getFile("classpath:8k16bit.pcm"));
                int offset = 0;
                int length = 1024;
                while (true) {
                    if (offset + length > pcm.length) {
                        offset = 0;
                    }
                    byte[] chunk = new byte[length];
                    System.arraycopy(pcm, offset, chunk, 0, length);
                    session.sendMessage(new BinaryMessage(chunk));
                    offset += length;
                    log.info("send binary {}/{}",offset,pcm.length);
                    Thread.sleep(10);
                }
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                log.info("handleMessage message {}",message);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.info("handleTransportError exception ",exception);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                log.info("afterConnectionClosed closeStatus {} ",closeStatus);
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, url);
        Thread.sleep(100000);
    }
}
