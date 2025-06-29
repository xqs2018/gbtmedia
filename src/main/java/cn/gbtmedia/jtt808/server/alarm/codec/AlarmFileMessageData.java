package cn.gbtmedia.jtt808.server.alarm.codec;

import lombok.Data;
import java.util.Arrays;

/**
 * @author xqs
 */
@Data
public class AlarmFileMessageData {

    /**
     *  帧头标识[4] 0x30 0x31 0x63 0x64
     */
    private int begin;

    /**
     * 文件名称(文件类型_通道号_报警类型_序号_报警编号.后缀名) 长度[50]
     */
    private String name;

    /**
     * 数据偏移量[4]
     */
    private int offset;

    /**
     * 数据长度[4]
     */
    private int length;

    /**
     * 数据体
     */
    private byte[] data;

    @Override
    public String toString() {
        return "AlarmFileMessageData{" +
                "begin=" + begin +
                ", name='" + name + '\'' +
                ", offset=" + offset +
                ", length=" + length +
                ", data=" + (data == null ? "null" :
                (data.length > 10 ?
                        Arrays.toString(Arrays.copyOf(data, 10)) + "..." :
                        Arrays.toString(data))) +
                '}';
    }
}
