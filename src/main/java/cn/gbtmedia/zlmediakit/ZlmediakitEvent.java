package cn.gbtmedia.zlmediakit;

import cn.hutool.json.JSONObject;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author xqs
 */
@Getter
public class ZlmediakitEvent extends ApplicationEvent {

    /**
     * 1 regist 2  unRegist 3  on_stream_none_reader
     */
    private final int type;

    private final String stream;

    private final JSONObject params;

    public ZlmediakitEvent(int type, String stream,JSONObject params,Object source) {
        super(source);
        this.type = type;
        this.stream = stream;
        this.params = params;
    }
}
