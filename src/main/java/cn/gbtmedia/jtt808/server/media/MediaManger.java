package cn.gbtmedia.jtt808.server.media;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.media.server.MediaServer;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
@Component("jtt808MediaManger")
public class MediaManger {

    public static MediaManger getInstance(){
        return SpringUtil.getBean(MediaManger.class);
    }

    private static final ExecutorService JTT808_MEDIA_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-jtt808-media-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("jtt808 media pool ex t {}", t, e))
                            .factory());

    @Resource
    private MediaService mediaService;

    @Resource
    private ServerConfig serverConfig;

    @PostConstruct
    void init(){
        String mediaModel = serverConfig.getJtt808().getMediaModel();
        log.info("jtt808Media model {} {}", mediaModel,mediaService.getClass());
    }

    public MediaServer createServer(MediaParam mediaParam) {
        return mediaService.createServer(mediaParam);
    }

    public synchronized Integer getFreePort(){
        List<Integer> mediaPorts = new ArrayList<>();
        String[] split = ServerConfig.getInstance().getJtt808().getMediaMultiplePort().split("-");
        for(int i = Integer.parseInt(split[0]); i < Integer.parseInt(split[1]); i++){
            mediaPorts.add(i);
        }
        for(Integer mediaPort : mediaPorts){
            try (ServerSocket serverSocket = new ServerSocket(mediaPort);
                 DatagramSocket datagramSocket = new DatagramSocket(mediaPort)) {
                log.info("media getFreePort {}",mediaPort);
                return mediaPort;
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("no freePort");
    }

    public void run(Runnable task){
        JTT808_MEDIA_POOL.execute(task);
    }
}
