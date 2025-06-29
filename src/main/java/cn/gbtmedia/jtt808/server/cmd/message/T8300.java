package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 下发文本信息
 * @author xqs
 */
@Data
public class T8300 {

    private String clientId;

    /**
     * 标志  [0]紧急  [1]保留  [2]终端显示器显示  [3]终端 TTS 播读
     *  [4]广告屏显示 [5]0.中心导航信息|1.CAN故障码信息 [6~7]保留
     *  BYTE
     */
    private int sign;

    /**
     * 类型：1.通知 2.服务  BYTE
     */
    private int type;

    /**
     * 文本信息
     */
    private String content;
}
