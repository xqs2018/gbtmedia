package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 音视频录像上传完成通知
 * @author xqs
 */
@Data
public class T1206 {

    /**
     * 应答流水号 WORD
     */
    private int responseSerialNo;

    /**
     * 结果：0.成功 1.失败 BYTE
     */
    private int result;
}
