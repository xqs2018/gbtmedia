package cn.gbtmedia.common.config;

import cn.gbtmedia.common.util.NetUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.net.InetAddress;


/**
 * @author xqs
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "server-config")
public class ServerConfig{

    public static ServerConfig getInstance(){
        return SpringUtil.getBean(ServerConfig.class);
    }

    /**
     * 服务器内网ip
     */
    private String ip = NetUtil.getLocalIp();

    /**
     * 设备接入到服务器通信的ip
     */
    private String accessIp = ip;

    /**
     * 生成播放、下载等访问的ip
     */
    private String publicIp = ip;

    /**
     * ffmpegPath
     */
    private String ffmpegPath = "/var/gbtmedia/ffmpeg/ffmpeg";
    /**
     * ffprobePath
     */
    private String ffprobePath = "/var/gbtmedia/ffmpeg/ffprobe";

    /**
     * fontPath
     */
    private String fontPath = "/var/gbtmedia/font/font.ttf";

    /**
     * ftp服务配置
     */
    private Ftp ftp = new Ftp();

    /**
     * gbt28181服务配置
     */
    private Gbt28181 gbt28181 = new Gbt28181();

    /**
     * jtt808服务配置
     */
    private Jtt808 jtt808 = new Jtt808();

    @Data
    public static class Ftp{

        /**
         * ftp监听端口
         */
        private int port = 10021;

        /**
         * 被动模式开放端口
         */
        private String passivePorts = "10100-10200";

        /**
         * 用户名
         */
        private String username = "admin";

        /**
         * 密码
         */
        private String password = "123456";

        /**
         * 文件存储路径
         */
        private String path = "/var/gbtmedia/ftp";
    }


    @Data
    public static class Gbt28181{

        /**
         *  国标信令id
         */
        private String sipId = "11010500202001000001";

        /**
         * 国标信令域
         */
        private String sipDomain = "1101050020";

        /**
         * 国标信令密码
         */
        private String sipPassword = "123456";

        /**
         * 国标信令端口
         */
        private int sipPort = 15060;

        /**
         * 国标收流端口模式 单端口 single / 多端口 multiple(兼容性高) / zlmediakit
         */
        private String mediaModel = "multiple";

        /**
         * 国标流媒体单端口收流
         */
        private int mediaSinglePort = 15100;

        /**
         * 国标流媒体多端口收流范围
         */
        private String mediaMultiplePort = "15200-15300";

        /**
         * 国标流媒体播放端口
         */
        private int flvPort = 15700;

        /**
         * 浏览器发送语音到到服务器的websocket端口
         */
        private int wsPort = 15800;

        /**
         * 国标录像文件保存路径
         */
        private String recordPath = "/var/gbtmedia/gbt28181/record";

        /**
         * 设备录像保存路径
         */
        private String recordPathDevice = recordPath + "/device";

        /**
         * 云端录像保存路径
         */
        private String recordPathCloud = recordPath + "/cloud";

        /**
         * 录制分割秒数 只能整分或者小时 60 / 3600
         */
        private int recordSecond = 3600;

        /**
         * 等待信令超时时间毫秒
         */
        private long sipTimeOut = 10000;

        /**
         * 等待上流超时时间毫秒
         */
        private long mediaTimeOut = 10000;

        /**
         * 流无人观看自动关闭毫秒
         */
        private long mediaAutoClose = 10000;

    }


    @Data
    public static class Jtt808{

        /**
         * 终端指令下发端口
         */
        private int cmdPort = 12808;

        /**
         * 1078收流端口模式 单端口 single / 多端口 multiple(兼容性高)
         */
        private String mediaModel = "multiple";

        /**
         * 单端口收流(实时) 1078协议中没有字段区分是实时还是回放
         */
        private int mediaSinglePlayPort = 12100;

        /**
         * 单端口收流(回放) 1078协议中没有字段区分是实时还是回放
         */
        private int mediaSinglePlaybackPort = 12101;

        /**
         * 流媒体多端口收流范围
         */
        private String mediaMultiplePort = "12200-12300";

        /**
         * 流媒体播放端口
         */
        private int flvPort = 12700 ;

        /**
         * 浏览器发送语音到到服务器的websocket端口
         */
        private int wsPort = 12800;

        /**
         * 报警附件上传端口（苏标）
         */
        private int alarmFilePort = 12900;

        /**
         * 录像文件保存路径
         */
        private String recordPath = "/var/gbtmedia/jtt808/record";

        /**
         * 报警附件保存路径
         */
        private String alarmFilePath = "/var/gbtmedia/jtt808/alarmfile";

        /**
         * 等待指令超时时间
         */
        private long cmdTimeOut = 10000;

        /**
         * 等待上流超时时间
         */
        private long mediaTimeOut = 10000;

        /**
         * 流无人观看自动关闭毫秒
         */
        private long mediaAutoClose = 10000;
    }

    /**
     * 初始化
     */
    @PostConstruct
    void init() throws Exception {
        log.info("init server config {}",this);
        boolean b = new File(ftp.getPath()).mkdirs();
        boolean b1 = new File(gbt28181.getRecordPath()).mkdirs();
        boolean b2 = new File(jtt808.getRecordPath()).mkdirs();
        boolean b3 = new File(jtt808.getAlarmFilePath()).mkdirs();
        boolean b4 = new File(gbt28181.getRecordPathDevice()).mkdirs();
        boolean b5 = new File(gbt28181.getRecordPathCloud()).mkdirs();
    }
}
