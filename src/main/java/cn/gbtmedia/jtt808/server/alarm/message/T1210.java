package cn.gbtmedia.jtt808.server.alarm.message;

import lombok.Data;
import java.util.List;

/**
 * 报警附件信息消息
 * @author xqs
 */
@Data
public class T1210 {

    /**
     * 终端ID [7] [30]
     */
    private String deviceId1;

    //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

    /**
     * 终端ID，长度[7] 长度[30]
     */
    private String deviceId;

    /**
     * 时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
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
     * 报警编号[32]
     */
    private String platformAlarmId;

    /**
     * 信息类型：0.正常报警文件信息 1.补传报警文件信息 [1]
     */
    private int type;

    /**
     * 信附件信息数量 [1]
     */
    private int totalItem;

    /**
     * 附件信息列表
     */
    private List<Item> items;

    @Data
    public static class Item {

        /**
         * 文件名称长度 [1]
         */
        private int nameLength;

        /**
         * 文件名称
         */
        private String name;

        /**
         * 文件大小 [4]
         */
        private long size;

    }
}
