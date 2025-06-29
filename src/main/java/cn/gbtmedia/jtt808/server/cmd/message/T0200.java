package cn.gbtmedia.jtt808.server.cmd.message;

import lombok.Data;
import java.util.List;

/**
 * 终端上报位置信息
 * @author xqs
 */
@Data
public class T0200 {

    /**
     * 报警标志 [4] 参考表25
     */
    private int warnBit;

    /**
     * 状态 [4] 参考表24
     */
    private int statusBit;

    /**
     * 纬度 [4] 乘以10的6次方，百万分之一
     */
    private int latitude;

    /**
     * 经度 [4] 乘以10的6次方，百万分之一
     */
    private int longitude;

    /**
     * 高程(米) [2]
     */
    private int altitude;

    /**
     * 速度(1/10公里每小时) [2]
     */
    private int speed;

    /**
     * 方向 [2]
     */
    private int direction;

    /**
     * 时间(YYMMDDHHMMSS) BCD[6]
     */
    private String deviceTime;

    //*****************************************  以下附加信息 ****************************************

    /**
     *  0x01 累计行驶里程(1/10km)[4]
     */
    private long attr0x01;

    /**
     * 0x02 油量(1/10L)[2]
     */
    private int attr0x02;

    /**
     *  0x03 记录仪速度(1/10km)[2]
     */
    private int attr0x03;

    /**
     * 0x04 需人工确认报警事件ID [2]
     */
    private int attr0x04;

    /**
     * 0x06 车厢温度 [2]
     */
    private int attr0x06;

    /**
     *  0x25 车辆信号状态 [4] 定义参考表31
     */
    private int attr0x25;

    /**
     *  0x2A IO状态位[2] 定义参考表32
     */
    private int attr0x2A;

    /**
     *  0x2B 模拟量[4]
     */
    private int attr0x2B;

    /**
     * 0x30 网络信号[1]
     */
    private int attr0x30;

    /**
     * 0x31 GNSS卫星数[1]
      */
    private int attr0x31;

    /**
     * 0x11 超速附加报警信息
     */
    private Attr0x11 attr0x11;

    /**
     * 0x12 进出区域/路线附加报警信息
     */
    private Attr0x12 attr0x12;

    /**
     * 0x13 路线行驶时间不足/过长附加报警信息
     */
    private Attr0x13 attr0x13;

    /**
     * 0x64 (苏标) 高级驾驶辅助系统报警 ADAS
     */
    private Attr0x64 attr0x64;

    /**
     *  0x65 (苏标) 驾驶员行为监测报警 DSM
     */
    private Attr0x65 attr0x65;

    /**
     *  0x66 (苏标) 轮胎状态监测报警 TPMS
     */
    private Attr0x66 attr0x66;

    /**
     *  0x67 (苏标) 盲区监测系统报警 BSD
     */
    private Attr0x67 attr0x67;


    //******************************************  附加信息结束  **************************************


    @Data
    public static class Attr0x11{

        /**
         * 位置类型：0.无特定位置 1.圆形区域 2.矩形区域 3.多边形区域 4.路段
         */
        private byte areaType;

        /**
         *  区域或路段ID, 位置类型为0则无该字段
         */
        private int areaId;
    }

    @Data
    public static class Attr0x12{

        /**
         * 位置类型：1.圆形区域 2.矩形区域 3.多边形区域 4.路线
         */
        private byte areaType;

        /**
         * 区域或路段ID
         */
        private int areaId;

        /**
         * 方向：0.进 1.出
         */
        private byte direction;
    }

    @Data
    public static class Attr0x13{

        /**
         * 路段ID[4]
         */
        private int areaId;

        /**
         * 路段行驶时间(秒)[2]
         */
        private int driveTime;

        /**
         * 结果：0.不足 1.过长[1]
         */
        private byte result;
    }

    /**
     * 0x64 (苏标) 高级驾驶辅助系统报警 ADAS
     */
    @Data
    public static class Attr0x64 {

        /**
         * 报警ID 长度[4]
         */
        private long id;

        /**
         * 标志状态：0.不可用 1.开始标志 2.结束标志 [1]
         */
        private int state;

        /**
         * 报警/事件类型 1.前向碰撞报警 2.车道偏离报警 长度[1]
         */
        private int type;

        /**
         * 报警级别 长度[1]
         */
        private int level;

        /**
         * 前车车速(Km/h)范围0~250,仅报警类型为1和2时有效 长度[1]
         */
        private int frontSpeed;

        /**
         * 前车/行人距离(100ms),范围0~100,仅报警类型为1、2和4时有效 长度[1]
         */
        private int frontDistance;

        /**
         * 偏离类型：1.左侧偏离 2.右侧偏离(报警类型为2时有效) 长度[1]
         */
        private int deviateType;

        /**
         * 道路标志识别类型：1.限速标志 2.限高标志 3.限重标志 长度[1]
         */
        private int roadSign;

        /**
         * 道路标志识别数据 长度[1]
         */
        private int roadSignValue;

        /**
         * 车速 长度[1]
         */
        private int speed;

        /**
         * 高程 长度[2]
         */
        private int altitude;

        /**
         * 纬度 长度[4]
         */
        private int latitude;

        /**
         * 经度 长度[4]
         */
        private int longitude;

        /**
         * 日期时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String alarmTime;

        /**
         * 车辆状态 长度[2]
         */
        private int statusBit;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 终端ID，长度[7] 长度[30]  终端上报的长度不足有空串，转json会有问题
         */
        private String deviceId;

        /**
         * 时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String dateTime;

        /**
         * 序号(同一时间点报警的序号，从0循环累加) 长度[1]
         */
        private int sequenceNo;

