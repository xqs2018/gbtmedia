package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xqs
 */
@Data
public class RecordFileDto {

    private String deviceId;

    private String channelId;

    private Date startTime;

    private Date endTime;

    private String fileName;

    private String fileSize;

    private Date createTime;

    /**
     * 1 设备录像  2 云端录像
     */
    private int type;
}
