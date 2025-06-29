package cn.gbtmedia.gbt28181.server.sip.send;


import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;

import java.util.List;

/**
 * 给国标上级发送指令
 * @author xqs
 */
public interface SipPlatformSend {

    /**
     * 注册上级平台
     *
     */
    SipResult<?> register(String platformId);

    /**
     * 取消注册上级平台
     *
     */
    SipResult<?> unRegister(String platformId);

    /**
     * 向上级平台发送心跳
     *
     */
    SipResult<?> keepalive(String platformId);

    /**
     *向上级发送设备状态响应
     */
    SipResult<?> responseDeviceStatus(String platformId, String reqSn, String fromTag);

    /**
     *向上级发送设备信息响应
     */
    SipResult<?> responseDeviceInfo(String platformId, String reqSn, String fromTag);

    /**
     *向上级发送通道响应
     */
    SipResult<?> responseCatalog(String platformId, String reqSn, String fromTag);

    /**
     * 向上级发送通道变化通知
     */
    SipResult<?> notifyCatalog(String platformId, List<PlatformChannel> platformChannelList, String eventTye);

    /**
     *向上级发送录像响应
     */
    SipResult<?> responseRecordInfo(String platformId, String reqSn, String fromTag , RecordDto recordDto);

    /**
     *向上级发送流推送完毕
     */
    SipResult<?> notifyMediaStatus(String callId);

    /**
     * 向上级发送广播响应
     */
    SipResult<?> responseBroadcast(String platformId, String deviceId, String channelId,String reqSn,String result);

    /**
     * 向上级发送控制响应
     */
    SipResult<?> responseDeviceControl(String platformId, String deviceId, String channelId,String reqSn,String result);

    /**
     * 向上级发送广播invite
     */
    SipResult<?> broadcast(String platformId, String deviceId, String channelId);

    /**
     * 向上级发送停止广播invite
     */
    SipResult<?> stopBroadcast(String ssrc);


}
