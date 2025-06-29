package cn.gbtmedia.zlmediakit;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * @author xqs
 */
@Slf4j
@Component
@ConditionalOnBean(ZlmediakitConfig.class)
public class ZlmediakitApi implements InitializingBean {

    @Resource
    private ZlmediakitConfig config;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("zlmediakit init");
        JSONObject params = new JSONObject();
        params.set("rtp_proxy.port",config.getRtpProxyPort());
        params.set("rtp_proxy.port_range",config.getRtpProxyPortRange());
        params.set("record.mp4_as_player","1");
        params.set("hook.enable","1");
        String url = "http://" + ServerConfig.getInstance().getIp() + ":"+ SpringUtil.getProperty("server.port");
        params.set("hook.on_play",url + "/zlmediakit/hook/on_play");
        params.set("hook.on_publish", url + "/zlmediakit/hook/on_publish");
        params.set("hook.on_server_started",url + "/zlmediakit/hook/on_server_started");
        params.set("hook.on_stream_changed",url + "/zlmediakit/hook/on_stream_changed");
        params.set("hook.on_stream_none_reader",url + "/zlmediakit/hook/on_stream_none_reader");
        params.set("hook.on_stream_not_found",url + "/zlmediakit/hook/on_stream_not_found");
        params.set("hook.on_server_keepalive",url + "/zlmediakit/hook/on_server_keepalive");
        params.set("hook.on_send_rtp_stopped",url + "/zlmediakit/hook/on_send_rtp_stopped");
        params.set("hook.on_rtp_server_timeout",url + "/zlmediakit/hook/on_rtp_server_timeout");
        params.set("hook.on_record_mp4",url + "/zlmediakit/hook/on_record_mp4");

        log.info("zlmediakit setServerConfig ...");
        JSONObject result1 = setServerConfig(params);
        log.info("setServerConfig result {}",result1);

        log.info("zlmediakit restartServer ...");
        JSONObject result2 = restartServer(new JSONObject());
        log.info("restartServer result {}",result2);
    }

    public int getFreePort(){
        int mediaPort = 0;
        JSONObject result = listRtpServer(new JSONObject());
        JSONArray data = result.getJSONArray("data");
        if(data == null){
            data = new JSONArray();
        }
        List<Integer> usedPort = data.stream().map(v -> JSONUtil.parseObj(v).getInt("port")).toList();
        String[] split = config.getRtpProxyPortRange().split("-");
        for(int i = Integer.parseInt(split[0]); i < Integer.parseInt(split[1]); i++){
            if(!usedPort.contains(i)){
                mediaPort = i;
                break;
            }
        }
        if(mediaPort == 0){
            throw new RuntimeException("no freePort");
        }
        return mediaPort;
    }

    public JSONObject setServerConfig(JSONObject params){
        return sendPost("/index/api/setServerConfig",params);
    }

    public JSONObject getServerConfig(JSONObject params){
        return sendPost("/index/api/getServerConfig",params);
    }

    public JSONObject restartServer(JSONObject params){
        return sendPost("/index/api/restartServer",params);
    }

    public JSONObject openRtpServer(JSONObject params){
        return sendPost("/index/api/openRtpServer",params);
    }

    public JSONObject closeRtpServer(JSONObject params){
        return sendPost("/index/api/closeRtpServer",params);
    }

    public JSONObject connectRtpServer(JSONObject params){
        return sendPost("/index/api/connectRtpServer",params);
    }

    public JSONObject listRtpServer(JSONObject params){
        return sendPost("/index/api/listRtpServer",params);
    }

    public JSONObject startSendRtp(JSONObject params){
        return sendPost("/index/api/startSendRtp",params);
    }

    public JSONObject startSendRtpPassive(JSONObject params){
        return sendPost("/index/api/startSendRtpPassive",params);
    }

    public JSONObject stopSendRtp(JSONObject params){
        return sendPost("/index/api/stopSendRtp",params);
    }

    public JSONObject getMediaList(JSONObject params){
        return sendPost("/index/api/getMediaList",params);
    }

    public JSONObject startRecord(JSONObject params){
        return sendPost("/index/api/startRecord",params);
    }

    public JSONObject sendPost(String path, JSONObject params) {
        String serverUlr = String.format("http://%s:%s", config.getIp(), config.getHttpPort());
        String url = serverUlr + path;
        params.set("secret", config.getSecret());
        String jsonStr = HttpUtil.post(url, params);
        if(!JSONUtil.isTypeJSON(jsonStr)){
            log.error("zlmediakit result {}",jsonStr);
        }
        if(log.isDebugEnabled()){
            log.debug("zlmediakit send url {} params {}",url,params);
        }
        JSONObject result = JSONUtil.parseObj(jsonStr);
        if(result.getInt("code") != 0){
            log.error("zlmediakit err {}",result);
        }
        if(log.isDebugEnabled()){
            log.debug("zlmediakit result {} ", result);
        }
        return result;
    }
}
