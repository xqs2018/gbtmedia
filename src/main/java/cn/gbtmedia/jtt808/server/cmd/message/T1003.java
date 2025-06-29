package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 终端上传音视频属性
 * @author xqs
 */
@Data
public class T1003 {

    /**
     * 0 BYTE 输入音频编码方式
     */
    private int audioFormat;

    /**
     * 1 BYTE 输入音频声道数
     */
    private int audioChannels;

    /**
     * 2 BYTE 输入音频采样率：0.8kHz 1.22.05kHz 2.44.1kHz 3.48kHz
     */
    private int audioSamplingRate;

    /**
     * 3 BYTE 输入音频采样位数：0.8位 1.16位 2.32位
     */
    private int audioBitDepth;

    /**
     * 4 WORD 音频帧长度
     */
    private int audioFrameLength;

    /**
     * 6 BYTE 是否支持音频输出
     */
    private int audioSupport;

    /**
     * 7 BYTE 视频编码方式
     */
    private int videoFormat;

    /**
     * 8 BYTE 终端支持的最大音频物理通道
     */
    private int maxAudioChannels;

    /**
     * 9 BYTE 终端支持的最大视频物理通道
     */
    private int maxVideoChannels;


}
