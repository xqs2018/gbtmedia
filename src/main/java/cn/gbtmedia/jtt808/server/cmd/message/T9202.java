package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 音视频录像播放控制
 * @author xqs
 */
@Data
public class T9202 {

    /**
     * 终端手机号
     */
    private String clientId;

    /**
     * 逻辑通道号 BYTE
     */
    private int channelNo;

    /**
     * 回放控制：0.开始回放 1.暂停回放 2.结束回放 3.快进回放 4.关键帧快退回放 5.拖动回放 6.关键帧播放 BYTE
     */
    private int playbackMode;

    /**
     * 快进或快退倍数：0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为3和4时,此字段内容有效,否则置0) BYTE
     */
    private int playbackSpeed;

    /**
     * 拖动回放位置(YYMMDDHHMMSS,回放控制为5时,此字段有效 BCD[6] 全0表示无起始时间 BYTE
     */
    private String playbackTime;
}
