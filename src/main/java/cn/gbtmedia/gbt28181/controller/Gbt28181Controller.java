package cn.gbtmedia.gbt28181.controller;

import cn.gbtmedia.common.vo.Result;
import cn.gbtmedia.gbt28181.dto.InfoDto;
import cn.gbtmedia.gbt28181.service.Gbt28181Service;
import cn.gbtmedia.gbt28181.dto.BroadcastDto;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.ChannelTreeDto;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * @author xqs
 */
@RequestMapping("/backend/gbt28181")
@RestController
public class Gbt28181Controller {

    @Resource
    private Gbt28181Service gbt28181Service;

    @PostMapping("/info")
    public Result<InfoDto> info(@RequestBody QueryParam param){
        InfoDto info = gbt28181Service.info(param);
        return Result.success(info);
    }

    @PostMapping("/pageDevice")
    public Result<Page<Device>> pageDevice(@RequestBody QueryParam param){
        Page<Device> page = gbt28181Service.pageDevice(param);
        return Result.success(page);
    }

    @PostMapping("/updateDevice")
    public Result<?> updateDevice(@RequestBody Device device){
        gbt28181Service.updateDevice(device);
        return Result.success();
    }

    @PostMapping("/deleteDevice")
    public Result<?> deleteDevice(@RequestBody Device device){
        gbt28181Service.deleteDevice(device);
        return Result.success();
    }

    @PostMapping("/queryDeviceInfo")
    public Result<?> queryDeviceInfo(@RequestBody QueryParam param){
        gbt28181Service.queryDeviceInfo(param);
        return Result.success();
    }

    @PostMapping("/pageDeviceChannel")
    public Result<Page<DeviceChannel>> pageDeviceChannel(@RequestBody QueryParam param){
        Page<DeviceChannel> page = gbt28181Service.pageDeviceChannel(param);
        return Result.success(page);
    }

    @PostMapping("/updateDeviceChannel")
    public Result<?> updateDeviceChannel(@RequestBody DeviceChannel deviceChannel){
        gbt28181Service.updateDeviceChannel(deviceChannel);
        return Result.success();
    }

    @PostMapping("/deleteDeviceChannel")
    public Result<?> deleteDeviceChannel(@RequestBody DeviceChannel deviceChannel){
        gbt28181Service.deleteDeviceChannel(deviceChannel);
        return Result.success();
    }

    @PostMapping("/channelTree")
    public Result<List<ChannelTreeDto>> channelTree(@RequestBody QueryParam param){
        List<ChannelTreeDto> treeDtos = gbt28181Service.channelTree(param);
        return Result.success(treeDtos);
    }

    @PostMapping("/queryCatalog")
    public Result<CatalogDto> queryCatalog(@RequestBody QueryParam param){
        CatalogDto catalogDto = gbt28181Service.queryCatalog(param);
        return Result.success(catalogDto);
    }

    @PostMapping("/play")
    public Result<StreamDto> play(@RequestBody QueryParam param){
        StreamDto streamDto = gbt28181Service.play(param);
        return Result.success(streamDto);
    }

    @PostMapping("/stopPlay")
    public Result<?> stopPlay(@RequestBody QueryParam param){
        gbt28181Service.stopPlay(param);
        return Result.success();
    }

    @PostMapping("/queryRecordInfo")
    public Result<RecordDto> queryRecordInfo(@RequestBody QueryParam param){
        RecordDto recordDto = gbt28181Service.queryRecordInfo(param);
        return Result.success(recordDto);
    }

    @PostMapping("/playback")
    public Result<StreamDto> playback(@RequestBody QueryParam param){
        StreamDto streamDto = gbt28181Service.playback(param);
        return Result.success(streamDto);
    }

    @PostMapping("/playbackSpeed")
    public Result<?> playbackSpeed(@RequestBody QueryParam param){
        gbt28181Service.playbackSpeed(param);
        return Result.success();
    }

