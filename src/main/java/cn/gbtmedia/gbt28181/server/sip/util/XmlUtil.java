package cn.gbtmedia.gbt28181.server.sip.util;


import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xqs
 */
public class XmlUtil {


    /**
     * json对象的通道转成对象
     */
    public static DeviceChannel jsonItemToDeviceChannel(JSONObject itemDevice){

        DeviceChannel deviceChannel = new DeviceChannel();

        String name = itemDevice.getStr("Name");
        deviceChannel.setName(name);

        String status = ObjectUtil.isEmpty(itemDevice.getStr("Status"))?"ON":itemDevice.getStr("Status");
        // ONLINE OFFLINE HIKVISION DS-7716N-E4 NVR的兼容性处理
        if (status.equals("ON") || status.equals("On") || status.equals("ONLINE") || status.equals("OK")) {
            deviceChannel.setOnline(1);
        }else if (status.equals("OFF") || status.equals("Off") || status.equals("OFFLINE")) {
            deviceChannel.setOnline(0);
        }else {
            deviceChannel.setOnline(1);
        }

        String channelId = itemDevice.getStr("DeviceID");
        deviceChannel.setChannelId(channelId);

        deviceChannel.setManufacturer(itemDevice.getStr("Manufacturer"));
        deviceChannel.setModel(itemDevice.getStr("Model"));
        deviceChannel.setOwner(itemDevice.getStr( "Owner"));
        deviceChannel.setCivilCode(itemDevice.getStr("CivilCode"));
        deviceChannel.setBlock(itemDevice.getStr("Block"));
        deviceChannel.setAddress(itemDevice.getStr( "Address"));
        String businessGroupID = itemDevice.getStr("BusinessGroupID");

        if (ObjectUtil.isEmpty(itemDevice.getStr("Parental"))) {
            if (deviceChannel.getChannelId().length() <= 10
                    || (deviceChannel.getChannelId().length() == 20 && (
                    Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 215
                            || Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 216
            )
            )
            ) {
                deviceChannel.setParental(1);
            }else {
                deviceChannel.setParental(0);
            }
        } else {
            // 由于海康会错误的发送65535作为这里的取值,所以这里除非是0否则认为是1
            deviceChannel.setParental(itemDevice.getInt("Parental") == 1?1:0);
        }
        /**
         * 行政区划展示设备树与业务分组展示设备树是两种不同的模式
         * 行政区划展示设备树 各个目录之间主要靠deviceId做关联,摄像头通过CivilCode指定其属于那个行政区划;都是不超过十位的编号; 结构如下:
         * 河北省
         *    --> 石家庄市
         *          --> 摄像头
         *          --> 正定县
         *                  --> 摄像头
         *                  --> 摄像头
         *
         * 业务分组展示设备树是顶级是业务分组,其下的虚拟组织靠BusinessGroupID指定其所属的业务分组;摄像头通过ParentId来指定其所属于的虚拟组织:
         * 业务分组
         *    --> 虚拟组织
         *         --> 摄像头
         *         --> 虚拟组织
         *             --> 摄像头
         *             --> 摄像头
         */
        String parentId = itemDevice.getStr("ParentID");
        if (parentId != null) {
            if (parentId.contains("/")) {
                String lastParentId = parentId.substring(parentId.lastIndexOf("/") + 1);
                deviceChannel.setParentId(lastParentId);
            }else {
                deviceChannel.setParentId(parentId);
            }
        }else {
            if (deviceChannel.getChannelId().length() <= 10) { // 此时为行政区划, 上下级行政区划使用DeviceId关联
                deviceChannel.setParentId(deviceChannel.getChannelId().substring(0, deviceChannel.getChannelId().length() - 2));
            }else if (deviceChannel.getChannelId().length() == 20) {
                if (Integer.parseInt(deviceChannel.getChannelId().substring(10, 13)) == 216) { // 虚拟组织
                    deviceChannel.setParentId(businessGroupID);
                }else if (deviceChannel.getCivilCode() != null) {
                    // 设备， 无parentId的20位是使用CivilCode表示上级的设备，
                    // 注：215 业务分组是需要有parentId的
                    deviceChannel.setParentId(deviceChannel.getCivilCode());
                }
            }else {
                deviceChannel.setParentId(deviceChannel.getDeviceId());
            }
        }

        if (ObjectUtil.isEmpty(itemDevice.getStr("SafetyWay"))) {
            deviceChannel.setSafetyWay(0);
        } else {
            deviceChannel.setSafetyWay(itemDevice.getInt( "SafetyWay"));
        }
        if (ObjectUtil.isEmpty(itemDevice.getStr("RegisterWay"))) {
            deviceChannel.setRegisterWay(1);
        } else {
            deviceChannel.setRegisterWay(itemDevice.getInt("RegisterWay"));
        }
        deviceChannel.setCertNum(itemDevice.getStr("CertNum"));
        if (ObjectUtil.isEmpty(itemDevice.getStr("Certifiable"))) {
            deviceChannel.setCertifiable(0);
        } else {
            deviceChannel.setCertifiable(itemDevice.getInt("Certifiable"));
        }
        if (ObjectUtil.isEmpty(itemDevice.getStr("ErrCode"))) {
            deviceChannel.setErrCode(0);
        } else {
            deviceChannel.setErrCode(itemDevice.getInt("ErrCode"));
        }
        deviceChannel.setEndTime(itemDevice.getStr("EndTime"));
        deviceChannel.setSecrecy(itemDevice.getStr( "Secrecy"));
        deviceChannel.setIpAddress(itemDevice.getStr( "IPAddress"));
        if (ObjectUtil.isEmpty(itemDevice.getStr( "Port"))) {
            deviceChannel.setPort(0);
        } else {
            deviceChannel.setPort(itemDevice.getInt( "Port"));
        }
        deviceChannel.setPassword(itemDevice.getStr( "Password"));
        if (NumberUtil.isDouble(itemDevice.getStr("Longitude"))) {
            deviceChannel.setLongitude(itemDevice.getDouble("Longitude"));
        } else {
            deviceChannel.setLongitude(0.00);
        }
        if (NumberUtil.isDouble(itemDevice.getStr( "Latitude"))) {
            deviceChannel.setLatitude(itemDevice.getDouble("Latitude"));
        } else {
            deviceChannel.setLatitude(0.00);
        }
        if (ObjectUtil.isEmpty(itemDevice.getStr( "PTZType"))) {
            //兼容INFO中的信息
            JSONObject info = itemDevice.getJSONObject("Info");
            if(ObjectUtil.isEmpty(info) || ObjectUtil.isEmpty(info.getStr( "PTZType"))){
                deviceChannel.setPTZType(0);
            }else{
                deviceChannel.setPTZType(info.getInt( "PTZType"));
            }
        } else {
            deviceChannel.setPTZType(itemDevice.getInt( "PTZType"));
        }
        deviceChannel.setHasAudio(1); // 默认含有音频，播放时再检查是否有音频及是否AAC
        deviceChannel.setCloudRecord(0);
        return deviceChannel;
    }

