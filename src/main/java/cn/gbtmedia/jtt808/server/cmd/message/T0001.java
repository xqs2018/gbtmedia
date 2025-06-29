package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 终端通用应答
 * @author xqs
 */
@Data
public class T0001 {

    /**
     *  0 应答流水号 WORD 对应的平台消息的流水号
     */
    private int responseSerialNo;

    /**
     * 2 应答 ID WORD 对应的平台消息的 ID
     */
    private int responseMessageId;

    /**
     * 4 结果 BYTE 0：成功/确认；1：失败；2：消息有误；3：不支持
     */
    private int resultCode;

}
