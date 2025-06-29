package cn.gbtmedia.jtt808.dto;

import lombok.Data;
import java.util.List;

/**
 * @author xqs
 */
@Data
public class RecordDto {

    private String clientId;

    /**
     * 音视频资源列表
     */
    private List<RecordDto.Item> recordList;

    @Data
    public static class Item {

        /**
         * 逻辑通道号 BYTE
         */
        private int channelNo;

        /**
         * 开始时间 YYMMDDHHMMSS BCD[6] 全0表示无起始时间
         */
        private String startTime;

        /**
         * 结束时间 YYMMDDHHMMSS BCD[6] 全0表示无结束时间
         */
        private String endTime;

        /**
         * 报警标志0~31(参考808协议文档报警标志位定义) DWORD
         */
        private int warnBit1;

        /**
         * 报警标志32~63 DWORD
         */
        private int warnBit2;

        /**
         * 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频 BYTE
         */
        private int mediaType;

        /**
         * 码流类型：0.所有码流 1.主码流 2.子码流  BYTE
         */
        private int streamType;

        /**
         * 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器  BYTE
         */
        private int storageType;

        /**
         * 文件大小 DWORD
         */
        private long size;

        /**
         * 文件大小
         */
        private String fileSize;

        /**
         * 下载进度0 未上传 1-99上传中 100上传完成
         */
        private int progress;

        /**
         * 下载序列号
         */
        private int downloadSerialNo;

        /**
         * 下载完成的文件名
         */
        private String fileName;

    }
}
