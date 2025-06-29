package cn.gbtmedia.gbt28181.server.sip.send;

import cn.gbtmedia.gbt28181.dto.BroadcastDto;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.dto.StreamDto;
import cn.gbtmedia.gbt28181.dto.TalkDto;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import java.util.Date;

/**
 * 给国标下级发送指令
 * @author xqs
 */
public interface SipDeviceSend {

    /**
     * 查询设备状态
     */
    SipResult<?> queryDeviceStatus(String deviceId);

    /**
     * 查询设备信息
     */
    SipResult<?> queryDeviceInfo(String deviceId);

    /**
     * 查询通道信息
     */
    SipResult<CatalogDto> queryCatalog(String deviceId);

    /**
     * 订阅通道信息
     */
    SipResult<?> subscribeCatalog(String deviceId);

    /**
     * 查询录像信息
     */
    SipResult<RecordDto> queryRecordInfo(String deviceId, String channelId, Date startTime, Date endTime);

    /**
     * 下发播放
     */
    SipResult<StreamDto> play(String deviceId, String channelId);

    /**
     * 停止播放
     */
    SipResult<?> stopPlay(String ssrc);

    /**
     * 下发回放
     */
    SipResult<StreamDto> playback(String deviceId, String channelId, Date startTime, Date endTime);

    /**
     * 回放倍速
     */
    SipResult<?> playbackSpeed(String ssrc,double speed);

    /**
     * 停止回放
     */
    SipResult<?> stopPlayback(String ssrc);

    /**
     * 下载录像
     */
    SipResult<StreamDto> download(String deviceId, String channelId, Date startTime, Date endTime, int downloadSpeed);

    /**
     * 停止下载
     */
    SipResult<?> stopDownload(String ssrc);

    /**
     * 语音对讲
     */
    SipResult<TalkDto> talk(String deviceId, String channelId);

    /**
     * 停止对讲
     */
    SipResult<?> stopTalk(String ssrc);

    /**
     * 语音广播
     */
    SipResult<BroadcastDto> broadcast(String deviceId, String channelId);

    /**
     * 停止广播
     */
    SipResult<?> stopBroadcast(String callId);

    /**
     * 停止服务端下发的Invite
     */
    SipResult<?> stopServerInvite(String ssrc);

    /**
     * 停止客户端下发的Invite
     */
    SipResult<?> stopClientInvite(String callId);

    /**
     * 云台控制，支持方向与缩放控制
     *
     * @param deviceId  设备id
     * @param channelId 通道id
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed 镜头移动速度
     * @param zoomSpeed 镜头缩放速度
     */
    SipResult<?> controlPtzCmd(String deviceId, String channelId, int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed);

    /**
     * 云台控制
     */
    SipResult<?> controlPtzCmd(String deviceId, String channelId, String ptzCmdStr);
}
