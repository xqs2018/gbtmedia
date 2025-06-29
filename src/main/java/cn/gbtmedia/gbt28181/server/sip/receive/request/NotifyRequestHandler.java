package cn.gbtmedia.gbt28181.server.sip.receive.request;

import cn.gbtmedia.gbt28181.server.sip.event.CatalogEvent;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.server.sip.receive.IRequestHandler;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.util.XmlUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.RequestEvent;
import javax.sip.message.Request;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Component
public class NotifyRequestHandler implements IRequestHandler {

    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;

    @Override
    public boolean support(RequestEvent requestEvent) {
        return requestEvent.getRequest().getMethod().equals(Request.NOTIFY);
    }

    @Override
    @SneakyThrows
    public void handle(RequestEvent requestEvent) {
        // 先回复200
        sipServer.response200(requestEvent);
        String userId = SipUtil.getUserIdFromFromHeader(requestEvent.getRequest());
        Request request = requestEvent.getRequest();
        String xmlStr = new String(request.getRawContent(), "gb2312");
        JSONObject jsonObject = JSONUtil.parseFromXml(xmlStr);
        String CmdType = jsonObject.getJSONObject("Notify").getStr("CmdType");
        // 处理通道订阅
        if (CmdType.equals("Catalog")) {
            sipServer.response200(requestEvent);
            processNotifyCatalogList(userId, jsonObject);
        }
    }

    private void processNotifyCatalogList(String deviceId, JSONObject jsonObject) {
        log.info("Catalog Notify deviceId {} ",deviceId);
        JSONObject catalogResponse = jsonObject.getJSONObject("Notify");
        Object deviceObjects = catalogResponse.getJSONObject("DeviceList").get("Item");
        JSONArray deviceList = new JSONArray();
        if (deviceObjects instanceof JSONArray) {
            deviceList = (JSONArray) deviceObjects;
        } else if (deviceObjects instanceof JSONObject) {
            deviceList.put(deviceObjects);
        }
        deviceList.forEach(jsonItem -> {
            JSONObject jsonObjectItem = (JSONObject) jsonItem;
            DeviceChannel channel = XmlUtil.jsonItemToDeviceChannel(jsonObjectItem);
            channel.setDeviceId(deviceId);
            // 具体的事件
            String event = jsonObjectItem.getStr("Event");
            if (ObjectUtil.isEmpty(event)) {
                event = "ADD";
            }
            log.info("Catalog Notify deviceId {} channelId {} event {}",deviceId, channel.getChannelId(),event);
            DeviceChannel old = deviceChannelRepository.findByDeviceIdAndChannelId(deviceId, channel.getChannelId());
            switch (event) {
                case "ON":
                    // 上线
                    if(old != null){
                        old.setOnline(1);
                        deviceChannelRepository.save(old);
                        // 继续向上级平台发送通知
                        publishPlatform(channel,event);
                    }
                    break;
                case "OFF":
                    // 离线
                    if(old != null){
                        old.setOnline(0);
                        deviceChannelRepository.save(old);
                        // 继续向上级平台发送通知
                        publishPlatform(channel,event);
                    }
                    break;
                case "VLOST":
                    // 视频丢失
                    break;
                case "DEFECT":
                    // 故障
                    break;
                case "ADD":
                    // 增加
                    if(old != null){
                        log.error("event ADD has old deviceId {} channelId {}",channel.getDeviceId()
                                ,channel.getChannelId());
                        break;
                    }
                    deviceChannelRepository.save(channel);
                    break;
                case "DEL":
                    // 删除
                    if(old != null){
                        deviceChannelRepository.delete(old);
                        // 继续向上级平台发送通知
                        publishPlatform(channel,event);
                    }
                    break;
                case "UPDATE":
                    // 更新
                    if(old != null){
                        old.setOnline(channel.getOnline());
                        old.setName(channel.getName());
                        deviceChannelRepository.save(old);
                        // 继续向上级平台发送通知
                        publishPlatform(channel,event);
                    }
                    break;
                default:
                    log.warn("Catalog Notify event not found ： {}", event);
            }

        });
    }

    private void publishPlatform(DeviceChannel channel,String event){
        List<PlatformChannel> platformChannelList = platformChannelRepository.findByDeviceIdAndChannelId(channel.getDeviceId(),
                channel.getChannelId());
        if(ObjectUtil.isNotEmpty(platformChannelList)){
            for(PlatformChannel v: platformChannelList){
                v.setDeviceChannel(channel);
                SpringUtil.publishEvent(new CatalogEvent(this, v.getPlatformId(),List.of(v),event));
            }
        }
    }
}
