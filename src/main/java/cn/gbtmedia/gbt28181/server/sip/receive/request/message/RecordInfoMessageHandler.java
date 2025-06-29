package cn.gbtmedia.gbt28181.server.sip.receive.request.message;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.sip.send.FutureContext;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.server.sip.receive.request.MessageRequestHandler;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.util.XmlUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
@Component
public class RecordInfoMessageHandler implements MessageRequestHandler.Process{

    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private SipDeviceSend sipDeviceSend;

    private final Cache<String, RecordDto> recordCache = Caffeine.newBuilder()
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .removalListener((key, value, cause) -> {
                if(cause == RemovalCause.EXPIRED){
                    log.warn("Cache TimeOut RecordInfo Response future callBack snKey {}",key);
                    FutureContext.callBack((String) key, value);
                }
            })
            .build();
    {
        SchedulerTask.getInstance().startPeriod("recordCache", recordCache::cleanUp,100);
    }

    @Override
    public boolean support(RequestEvent requestEvent, JSONObject message) {
        JSONObject data = (JSONObject) message.values().stream().findFirst().orElse(new JSONObject());
        return "RecordInfo".equals(data.getStr("CmdType"));
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent, JSONObject message) {
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        // 收到下级返回的录像信息
        if(message.containsKey("Response")){
            // 回复200
            sipServer.response200(requestEvent);
            Device device = deviceRepository.findByDeviceId(userId);
            if(device == null){
                log.error("RecordInfo Response device is null userId {}",userId);
                return;
            }
            JSONObject response = message.getJSONObject("Response");
            Integer sumNum = response.getInt("SumNum");
            String sn = response.getStr("SN");
            String snKey = device.getDeviceId() + "_" + sn;
            // xml解析录像数据
            RecordDto recordDto = XmlUtil.jsonToRecordInfo(userId, response);
            RecordDto cache = recordCache.get(snKey, k -> {
                RecordDto create = new RecordDto();
                BeanUtil.copyProperties(recordDto,create);
                create.setRecordList(new CopyOnWriteArrayList<>());
                return create;
            });
            cache.getRecordList().addAll(recordDto.getRecordList());
            int size = cache.getRecordList().size();
            log.info("RecordInfo Response userId {} sn {} sumNum {}/{}",userId, sn, sumNum, size);
            // 全部接收到了，触发回调
            if(size == sumNum){
                recordCache.invalidate(snKey);
                log.info("RecordInfo Response future callBack snKey {}", snKey);
                FutureContext.callBack(snKey, cache);
            }
        }
        // 收到上级下发的查询录像信息请求
        if(message.containsKey("Query")){
            JSONObject query = message.getJSONObject("Query");
            Date startTime = DateUtil.parse(query.getStr("StartTime").replace("T"," "));
            Date endTime = DateUtil.parse(query.getStr("EndTime").replace("T"," "));
            String channelId = query.getStr("DeviceID");
            log.info("RecordInfo Query userId {} channelId {}",userId,channelId);
            Platform platform = platformRepository.findByPlatformId(userId);
            if(platform == null || platform.getEnable() == 0){
                log.error("no platform or not enable userId {} ",userId);
                sipServer.response403(requestEvent);
                return;
            }
            PlatformChannel platformChannel = platformChannelRepository.findByPlatformIdAndChannelId(userId, channelId);
            if(platformChannel == null){
                log.error("no platformChannel userId {} channelId {}",userId,channelId);
                sipServer.response403(requestEvent);
                return;
            }
            String deviceId = platformChannel.getDeviceId();
            // 查询下级录像，再发给上级
            SipResult<RecordDto> result = sipDeviceSend.queryRecordInfo(deviceId, channelId, startTime, endTime);
            if(result.isSuccess()){
                // 回复200
                sipServer.response200(requestEvent);
                String sn = query.getStr("SN");
                FromHeader fromHeader = (FromHeader) requestEvent.getRequest().getHeader(FromHeader.NAME);
                SipResult<?> result1 = sipPlatformSend.responseRecordInfo(userId, sn, fromHeader.getTag(), result.getData());
            }else {
                sipServer.response500(requestEvent);
            }
        }
    }
}