    @PostMapping("/stopPlayback")
    public Result<?> stopPlayback(@RequestBody QueryParam param){
        gbt28181Service.stopPlayback(param);
        return Result.success();
    }

    @PostMapping("/download")
    public Result<StreamDto> download(@RequestBody QueryParam param){
        StreamDto streamDto = gbt28181Service.download(param);
        return Result.success(streamDto);
    }

    @PostMapping("/stopDownload")
    public Result<?> stopDownload(@RequestBody QueryParam param){
        gbt28181Service.stopDownload(param);
        return Result.success();
    }

    @PostMapping("/talk")
    public Result<TalkDto> talk(@RequestBody QueryParam param){
        TalkDto talkDto = gbt28181Service.talk(param);
        return Result.success(talkDto);
    }

    @PostMapping("/stopTalk")
    public Result<?> stopTalk(@RequestBody QueryParam param){
        gbt28181Service.stopTalk(param);
        return Result.success();
    }

    @PostMapping("/broadcast")
    public Result<BroadcastDto> broadcast(@RequestBody QueryParam param){
        BroadcastDto broadcastDto = gbt28181Service.broadcast(param);
        return Result.success(broadcastDto);
    }

    @PostMapping("/stopBroadcast")
    public Result<?> stopBroadcast(@RequestBody QueryParam param){
        gbt28181Service.stopBroadcast(param);
        return Result.success();
    }

    @PostMapping("/controlPtzCmd")
    public Result<?> controlPtzCmd(@RequestBody QueryParam param){
        gbt28181Service.controlPtzCmd(param);
        return Result.success();
    }

    @PostMapping("/listRecordFile")
    public Result<List<RecordFileDto>> listRecordFile(@RequestBody QueryParam param){
        List<RecordFileDto> recordFileDtos = gbt28181Service.listRecordFile(param);
        return Result.success(recordFileDtos);
    }

    @GetMapping(value = "/downloadRecordFile/{fileName}")
    public void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String fileName){
        gbt28181Service.downloadRecordFile(request,response, fileName);
    }

    @PostMapping(value = "/deleteRecordFile")
    public Result<?> downloadRecordFile(@RequestBody RecordFileDto recordFileDto){
        gbt28181Service.deleteRecordFile(recordFileDto);
        return Result.success();
    }

    @PostMapping("/listInvite")
    public Result<List<InviteDto>> listInvite(@RequestBody QueryParam param){
        List<InviteDto> inviteDtos = gbt28181Service.listInvite(param);
        return Result.success(inviteDtos);
    }

    @PostMapping("/stopInvite")
    public Result<?> stopInvite(@RequestBody QueryParam param){
        gbt28181Service.stopInvite(param);
        return Result.success();
    }

    @PostMapping("/pagePlatform")
    public Result<Page<Platform>> pagePlatform(@RequestBody QueryParam param){
        Page<Platform> page = gbt28181Service.pagePlatform(param);
        return Result.success(page);
    }

    @PostMapping("/savePlatform")
    public Result<?> savePlatform(@RequestBody Platform platform){
        gbt28181Service.savePlatform(platform);
        return Result.success();
    }

    @PostMapping("/deletePlatform")
    public Result<?> deletePlatform(@RequestBody Platform platform){
        gbt28181Service.deletePlatform(platform);
        return Result.success();
    }

    @PostMapping("/enablePlatform")
    public Result<?> enablePlatform(@RequestBody Platform platform){
        gbt28181Service.enablePlatform(platform);
        return Result.success();
    }

    @PostMapping("/savePlatformChannelList")
    public Result<?> savePlatformChannelList(@RequestBody Platform platform){
        gbt28181Service.savePlatformChannelList(platform);
        return Result.success();
    }

    @PostMapping("/listPlatformChannel")
    public Result<List<PlatformChannel> > listPlatformChannel(@RequestBody QueryParam param){
        List<PlatformChannel> platformChannels = gbt28181Service.listPlatformChannel(param);
        return Result.success(platformChannels);
    }
}
