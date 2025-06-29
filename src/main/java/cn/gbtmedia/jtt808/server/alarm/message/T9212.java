package cn.gbtmedia.jtt808.server.alarm.message;

import lombok.Data;

/**
 * 文件上传完成消息应答
 * @author xqs
 */
@Data
public class T9212 {

    /**
     * 文件名称长度
     */
    private int nameLength;

    /**
     * 文件名称(文件类型_通道号_报警类型_序号_报警编号.后缀名)
     */
    private String name;

    /**
     * 文件类型：0.图片 1.音频 2.视频 3.文本 4.面部特征图片(粤标) 5.其它 [1]
     */
    private int type;

    /**
     * 上传结果：0.完成 1.需要补传 [1]
     */
    private int result;

    /**
     * 补传数据包数量[1]
     */
    private int total;

    /**
     * 补传数据包列表
     */
    private int[] items;

}
