package cn.gbtmedia.jtt808.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xqs
 */
@Data
public class QueryParam {

    private int pageNo = 0;

    private int pageSize = 10;

    private String clientId;

    private String clientIp;

    private Integer online;

    private Integer channelNo;

    private String mediaKey;

    private Date startTime;

    private Date endTime;

    private String fileName;

    private int mediaType;

    private int streamType;

    private int storageType;

    private int size;

    private int serialNo;

    // 0.暂停 1.继续 2.取消
    private int command;

    private String tempPath;

    private String platformAlarmId;
}
