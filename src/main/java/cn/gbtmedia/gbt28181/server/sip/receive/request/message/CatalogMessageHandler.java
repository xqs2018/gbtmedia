package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.server.sip.send.FutureContext;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.util.XmlUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;


/**
 * @author xqs
 */
@Slf4j
@Component
public class CatalogMessageHandler implements MessageRequestHandler.Process{
    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private PlatformRepository platformRepository;

    private final Cache<String,CatalogDto> catalogCache = Caffeine.newBuilder()
                         .expireAfterWrite(120, TimeUnit.SECONDS)
                         .removalListener((key, value, cause) -> {
                             if(cause == RemovalCause.EXPIRED){
                                 log.warn("Cache TimeOut Catalog Response snKey {}",key);
                             }
                          })
                         .build();
    {
        SchedulerTask.getInstance().startPeriod("catalogCache", catalogCache::cleanUp,100);
    }

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "Catalog".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级返回的通道信息
        if(message.containsKey("Response")){
            // 回复200
            sipServer.response200(requestEvent);
            Device device = deviceRepository.findByDeviceId(userId);
            if(device == null){
                log.error("Catalog Response device is null userId {}",userId);
                return;
            }
            JSONObject response = message.getJSONObject("Response");
            Integer sumNum = response.getInt("SumNum");
            String sn = response.getStr("SN");
            String snKey = device.getDeviceId() + "_" + sn;
            // xml解析通道数据
            Object deviceObjects = response.getJSONObject("DeviceList").get("Item");
            JSONArray deviceList = new JSONArray();
            if (deviceObjects instanceof JSONArray) {
                deviceList = (JSONArray) deviceObjects;
            } else if (deviceObjects instanceof JSONObject) {
                deviceList.put(deviceObjects);
            }
            List<DeviceChannel> channelList = deviceList.stream().map(item -> {
                DeviceChannel deviceChannel = XmlUtil.jsonItemToDeviceChannel((JSONObject) item);
                deviceChannel.setDeviceId(userId);
                return deviceChannel;
            }).toList();
            CatalogDto cache = catalogCache.get(snKey, k -> {
                CatalogDto create = new CatalogDto();
                create.setDeviceId(device.getDeviceId());
                create.setSn(sn);
                create.setSumNum(sumNum);
                create.setDeviceChannelList(new CopyOnWriteArrayList<>());
                return create;
            });
            cache.getDeviceChannelList().addAll(channelList);
            int size = cache.getDeviceChannelList().size();
            log.info("Catalog Response deviceId {} sumNum {}/{}",userId, sumNum, size);
            // TODO 批量保存
            for(DeviceChannel deviceChannel : channelList){
                DeviceChannel old = deviceChannelRepository.findByDeviceIdAndChannelId(deviceChannel.getDeviceId(),
                        deviceChannel.getChannelId());
                // 存在通道进行更新
                if(old != null){
                    old.setName(deviceChannel.getName());
                    deviceChannelRepository.save(old);
                    // 标记这条已经保存了
                    deviceChannel.setId(old.getId());
                }else {
                    deviceChannelRepository.save(deviceChannel);
                }
            }
            // 直接回调了,不等全部数据 太久了
            log.info("Catalog Response future callBack snKey {}", snKey);
            FutureContext.callBack(snKey, getResponseCatalog(device.getDeviceId()));
            if(size == sumNum){
                catalogCache.invalidate(snKey);
            }
        }
        // 收到上级下发的查询通道信息请求
        if(message.containsKey("Query")){
            JSONObject query = message.getJSONObject("Query");
            String sn = query.getStr("SN");
            log.info("Catalog Query userId {} sn {}",userId, sn);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            FromHeader fromHeader = (FromHeader) requestEvent.getRequest().getHeader(FromHeader.NAME);
            SipResult<?> result = sipPlatformSend.responseCatalog(userId, sn, fromHeader.getTag());
        }
    }


    /**
     * 获取通道响应进度
     */
    public CatalogDto getResponseCatalog(String deviceId){
        String key = catalogCache.asMap().keySet().stream().filter(v -> v.startsWith(deviceId)).findFirst().orElse(null);
        if(key == null){
            return null;
        }
        CatalogDto cache = catalogCache.getIfPresent(key);
        if(cache == null){
            return null;
        }
        List<DeviceChannel> cacheList = cache.getDeviceChannelList();
        cache.setNowNum(cacheList.size());
        cache.setSaveNum(cacheList.stream().filter(v->v.getId()!=null).toList().size());
        return cache;
    }
}
