package cn.gbtmedia.jtt808.server.cmd.session;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LFUCache;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xqs
 */
public class Jtt808SessionManager {

    private static final  Jtt808SessionManager SESSION_MANAGER = new Jtt808SessionManager();

    public static Jtt808SessionManager getInstance(){
        return SESSION_MANAGER;
    }

    //*************************************** 客户端连接缓存 *****************************************

    private static final Cache<String, ClientSession> CLIENT_SESSION_CACHE = new LFUCache<>(1000);

    public void putClientSession(ClientSession session) {
        CLIENT_SESSION_CACHE.put(session.getClientId(), session);
    }

    public ClientSession getClientSession(String clientId) {
        return CLIENT_SESSION_CACHE.get(clientId);
    }

    public void removeClientSession(String clientId) {
        CLIENT_SESSION_CACHE.remove(clientId);
    }

    public List<ClientSession> getAllClientSession() {
        List<ClientSession> clientSessions = new ArrayList<>();
        CLIENT_SESSION_CACHE.forEach(clientSessions::add);
        return clientSessions;
    }

    //************************************** 客户端播放缓存 *********************************************

    private static final Cache<String, ClientMedia> CLIENT_MEDIA_CACHE  = new LFUCache<>(1000);

    public void putClientMedia(ClientMedia media) {
        CLIENT_MEDIA_CACHE.put(media.getMediaKey(), media);
    }

    public ClientMedia getClientMedia(String mediaKey) {
        return CLIENT_MEDIA_CACHE.get(mediaKey);
    }

    public List<ClientMedia> getAllClientMedia() {
        List<ClientMedia> clientMedias  = new ArrayList<>();
        CLIENT_MEDIA_CACHE.forEach(clientMedias::add);
        return clientMedias;
    }

    public void removeClientMedia(ClientMedia media) {
        CLIENT_MEDIA_CACHE.remove(media.getMediaKey());
    }

}
