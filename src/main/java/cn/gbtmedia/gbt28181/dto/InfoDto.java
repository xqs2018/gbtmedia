package cn.gbtmedia.gbt28181.dto;

import lombok.Data;

/**
 * @author xqs
 */
@Data
public class InfoDto {

    private Gbt28181 gbt28181;

    private Devices devices;

    private Channels channels;

    @Data
    public static class Gbt28181{
        private String sipId;   // 信令id
        private String sipDomain;  // 国标信令域
        private String accessIp;   // 接入ip
        private String sipPort;    // 接入端口
        private String sipPassword;  // 信令密码
    }

    @Data
    public static class Devices {
        private int total;    // 总设备数
        private int online;   // 在线设备数
        private int offline;  // 离线设备数
    }

    @Data
    public static class Channels {
        private int total;    // 总通道数
        private int online;   // 在线通道数
        private int offline;  // 离线通道数
    }

}
