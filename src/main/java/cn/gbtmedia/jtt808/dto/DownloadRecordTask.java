package cn.gbtmedia.jtt808.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author xqs
 */
@Data
public class DownloadRecordTask {

    private String taskId;

    private String clientId;

    private int serialNo;

    private String tempPath;

    private String tempFullPath;

    private String fileName;

    private String recordFullPath;

    private int progress;

    private int lastProgress;

    private int size;

    private Date taskStartTime;

    private Date recordStartTime;

    private Date recordEndTime;

}
