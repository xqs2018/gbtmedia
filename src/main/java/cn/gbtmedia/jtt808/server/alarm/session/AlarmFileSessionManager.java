package cn.gbtmedia.jtt808.server.alarm.session;

import cn.hutool.cache.Cache;
import cn.hutool.cache.impl.LFUCache;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xqs
 */
public class AlarmFileSessionManager {

    private static final AlarmFileSessionManager SESSION_MANAGER = new AlarmFileSessionManager();

    public static AlarmFileSessionManager getInstance(){
        return SESSION_MANAGER;
    }

    //*************************************** 客户端连接缓存 *****************************************

    private static final Cache<String, AlarmFileSession> CLIENT_SESSION_CACHE = new LFUCache<>(1000);

    public void putSession(AlarmFileSession session) {
        CLIENT_SESSION_CACHE.put(session.getClientId(), session);
    }

    public AlarmFileSession getSession(String clientId) {
        return CLIENT_SESSION_CACHE.get(clientId);
    }

    public void removeSession(String clientId) {
        CLIENT_SESSION_CACHE.remove(clientId);
    }

    public List<AlarmFileSession> getAlleSession() {
        List<AlarmFileSession> alarmFileSessions = new ArrayList<>();
        CLIENT_SESSION_CACHE.forEach(alarmFileSessions::add);
        return alarmFileSessions;
    }

    public AlarmFileSession getSessionBySocketAddress(InetSocketAddress socketAddress) {
        return getAlleSession().stream().filter(v->v.getSocketAddress().equals(socketAddress))
                .findFirst().orElse(null);
    }
}
