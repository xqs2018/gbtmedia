package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;


/**
 * 上传报警附件
 * @author xqs
 */
@Data
public class T9208 {

    private String clientId;

    /**
     * 服务器IP地址长度 [1]
     */
    private int ipLength;

    /**
     * 服务器IP地址
     */
    private String ip;

    /**
     * TCP端口 [2]
     */
    private int tcpPort;

    /**
     * UDP端口 [2]
     */
    private int udpPort;

    //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

    /**
     * 终端ID [7]  [30]
     */
    private String deviceId;

    /**
     * 时间(YYMMDDHHMMSS) BCD[6]
     */
    private String dateTime;

    /**
     * 序号(同一时间点报警的序号，从0循环累加) 长度[1]
     */
    private int sequenceNo;

    /**
     * 附件数量 长度[1]
     */
    private int fileTotal;

    /**
     * 预留，长度[1] 长度[2]
     */
    private int reserved;

    //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

    /**
     * 报警编号 平台分给报警的唯一编号 [32]
     */
    private String platformAlarmId;

    /**
     * 预留 [16]
     */
    private byte[] reserves = new byte[16];
}
