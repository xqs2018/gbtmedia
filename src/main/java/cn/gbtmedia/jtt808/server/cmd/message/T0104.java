package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 查询终端参数应答
 * @author xqs
 */
@Data
public class T0104 {

    /**
     * 应答流水号 WORD
     */
    private int responseSerialNo;

    /**
     * 应答参数个数 BYTE
     */
    private int total;

    /**
     * 参数项列表
     */
    private Map<Integer, Object> parameters;

    /**
     * 音视频通道参数
     */
    private Param0x0076 param0x0076;

    // 0x0076 音视频通道参数
    @Data
    public static class Param0x0076 {
        /**
         * 音视频通道总数 [1]
         */
        private int audioVideoChannels;
        /**
         * 音频通道总数 [1]
         */
        private int audioChannels;

        /**
         * 视频通道总数 [1]
         */
        private int videoChannels;

        /**
         * 音视频通道对照表
         */
        private List<Item> items;

        @Data
        public static class Item {

            /**
             * 物理通道号(从1开始) [1]
             */
            private int channelId;

            /**
             * 逻辑通道号(按照JT/T 1076-2016 中的表2) [1]
             */
            private int channelNo;

            /**
             * 通道类型：0.音视频 1.音频 2.视频 [1]
             */
            private int channelType;

            /**
             * 是否连接云台 0.未连接 1.连接 [1]
             */
            private int hasPtz;
        }
    }
}
