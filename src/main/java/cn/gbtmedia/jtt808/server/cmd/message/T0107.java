package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;

/**
 * 查询终端属性应答
 * @author xqs
 */
@Data
public class T0107 {

    /**
     * 终端类型 长度2
     */
    private int deviceType;

    /**
     * 制造商ID,终端制造商编码  长度5
     */
    private String makerId;

    /**
     * 终端型号 2013长度20 2019长度30
     */
    private String deviceModel;

    /**
     * 终端ID 2013长度7 2019长度30
     */
    private String deviceId;

    /**
     * 终端SIM卡ICCID HEX 长度10
     */
    private String iccid;

    /**
     * 硬件版本号 长度
     */
    private int hardwareVersionLength;

    /**
     * 硬件版本号
     */
    private String hardwareVersion;

    /**
     * 固件版本号 长度
     */
    private int firmwareVersionLength;

    /**
     * 固件版本号
     */
    private String firmwareVersion;

    /**
     * GNSS模块属性  长度1
     */
    private int gnssAttribute;

    /**
     * 通信模块属性  长度1
     */
    private int networkAttribute;
}
