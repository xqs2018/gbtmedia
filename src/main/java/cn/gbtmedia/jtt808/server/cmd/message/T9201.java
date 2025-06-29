package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 录像音视频播放请求
 * @author xqs
 */
@Data
public class T9201 {

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

    /**
     * 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器  BYTE
     */
    private int storageType;

    /**
     * 回放方式：0.正常回放 1.快进回放 2.关键帧快退回放 3.关键帧播放 4.单帧上传  BYTE
     */
    private int playbackMode;

    /**
     * 快进或快退倍数：0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为1和2时,此字段内容有效,否则置0  BYTE
     */
    private int playbackSpeed;

    /**
     * 开始时间(YYMMDDHHMMSS,回放方式为4时,该字段表示单帧上传时间  BCD[6]
     */
    private String startTime;

    /**
     * 结束时间(YYMMDDHHMMSS,回放方式为4时,该字段无效,为0表示一直回放) BCD[6]
     */
    private String endTime;
}
