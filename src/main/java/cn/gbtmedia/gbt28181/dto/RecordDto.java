package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * @author xqs
 */
@Data
public class RecordDto {

    private String deviceId;

    private String channelId;

    private String sn;

    private String name;

    private int sumNum;

    private Instant lastTime;

    private List<Item> recordList;

    @Data
    public static class Item {

        private String deviceId;

        private String name;

        private String filePath;

        private String fileSize;

        private String address;

        private String startTime;

        private String endTime;

        private int secrecy;

        private String type;

        private String recorderId;

        /**
         * 下载进度0 未上传 1-99上传中 100上传完成
         */
        private int progress;

        private String callId;

        private String ssrc;

        private String fileName;

    }
}
