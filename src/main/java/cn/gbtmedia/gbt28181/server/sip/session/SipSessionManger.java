package cn.gbtmedia.gbt28181.server.sip.session;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LFUCache;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xqs
 */
@Slf4j
public class SipSessionManger {

    private static final SipSessionManger SIP_SESSION_MANGER = new SipSessionManger();

    public static SipSessionManger getInstance(){
        return SIP_SESSION_MANGER;
    }

    //********************************************************************************************************
    //************************************** 服务端发起的invite使用缓存 ****************************************

    /**
     * ssrc为key，确保每个不重复
     */
    private static final Cache<String, ServerInvite> SERVER_INVITE_CACHE  = new LFUCache<>(1000);

    /**
     * 十进制整数字符串，标识SSRC值。其中第一位为历史或者实时媒体流的标识位，0为实时，1为历史
     * 第2位到第6位取20位SIP监控域ID之中的4-8位作为域标识；
     * 第7-10位作为域内媒体流标识，是一个与当前域内产生的媒体流SSRC值后4位不重复的四位十进制整数；
     */
    private String getSsrc(String type){
        String sipDomain = ServerConfig.getInstance().getGbt28181().getSipDomain().substring(3, 8);
        String ssrc = null;
        for(int i = 1; i < 10000; i++){
            ssrc = String.format("%s%s%04d", type ,sipDomain,i);
            if(SERVER_INVITE_CACHE.containsKey(ssrc)){
                ssrc = null;
                continue;
            }
            break;
        }
        return ssrc;
    }

    public synchronized ServerInvite createServerInvite(MediaType type){
        String ssrc;
        if(type.equals(MediaType.playback) || type.equals(MediaType.download)){
            ssrc = getSsrc("1");
        }else {
            ssrc = getSsrc("0");
        }
        if(ssrc == null){
            log.error("ssrc not enough");
            return null;
        }
        ServerInvite invite = new ServerInvite();
        invite.setMediaType(type);
        invite.setSsrc(ssrc);
        SERVER_INVITE_CACHE.put(ssrc,invite);
        return invite;
    }

    public void putServerInvite(ServerInvite invite){
        SERVER_INVITE_CACHE.put(invite.getSsrc(), invite);
    }

    public ServerInvite getServerInvite(String ssrc) {
        if(ssrc == null){
            return null;
        }
        return SERVER_INVITE_CACHE.get(ssrc);
    }

    public List<ServerInvite> getAllServerInvite() {
        List<ServerInvite> serverInvites = new ArrayList<>();
        SERVER_INVITE_CACHE.forEach(serverInvites::add);
        return serverInvites;
    }

    public ServerInvite getServerInviteByCallId(String callId) {
        return getAllServerInvite().stream()
                .filter(v->v.getCallId().equals(callId))
                .findFirst().orElse(null);
    }

    public boolean resetServerInviteSsrc(ServerInvite invite, String newSsrc) {
        if(SERVER_INVITE_CACHE.containsKey(newSsrc)){
            return false;
        }
        SERVER_INVITE_CACHE.remove(invite.getSsrc());
        SERVER_INVITE_CACHE.put(newSsrc,invite);
        invite.setSsrc(newSsrc);
        return true;
    }

    public void removeServerInvite(ServerInvite invite) {
        if(invite != null){
            SERVER_INVITE_CACHE.remove(invite.getSsrc());
        }
    }

    public ServerInvite getServerInvite(String deviceId, String channelId, MediaType mediaType) {
        return  getAllServerInvite().stream()
                .filter(v->v.getDeviceId().equals(deviceId) && v.getChannelId().equals(channelId)
                        && v.getMediaType().equals(mediaType))
                .findFirst().orElse(null);
    }

    public List<ServerInvite> getServerInviteList(String deviceId, String channelId, MediaType mediaType) {
        return  getAllServerInvite().stream()
                .filter(v->v.getDeviceId().equals(deviceId) && v.getChannelId().equals(channelId)
                        && v.getMediaType().equals(mediaType))
                .collect(Collectors.toList());
    }

    public List<ServerInvite> getServerInviteList(String deviceId, MediaType mediaType) {
        return  getAllServerInvite().stream()
                .filter(v->v.getDeviceId().equals(deviceId)
                        && v.getMediaType().equals(mediaType))
                .collect(Collectors.toList());
    }


    //********************************************************************************************************
    //************************************** 客户端发起的invite使用缓存 ****************************************

    /**
     * callId为key,客户发起携带的ssrc无法判断重复
     */
    private static final Cache<String, ClientInvite> CLIENT_INVITE_CACHE  = new LFUCache<>(1000);

    public void putClientInvite(ClientInvite invite){
        CLIENT_INVITE_CACHE.put(invite.getCallId(), invite);
    }

    public ClientInvite getClientInvite(String callId) {
        if(callId == null){
            return null;
        }
        return CLIENT_INVITE_CACHE.get(callId);
    }

    public List<ClientInvite> getAllClientInvite() {
        List<ClientInvite> clientInvites = new ArrayList<>();
        CLIENT_INVITE_CACHE.forEach(clientInvites::add);
        return clientInvites;
    }

    public ClientInvite getClientInvite(String deviceId, String channelId, MediaType mediaType) {
        return  getAllClientInvite().stream()
                .filter(v->v.getDeviceId().equals(deviceId) && v.getChannelId().equals(channelId)
                        && v.getMediaType().equals(mediaType))
                .findFirst().orElse(null);
    }

    public void removeClientInvite(ClientInvite invite){
        CLIENT_INVITE_CACHE.remove(invite.getCallId());
    }
}
