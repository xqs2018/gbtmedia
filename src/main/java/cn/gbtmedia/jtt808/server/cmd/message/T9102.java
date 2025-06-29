package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 音视频实时传输控制
 * @author xqs
 */
@Data
public class T9102 {

    private String clientId;

    /**
     * 0 BYTE 逻辑通道号
     */
    private int channelNo;

    /**
     * 1 BYTE
     * 控制指令：
     *   0.关闭音视频传输指令
     *   1.切换码流(增加暂停和继续)
     *   2.暂停该通道所有流的发送
     *   3.恢复暂停前流的发送,与暂停前的流类型一致
     *   4.关闭双向对讲
     */
    private int command;

    /**
     * 2 BYTE
     * 关闭音视频类型：
     *   0.关闭该通道有关的音视频数据
     *   1.只关闭该通道有关的音频,保留该通道有关的视频
     *   2.只关闭该通道有关的视频,保留该通道有关的音频
     */
    private int closeType;

    /**
     * 3 BYTE
     * 切换码流类型：0.主码流 1.子码流
     */
    private int streamType;

}
