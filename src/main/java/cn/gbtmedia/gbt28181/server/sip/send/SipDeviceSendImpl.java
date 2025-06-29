package cn.gbtmedia.gbt28181.server.sip.send;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.aop.SyncWait;
import cn.gbtmedia.common.util.SpringUtil;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.server.media.MediaManger;
import cn.gbtmedia.gbt28181.server.media.MediaParam;
import cn.gbtmedia.gbt28181.server.media.MediaTransport;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.dto.BroadcastDto;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.dto.StreamDto;
import cn.gbtmedia.gbt28181.dto.TalkDto;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.receive.request.message.CatalogMessageHandler;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.server.sip.SipServer;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.gbtmedia.gbt28181.server.sip.util.SipParam;
import cn.gbtmedia.gbt28181.server.sip.util.SipUtil;
import cn.gbtmedia.gbt28181.server.sip.util.XmlUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.sip.ResponseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author xqs
 */
@Slf4j
@Component
public class SipDeviceSendImpl implements SipDeviceSend {

    @Resource
    private ServerConfig serverConfig;
    @Resource
    private SipServer sipServer;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private PlatformRepository platformRepository;

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<?> queryDeviceStatus(String deviceId) {
        log.info("queryDeviceStatus deviceId {}",deviceId);
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuilder params = new StringBuilder(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Query>\r\n");
            params.append("<CmdType>DeviceStatus</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
            params.append("</Query>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            sipParam.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("queryDeviceStatus future regist callId {} snKey {} ",callId, snKey);
            Future<Object> future = FutureContext.regist(callId, snKey);
            sipServer.sendMessageRequest(sipParam);
            Object data = future.get();
            if(data == null){
                throw new RuntimeException("queryDeviceStatus timeOut");
            }
            if(data instanceof ResponseEvent responseEvent){
                int code = responseEvent.getResponse().getStatusCode();
                String phrase = responseEvent.getResponse().getReasonPhrase();
                if(code != 200){
                    throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
                }
            }
            return SipResult.success();
        }catch (Exception ex){
            log.error("queryDeviceStatus ex deviceId{}", deviceId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<?> queryDeviceInfo(String deviceId) {
        log.info("queryDeviceInfo deviceId {}",deviceId);
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuilder params = new StringBuilder(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Query>\r\n");
            params.append("<CmdType>DeviceInfo</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
            params.append("</Query>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            sipParam.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("queryDeviceInfo future regist callId {} snKey {} ",callId, snKey);
            Future<Object> future = FutureContext.regist(callId, snKey);
            sipServer.sendMessageRequest(sipParam);
            Object data = future.get();
            if(data == null){
                throw new RuntimeException("queryDeviceInfo timeOut");
            }
            if(data instanceof ResponseEvent responseEvent){
                int code = responseEvent.getResponse().getStatusCode();
                String phrase = responseEvent.getResponse().getReasonPhrase();
                if(code != 200){
                    throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
                }
            }
            return SipResult.success();
        }catch (Exception ex){
            log.error("queryDeviceInfo ex deviceId {}", deviceId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<CatalogDto> queryCatalog(String deviceId) {
        log.info("queryCatalog deviceId {}",deviceId);
        // 直接返回正在查询的响应
        CatalogDto v = SpringUtil.getBean(CatalogMessageHandler.class).getResponseCatalog(deviceId);
        if(v != null){
            return SipResult.success(v);
        }
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Query>\r\n");
            params.append("<CmdType>Catalog</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
            params.append("</Query>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            sipParam.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("queryCatalog future regist callId {} snKey {} ",callId, snKey);
            Future<Object> future = FutureContext.regist(callId, snKey);
            sipServer.sendMessageRequest(sipParam);
            Object data = future.get();
            if(data == null){
                throw new RuntimeException("queryCatalog timeOut");
            }
            if(data instanceof ResponseEvent responseEvent){
                int code = responseEvent.getResponse().getStatusCode();
                String phrase = responseEvent.getResponse().getReasonPhrase();
                if(code != 200){
                    throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
                }
            }
            return SipResult.success((CatalogDto)data);
        }catch (Exception ex){
            log.error("queryCatalog ex deviceId {}", deviceId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<?> subscribeCatalog(String deviceId) {
        log.info("subscribeCatalog deviceId {}",deviceId);
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Query>\r\n");
            params.append("<CmdType>Catalog</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
            params.append("</Query>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            sipParam.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("subscribeCatalog future regist callId {} snKey {} ",callId, snKey);
            Future<Object> future = FutureContext.regist(callId, snKey);
            sipServer.sendSubscribeRequest(sipParam);
            Object data = future.get();
            if(data == null){
                throw new RuntimeException("subscribeCatalog timeOut");
            }
            if(data instanceof ResponseEvent responseEvent){
                int code = responseEvent.getResponse().getStatusCode();
                String phrase = responseEvent.getResponse().getReasonPhrase();
                if(code != 200){
                    throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
                }
            }
            return SipResult.success();
        }catch (Exception ex){
            log.error("subscribeCatalog ex deviceId {}", deviceId,ex);
            return SipResult.error(ex.getMessage());
        }
    }


    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<RecordDto> queryRecordInfo(String deviceId, String channelId, Date startTime, Date endTime) {
        log.info("queryRecordInfo deviceId {} channelId {} startTime {} endTime {}",deviceId, channelId,
                new DateTime(startTime),new DateTime(endTime));
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            String charset = device.getCharset();
            int secrecy = 0;
            String type = "all";
            // 默认时间范围，今天
            if (startTime == null) {
                startTime = new DateTime(new DateTime().toDateStr()).toJdkDate();
            }
            if (endTime == null) {
                endTime = new DateTime().toJdkDate();
            }
            String startTimeStr = DateUtil.date(startTime).toString("yyyy-MM-dd'T'HH:mm:ss");
            String endTimeStr = DateUtil.date(endTime).toString("yyyy-MM-dd'T'HH:mm:ss");
            String sn = SipUtil.getNewSn();
            StringBuffer params = new StringBuffer(200);
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Query>\r\n");
            params.append("<CmdType>RecordInfo</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
            params.append("<StartTime>" + startTimeStr + "</StartTime>\r\n");
            params.append("<EndTime>" + endTimeStr + "</EndTime>\r\n");
            params.append("<Secrecy> " + secrecy + " </Secrecy>\r\n");
            // 大华NVR要求必须增加一个值为all的文本元素节点Type
            params.append("<Type>" + type + "</Type>\r\n");
            params.append("</Query>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            sipParam.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            String snKey = device.getDeviceId() + "_" + sn;
            log.info("queryRecordInfo future regist callId {} snKey {} ",callId, snKey);
            Future<Object> future = FutureContext.regist(callId, snKey);
            sipServer.sendMessageRequest(sipParam);
            Object data = future.get();
            if(data == null){
                throw new RuntimeException("queryRecordInfo timeOut");
            }
            if(data instanceof ResponseEvent responseEvent){
                int code = responseEvent.getResponse().getStatusCode();
                String phrase = responseEvent.getResponse().getReasonPhrase();
                if(code != 200){
                    throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
                }
            }
            return SipResult.success((RecordDto) data);
        }catch (Exception ex){
            log.error("queryRecordInfo ex deviceId {} channelId {}",deviceId,channelId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<StreamDto> play(String deviceId, String channelId) {
        log.info("play deviceId {} channelId {} ",deviceId,channelId);
        String ssrc = null;
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            // 已经在播放了直接返回缓存
            ServerInvite invite = SipSessionManger.getInstance().getServerInvite(deviceId, channelId, MediaType.play);
            if(invite != null){
                StreamDto streamDto = new StreamDto();
                BeanUtil.copyProperties(invite,streamDto);
                return SipResult.success(streamDto);
            }
            // 创建新的invite
            invite = SipSessionManger.getInstance().createServerInvite(MediaType.play);
            if(invite == null){
                throw new RuntimeException("invite err");
            }
            invite.setDeviceId(deviceId);
            invite.setChannelId(channelId);
            invite.setMediaTransport(device.getMediaTransport());
            ssrc = invite.getSsrc();
            String mediaTransport = invite.getMediaTransport();
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setSsrc(ssrc);
            mediaParam.setMediaType(MediaType.play.name());
            mediaParam.setMediaTransport(mediaTransport);
            // 判断是否需要云端录像
            DeviceChannel deviceChannel = deviceChannelRepository.findByDeviceIdAndChannelId(deviceId, channelId);
            if(deviceChannel.getCloudRecord()!= null && deviceChannel.getCloudRecord() ==  1){
                String recordPathCloud = serverConfig.getGbt28181().getRecordPathCloud();
                String a = invite.getDeviceId();
                String b = invite.getChannelId();
                recordPathCloud = recordPathCloud + "/" + (a + "_" +  b);
                // 创建完整的目录
                FileUtil.mkdir(recordPathCloud);
                mediaParam.setRecordPath(recordPathCloud);
                mediaParam.setRecordSecond(ServerConfig.getInstance().getGbt28181().getRecordSecond());
                mediaParam.setRecordSlice(true);
            }
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new RuntimeException("media err");
            }
            invite.setMediaServer(mediaServer);
            invite.setHttpFlv(mediaServer.getHttpFlv());
            mediaServer.setSsrc(ssrc);
            // 拼接sdp发送参数
            String mediaIp = mediaServer.getMediaIp();
            int mediaPort = mediaServer.getMediaPort();
            invite.setMediaIp(mediaIp);
            invite.setMediaPort(mediaPort);
            boolean tcp = mediaTransport.startsWith("tcp");
            boolean active = mediaTransport.equals(MediaTransport.tcpActive.name());
            StringBuffer params = new StringBuffer(200);
            params.append("v=0\r\n");
            params.append("o=" + channelId + " 0 0 IN IP4 " + mediaIp + "\r\n");
            params.append("s=Play\r\n");
            params.append("c=IN IP4 " + mediaIp + "\r\n");
            params.append("t=0 0\r\n");
            params.append("m=video " + mediaPort + " "+ (tcp ?"TCP/":"")+ "RTP/AVP 96 97 98 99\r\n");
            params.append("a=recvonly\r\n");
            params.append("a=rtpmap:96 PS/90000\r\n");
            params.append("a=rtpmap:98 H264/90000\r\n");
            params.append("a=rtpmap:97 MPEG4/90000\r\n");
            params.append("a=rtpmap:99 H265/90000\r\n");
            if(tcp){
                params.append("a=setup:"+(active?"active\r\n":"passive\r\n"));
                params.append("a=connection:new\r\n");
            }
            params.append("y="+ssrc+"\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            invite.setCallId(callId);
            mediaServer.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setChannelId(channelId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipParam.setSsrc(ssrc);
            log.info("play ssrc {} mediaTransport {} mediaIp {} mediaPort {} callId {} ",
                    ssrc, mediaServer.getMediaTransport(), mediaServer.getMediaIp(),mediaServer.getMediaPort(), callId);
            log.info("play future regist callId {} ",callId);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendInviteRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("play timeOut");
            }
            int code = responseEvent.getResponse().getStatusCode();
            String phrase = responseEvent.getResponse().getReasonPhrase();
            if(code != 200){
                throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
            }
            ssrc = invite.getSsrc();
            boolean await = mediaServer.awaitStream();
            if(!await){
                throw new RuntimeException("stream timeOut");
            }
            StreamDto streamDto = new StreamDto();
            BeanUtil.copyProperties(invite,streamDto);
            return SipResult.success(streamDto);
        }catch (Exception ex){
            log.error("play ex deviceId {} channelId {}",deviceId, channelId,ex);
            SipResult<?> result = stopPlay(ssrc);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopPlay(String ssrc) {
        return stopServerInvite(ssrc);
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<StreamDto> playback(String deviceId, String channelId, Date startTime, Date endTime) {
        log.info("playback deviceId {} channelId {} startTime {} endTime {}", deviceId,channelId
                ,new DateTime(startTime),new DateTime(endTime));
        String ssrc = null;
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            // 最多几路同时回放，超出停止没有人观看的，或者时间最早的
            List<ServerInvite> inviteList = SipSessionManger.getInstance().getServerInviteList(deviceId,MediaType.playback);
            Integer maxPlaybackStream = device.getMaxPlaybackStream();
            if(inviteList != null && maxPlaybackStream != null && inviteList.size() >= maxPlaybackStream){
                log.warn("playback maxPlaybackStream {}",maxPlaybackStream);
                inviteList = new ArrayList<>(inviteList);
                inviteList.sort(Comparator.comparing(ServerInvite::getCreateTime));
                ServerInvite stop = inviteList.stream()
                        .filter(v -> v.getMediaServer().getViewNum() == 0).findFirst().orElse(inviteList.get(0));
                stopPlayback(stop.getSsrc());
            }
            // 创建新的invite
            ServerInvite invite = SipSessionManger.getInstance().createServerInvite(MediaType.playback);
            if(invite == null){
                throw new RuntimeException("invite err");
            }
            invite.setDeviceId(deviceId);
            invite.setChannelId(channelId);
            invite.setMediaTransport(device.getMediaTransport());
            invite.setStartTime(startTime);
            invite.setEndTime(endTime);
            ssrc = invite.getSsrc();
            String mediaTransport = invite.getMediaTransport();
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaType(MediaType.playback.name());
            mediaParam.setSsrc(ssrc);
            mediaParam.setMediaTransport(mediaTransport);
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new RuntimeException("media err");
            }
            invite.setHttpFlv(mediaServer.getHttpFlv());
            invite.setMediaServer(mediaServer);
            mediaServer.setSsrc(ssrc);
            invite.setMediaServer(mediaServer);
            // 拼接sdp发送参数
            String mediaIp = mediaServer.getMediaIp();
            int mediaPort = mediaServer.getMediaPort();
            invite.setMediaIp(mediaIp);
            invite.setMediaPort(mediaPort);
            boolean tcp = invite.getMediaTransport().startsWith("tcp");
            boolean active = invite.getMediaTransport().equals(MediaTransport.tcpActive.name());
            StringBuffer params = new StringBuffer(200);
            params.append("v=0\r\n");
            params.append("o=" + channelId + " 0 0 IN IP4 " + mediaIp + "\r\n");
            params.append("s=Playback\r\n");
            params.append("u=" + channelId + ":0\r\n");
            params.append("c=IN IP4 " + mediaIp + "\r\n");
            params.append("t=" + startTime.getTime()/1000 + " " + endTime.getTime()/1000 + "\r\n");
            params.append("m=video " + mediaPort + " "+ (tcp ?"TCP/":"")+ "RTP/AVP 96 97 98 99\r\n");
            params.append("a=recvonly\r\n");
            params.append("a=rtpmap:96 PS/90000\r\n");
            params.append("a=rtpmap:98 H264/90000\r\n");
            params.append("a=rtpmap:97 MPEG4/90000\r\n");
            params.append("a=rtpmap:99 H265/90000\r\n");
            if(tcp){
                params.append("a=setup:"+(active?"active\r\n":"passive\r\n"));
                params.append("a=connection:new\r\n");
            }
            params.append("y=" + ssrc + "\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            invite.setCallId(callId);
            mediaServer.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setChannelId(channelId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipParam.setSsrc(ssrc);
            log.info("playback ssrc {} mediaTransport {} mediaIp {} mediaPort {} callId {} ",
                    ssrc, mediaServer.getMediaTransport(), mediaServer.getMediaIp(),mediaServer.getMediaPort(), callId);
            log.info("playback future regist callId {} ",callId);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendInviteRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("playback timeOut");
            }
            int code = responseEvent.getResponse().getStatusCode();
            String phrase = responseEvent.getResponse().getReasonPhrase();
            if(code != 200){
                throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
            }
            ssrc = invite.getSsrc();
            boolean await = mediaServer.awaitStream();
            if(!await){
                throw new RuntimeException("stream timeOut");
            }
            StreamDto streamDto = new StreamDto();
            BeanUtil.copyProperties(invite,streamDto);
            return SipResult.success(streamDto);
        }catch (Exception ex){
            log.error("playback ex deviceId {} channelId {}",deviceId, channelId,ex);
            SipResult<?> result = stopPlayback(ssrc);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> playbackSpeed(String ssrc, double speed) {
        log.info("playbackSpeed ssrc {} speed {}", ssrc,speed);
        try {
            ServerInvite serverInvite = SipSessionManger.getInstance().getServerInvite(ssrc);
            if(serverInvite == null){
                return SipResult.success();
            }
            StringBuffer params = new StringBuffer(200);
            params.append("PLAY RTSP/1.0\r\n");
            params.append("CSeq: " + (int) ((Math.random() * 9 + 1) * Math.pow(10, 8)) + "\r\n");
            params.append("Scale: " + String.format("%.6f", speed) + "\r\n");
            SipParam sipParam = new SipParam();
            sipParam.setCallId(serverInvite.getCallId());
            sipParam.setChannelId(serverInvite.getChannelId());
            sipParam.setFromTag(serverInvite.getFromTag());
            sipParam.setToTag(serverInvite.getToTag());
            sipParam.setViaBranch(serverInvite.getViaBranch());
            Device device = deviceRepository.findByDeviceId(serverInvite.getDeviceId());
            sipParam.setSipId(device.getDeviceId());
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendInfoRequest(sipParam);
            return SipResult.success();
        }catch (Exception ex){
            log.info("playbackSpeed ex ssrc {} speed {}", ssrc,speed,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopPlayback(String ssrc) {
        return stopServerInvite(ssrc);
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<StreamDto> download(String deviceId, String channelId, Date startTime, Date endTime, int downloadSpeed) {
        log.info("download deviceId {} channelId {} startTime {} endTime {} downloadSpeed {}",
                deviceId,channelId,new DateTime(startTime),new DateTime(endTime),downloadSpeed);
        String ssrc = null;
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            // 最多几路同时下载，超出停止时间最早的
            List<ServerInvite> inviteList = SipSessionManger.getInstance().getServerInviteList(deviceId,MediaType.download);
            Integer maxDownloadStream = device.getMaxDownloadStream();
            if(inviteList != null && maxDownloadStream != null && inviteList.size() >= maxDownloadStream){
                log.warn("download maxDownloadStream {}",maxDownloadStream);
                inviteList.sort(Comparator.comparing(ServerInvite::getCreateTime));
                ServerInvite stop = inviteList.get(0);
                stopDownload(stop.getSsrc());
            }
            if(inviteList != null && inviteList.stream().anyMatch(v -> v.getStartTime().getTime() == startTime.getTime()
                    && v.getEndTime().getTime() == endTime.getTime())){
                throw new RuntimeException("当前时间段存在下载任务");
            }
            // 创建新的invite
            ServerInvite invite = SipSessionManger.getInstance().createServerInvite(MediaType.download);
            if(invite == null){
                throw new RuntimeException("invite err");
            }
            invite.setDeviceId(deviceId);
            invite.setChannelId(channelId);
            invite.setMediaTransport(device.getMediaTransport());
            invite.setStartTime(startTime);
            invite.setEndTime(endTime);
            invite.setDownloadSpeed(downloadSpeed);
            ssrc = invite.getSsrc();
            String mediaTransport = invite.getMediaTransport();
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaType(MediaType.download.name());
            mediaParam.setSsrc(ssrc);
            mediaParam.setMediaTransport(mediaTransport);
            String recordPathDevice = serverConfig.getGbt28181().getRecordPathDevice();
            String a = invite.getDeviceId();
            String b = invite.getChannelId();
            String c = new DateTime(invite.getStartTime()).toString("yyyyMMddHHmmss");
            String d = new DateTime(invite.getEndTime()).toString("yyyyMMddHHmmss");
            recordPathDevice = recordPathDevice + "/" + (a + "_" +  b + "_" +  c + "_" +  d );
            // 创建完整的目录 ，先删除存储目录
            FileUtil.del(recordPathDevice);
            FileUtil.mkdir(recordPathDevice);
            mediaParam.setRecordPath(recordPathDevice);
            mediaParam.setRecordSecond((endTime.getTime() - startTime.getTime())/1000);
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new RuntimeException("media err");
            }
            invite.setHttpFlv(mediaServer.getHttpFlv());
            invite.setMediaServer(mediaServer);
            mediaServer.setSsrc(ssrc);
            invite.setMediaServer(mediaServer);
            // 拼接sdp发送参数
            String mediaIp = mediaServer.getMediaIp();
            int mediaPort = mediaServer.getMediaPort();
            invite.setMediaIp(mediaIp);
            invite.setMediaPort(mediaPort);
            boolean tcp = invite.getMediaTransport().startsWith("tcp");
            boolean active = invite.getMediaTransport().equals(MediaTransport.tcpActive.name());
            StringBuffer params = new StringBuffer(200);
            params.append("v=0\r\n");
            params.append("o=" + channelId + " 0 0 IN IP4 " + mediaIp + "\r\n");
            params.append("s=Download\r\n");
            params.append("u=" + channelId + ":0\r\n");
            params.append("c=IN IP4 " + mediaIp + "\r\n");
            params.append("t=" + startTime.getTime()/1000 + " " + endTime.getTime()/1000 + "\r\n");
            params.append("m=video " + mediaPort + " "+ (tcp ?"TCP/":"")+ "RTP/AVP 96 97 98 99\r\n");
            params.append("a=recvonly\r\n");
            params.append("a=rtpmap:96 PS/90000\r\n");
            params.append("a=rtpmap:98 H264/90000\r\n");
            params.append("a=rtpmap:97 MPEG4/90000\r\n");
            params.append("a=rtpmap:99 H265/90000\r\n");
            if(tcp){
                params.append("a=setup:"+(active?"active\r\n":"passive\r\n"));
                params.append("a=connection:new\r\n");
            }
            params.append("a=downloadspeed:" + downloadSpeed + "\r\n");
            params.append("y=" + ssrc + "\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            String callId = IdUtil.fastSimpleUUID();
            invite.setCallId(callId);
            mediaServer.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setChannelId(channelId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipParam.setSsrc(ssrc);
            log.info("download ssrc {} mediaTransport {} mediaIp {} mediaPort {} callId {} ",
                    ssrc, mediaServer.getMediaTransport(), mediaServer.getMediaIp(),mediaServer.getMediaPort(), callId);
            log.info("download future regist callId {} ",callId);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendInviteRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("download timeOut");
            }
            int code = responseEvent.getResponse().getStatusCode();
            String phrase = responseEvent.getResponse().getReasonPhrase();
            if(code != 200){
                throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
            }
            ssrc = invite.getSsrc();
            boolean await = mediaServer.awaitStream();
            if(!await){
                throw new RuntimeException("stream timeOut");
            }
            StreamDto streamDto = new StreamDto();
            BeanUtil.copyProperties(invite,streamDto);
            return SipResult.success(streamDto);
        }catch (Exception ex){
            log.error("download ex deviceId {} channelId {}",deviceId, channelId,ex);
            SipResult<?> result = stopDownload(ssrc);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopDownload(String ssrc) {
        return stopServerInvite(ssrc);
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<TalkDto> talk(String deviceId, String channelId) {
        log.info("talk deviceId {} channelId {} ", deviceId,channelId);
        String ssrc = null;
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            ServerInvite invite = SipSessionManger.getInstance().getServerInvite(deviceId, channelId, MediaType.talk);
            if(invite != null){
                TalkDto talkDto = new TalkDto();
                BeanUtil.copyProperties(invite,talkDto);
                ServerConfig serverConfig = ServerConfig.getInstance();
                String serverIp = serverConfig.getPublicIp();
                int wsPort = serverConfig.getGbt28181().getWsPort();
                talkDto.setHttpWs(String.format("ws://%s:%s/chat/talk_%s",serverIp,wsPort,invite.getCallId()));
                return SipResult.success(talkDto);
            }
            // 创建新的invite
            invite = SipSessionManger.getInstance().createServerInvite(MediaType.talk);
            if(invite == null){
                throw new RuntimeException("invite err");
            }
            invite.setDeviceId(deviceId);
            invite.setChannelId(channelId);
            // 对讲只能使用tcpPassive
            invite.setMediaTransport(MediaTransport.tcpPassive.name());
            ssrc = invite.getSsrc();
            String mediaTransport = invite.getMediaTransport();
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaType(MediaType.talk.name());
            mediaParam.setSsrc(ssrc);
            mediaParam.setMediaTransport(mediaTransport);
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new RuntimeException("media err");
            }
            invite.setHttpFlv(mediaServer.getHttpFlv());
            invite.setMediaServer(mediaServer);
            mediaServer.setSsrc(ssrc);
            invite.setMediaServer(mediaServer);
            // 拼接sdp发送参数
            String mediaIp = mediaServer.getMediaIp();
            int mediaPort = mediaServer.getMediaPort();
            invite.setMediaIp(mediaIp);
            invite.setMediaPort(mediaPort);
            //  passive tcp被动 设备向服务器推流 ，双向通道可以主动下发语音
            StringBuffer params = new StringBuffer(200);
            params.append("v=0\r\n");
            params.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + mediaIp + "\r\n");
            params.append("s=Talk\r\n");
            params.append("c=IN IP4 " + mediaIp + "\r\n");
            params.append("t=0 0\r\n");
            params.append("m=audio " + mediaPort + " TCP/RTP/AVP 8\r\n");
            // 设备会把音视频一起发来
            //params.append("m=video " + mediaPort + " TCP/RTP/AVP 96 97 98 99\r\n");
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
            invite.setCallId(callId);
            mediaServer.setCallId(callId);
            sipParam.setSipId(deviceId);
            sipParam.setChannelId(channelId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipParam.setCallId(callId);
            sipParam.setSsrc(ssrc);
            log.info("talk ssrc {} mediaTransport {} mediaIp {} mediaPort {} callId {} ",
                    ssrc, mediaServer.getMediaTransport(), mediaServer.getMediaIp(),mediaServer.getMediaPort(), callId);
            log.info("talk future regist callId {} ",callId);
            Future<Object> future = FutureContext.regist(callId);
            sipServer.sendInviteRequest(sipParam);
            ResponseEvent responseEvent = (ResponseEvent) future.get();
            if(responseEvent == null){
                throw new RuntimeException("talk timeOut");
            }
            int code = responseEvent.getResponse().getStatusCode();
            String phrase = responseEvent.getResponse().getReasonPhrase();
            if(code != 200){
                throw new RuntimeException(String.format("code %s phrase %s",code,phrase));
            }
            ssrc = invite.getSsrc();
            // TODO awaitMedia
            // StreamTask只有存在视频才解码出来
            boolean await = mediaServer.awaitMedia();
            if(!await){
                throw new RuntimeException("media timeOut");
            }
            TalkDto talkDto = new TalkDto();
            BeanUtil.copyProperties(invite,talkDto);
            ServerConfig serverConfig = ServerConfig.getInstance();
            String serverIp = serverConfig.getPublicIp();
            int wsPort = serverConfig.getGbt28181().getWsPort();
            talkDto.setHttpWs(String.format("ws://%s:%s/chat/talk_%s",serverIp,wsPort,invite.getCallId()));
            return SipResult.success(talkDto);
        }catch (Exception ex){
            log.error("talk ex deviceId {} channelId {}",deviceId, channelId,ex);
            SipResult<?> result = stopTalk(ssrc);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopTalk(String ssrc) {
        return stopServerInvite(ssrc);
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<BroadcastDto> broadcast(String deviceId, String channelId) {
        log.info("broadcast deviceId {} channelId {}",deviceId,channelId);
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            ClientInvite invite = SipSessionManger.getInstance().getClientInvite(deviceId,channelId, MediaType.broadcast);
            if(invite != null){
                BroadcastDto broadcastDto = new BroadcastDto();
                BeanUtil.copyProperties(invite,broadcastDto);
                ServerConfig serverConfig = ServerConfig.getInstance();
                String serverIp = serverConfig.getPublicIp();
                int wsPort = serverConfig.getGbt28181().getWsPort();
                broadcastDto.setHttpWs(String.format("ws://%s:%s/chat/broadcast_%s",serverIp,wsPort,invite.getCallId()));
                return SipResult.success(broadcastDto);
            }
            StringBuffer params = new StringBuffer(200);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Notify>\r\n");
            params.append("<CmdType>Broadcast</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<SourceID>" + serverConfig.getGbt28181().getSipId() + "</SourceID>\r\n");
            params.append("<TargetID>" + channelId + "</TargetID>\r\n");
            params.append("</Notify>\r\n");
            // 创建发送任务
            String callBackKey = "broadcast_" + deviceId + "_" + channelId;
            SipParam sipParam = new SipParam();
            sipParam.setSipId(deviceId);
            sipParam.setChannelId(channelId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            log.info("broadcast future regist key {} ",callBackKey);
            Future<Object> future = FutureContext.regist(callBackKey);
            sipServer.sendMessageRequest(sipParam);
            invite = (ClientInvite) future.get();
            if(invite == null){
                throw new RuntimeException("broadcast timeOut");
            }
            BroadcastDto broadcastDto = new BroadcastDto();
            BeanUtil.copyProperties(invite,broadcastDto);
            ServerConfig serverConfig = ServerConfig.getInstance();
            String serverIp = serverConfig.getPublicIp();
            int wsPort = serverConfig.getGbt28181().getWsPort();
            broadcastDto.setHttpWs(String.format("ws://%s:%s/chat/broadcast_%s",serverIp,wsPort,invite.getCallId()));
            return SipResult.success(broadcastDto);
        }catch (Exception ex){
            log.error("broadcast ex deviceId {} channelId {}",deviceId, channelId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> stopBroadcast(String callId) {
        return stopClientInvite(callId);
    }

    @Override
    @SyncWait(key = "#ssrc")
    public SipResult<?> stopServerInvite(String ssrc) {
        log.info("stopServerInvite ssrc {} ", ssrc);
        try {
            ServerInvite serverInvite = SipSessionManger.getInstance().getServerInvite(ssrc);
            if(serverInvite == null){
                return SipResult.success();
            }
            String deviceId = serverInvite.getDeviceId();
            String platformId = serverInvite.getPlatformId();
            List<ClientInvite> clientInvites = serverInvite.getClientInvites();
            // 广播特殊，需要先关闭级联下级的，不然会造成永远关不了
            if(serverInvite.getMediaType().equals(MediaType.broadcast) && !clientInvites.isEmpty()){
                SipResult<?> result = stopBroadcast(clientInvites.get(0).getCallId());
            }
            // 存在级联不允许关闭
            if(ObjectUtil.isNotEmpty(clientInvites)){
                log.error("not stop has clientInvites {}",ssrc);
                return SipResult.error("not stop has clientInvites");
            }
            // 发送信令
            if(serverInvite.isInviteAck() && !serverInvite.isSendBye()){
                serverInvite.setSendBye(true);
                SipParam sipParam = new SipParam();
                sipParam.setCallId(serverInvite.getCallId());
                sipParam.setChannelId(serverInvite.getChannelId());
                sipParam.setFromTag(serverInvite.getFromTag());
                sipParam.setToTag(serverInvite.getToTag());
                sipParam.setViaBranch(serverInvite.getViaBranch());
                try {
                    // 广播特殊，对应的sip信息应该是上级的
                    if(serverInvite.getMediaType().equals(MediaType.broadcast)){
                        Platform platform = platformRepository.findByPlatformId(platformId);
                        sipParam.setSipId(platform.getPlatformId());
                        sipParam.setSipIp(platform.getSipIp());
                        sipParam.setSipPort(platform.getSipPort());
                        sipParam.setSipTransport(platform.getSipTransport());
                    }else {
                        Device device = deviceRepository.findByDeviceId(deviceId);
                        sipParam.setSipId(device.getDeviceId());
                        sipParam.setSipIp(device.getSipIp());
                        sipParam.setSipPort(device.getSipPort());
                        sipParam.setSipTransport(device.getSipTransport());
                    }
                    log.info("sendByeRequest ssrc {} callId {}",ssrc,sipParam.getCallId());
                    sipServer.sendByeRequest(sipParam);
                }catch (Exception ex){
                    log.error("sendByeRequest ex",ex);
                }
            }
            // 关闭流媒体
            MediaServer mediaServer = serverInvite.getMediaServer();
            if(mediaServer != null){
                mediaServer.stop();
            }
            // 移除缓存
            log.info("removeServerInvite ssrc {}",serverInvite.getSsrc());
            SipSessionManger.getInstance().removeServerInvite(serverInvite);
            return SipResult.success();
        }catch (Exception ex){
            log.error("stopServerInvite ex ssrc {}",ssrc,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    @SyncWait(key = "#callId")
    public SipResult<?> stopClientInvite(String callId) {
        log.info("stopClientInvite callId {} ", callId);
        try {
            ClientInvite clientInvite = SipSessionManger.getInstance().getClientInvite(callId);
            if(clientInvite == null){
                return SipResult.success();
            }
            // 发送信令
            if(clientInvite.isInviteAck() && !clientInvite.isSendBye()){
                clientInvite.setSendBye(true);
                SipParam sipParam = new SipParam();
                sipParam.setCallId(clientInvite.getCallId());
                sipParam.setChannelId(clientInvite.getChannelId());
                sipParam.setFromTag(clientInvite.getToTag());
                sipParam.setToTag(clientInvite.getFromTag());
                sipParam.setViaBranch(clientInvite.getViaBranch());
                try {
                    // 广播特殊，对应的sip信息应该是下级的
                    // TODO 模拟器广播发送bye无效
                    if(clientInvite.getMediaType().equals(MediaType.broadcast)){
                        Device device = deviceRepository.findByDeviceId(clientInvite.getDeviceId());
                        sipParam.setSipId(device.getDeviceId());
                        sipParam.setSipIp(device.getSipIp());
                        sipParam.setSipPort(device.getSipPort());
                        sipParam.setSipTransport(device.getSipTransport());
                    }else {
                        Platform platform = platformRepository.findByPlatformId(clientInvite.getPlatformId());
                        sipParam.setSipId(platform.getPlatformId());
                        sipParam.setSipIp(platform.getSipIp());
                        sipParam.setSipPort(platform.getSipPort());
                        sipParam.setSipTransport(platform.getSipTransport());
                    }
                    log.info("sendByeRequest callId {} ",callId);
                    sipServer.sendByeRequest(sipParam);
                }catch (Exception ex){
                    log.error("sendByeRequest ex",ex);
                }
            }
            // 关闭流媒体
            MediaClient mediaClient = clientInvite.getMediaClient();
            if(mediaClient != null){
                mediaClient.stop();
            }
            // 移除缓存
            log.info("removeClientInvite callId {}",clientInvite.getCallId());
            SipSessionManger.getInstance().removeClientInvite(clientInvite);
            // 关闭级联的推流
            ServerInvite serverInvite = clientInvite.getServerInvite();
            if(serverInvite == null){
                return SipResult.success();
            }
            // 移除关联的invite
            List<ClientInvite> clientInvites = serverInvite.getClientInvites();
            clientInvites.removeIf(s -> callId.equals(s.getCallId()));
            // 移除关联流转推
            MediaServer mediaServer = serverInvite.getMediaServer();
            mediaServer.removeMediaClient(mediaClient);
            // 关闭本级级联
            if(clientInvites.isEmpty()&&mediaServer.getViewNum()==0){
                stopServerInvite(serverInvite.getSsrc());
            }
            return SipResult.success();
        }catch (Exception ex){
            log.error("stopClientInvite ex callId {}",callId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

    @Override
    public SipResult<?> controlPtzCmd(String deviceId, String channelId, int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
        String ptzCmdStr = XmlUtil.cmdString(leftRight,upDown,inOut,moveSpeed,zoomSpeed);
        return controlPtzCmd(deviceId,channelId,ptzCmdStr);
    }

    @Override
    @SyncWait(key = "#deviceId")
    public SipResult<?> controlPtzCmd(String deviceId, String channelId, String ptzCmdStr) {
        log.info("controlPtzCmd deviceId {} channelId {} ptzCmdStr {}", deviceId,channelId,ptzCmdStr);
        try {
            Device device = deviceRepository.findByDeviceId(deviceId);
            StringBuffer params = new StringBuffer(200);
            String charset = device.getCharset();
            String sn = SipUtil.getNewSn();
            params.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
            params.append("<Control>\r\n");
            params.append("<CmdType>DeviceControl</CmdType>\r\n");
            params.append("<SN>" + sn + "</SN>\r\n");
            params.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
            params.append("<PTZCmd>" + ptzCmdStr + "</PTZCmd>\r\n");
            params.append("<Info>\r\n");
            params.append("<ControlPriority>5</ControlPriority>\r\n");
            params.append("</Info>\r\n");
            params.append("</Control>\r\n");
            // 创建发送任务
            SipParam sipParam = new SipParam();
            sipParam.setSipId(deviceId);
            sipParam.setSipIp(device.getSipIp());
            sipParam.setSipPort(device.getSipPort());
            sipParam.setSipTransport(device.getSipTransport());
            sipParam.setContent(params.toString());
            sipServer.sendMessageRequest(sipParam);
            return SipResult.success();
        }catch (Exception ex){
            log.error("controlPtzCmd ex deviceId {} channelId {}", deviceId,channelId,ex);
            return SipResult.error(ex.getMessage());
        }
    }

}