        /**
         * 附件数量 长度[1]
         */
        private int fileTotal;

        /**
         * 预留，长度[1] 长度[2]
         */
        private int reserved;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 平台报警编号[32] 平台产生
         */
        private String platformAlarmId;

    }

    /**
     *  0x65 (苏标) 驾驶员行为监测报警 DSM
     */
    @Data
    public static class Attr0x65 {

        /**
         * 报警ID 长度[4]
         */
        private long id;

        /**
         * 标志状态：0.不可用 1.开始标志 2.结束标志 长度[1]
         */
        private int state;

        /**
         * 报警/事件类型：1.疲劳驾驶报警 2.接打电话报警 3.抽烟报警 长度[1]
         */
        private int type;

        /**
         * 报警级别 长度[1]
         */
        private int level;

        /**
         * 疲劳程度 长度[1]
         */
        private int fatigueDegree;

        /**
         * 预留 长度[4]
         */
        private int reserves;

        /**
         * 车速 长度[1]
         */
        private int speed;

        /**
         * 高程 长度[2]
         */
        private int altitude;

        /**
         * 纬度 长度[4]
         */
        private int latitude;

        /**
         * 经度 长度[4]
         */
        private int longitude;

        /**
         * 日期时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String alarmTime;

        /**
         * 车辆状态 长度[2]
         */
        private int statusBit;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 终端ID，长度[7] 长度[30]  终端上报的长度不足有空串，转json会有问题
         */
        private String deviceId;

        /**
         * 时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String dateTime;

        /**
         * 序号(同一时间点报警的序号，从0循环累加) 长度[1]
         */
        private int sequenceNo;

        /**
         * 附件数量 长度[1]
         */
        private int fileTotal;

        /**
         * 预留，长度[1] 长度[2]
         */
        private int reserved;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 平台报警编号[32] 平台产生
         */
        private String platformAlarmId;

    }


    /**
     *  0x66 (苏标) 轮胎状态监测报警 TPMS
     */
    @Data
    public static class Attr0x66 {

        /**
         * 报警ID 长度[4]
         */
        private long id;

        /**
         * 标志状态 长度[1]
         */
        private int state;

        /**
         * 车速 长度[1]
         */
        private int speed;

        /**
         * 高程 长度[2]
         */
        private int altitude;

        /**
         * 纬度 长度[4]
         */
        private int latitude;

        /**
         * 经度 长度[4]
         */
        private int longitude;

        /**
         * 日期时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String alarmTime;

        /**
         * 车辆状态 长度[2]
         */
        private int statusBit;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 终端ID，长度[7] 长度[30]  终端上报的长度不足有空串，转json会有问题
         */
        private String deviceId;

        /**
         * 时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String dateTime;

        /**
         * 序号(同一时间点报警的序号，从0循环累加) 长度[1]
         */
        private int sequenceNo;

        /**
         * 附件数量 长度[1]
         */
        private int fileTotal;

        /**
         * 预留，长度[1] 长度[2]
         */
        private int reserved;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 事件信息列表总数 长度[1]
         */
        private int totalItem;

        /**
         * 事件信息列表
         */
        private List<Item> items;

        /**
         * 平台报警编号[32] 平台产生
         */
        private String platformAlarmId;


        @Data
        public static class Item {

            /**
             * 胎压报警位置(从左前轮开始以Z字形从00依次编号,编号与是否安装TPMS无关) 长度[1]
             */
            private int position;

            /**
             * 报警类型：0.胎压(定时上报) 1.胎压过高报警 2.胎压过低报警 3.胎温过高报警 4.传感器异常报警长度[2]
             */
            private int type;

            /**
             * 胎压(Kpa) 长度[2]
             */
            private int pressure;

            /**
             * 温度(℃) 长度[2]
             */
            private int temperature;

            /**
             * 电池电量(%) 长度[2]
             */
            private int batteryLevel;
        }
    }


    /**
     * 0x67 (苏标) 盲区监测系统报警 BSD
     */
    @Data
    public static class Attr0x67 {

        /**
         * 报警ID 长度[4]
         */
        private long id;

        /**
         * 标志状态：0.不可用 1.开始标志 2.结束标志 长度[1]
         */
        private int state;

        /**
         * 报警/事件类型：1.后方接近报警 2.左侧后方接近报警 3.右侧后方接近报警 长度[1]
         */
        private int type;

        /**
         * 车速 长度[1]
         */
        private int speed;

        /**
         * 高程 长度[2]
         */
        private int altitude;

        /**
         * 纬度 长度[4]
         */
        private int latitude;

        /**
         * 经度 长度[4]
         */
        private int longitude;

        /**
         * 日期时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String alarmTime;

        /**
         * 车辆状态 长度[2]
         */
        private int statusBit;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 终端ID，长度[7] 长度[30]  终端上报的长度不足有空串，转json会有问题
         */
        private String deviceId;

        /**
         * 时间(YYMMDDHHMMSS) 长度[6]，字符集 BCD
         */
        private String dateTime;

        /**
         * 序号(同一时间点报警的序号，从0循环累加) 长度[1]
         */
        private int sequenceNo;

        /**
         * 附件数量 长度[1]
         */
        private int fileTotal;

        /**
         * 预留，长度[1] 长度[2]
         */
        private int reserved;

        //*************** 报警标识号[16] 终端ID[7] + 时间[6] + 序号[1] + 附件数量[1] + 预留[1] ******************

        /**
         * 平台报警编号[32] 平台产生
         */
        private String platformAlarmId;

    }
}
