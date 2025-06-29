package cn.gbtmedia.gbt28181.service;

import cn.gbtmedia.gbt28181.dto.BroadcastDto;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.ChannelTreeDto;
import cn.gbtmedia.gbt28181.dto.InfoDto;
import cn.gbtmedia.gbt28181.dto.InviteDto;
import cn.gbtmedia.gbt28181.dto.QueryParam;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.dto.RecordFileDto;
import cn.gbtmedia.gbt28181.dto.StreamDto;
import cn.gbtmedia.gbt28181.dto.TalkDto;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * @author xqs
 */
public interface Gbt28181Service {

    /**
     * 配置信息
     */
    InfoDto info(QueryParam param);

    /**
     *分页查询设备
     */
    Page<Device> pageDevice(QueryParam param);

    /**
     *更新设备
     */
    void updateDevice(Device device);

    /**
     * 删除设备
     */
    void deleteDevice(Device device);

    /**
     * 查询设备信息
     */
    void queryDeviceInfo(QueryParam param);

    /**
     *分页查询设备通道
     */
    Page<DeviceChannel> pageDeviceChannel(QueryParam param);

    /**
     *更新通道
     */
    void updateDeviceChannel(DeviceChannel deviceChannel);

    /**
     * 删除通道
     */
    void deleteDeviceChannel(DeviceChannel deviceChannel);

    /**
     * 通道树
     */
    List<ChannelTreeDto> channelTree(QueryParam param);

    /**
     * 查询通道
     */
    CatalogDto queryCatalog(QueryParam param);

    /**
     *实时播放
     */
    StreamDto play(QueryParam param);

    /**
     *停止播放
     */
    void stopPlay(QueryParam param);

    /**
     *查询录像信息
     */
    RecordDto queryRecordInfo(QueryParam param);

    /**
     * 录像回放
     */
    StreamDto playback(QueryParam param);

    /**
     * 录像倍速
     */
    void playbackSpeed(QueryParam param);

    /**
     * 停止回放
     */
    void stopPlayback(QueryParam param);

    /**
     * 录像下载
     */
    StreamDto download(QueryParam param);

    /**
     * 停止下载
     */
    void stopDownload(QueryParam param);

    /**
     * 语音对讲
     */
    TalkDto talk(QueryParam param);

    /**
     * 停止对讲
     */
    void stopTalk(QueryParam param);

    /**
     *语音广播
     */
    BroadcastDto broadcast(QueryParam param);

    /**
     * 停止广播
     */
    void stopBroadcast(QueryParam param);

    /**
     * 云台控制
     */
    void controlPtzCmd(QueryParam param);

    /**
     * 查询录像文件
     */
    List<RecordFileDto> listRecordFile(QueryParam param);

    /**
     * 下载录像文件
     */
    void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, String fileName);

    /**
     * 删除录像文件
     */
    void deleteRecordFile(RecordFileDto recordFileDto);

    /**
     * 获取设备所有推流
     */
    List<InviteDto> listInvite(QueryParam param);

    /**
     * 停止推流
     */
    void stopInvite(QueryParam param);

    /**
     *分页查询上级平台
     */
    Page<Platform> pagePlatform(QueryParam param);

    /**
     * 保存上级平台
     */
    void savePlatform(Platform platform);

    /**
     * 删除上级平台
     */
    void deletePlatform(Platform platform);

    /**
     * 启用/停止 上级平台
     */
    void enablePlatform(Platform platform);

    /**
     *保存上级平台关联通道
     */
    void savePlatformChannelList(Platform platform);

    /**
     *查询上级平台关联的通道
     */
    List<PlatformChannel> listPlatformChannel(QueryParam param);



}
