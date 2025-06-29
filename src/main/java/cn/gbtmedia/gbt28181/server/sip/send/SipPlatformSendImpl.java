package cn.gbtmedia.gbt28181.server.sip.send;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.aop.SyncWait;
import cn.gbtmedia.gbt28181.server.media.MediaManger;
import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.sip.util.SipParam;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import javax.sip.ResponseEvent;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author xqs
 */
@Slf4j
@Service
public class SipPlatformSendImpl implements SipPlatformSend{

    @Resource
    private ServerConfig serverConfig;
    @Resource
    private SipServer sipServer;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Lazy
    @Resource
    private SipDeviceSend deviceSend;

    @Override
    @SyncWait(key = "#platformId")
    public SipResult<?> register(String platformId) {
        log.info("register platformId {}",platformId);
        return registerFun(platformId, true);
    }

    @Override
    @SyncWait(key = "#platformId")
    public SipResult<?> unRegister(String platformId) {
        log.error("unRegister platformId {}",platformId);
        return registerFun(platformId, false);
    }

    private SipResult<?> registerFun(String platformId,boolean isRegister){
        Platform platform = platformRepository.findByPlatformId(platformId);
        try {
            String callId = IdUtil.fastSimpleUUID();
            SipParam sipParam = new SipParam();
            sipParam.setCallId(callId);
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setExpires(isRegister ? platform.getExpires() : 0);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendRegisterRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("registerFun timeOut");
            }
            Response response = responseEvent.getResponse();
            // 返回401 添加密码再注册
            if(response.getStatusCode() == 401){
                log.info("registerFun set password platformId {} ", platformId);
                WWWAuthenticateHeader www = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
                callId = IdUtil.fastSimpleUUID();
                sipParam.setCallId(callId);
                sipParam.setPassword(platform.getPassword());
                sipParam.setWww(www);
                future = FutureContext.regist(callId);
                sipServer.sendRegisterRequest(sipParam);
                responseEvent = (ResponseEvent) future.get();
                if(responseEvent == null){
                    throw new RuntimeException("registerFun timeOut");
                }
                response = responseEvent.getResponse();
                if (response.getStatusCode() != 200) {
                    throw new RuntimeException("registerFun fail " + response.getReasonPhrase());
                }
            }
            platform.setOnline(isRegister?1:0);
            platform.setRegistTime(new Date());
            platform.setKeepaliveTime(new Date());
            platformRepository.save(platform);
            return SipResult.success();
        }catch (Exception ex){
            log.error("registerFun ex platformId {}",platformId,ex);
            platform.setOnline(0);
            platformRepository.save(platform);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> keepalive(String platformId) {
        if(log.isDebugEnabled()){
            log.debug("keepalive platformId {}",platformId);
        }
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Notify>\r\n");
            params.append("<CmdType>Keepalive</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + serverConfig.getGbt28181().getSipId() + "</DeviceID>\r\n");
            params.append("<Status>OK</Status>\r\n");
            params.append("</Notify>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            platform.setKeepaliveTime(new Date());
            platformRepository.save(platform);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("keepalive ex platformId {}",platformId, ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseDeviceStatus(String platformId, String reqSn, String fromTag) {
        log.info("responseDeviceStatus platformId {} reqSn {} fromTag {} ", platformId,reqSn,fromTag);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            StringBuffer params = new StringBuffer(600);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Response>\r\n");
            params.append("<CmdType>DeviceStatus</CmdType>\r\n");
            params.append("<SN>" +reqSn + "</SN>\r\n");
            params.append("<DeviceID>" + serverConfig.getGbt28181().getSipId() + "</DeviceID>\r\n");
            params.append("<Result>OK</Result>\r\n");
            params.append("<Online>ONLINE</Online>\r\n");
            params.append("<Status>OK</Status>\r\n");
            params.append("</Response>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("responseDeviceStatus ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseDeviceInfo(String platformId, String reqSn, String fromTag) {
        log.info("responseDeviceInfo platformId {} reqSn {} fromTag {} ", platformId,reqSn,fromTag);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            StringBuffer params = new StringBuffer(600);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Response>\r\n");
            params.append("<CmdType>DeviceInfo</CmdType>\r\n");
            params.append("<SN>" + reqSn + "</SN>\r\n");
            params.append("<DeviceID>" + serverConfig.getGbt28181().getSipId() + "</DeviceID>\r\n");
            params.append("<DeviceName>" + platform.getName() + "</DeviceName>\r\n");
            params.append("<Manufacturer>gbtmedia</Manufacturer>\r\n");
            params.append("<Model>platform.v1</Model>\r\n");
            params.append("<Firmware>1.0.0</Firmware>\r\n");
            params.append("<Result>OK</Result>\r\n");
            params.append("</Response>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("responseDeviceInfo ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseCatalog(String platformId, String reqSn, String fromTag) {
        log.info("responseCatalog platformId {} reqSn {} fromTag {} ", platformId,reqSn,fromTag);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            // 查询关联的通道 TODO 优化
            List<PlatformChannel> platformChannels = platformChannelRepository.findByPlatformId(platformId);
            List<DeviceChannel> deviceChannels = platformChannels.stream().map(v->deviceChannelRepository
                    .findByDeviceIdAndChannelId(v.getDeviceId(), v.getChannelId())).toList();
            // 参数拼接
            Function<List<DeviceChannel>, String> getCatalogXml = (channels) -> {
                String charset = platform.getCharset();
                StringBuffer params = new StringBuffer(600);
                params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
                params.append("<Response>\r\n");
                params.append("<CmdType>Catalog</CmdType>\r\n");
                params.append("<SN>" + reqSn + "</SN>\r\n");
                params.append("<DeviceID>" + serverConfig.getGbt28181().getSipId() + "</DeviceID>\r\n");
                params.append("<SumNum>" + deviceChannels.size() + "</SumNum>\r\n");
                params.append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
                for (DeviceChannel channel : channels) {
                    params.append("<Item>\r\n");
                    params.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                    params.append("<Name>" + ObjectUtil.defaultIfEmpty(channel.getCustomName(),channel.getName()) + "</Name>\r\n");
                    params.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                    if (channel.getParentId() != null) {
                        params.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                    }
                    if (channel.getChannelId().length() == 20) {
                        if (Integer.parseInt(channel.getChannelId().substring(10, 13)) == 216) { //
                            // 虚拟组织增加BusinessGroupID字段
                            params.append("<BusinessGroupID>" + channel.getParentId() + "</BusinessGroupID>\r\n");
                        }
                        params.append("<Manufacturer>" + channel.getManufacturer() + "</Manufacturer>\r\n");
                        params.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                        params.append("<Status>" + (channel.getOnline() == 0 ? "OFF" : "ON") + "</Status>\r\n");
                        params.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                        params.append("<Model>" + channel.getModel() + "</Model>\r\n");
                        params.append("<Owner>" + channel.getOwner() + "</Owner>\r\n");
                        params.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
                        params.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                        params.append("<Longitude>" + channel.getLongitude() + "</Longitude>\r\n");
                        params.append("<Latitude>" + channel.getLatitude() + "</Latitude>\r\n");
                        params.append("<IPAddress>" + channel.getIpAddress() + "</IPAddress>\r\n");
                        params.append("<Port>" + channel.getPort() + "</Port>\r\n");
                        params.append("<Info>\r\n");
                        params.append("<PTZType>" + channel.getPTZType() + "</PTZType>\r\n");
                        params.append("</Info>\r\n");
                    }
                    params.append("</Item>\r\n");
                }
                params.append("</DeviceList>\r\n");
                params.append("</Response>\r\n");
                return params.toString();
            };
            // 每次发送10个通道
            int count = 0;
            int groupSize = 10;
            List<List<DeviceChannel>> groupedChannels = new ArrayList<>(IntStream.range(0, (deviceChannels.size() + groupSize - 1) / groupSize)
                    .mapToObj(i -> deviceChannels.subList(i * groupSize, Math.min((i + 1) * groupSize, deviceChannels.size())))
                    .toList());
            // 默认发0
            if(groupedChannels.isEmpty()){
                groupedChannels.add(new ArrayList<>());
            }
            for(List<DeviceChannel> groupedChannel : groupedChannels){
                    count += groupedChannel.size();
                    log.info("responseCatalog send {}/{}",count, deviceChannels.size());
                    String params = getCatalogXml.apply(groupedChannel);
                    SipParam sipParam = new SipParam();
                    sipParam.setSipId(platformId);
                    sipParam.setSipIp(platform.getSipIp());
                    sipParam.setSipPort(platform.getSipPort());
                    sipParam.setSipTransport(platform.getSipTransport());
                    sipParam.setContent(params);
                    sipServer.sendMessageRequest(sipParam);
            }
            return SipResult.success();
        }catch (Exception ex){
            log.info("responseCatalog ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> notifyCatalog(String platformId, List<PlatformChannel> platformChannelList, String eventTye) {
        log.info("notifyCatalog platformId {} platformChannelList {}  eventTye {}",
                platformId, platformChannelList.size(),eventTye);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            // 查询关联的通道 TODO 优化
            List<DeviceChannel> deviceChannels = platformChannelList.stream().map(v->{
                return v.getDeviceChannel() != null ? v.getDeviceChannel():
                deviceChannelRepository.findByDeviceIdAndChannelId(v.getDeviceId(),v.getChannelId());
            }).toList();
            // 参数拼接
            Function<List<DeviceChannel>, String> getCatalogXml = (channels) -> {
                String charset = platform.getCharset();
                StringBuffer params = new StringBuffer(600);
                params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
                params.append("<Notify>\r\n");
                params.append("<CmdType>Catalog</CmdType>\r\n");
                params.append("<SN>" + SipUtil.getNewSn() + "</SN>\r\n");
                params.append("<DeviceID>" + serverConfig.getGbt28181().getSipId() + "</DeviceID>\r\n");
                params.append("<SumNum>1</SumNum>\r\n");
                params.append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
                for (DeviceChannel channel : channels) {
                params.append("<Item>\r\n");
                params.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                params.append("<Name>" + channel.getName() + "</Name>\r\n");
                params.append("<Manufacturer>" + channel.getManufacturer() + "</Manufacturer>\r\n");
                params.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                if (channel.getParentId() != null) {
                    params.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                }
                params.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                params.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                params.append("<Status>" + (channel.getOnline() == 0 ? "OFF" : "ON") + "</Status>\r\n");
                if (channel.getChannelId().length() == 20 && Integer.parseInt(channel.getChannelId().substring(10
                        , 13)) == 216) { // 虚拟组织增加BusinessGroupID字段
                    params.append("<BusinessGroupID>" + channel.getParentId() + "</BusinessGroupID>\r\n");
                }
                params.append("<Model>" + channel.getModel() + "</Model>\r\n");
                params.append("<Owner>0</Owner>\r\n");
                params.append("<CivilCode>CivilCode</CivilCode>\r\n");
                params.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                params.append("<Event>" + eventTye + "</Event>\r\n");
                params.append("</Item>\r\n");
                }
                params.append("</DeviceList>\r\n");
                params.append("</Notify>\r\n");
                return params.toString();
            };
            // 每次发送10个通道
            int count = 0;
            int groupSize = 10;
            List<List<DeviceChannel>> groupedChannels = new ArrayList<>(IntStream.range(0, (deviceChannels.size() + groupSize - 1) / groupSize)
                    .mapToObj(i -> deviceChannels.subList(i * groupSize, Math.min((i + 1) * groupSize, deviceChannels.size())))
                    .toList());
            for(List<DeviceChannel> groupedChannel : groupedChannels){
                count += groupedChannel.size();
                log.info("notifyCatalog send {}/{}",count, deviceChannels.size());
                String params = getCatalogXml.apply(groupedChannel);
                SipParam sipParam = new SipParam();
                sipParam.setSipId(platformId);
                sipParam.setSipDomain(platform.getSipDomain());
                sipParam.setSipIp(platform.getSipIp());
                sipParam.setSipPort(platform.getSipPort());
                sipParam.setSipTransport(platform.getSipTransport());
                sipParam.setContent(params);
                JSONObject info = JSONUtil.parseObj(ObjectUtil.isEmpty(platform.getSubscribeCatalogInfo())?"{}"
                        :platform.getSubscribeCatalogInfo());
                sipParam.setCallId(info.getStr("callId",IdUtil.fastSimpleUUID()));
                sipParam.setFromTag(info.getStr("fromTag",IdUtil.fastSimpleUUID()));
                sipParam.setToTag(info.getStr("toTag",IdUtil.fastSimpleUUID()));
                sipParam.setViaBranch(info.getStr("viaBranch",IdUtil.fastSimpleUUID()));
                sipParam.setEventType("Catalog");
                sipServer.sendNotifyRequest(sipParam);
            }
            return SipResult.success();
        }catch (Exception ex){
            log.info("notifyCatalog ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseRecordInfo(String platformId, String reqSn, String fromTag, RecordDto recordDto) {
        log.info("responseRecordInfo platformId {} reqSn {} fromTag {}", platformId,reqSn,fromTag);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            StringBuffer params = new StringBuffer(600);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Response>\r\n");
            params.append("<CmdType>RecordInfo</CmdType>\r\n");
            params.append("<SN>" + reqSn + "</SN>\r\n");
            params.append("<DeviceID>" + recordDto.getDeviceId() + "</DeviceID>\r\n");
            params.append("<SumNum>" + recordDto.getSumNum() + "</SumNum>\r\n");
            if (recordDto.getRecordList() == null) {
                params.append("<RecordList Num=\"0\">\r\n");
            } else {
                params.append("<RecordList Num=\"" + recordDto.getRecordList().size() + "\">\r\n");
                if (!recordDto.getRecordList().isEmpty()) {
                    for (RecordDto.Item recordItem : recordDto.getRecordList()) {
                        params.append("<Item>\r\n");
                        params.append("<DeviceID>" + recordItem.getDeviceId() + "</DeviceID>\r\n");
                        params.append("<Name>" + recordItem.getName() + "</Name>\r\n");
                        params.append("<StartTime>" + recordItem.getStartTime() + "</StartTime>\r\n");
                        params.append("<EndTime>" + recordItem.getEndTime() + "</EndTime>\r\n");
                        params.append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n");
                        params.append("<Type>" + recordItem.getType() + "</Type>\r\n");
                        if (!ObjectUtil.isEmpty(recordItem.getFileSize())) {
                            params.append("<FileSize>" + recordItem.getFileSize() + "</FileSize>\r\n");
                        }
                        if (!ObjectUtil.isEmpty(recordItem.getFilePath())) {
                            params.append("<FilePath>" + recordItem.getFilePath() + "</FilePath>\r\n");
                        }
                        params.append("</Item>\r\n");
                    }
                }
            }
            params.append("</RecordList>\r\n");
            params.append("</Response>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("responseRecordInfo ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> notifyMediaStatus(String callId) {
        log.info("notifyMediaStatus callId {}", callId);
        try {
            ClientInvite clientInvite = SipSessionManger.getInstance().getClientInvite(callId);
            if(clientInvite == null){
                throw new RuntimeException("clientInvite is null");
            }
            String platformId = clientInvite.getPlatformId();
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Notify>\r\n");
            params.append("<CmdType>MediaStatus</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + clientInvite.getChannelId() + "</DeviceID>\r\n");
            params.append("<NotifyType>121</NotifyType>\r\n");
            params.append("</Notify>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("notifyMediaStatus ex callId {}",callId, ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseBroadcast(String platformId, String deviceId, String channelId, String reqSn, String result) {
        log.info("responseBroadcast platformId {} deviceId {} channelId {} reqSn {} result {} ",
                platformId, deviceId,channelId,reqSn,reqSn);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            // 这里需要的DeviceID参数是下级设备对应的通道id
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Response>\r\n");
            params.append("<CmdType>Broadcast</CmdType>\r\n");
            params.append("<SN>" + reqSn + "</SN>\r\n");
            params.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
            params.append("<Result>" + result + "</Result>\r\n");
            params.append("</Response>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("responseBroadcast ex platformId{}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> responseDeviceControl(String platformId, String deviceId, String channelId, String reqSn, String result) {
        log.info("responseDeviceControl platformId {} deviceId {} channelId {} reqSn {} result {} ",
                platformId, deviceId,channelId,reqSn,reqSn);
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            String charset = platform.getCharset();
            // 这里需要的DeviceID参数是下级设备对应的通道id
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Response>\r\n");
            params.append("<CmdType>DeviceControl</CmdType>\r\n");
            params.append("<SN>" + reqSn + "</SN>\r\n");
            params.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
            params.append("<Result>" + result + "</Result>\r\n");
            params.append("</Response>\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        } catch (Exception ex) {
            log.info("responseDeviceControl ex platformId {}",platformId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> broadcast(String platformId, String deviceId, String channelId) {
        log.info("broadcast platformId {} deviceId {} channelId {}", platformId, deviceId,channelId);
        String ssrc = null;
        try {
            Platform platform = platformRepository.findByPlatformId(platformId);
            ServerInvite serverInvite = SipSessionManger.getInstance().getServerInvite(deviceId, platformId, MediaType.broadcast);
            if(serverInvite != null){
                return SipResult.success();
            }
            // 创建新的invite
            serverInvite = SipSessionManger.getInstance().createServerInvite(MediaType.broadcast);
            if(serverInvite == null){
                throw new RuntimeException("invite err");
            }
            // 获取这个广播对应的 clientInvite 需要下级成功进行广播
            ClientInvite clientInvite = SipSessionManger.getInstance().getClientInvite(deviceId,channelId, MediaType.broadcast);
            if(clientInvite == null){
                throw new RuntimeException("clientInvite is null");
            }
            serverInvite.getClientInvites().add(clientInvite);
            serverInvite.getMediaServer().addMediaClient(clientInvite.getMediaClient());
            clientInvite.setServerInvite(serverInvite);
            // 向上级发送广播Invite
            serverInvite.setPlatformId(platformId);
            serverInvite.setDeviceId(deviceId);
            serverInvite.setChannelId(channelId);
            serverInvite.setMediaTransport(MediaTransport.tcpPassive.name());
            ssrc = serverInvite.getSsrc();
            String mediaTransport = serverInvite.getMediaTransport();
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaType(MediaType.broadcast.name());
            mediaParam.setSsrc(ssrc);
            mediaParam.setMediaTransport(mediaTransport);
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new RuntimeException("media err");
            }
            clientInvite.getMediaClient().setMediaServer(mediaServer);
            serverInvite.setMediaServer(mediaServer);
            mediaServer.setSsrc(ssrc);
            // 拼接sdp发送参数
            String mediaIp = mediaServer.getMediaIp();
            int mediaPort = mediaServer.getMediaPort();
            //  passive tcp被动 设备向服务器推流 ，双向通道可以主动下发语音
            StringBuffer params = new StringBuffer(200);
            params.append("v=0\r\n");
            params.append("o=" + deviceId + " 0 0 IN IP4 " + mediaIp + "\r\n");
            params.append("s=Talk\r\n");
            params.append("c=IN IP4 " + mediaIp + "\r\n");
            params.append("t=0 0\r\n");
            params.append("m=audio " + mediaPort + " TCP/RTP/AVP 8\r\n");
            params.append("a=setup:passive\r\n");
            params.append("a=connection:new\r\n");
            params.append("a=sendrecv\r\n");
            params.append("a=rtpmap:8 PCMA/8000\r\n");
            params.append("y=" + ssrc + "\r\n");//ssrc
            // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
            params.append("f=v/////a/1/8/1" + "\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            serverInvite.setCallId(callId);
            mediaServer.setCallId(callId);
            sipParam.setSipId(platformId);
            sipParam.setSipIp(platform.getSipIp());
            sipParam.setSipPort(platform.getSipPort());
            sipParam.setSipTransport(platform.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipParam.setSsrc(ssrc);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendInviteRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("broadcast timeOut");
            }
            int code = responseEvent.getResponse().getStatusCode();
            String phrase = responseEvent.getResponse().getReasonPhrase();
            if(code != 200){
                throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
            }
            ssrc = serverInvite.getSsrc();
            boolean await = mediaServer.awaitMedia();
            if(!await){
                throw new RuntimeException("media timeOut");
            }
            return SipResult.success();
        }catch (Exception ex){
            log.error("broadcast ex platformId {} ",platformId,ex);
            SipResult<?> result = stopBroadcast(ssrc);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopBroadcast(String ssrc) {
        return deviceSend.stopServerInvite(ssrc);
    }

}
