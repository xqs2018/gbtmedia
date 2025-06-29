package cn.gbtmedia.jtt808.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.common.util.SpringUtil;

import cn.gbtmedia.jtt808.service.Jtt808Service;
import cn.gbtmedia.jtt808.dto.DownloadRecordTask;
import cn.gbtmedia.jtt808.dto.QueryParam;
import cn.gbtmedia.jtt808.server.cmd.event.ClientEvent;
import cn.gbtmedia.jtt808.server.cmd.session.ClientMedia;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import cn.gbtmedia.jtt808.server.media.event.MediaServerStopEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xqs
 */
@Slf4j
@Component
public class Jtt808Runner implements ApplicationRunner {

    @Resource
    private ServerConfig serverConfig;

    @Resource
    private Jtt808Service jtt808Service;

    @Resource
    private ClientRepository clientRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 终端3分钟没上报过心跳视为离线
        SchedulerTask.getInstance().startPeriod("checkJtt808ClientOnline",this::checkClientOnline,1000 * 180);

        // 30秒无人观看关闭流
        long mediaAutoClose = serverConfig.getJtt808().getMediaAutoClose();
        SchedulerTask.getInstance().startPeriod("checkJtt808ClientMedia",this::checkClientMedia,mediaAutoClose/2);

        // 3分钟下载任务取消
        SchedulerTask.getInstance().startPeriod("checkJtt808DownloadRecordTask",this::checkDownloadRecordTask,1000 * 180);

        // 清除下载任务
        jtt808Service.cleanDownloadTask(new QueryParam());
    }

    public void checkClientOnline() {
        List<Client> clientList = clientRepository.findByOnline(1);
        clientList.forEach(client -> {
            if(client.getKeepaliveTime().getTime() + (1000 * 180) < System.currentTimeMillis()){
                log.warn("check client is offline clientId {}",client.getClientId());
                SpringUtil.publishEvent(new ClientEvent(this,client.getClientId(),0));
            }
        });
    }

    private final Cache<String, AtomicInteger> clientMediaCheckCount = Caffeine.newBuilder()
            .expireAfterWrite(180, TimeUnit.SECONDS)
            .build();

    {
        SchedulerTask.getInstance().startPeriod("clientMediaCheckCount", clientMediaCheckCount::cleanUp,100);
    }

    public void checkClientMedia() {
        List<ClientMedia> allClientMedia = Jtt808SessionManager.getInstance().getAllClientMedia();
        for(ClientMedia clientMedia: allClientMedia){
            if(clientMedia.getMediaServer() == null){
                continue;
            }
            int viewNum = clientMedia.getMediaServer().getViewNum();
            String mediaKey = clientMedia.getMediaKey();
            // 无人观看
            if(viewNum == 0 ){
                AtomicInteger count = clientMediaCheckCount.get(mediaKey,k-> new AtomicInteger(0));
                if(count.incrementAndGet() > 1){
                    clientMediaCheckCount.invalidate(mediaKey);
                    log.info("no viewNum stopServerInvite mediaKey {} ",mediaKey);
                    jtt808Service.stopClientMedia(mediaKey);
                }
            }else {
                clientMediaCheckCount.invalidate(mediaKey);
            }
        }
    }


    public void checkDownloadRecordTask() {
        List<DownloadRecordTask> taskList = jtt808Service.listDownloadRecordTask(new QueryParam());
        for(DownloadRecordTask task: taskList){
            QueryParam param = new QueryParam();
            param.setTempPath(param.getTempPath());
            DownloadRecordTask checked = jtt808Service.checkDownloadTask(param);
            if(checked.getTaskStartTime().getTime() + 1000*180 < System.currentTimeMillis()
                    && checked.getProgress() == checked.getLastProgress()){
                log.info("checkDownloadRecordTask stop task {}",task);
                param.setCommand(2);
                jtt808Service.downloadRecordControl(param);
            }
        }
    }

    @Async
    @EventListener
    public void mediaServerStopEvent(MediaServerStopEvent stopEvent){
        String mediaKey = stopEvent.getMediaKey();
        jtt808Service.stopClientMedia(mediaKey);
    }

    @Async
    @EventListener
    public void clientEvent(ClientEvent clientEvent){
        String clientId = clientEvent.getClientId();
        Integer online = clientEvent.getOnline();
        if(online == 0){
            Jtt808SessionManager.getInstance().removeClientSession(clientId);
            Client client = clientRepository.findByClientId(clientId);
            if(client != null){
                client.setOnline(0);
                clientRepository.save(client);
            }
            QueryParam param = new QueryParam();
            param.setClientId(clientId);
            jtt808Service.cleanDownloadTask(param);
        }
    }

    @EventListener
    public void contextClosedEvent(ContextClosedEvent event) {
        log.info("contextClosedEvent stopClientMedia  ...");
        List<ClientMedia> allClientMedia = Jtt808SessionManager.getInstance().getAllClientMedia();
        for(ClientMedia clientMedia: allClientMedia){
            jtt808Service.stopClientMedia(clientMedia.getMediaKey());
        }
    }
}