    /**
     * json成录像对象
     */
    public static RecordDto jsonToRecordInfo(String deviceId, JSONObject response){
        String sn = response.getStr("SN");
        String channelId = response.getStr("DeviceID");
        RecordDto recordInfo = new RecordDto();
        recordInfo.setChannelId(channelId);
        recordInfo.setDeviceId(deviceId);
        recordInfo.setSn(sn);
        recordInfo.setName(response.getStr("Name"));
        int sumNum = response.getInt("SumNum");
        recordInfo.setSumNum(sumNum);
        //没有录像信息
        if(!response.containsKey("RecordList")){
            recordInfo.setRecordList(new ArrayList<>());
            return recordInfo;
        }
        Object itemObjects = response.getJSONObject("RecordList").get("Item");
        JSONArray itemList = new JSONArray();
        //如果只有一个json解析出来是对象
        if(itemObjects instanceof JSONArray){
            itemList = (JSONArray) itemObjects;
        }else if(itemObjects instanceof JSONObject){
            itemList.put(itemObjects);
        }
        List<RecordDto.Item> recordItems = itemList.stream().map(item -> {
            JSONObject jsonItem = (JSONObject) item;
            RecordDto.Item record = new  RecordDto.Item();
            record.setDeviceId(jsonItem.getStr("DeviceID"));
            record.setName(jsonItem.getStr( "Name"));
            record.setFilePath(jsonItem.getStr( "FilePath"));
            record.setFileSize(jsonItem.getStr("FileSize"));
            record.setAddress(jsonItem.getStr( "Address"));

            String startTimeStr = jsonItem.getStr( "StartTime");
            record.setStartTime(startTimeStr);

            String endTimeStr = jsonItem.getStr( "EndTime");
            record.setEndTime(endTimeStr);

            record.setSecrecy(jsonItem.containsKey("Secrecy")?jsonItem.getInt( "Secrecy") :0);
            record.setType(jsonItem.getStr(  "Type"));
            record.setRecorderId(jsonItem.getStr(  "RecorderID"));
            return record;
        }).collect(Collectors.toList());
        recordInfo.setRecordList(recordItems);
        return recordInfo;
    }


    /**
     * 云台指令码计算
     *
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed 镜头移动速度 默认 0XFF (0-255)
     * @param zoomSpeed 镜头缩放速度 默认 0X1 (0-255)
     */
    public static String cmdString(int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
        int cmdCode = 0;
        if (leftRight == 2) {
            cmdCode |= 0x01;        // 右移
        } else if (leftRight == 1) {
            cmdCode |= 0x02;        // 左移
        }
        if (upDown == 2) {
            cmdCode |= 0x04;        // 下移
        } else if (upDown == 1) {
            cmdCode |= 0x08;        // 上移
        }
        if (inOut == 2) {
            cmdCode |= 0x10;    // 放大
        } else if (inOut == 1) {
            cmdCode |= 0x20;    // 缩小
        }
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", moveSpeed);
        builder.append(strTmp, 0, 2);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%X", zoomSpeed);
        builder.append(strTmp, 0, 1).append("0");
        // 计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed /*<< 4*/ & 0XF0)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }
}
