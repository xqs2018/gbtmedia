package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 实时音视频传输请求
 * @author xqs
 */
@Data
public class T9101 {

    /**
     * 终端手机号
     */
    private String clientId;

    /**
     *   0 服务器 IP 地址长度 BYTE 长度 n
     */
    private int ipLength;

    /**
     *    1 服务器 IP 地址 STRING 实时视频服务器 IP 地址
      */
    private String ip;

    /**
     *   1 + n 服务器视频通道监听端口号 (TCP) WORD 实时视频服务器 TCP 端口号
     */
    private int tcpPort;

    /**
     *  3 + n 服务器视频通道监听端口号 (UDP) WORD 实时视频服务器 UDP 端口号
     */
    private int udpPort;

    /**
     * 5 + n 逻辑通道号 BYTE 按照 JT/ T 1076—2016 中的表 2
     */
    private int  channelNo;

    /**
     *  6 + n 数据类型 BYTE 0：音视频，1：视频，2：双向对讲，3：监听， 4：中心广播，5：透传
     */
    private int  mediaType;

    /**
     * 7 + n 码流类型 BYTE 0：主码流，1：子码流
     */
    private int streamType;

}
