package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 平台通用应答
 * @author xqs
 */
@Data
public class T8001{

    /**
     *  0 应答流水号 WORD 对应的终端消息的流水号
     */
    private int responseSerialNo;

    /**
     *  2 应答 ID WORD 对应的终端消息的 ID
     */
    private int responseMessageId;

    /**
     *  4 结果 BYTE 0：成功/确认；1：失败；2：消息有误；3：不支持；4：报警
     */
    private int resultCode;

}
