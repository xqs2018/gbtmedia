package cn.gbtmedia.gbt28181.server;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.entity.InviteInfo;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.repository.InviteInfoRepository;
import cn.gbtmedia.gbt28181.server.media.event.MediaClientStopEvent;
import cn.gbtmedia.gbt28181.server.media.event.MediaServerStopEvent;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.event.CatalogEvent;
import cn.gbtmedia.gbt28181.server.sip.event.DeviceEvent;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import javax.sip.ClientTransaction;
import javax.sip.TimeoutEvent;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xqs
 */
@Slf4j
@Component
public class Gbt28181Runner implements ApplicationRunner {

    @Resource
    private ServerConfig serverConfig;
    @Resource
    private SipDeviceSend sipDeviceSend;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private InviteInfoRepository inviteInfoRepository;

    private static final ExecutorService GBT28181_RUN_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-gbt28181-run-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("gbt28181 run pool ex t {}", t, e))
                            .factory());

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 设备5分钟没上报过心跳视为离线
        SchedulerTask.getInstance().startPeriod("checkGbt28181DeviceOnline",this::checkDeviceOnline,1000 * 60 * 5);

        // 上级平台5分钟没上报过心跳视为离线
        SchedulerTask.getInstance().startPeriod("checkGbt28181PlatformOnline", this::checkPlatformOnline,1000 * 60 * 5);

        // 上级平台10秒扫描一次，按周期重新注册
        SchedulerTask.getInstance().startDelay("reRegisterGbt28181Platform", this::reRegisterPlatform,1000 * 10);

        // 上级平台10秒扫描一次，按周期发送心跳
        SchedulerTask.getInstance().startPeriod("keepTimeoutGbt28181Platform" ,this::keepTimeoutPlatform,1000 * 10);

        // 30秒无人观看关闭流
        long mediaAutoClose = serverConfig.getGbt28181().getMediaAutoClose();
        SchedulerTask.getInstance().startPeriod("checkGbt28181Invite",this::checkInvite,mediaAutoClose/2);

        // 3s后尝试注册上级平台
        SchedulerTask.getInstance().startDelay("checkGbt28181PlatformRegister",this::checkPlatformRegister, 1000 * 3);

        // 10s备份一次invite，防止jvm崩溃丢失
        SchedulerTask.getInstance().startPeriod("syncGbt28181Invite",this::backupInvite,1000 * 10);

        // 3s后尝试关闭错误的播放
        SchedulerTask.getInstance().startDelay("revertGbt28181Invite",this::revertInvite,1000 * 3);

        // 1小时检测磁盘大小删除云端录像文件
        SchedulerTask.getInstance().startDelay("cleanGbt28181CloudRecord",this::cleanCloudRecord,1000 * 60 * 60);
    }

    public void checkDeviceOnline(){
        List<Device> deviceList = deviceRepository.findByOnline(1);
        deviceList.forEach(device -> {
            if(device.getKeepaliveTime().getTime() + (1000 * 180) < System.currentTimeMillis()){
                GBT28181_RUN_POOL.execute(()->{
                    SipResult<?> sipResult = sipDeviceSend.queryDeviceStatus(device.getDeviceId());
                    if(!sipResult.isSuccess()){
                        log.warn("check device is offline deviceId {}",device.getDeviceId());
                        SpringUtil.publishEvent(new DeviceEvent(this,device.getDeviceId(),0));
                    }
                });
            }
        });
    }

    public void checkPlatformOnline(){
        List<Platform> platformList = platformRepository.findByOnline(1);
        platformList.forEach(platform -> {
            if(platform.getKeepaliveTime().getTime() + (1000 * 180) < System.currentTimeMillis()){
                SipResult<?> sipResult = sipPlatformSend.register(platform.getPlatformId());
                if(!sipResult.isSuccess()){
                    log.warn("check platform is offline platformId {}",platform.getPlatformId());
                    platform.setOnline(0);
                    platformRepository.save(platform);
                    // 关闭所有推流
                    List<ClientInvite> clientInvites = SipSessionManger.getInstance().getAllClientInvite();
                    for (ClientInvite clientInvite : clientInvites){
                        if(platform.getPlatformId().equals(clientInvite.getPlatformId())){
                            sipDeviceSend.stopClientInvite(clientInvite.getCallId());
                        }
                    }
                    List<ServerInvite> serverInvites = SipSessionManger.getInstance().getAllServerInvite();
                    for (ServerInvite serverInvite : serverInvites){
                        if(platform.getPlatformId().equals(serverInvite.getPlatformId())){
                            sipDeviceSend.stopServerInvite(serverInvite.getSsrc());
                        }
                    }
                }
            }
        });
    }

    public void reRegisterPlatform() {
        List<Platform> platformList = platformRepository.findByEnableAndOnline(1,1);
        for(Platform platform: platformList){
            Date registTime = platform.getRegistTime();
            Integer expires = platform.getExpires();
            // 已经超出注册周期了，重新注册
            if( (registTime.getTime() + expires *1000) < System.currentTimeMillis()){
                GBT28181_RUN_POOL.execute(()->{
                    SipResult<?> sipResult = sipPlatformSend.register(platform.getPlatformId());
                });
            }
        }
    }

    public void keepTimeoutPlatform() {
        List<Platform> platformList = platformRepository.findByEnableAndOnline(1,1);
        for(Platform platform: platformList){
            Date keepaliveTime = platform.getKeepaliveTime();
            Integer keepTimeout = platform.getKeepTimeout();
            // 已经超出心跳周期了，发送心跳
            if( (keepaliveTime.getTime() + keepTimeout *1000) < System.currentTimeMillis()){
                GBT28181_RUN_POOL.execute(()->{
                    SipResult<?> sipResult = sipPlatformSend.keepalive(platform.getPlatformId());
                });
            }
        }
    }

    public void checkPlatformRegister(){
        // 之前启用过和在线的
        List<Platform> platformList = platformRepository.findByEnableAndOnline(1,1);
        platformList.forEach(platform -> {
            GBT28181_RUN_POOL.execute(()->{
                SipResult<?> sipResult = sipPlatformSend.register(platform.getPlatformId());
            });
        });
    }

    private final Cache<String, AtomicInteger> inviteCheckCount = Caffeine.newBuilder()
            .expireAfterWrite(180, TimeUnit.SECONDS)
            .build();

    {
        SchedulerTask.getInstance().startPeriod("inviteCheckCount", inviteCheckCount::cleanUp,100);
    }

    public void checkInvite(){
        // serverInvite broadcast是上级级联的不用关闭 download是下载不用关闭
        List<ServerInvite> allServerInvite = SipSessionManger.getInstance().getAllServerInvite();
        allServerInvite = allServerInvite.stream().filter(v->!v.getMediaType().equals(MediaType.broadcast)
                &&!v.getMediaType().equals(MediaType.download)).toList();
        for(ServerInvite serverInvite: allServerInvite){
            if(serverInvite.getMediaServer() == null){
                continue;
            }
            // 没有ack，创建时间不足30s
            if(!serverInvite.isInviteAck() &&
                    serverInvite.getCreateTime() + 1000*30 < System.currentTimeMillis()){
                continue;
            }
            int viewNum = serverInvite.getMediaServer().getViewNum();
            int consumerSize = serverInvite.getMediaServer().getConsumerRtpMessageMap().size();
            int clientSize = serverInvite.getClientInvites().size();
            String ssrc = serverInvite.getSsrc();
            // 无人观看 无人消费  没有级联到上级
            if(viewNum == 0 && consumerSize == 0 && clientSize == 0){
                AtomicInteger count = inviteCheckCount.get(ssrc,k-> new AtomicInteger(0));
                if(count.incrementAndGet() > 1){
                    inviteCheckCount.invalidate(ssrc);
                    log.info("no view no consumer stopServerInvite ssrc {} ",ssrc);
                    sipDeviceSend.stopServerInvite(ssrc);
                }
            }else {
                inviteCheckCount.invalidate(ssrc);
            }
        }
        // clientInvite 只有broadcast是需要主动关闭的，其它是上级级联的不用关闭
        List<ClientInvite> allClientInvite = SipSessionManger.getInstance().getAllClientInvite();
        allClientInvite = allClientInvite.stream().filter(v->v.getMediaType().equals(MediaType.broadcast)).toList();
        for(ClientInvite clientInvite: allClientInvite){
            if(clientInvite.getMediaClient() == null){
                continue;
            }
            // 没有ack，创建时间不足30s
            if(!clientInvite.isInviteAck() &&
                    clientInvite.getCreateTime() + 1000*30 < System.currentTimeMillis()){
                continue;
            }
            int consumerSize = clientInvite.getMediaClient().getConsumerRtpMessageMap().size();
            ServerInvite serverInvite = clientInvite.getServerInvite();
            String callId = clientInvite.getCallId();
            // 无人消费  没有级联到上级
            if(consumerSize == 0 && serverInvite == null){
                AtomicInteger count = inviteCheckCount.get(callId,k-> new AtomicInteger(0));
                if(count.incrementAndGet() > 1){
                    inviteCheckCount.invalidate(callId);
                    log.info("no consumer stopClientInvite callId {} ",callId);
                    sipDeviceSend.stopClientInvite(callId);
                }
            }else {
                inviteCheckCount.invalidate(callId);
            }
        }
    }

    public void backupInvite(){
        // 先删除旧的
        inviteInfoRepository.deleteAll();
        List<ServerInvite> allServerInvite = SipSessionManger.getInstance().getAllServerInvite();
        allServerInvite = allServerInvite.stream()
                .filter(v->!v.getMediaType().equals(MediaType.broadcast))
                .map(v->{
                    ServerInvite invite = new ServerInvite();
                    BeanUtil.copyProperties(v,invite,"mediaServer","clientInvites");
                    return invite;
                }).toList();
        if(allServerInvite.isEmpty()){
            return;
        }
        // 创建新的并写入
        JSONObject json = new JSONObject();
        json.set("serverInvite",allServerInvite);
        InviteInfo inviteInfo = new InviteInfo();
        inviteInfo.setInfoJson(json.toString());
        inviteInfoRepository.save(inviteInfo);
    }

    public void revertInvite(){
        List<InviteInfo> inviteInfos = inviteInfoRepository.findAll();
        if(inviteInfos.isEmpty()){
            return;
        }
        InviteInfo inviteInfo = inviteInfos.get(0);
        // 重启5分钟内可以恢复，太久了信令已经断开了
        if(inviteInfo.getCreateTime().getTime() + 1000 * 60 * 5 < System.currentTimeMillis()){
            inviteInfoRepository.deleteAll();
            return;
        }
        String jsonStr = inviteInfo.getInfoJson();
        JSONObject json = JSONUtil.parseObj(jsonStr);
        log.info("revertInvite {}",json);
        List<ServerInvite> allServerInvite = json.getBeanList("serverInvite", ServerInvite.class);
        // 恢复原状
        allServerInvite.forEach(v->SipSessionManger.getInstance().putServerInvite(v));
        // 直接发送bye全部停掉
        allServerInvite.forEach(v->sipDeviceSend.stopServerInvite(v.getSsrc()));
    }

    public void cleanCloudRecord() {
        // 释放100G空间，删除最早的文件，最近7天内的避免删除
        long spaceToFree = 1024L * 1024 * 1024 * 100;
        long lastModified = 1000 * 60 * 60 * 24 * 7;
        String recordPathCloud = ServerConfig.getInstance().getGbt28181().getRecordPathCloud();
        long freeSpace = new File(recordPathCloud).getFreeSpace();
        if(freeSpace > spaceToFree){
            return;
        }
        log.info("cleanCloudRecord start freeSpace {}",freeSpace);
        List<File> files = FileUtil.loopFiles(recordPathCloud).stream()
                .sorted(Comparator.comparingLong(File::lastModified))
                .toList();
        long freedSpace = 0;
        for (File file : files) {
            if (freedSpace >= spaceToFree) {
                break;
            }
            try {
                if (file.lastModified() + lastModified > System.currentTimeMillis() && file.delete()) {
                    freedSpace += file.length();
                }
            }catch (Exception ex){
                log.error("cleanCloudRecord ex",ex);
            }
        }
        if(freedSpace < spaceToFree){
            log.error("cleanCloudRecord only freed {} bytes of old record files", freedSpace);
        }
    }

    @Async
    @EventListener
    public void sipTimeoutEvent(TimeoutEvent timeoutEvent){
        ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
        if (clientTransaction != null) {
            Request request = clientTransaction.getRequest();
            if (request != null) {
                String host = ((SipURI) request.getRequestURI()).getHost();
                int port = ((SipURI) request.getRequestURI()).getPort();
                Device device = deviceRepository.findBySipIpAndSipPort(host, port);
                if(device != null){
                    log.warn("sipTimeoutEvent offline deviceId {} host {} port {}",device.getDeviceId(),host,port);
                    device.setOnline(0);
                    deviceRepository.save(device);
                }
            }
        }
    }

    @Async
    @EventListener
    public void deviceEvent(DeviceEvent event){
        String deviceId = event.getDeviceId();
        Device device = deviceRepository.findByDeviceId(deviceId);
        device.setOnline(event.getOnline());
        deviceRepository.save(device);
        List<DeviceChannel> channelList = deviceChannelRepository.findByDeviceId(deviceId);
        channelList.forEach(v-> v.setOnline(event.getOnline()));
        deviceChannelRepository.saveAll(channelList);
    }

    @Async
    @EventListener
    public void mediaServerStopEvent(MediaServerStopEvent stopEvent){
        String ssrc = stopEvent.getSsrc();
        sipDeviceSend.stopServerInvite(ssrc);
    }

    @Async
    @EventListener
    public void mediaClientStopEvent(MediaClientStopEvent stopEvent){
        String callId = stopEvent.getCallId();
        sipDeviceSend.stopClientInvite(callId);
    }

    @Async
    @EventListener
    public void catalogEvent(CatalogEvent catalogEvent){
        Platform platform = platformRepository.findByPlatformId(catalogEvent.getPlatformId());
        if(platform.getOnline() == 1 && platform.getEnable() == 1
                && !catalogEvent.getPlatformChannelList().isEmpty()){
            SipResult<?> sipResult = sipPlatformSend.notifyCatalog(platform.getPlatformId(),
                    catalogEvent.getPlatformChannelList(), catalogEvent.getEventTye());
        }
    }

    @EventListener
    public void contextClosedEvent(ContextClosedEvent event) {
        log.info("contextClosedEvent stopServerInvite stopClientInvite ...");
        List<ServerInvite> allServerInvite = SipSessionManger.getInstance().getAllServerInvite();
        for(ServerInvite serverInvite: allServerInvite){
            sipDeviceSend.stopServerInvite(serverInvite.getSsrc());
        }
        List<ClientInvite> allClientInvite = SipSessionManger.getInstance().getAllClientInvite();
        for(ClientInvite clientInvite: allClientInvite){
            sipDeviceSend.stopClientInvite(clientInvite.getCallId());
        }
        // 备份当前无
        backupInvite();
    }
}
