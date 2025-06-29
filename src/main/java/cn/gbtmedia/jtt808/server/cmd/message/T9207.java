package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 音视频录像上传控制
 * @author xqs
 */
@Data
public class T9207 {

    /**
     * 终端手机号
     */
    private String clientId;

    /**
     * 应答流水号 WORD
     */
    private int responseSerialNo;

    /**
     * 上传控制：0.暂停 1.继续 2.取消 BYTE
     */
    private int command;
}
