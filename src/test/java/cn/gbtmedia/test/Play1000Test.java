
package cn.gbtmedia.test;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * @author xqs
 */
@Slf4j
public class Play1000Test {

    public static void main(String[] args) {
        String api = "http://127.0.0.1:18080/backend/gbt28181/play";
        for(int i = 1;i<950;i++){
            JSONObject params = new JSONObject();
            params.set("deviceId","11010000001320000"+String.format("%03d", i));
            params.set("channelId","11010000001310000"+String.format("%03d", i));
            String result = HttpUtil.post(api, params.toString());
            log.info("play-{} {} ",i,result);
        }
    }
}
