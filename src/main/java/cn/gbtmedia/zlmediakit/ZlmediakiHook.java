package cn.gbtmedia.zlmediakit;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xqs
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/zlmediakit/hook")
public class ZlmediakiHook {

    @PostMapping(value = "/on_server_started")
    public JSONObject on_server_started(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_server_started {}",params);
        }
        log.info("zlmediakit on_server_started ...");
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_stream_changed")
    public JSONObject on_stream_changed(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_stream_changed {}",params);
        }
        String stream = params.getStr("stream");
        Boolean regist = params.getBool("regist");
        if(regist){
            log.info("zlmediakit on_stream_changed regist {}",stream);
            SpringUtil.publishEvent(new ZlmediakitEvent(1,stream,params,this));
        }else {
            log.info("zlmediakit on_stream_changed unRegist {}",stream);
            SpringUtil.publishEvent(new ZlmediakitEvent(2,stream,params,this));
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_stream_none_reader")
    public JSONObject on_stream_none_reader(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_stream_none_reader {}",params);
        }
        String stream = params.getStr("stream");
        log.info("zlmediakit on_stream_none_reader {}",stream);
        SpringUtil.publishEvent(new ZlmediakitEvent(3,stream,params,this));
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("close", true);
        return result;
    }

    @PostMapping(value = "/on_record_mp4")
    public JSONObject on_record_mp4(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_record_mp4 {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_send_rtp_stopped")
    public JSONObject on_send_rtp_stopped( @RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_send_rtp_stopped {}",params);
        }
        String stream = params.getStr("stream");
        log.info("zlmediakit on_send_rtp_stopped {}",stream);
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_publish")
    public JSONObject on_publish(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_publish {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_play")
    public JSONObject on_play(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_play {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_server_keepalive")
    public JSONObject on_server_keepalive(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_server_keepalive {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_stream_not_found")
    public JSONObject on_stream_not_found(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_stream_not_found {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }

    @PostMapping(value = "/on_rtp_server_timeout")
    public JSONObject on_rtp_server_timeout(@RequestBody JSONObject params){
        if(log.isDebugEnabled()){
            log.debug("zlmediakit on_rtp_server_timeout {}",params);
        }
        JSONObject result = new JSONObject();
        result.set("code", 0);
        result.set("msg", "success");
        return result;
    }
}
