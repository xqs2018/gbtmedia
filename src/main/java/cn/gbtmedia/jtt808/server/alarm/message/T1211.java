package cn.gbtmedia.jtt808.server.alarm.message;

import lombok.Data;

/**
 * 文件信息上传
 * @author xqs
 */
@Data
public class T1211 {

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
