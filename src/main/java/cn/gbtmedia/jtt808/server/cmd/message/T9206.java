package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 音视频录像上传
 * @author xqs
 */
@Data
public class T9206 {

    /**
     * 终端手机号
     */
    private String clientId;

    /*
     *  服务器 IP 地址长度 BYTE
     */
    private int ipLength;

    /**
     *  服务器 IP 地址 STRING 实时视频服务器 IP 地址
     */
    private String ip;

    /**
     *  服务器端口 WORD
     */
    private int port;

    /**
     * 用户名长度 BYTE
     */
    private int usernameLength;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码长度 BYTE
     */
    private int passwordLength;

    /**
     * 密码
     */
    private String password;

    /**
     * 文件上传路径长度 BYTE
     */
    private int pathLength;
    /**
     * 文件上传路径
     */
    private String path;

    /**
     * 逻辑通道号 BYTE
     */
    private int channelNo;

    /**
     * 开始时间 YYMMDDHHMMSS BCD[6] 全0表示无起始时间
     */
    private String startTime;

    /**
     * 结束时间 YYMMDDHHMMSS BCD[6] 全0表示无结束时间
     */
    private String endTime;

    /**
     * 报警标志0~31(参考808协议文档报警标志位定义) DWORD
     */
    private int warnBit1;

    /**
     * 报警标志32~63 DWORD
     */
    private int warnBit2;

    /**
     * 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频 BYTE
     */
    private int mediaType;

    /**
     * 码流类型：0.所有码流 1.主码流 2.子码流  BYTE
     */
    private int streamType;

    /**
     * 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器  BYTE
     */
    private int storageType;

    /**
     * 任务执行条件(用bit位表示)：[0]WIFI下可下载 [1]LAN连接时可下载 [2]3G/4G连接时可下载 BYTE
     */
    private int condition = -1;
}
