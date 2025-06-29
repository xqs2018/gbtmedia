package cn.gbtmedia.zlmediakit;

import cn.gbtmedia.common.util.NetUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xqs
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "zlmediakit-config")
public class ZlmediakitConfig {

    /**
     * 接口通信服务器内网的ip
     */
    private String ip = NetUtil.getLocalIp();;

    /**
     * 设备接入到服务器通信的ip
     */
    private String accessIp = ip;

    /**
     * 生成播放、下载等访问的ip
     */
    private String publicIp = ip;

    /**
     * 流媒体id
     */
    private String mediaServerId = "media01";

    /**
     * 流媒体密钥
     */
    private String secret = "123456";

    /**
     * 收流端口模式 单端口 single / 多端口 multiple(兼容性高)
     */
    private String mediaModel = "multiple";

    /**
     * api接口端口
     */
    private int httpPort = 13800;

    /**
     * rtp收流端口
     */
    private int rtpProxyPort = 13100;

    /**
     * rtp收流端口范围
     */
    private String rtpProxyPortRange = "13200-13300";


    /**
     * 初始化
     */
    @PostConstruct
    void init() throws Exception {
        log.info("init zlmediakit config {}",this);
    }
}
