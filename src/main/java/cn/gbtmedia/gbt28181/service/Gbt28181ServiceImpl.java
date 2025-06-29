package cn.gbtmedia.gbt28181.service;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.exception.AppException;
import cn.gbtmedia.common.extra.LocalFileHandler;
import cn.gbtmedia.gbt28181.dto.InfoDto;
import cn.gbtmedia.gbt28181.server.media.client.MediaClient;
import cn.gbtmedia.gbt28181.server.media.server.MediaServer;
import cn.gbtmedia.gbt28181.dto.BroadcastDto;
import cn.gbtmedia.gbt28181.dto.CatalogDto;
import cn.gbtmedia.gbt28181.dto.ChannelTreeDto;
import cn.gbtmedia.gbt28181.dto.InviteDto;
import cn.gbtmedia.gbt28181.dto.QueryParam;
import cn.gbtmedia.gbt28181.dto.RecordDto;
import cn.gbtmedia.gbt28181.dto.RecordFileDto;
import cn.gbtmedia.gbt28181.dto.SipResult;
import cn.gbtmedia.gbt28181.dto.StreamDto;
import cn.gbtmedia.gbt28181.dto.TalkDto;
import cn.gbtmedia.gbt28181.server.sip.event.CatalogEvent;
import cn.gbtmedia.gbt28181.server.sip.send.SipDeviceSend;
import cn.gbtmedia.gbt28181.server.sip.send.SipPlatformSend;
import cn.gbtmedia.gbt28181.server.sip.session.ClientInvite;
import cn.gbtmedia.gbt28181.server.sip.session.ServerInvite;
import cn.gbtmedia.gbt28181.server.media.MediaType;
import cn.gbtmedia.gbt28181.server.sip.session.SipSessionManger;
import cn.gbtmedia.gbt28181.entity.Device;
import cn.gbtmedia.gbt28181.entity.DeviceChannel;
import cn.gbtmedia.gbt28181.repository.DeviceChannelRepository;
import cn.gbtmedia.gbt28181.repository.DeviceRepository;
import cn.gbtmedia.gbt28181.entity.Platform;
import cn.gbtmedia.gbt28181.entity.PlatformChannel;
import cn.gbtmedia.gbt28181.repository.PlatformChannelRepository;
import cn.gbtmedia.gbt28181.repository.PlatformRepository;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xqs
 */
@Slf4j
@Service("gbt28181Service")
public class Gbt28181ServiceImpl implements Gbt28181Service {

    @Resource
    private ServerConfig serverConfig;
    @Resource
    private DeviceRepository deviceRepository;
    @Resource
    private DeviceChannelRepository deviceChannelRepository;
    @Resource
    private SipDeviceSend sipDeviceSend;
    @Resource
    private PlatformRepository platformRepository;
    @Resource
    private PlatformChannelRepository platformChannelRepository;
    @Resource
    private SipPlatformSend sipPlatformSend;
    @Resource
    private LocalFileHandler localFileHandler;

    @Override
    public InfoDto info(QueryParam param) {
        InfoDto infoDto = new InfoDto();
        // 国标信息
        infoDto.setGbt28181(new InfoDto.Gbt28181());
        infoDto.getGbt28181().setAccessIp(serverConfig.getAccessIp());
        BeanUtil.copyProperties(serverConfig.getGbt28181(), infoDto.getGbt28181());
        // 设备统计
        List<Device> devices = deviceRepository.findAll();
        infoDto.setDevices(new InfoDto.Devices());
        infoDto.getDevices().setTotal(devices.size());
        infoDto.getDevices().setOnline((int) devices.stream().filter(d -> d.getOnline() == 1).count());
        infoDto.getDevices().setOffline(devices.size() - infoDto.getDevices().getOnline());
        // 通道统计
        List<DeviceChannel> channels = deviceChannelRepository.findAll();
        infoDto.setChannels(new InfoDto.Channels());
        infoDto.getChannels().setTotal(channels.size());
        infoDto.getChannels().setOnline((int) channels.stream().filter(c -> c.getOnline() == 1).count());
        infoDto.getChannels().setOffline(channels.size() - infoDto.getChannels().getOnline());
        return infoDto;
    }

    @Override
    public Page<Device> pageDevice(QueryParam param) {
        Specification<Device> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getDeviceId())){
                predicates.add(criteriaBuilder.like(root.get("deviceId"),"%" + param.getDeviceId() + "%"));
            }
            if(ObjectUtil.isNotEmpty(param.getSipIp())){
                predicates.add(criteriaBuilder.like(root.get("sipIp"),"%" + param.getSipIp() + "%"));
            }
            if(ObjectUtil.isNotEmpty(param.getOnline())){
                predicates.add(criteriaBuilder.equal(root.get("online"),param.getOnline()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return deviceRepository.findAll(specification,page);
    }

    @Override
    public void updateDevice(Device device) {
        Device old = deviceRepository.findByDeviceId(device.getDeviceId());
        if(ObjectUtil.isNotEmpty(device.getMediaTransport())){
            old.setMediaTransport(device.getMediaTransport());
        }
        if(ObjectUtil.isNotNull(device.getCustomName())){
            old.setCustomName(device.getCustomName());
        }
        if(ObjectUtil.isNotNull(device.getMaxPlaybackStream())){
            old.setMaxPlaybackStream(device.getMaxPlaybackStream());
        }
        if(ObjectUtil.isNotNull(device.getMaxDownloadStream())){
            old.setMaxDownloadStream(device.getMaxDownloadStream());
        }
        if(ObjectUtil.isNotNull(device.getMaxPlayStream())){
            old.setMaxPlayStream(device.getMaxPlayStream());
        }
        deviceRepository.save(old);
    }

    @Override
    public void deleteDevice(Device device) {
        Device old = deviceRepository.findByDeviceId(device.getDeviceId());
        deviceRepository.delete(old);
        List<DeviceChannel> deviceChannelList = deviceChannelRepository.findByDeviceId(device.getDeviceId());
        deviceChannelList.forEach(this::deleteDeviceChannel);
    }

    @Override
    public void queryDeviceInfo(QueryParam param) {
        SipResult<?> result = sipDeviceSend.queryDeviceInfo(param.getDeviceId());
        if(!result.isSuccess()){
            throw new AppException(result.getMessage());
        }
    }

    @Override
    public Page<DeviceChannel> pageDeviceChannel(QueryParam param) {
        Specification<DeviceChannel> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getDeviceId())){
                predicates.add(criteriaBuilder.equal(root.get("deviceId"),param.getDeviceId()));
            }
            if(ObjectUtil.isNotEmpty(param.getChannelId())){
                predicates.add(criteriaBuilder.like(root.get("channelId"),"%" + param.getChannelId() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<DeviceChannel> channelPage = deviceChannelRepository.findAll(specification, page);
        // 判断是否在实时播放中
        channelPage.getContent().forEach(v->{
            ServerInvite serverInvite = SipSessionManger.getInstance()
                    .getServerInvite(v.getDeviceId(), v.getChannelId(), MediaType.play);
            if(serverInvite != null){
                v.setPlaySsrc(serverInvite.getSsrc());
                v.setPlayCallId(serverInvite.getCallId());
            }
        });
        return channelPage;
    }

    @Override
    public void updateDeviceChannel(DeviceChannel deviceChannel) {
        DeviceChannel old = deviceChannelRepository.findByDeviceIdAndChannelId(deviceChannel.getDeviceId(),
                deviceChannel.getChannelId());
        if(ObjectUtil.isNotNull(deviceChannel.getCustomName())){
            old.setCustomName(deviceChannel.getCustomName());
        }
        if(ObjectUtil.isNotNull(deviceChannel.getCloudRecord())){
            old.setCloudRecord(deviceChannel.getCloudRecord());
        }
        deviceChannelRepository.save(old);
    }

    @Override
    public void deleteDeviceChannel(DeviceChannel deviceChannel) {
        DeviceChannel old = deviceChannelRepository.findByDeviceIdAndChannelId(deviceChannel.getDeviceId(), deviceChannel.getChannelId());
        deviceChannelRepository.delete(old);
        // 删除关联共享的上级通道
        List<PlatformChannel> platformChannelList = platformChannelRepository.findByDeviceIdAndChannelId(old.getDeviceId(), old.getChannelId());
        for(PlatformChannel platformChannel : platformChannelList){
            platformChannelRepository.delete(platformChannel);
            platformChannel.setDeviceChannel(old);
            SpringUtil.publishEvent(new CatalogEvent(this, platformChannel.getPlatformId(), List.of(platformChannel),"DEL"));
        }
        // 停止所有推流
        List<ServerInvite> serverInvites = SipSessionManger.getInstance().getAllServerInvite();
        for (ServerInvite serverInvite : serverInvites){
            if(old.getDeviceId().equals(serverInvite.getDeviceId())){
                sipDeviceSend.stopServerInvite(serverInvite.getSsrc());
            }
        }
    }

    @Override
    public List<ChannelTreeDto> channelTree(QueryParam param) {
        List<Device> deviceList = deviceRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<DeviceChannel> channelList = deviceChannelRepository.findAll();
        List<ChannelTreeDto> treeDtos = new ArrayList<>();
        for(Device device : deviceList){
            ChannelTreeDto dto = new ChannelTreeDto();
            treeDtos.add(dto);
            dto.setGbId(device.getDeviceId());
            dto.setOnline(device.getOnline());
            dto.setCustomName(ObjectUtil.defaultIfEmpty(device.getCustomName(),device.getName()));
            List<ChannelTreeDto> children = channelList.stream().filter(v -> v.getDeviceId().equals(device.getDeviceId()))
                    .map(v2 -> {
                        ChannelTreeDto dto2 = new ChannelTreeDto();
                        dto2.setGbId(v2.getChannelId());
                        dto2.setOnline(v2.getOnline());
                        dto2.setCustomName(ObjectUtil.defaultIfEmpty(v2.getCustomName(),v2.getName()));
                        return dto2;
                    }).toList();
            dto.setChildren(children);
        }
        return treeDtos;
    }

    @Override
    public CatalogDto queryCatalog(QueryParam param) {
        SipResult<CatalogDto> result = sipDeviceSend.queryCatalog(param.getDeviceId());
        if(!result.isSuccess()){
            throw new AppException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public StreamDto play(QueryParam param) {
        SipResult<StreamDto> sipResult = sipDeviceSend.play(param.getDeviceId(), param.getChannelId());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        return sipResult.getData();
    }

    @Override
    public void stopPlay(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.stopPlay(param.getSsrc());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public RecordDto queryRecordInfo(QueryParam param) {
        StopWatch stopWatch = new StopWatch("queryRecordInfo");
        // 发送信令
        stopWatch.start("sendSip");
        SipResult<RecordDto> sipResult = sipDeviceSend.queryRecordInfo(param.getDeviceId(), param.getChannelId(),
                param.getStartTime(), param.getEndTime());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        stopWatch.stop();
        // 修改时间格式
        List<RecordDto.Item> recordList = sipResult.getData().getRecordList();
        recordList.forEach(v->{
            v.setStartTime(DateUtil.parse(v.getStartTime()).toString());
            v.setEndTime(DateUtil.parse(v.getEndTime()).toString());
        });
        // 获取推流下载进度
        List<ServerInvite> serverInviteList = SipSessionManger.getInstance()
                .getServerInviteList(param.getDeviceId(), param.getChannelId(), MediaType.download);
        recordList.forEach(v->{
            serverInviteList.stream().filter(v1->new DateTime(v1.getStartTime()).toString().equals(new DateTime(v.getStartTime()).toString())
                            && new DateTime(v1.getEndTime()).toString().equals(new DateTime(v.getEndTime()).toString()))
                    .findFirst().ifPresent(v3->{
                        v.setCallId(v3.getCallId());
                        v.setSsrc(v3.getSsrc());
                        v.setProgress(1);
                        String progress = v3.getMediaServer().getRecordProgress();
                        if(ObjectUtil.isNotEmpty(progress)){
                            String[] split = progress.split("/");
                            int percentage = (int) Math.round((double) Integer.parseInt(split[0]) / Integer.parseInt(split[1]) * 100);
                            v.setProgress(Math.min(99, percentage>0?percentage:1));
                        }
                    });
        });
        // 读取文件获取下载进度
        stopWatch.start("getDownloadProgress");
        List<RecordFileDto> recordFileDtos = listRecordFile(new QueryParam());
        recordList.forEach(v->{
            recordFileDtos.stream()
                    .filter(v1->v1.getType() == 1)
                    .filter(v1->v1.getDeviceId().equals(param.getDeviceId()) && v1.getChannelId().equals(param.getChannelId()))
                    .filter(v1->new DateTime(v1.getStartTime()).toString().equals(new DateTime(v.getStartTime()).toString())
                            && new DateTime(v1.getEndTime()).toString().equals(new DateTime(v.getEndTime()).toString()))
                    .findFirst().ifPresent(v3->{
                        v.setProgress(100);
                        v.setFileName(v3.getFileName());
                    });
        });
        stopWatch.stop();
        // 时间排序
        recordList.sort((v1, v2) -> {
            try {
                return DateUtil.parse(v1.getStartTime()).compareTo(DateUtil.parse(v2.getStartTime()));
            } catch (Exception ex) {
                log.error("parse time ex",ex);
                return 0;
            }
        });
        log.info("queryRecordInfo deviceId {} cost {}",param.getDeviceId(),stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
        return sipResult.getData();
    }

    @Override
    public StreamDto playback(QueryParam param) {
        SipResult<StreamDto> sipResult = sipDeviceSend.playback(param.getDeviceId(), param.getChannelId(),
                param.getStartTime(), param.getEndTime());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        return sipResult.getData();
    }

    @Override
    public void playbackSpeed(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.playbackSpeed(param.getSsrc(),param.getSpeed());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public void stopPlayback(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.stopPlayback(param.getSsrc());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public StreamDto download(QueryParam param) {
        SipResult<StreamDto> sipResult = sipDeviceSend.download(param.getDeviceId(), param.getChannelId(),
                param.getStartTime(), param.getEndTime(),param.getDownloadSpeed());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        return sipResult.getData();
    }

    @Override
    public void stopDownload(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.stopDownload(param.getSsrc());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public TalkDto talk(QueryParam param) {
        SipResult<TalkDto> sipResult = sipDeviceSend.talk(param.getDeviceId(), param.getChannelId());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        return sipResult.getData();
    }

    @Override
    public void stopTalk(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.stopTalk(param.getSsrc());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public BroadcastDto broadcast(QueryParam param) {
        SipResult<BroadcastDto> sipResult = sipDeviceSend.broadcast(param.getDeviceId(), param.getChannelId());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
        return sipResult.getData();
    }

    @Override
    public void stopBroadcast(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.stopBroadcast(param.getCallId());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public void controlPtzCmd(QueryParam param) {
        SipResult<?> sipResult = sipDeviceSend.controlPtzCmd(param.getDeviceId(),param.getChannelId(),param.getLeftRight(),
                param.getUpDown(),param.getInOut(),param.getMoveSpeed(),param.getZoomSpeed());
        if(!sipResult.isSuccess()){
            throw new AppException(sipResult.getMessage());
        }
    }

    @Override
    public List<RecordFileDto> listRecordFile(QueryParam param) {
        List<RecordFileDto> fileDtoList = new ArrayList<>();
        ServerConfig.Gbt28181 gbt28181 = ServerConfig.getInstance().getGbt28181();
        List<File> deviceFiles = Arrays.stream(FileUtil.ls(gbt28181.getRecordPathDevice())).filter(File::isDirectory).toList();
        for (File subDir : deviceFiles) {
            String dirName = subDir.getName();
            String[] parts = dirName.split("_");
            if (parts.length == 4) {
                String deviceId = parts[0];
                String channelId = parts[1];
                String startTimeStr = parts[2];
                String endTimeStr = parts[3];
                Date startTime = DateUtil.parse(startTimeStr);
                Date endTime = DateUtil.parse(endTimeStr);
                File[] files = subDir.listFiles((file, name) -> !name.equals("temp"));
                if (files != null) {
                    for (File file : files) {
                        RecordFileDto fileDto = new RecordFileDto();
                        fileDto.setDeviceId(deviceId);
                        fileDto.setChannelId(channelId);
                        fileDto.setStartTime(startTime);
                        fileDto.setEndTime(endTime);
                        fileDto.setFileName(file.getName());
                        fileDto.setFileSize(FileUtil.readableFileSize(file));
                        fileDto.setCreateTime(new Date(file.lastModified()));
                        fileDto.setType(1);
                        fileDtoList.add(fileDto);
                    }
                }
            }
        }
        List<File> cloudFiles = Arrays.stream(FileUtil.ls(gbt28181.getRecordPathCloud())).filter(File::isDirectory).toList();
        for (File subDir : cloudFiles) {
            String dirName = subDir.getName();
            String[] parts = dirName.split("_");
            if (parts.length == 2) {
                String deviceId = parts[0];
                String channelId = parts[1];
                File[] files = subDir.listFiles((file, name) -> !name.equals("temp"));
                if (files != null) {
                    for (File file : files) {
                        String[] nameSplit = file.getName().replace(".mp4","").split("_");
                        if(nameSplit.length == 3){
                            String id = nameSplit[0];
                            String startTimeStr = nameSplit[1];
                            String endTimeStr = nameSplit[2];
                            Date startTime = DateUtil.parse(startTimeStr);
                            Date endTime = DateUtil.parse(endTimeStr);
                            RecordFileDto fileDto = new RecordFileDto();
                            fileDto.setDeviceId(deviceId);
                            fileDto.setChannelId(channelId);
                            fileDto.setStartTime(startTime);
                            fileDto.setEndTime(endTime);
                            fileDto.setFileName(file.getName());
                            fileDto.setFileSize(FileUtil.readableFileSize(file));
                            fileDto.setCreateTime(new Date(file.lastModified()));
                            fileDto.setType(2);
                            fileDtoList.add(fileDto);
                        }
                    }
                }
            }
        }
        fileDtoList.sort(Comparator.comparing(RecordFileDto::getCreateTime).reversed());
        return fileDtoList.stream().filter(v -> {
            boolean match = true;
            if (ObjectUtil.isNotEmpty(param.getDeviceId())) {
                match &= v.getDeviceId().equals(param.getDeviceId());
            }
            if (ObjectUtil.isNotEmpty(param.getChannelId())) {
                match &= v.getChannelId().equals(param.getChannelId());
            }
            return match;
        }).toList();
    }

    @Override
    @SneakyThrows
    public void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, String fileName) {
        String recordPath = ServerConfig.getInstance().getGbt28181().getRecordPath();
        List<File> files = FileUtil.loopFiles(recordPath);
        File file = files.stream().filter(v -> v.getName().equals(fileName)).findFirst().orElse(null);
        if(file == null){
            response.setStatus(404);
            return;
        }
        request.setAttribute(LocalFileHandler.FILE_PATH, file.getAbsolutePath());
        localFileHandler.handleRequest(request,response);
    }

    @Override
    public void deleteRecordFile(RecordFileDto recordFileDto) {
        String recordPath = ServerConfig.getInstance().getGbt28181().getRecordPath();
        List<File> files =  FileUtil.loopFiles(recordPath);
        File file = files.stream().filter(v -> v.getName().equals(recordFileDto.getFileName())).findFirst().orElse(null);
        if(file == null){
            return;
        }
        FileUtil.del(file);
        if(FileUtil.isEmpty(file.getParentFile())){
            FileUtil.del(file.getParentFile());
        }
    }

    @Override
    public List<InviteDto> listInvite(QueryParam param) {
        List<InviteDto> inviteDtos = new ArrayList<>();
        List<ServerInvite> serverInvites = SipSessionManger.getInstance().getAllServerInvite();
        for( ServerInvite serverInvite : serverInvites){
            if(serverInvite.getMediaType().equals(MediaType.broadcast)){
                continue;
            }
            if(!serverInvite.isInviteAck()){
                continue;
            }
            InviteDto inviteDto = new InviteDto();
            inviteDtos.add(inviteDto);
            BeanUtil.copyProperties(serverInvite,inviteDto);
            inviteDto.setInviteType(serverInvite.getMediaType().toString());
            MediaServer mediaServer = serverInvite.getMediaServer();
            if(mediaServer != null){
                inviteDto.setExtInfo(mediaServer.getRecordProgress());
                BeanUtil.copyProperties(mediaServer, inviteDto);
                inviteDto.setRxRate(mediaServer.getRxRate());
                inviteDto.setTxRate(mediaServer.getTxRate());
                inviteDto.setViewNum(mediaServer.getViewNum());
            }
            // 级联上级的推流
            List<ClientInvite> clientInvites = serverInvite.getClientInvites();
            List<InviteDto> list = clientInvites.stream().map(v -> {
                InviteDto v1 = new InviteDto();
                BeanUtil.copyProperties(v, v1);
                v1.setInviteType(v.getMediaType().toString());
                MediaClient mediaClient = v.getMediaClient();
                if (mediaClient != null) {
                    BeanUtil.copyProperties(mediaClient, v1);
                    v1.setRxRate(mediaClient.getRxRate());
                    v1.setTxRate(mediaClient.getTxRate());
                }
                return v1;
            }).toList();
            inviteDto.setPlatformInvites(list);
        }
        // 广播特殊
        List<ClientInvite> clientInvites = SipSessionManger.getInstance().getAllClientInvite();
        for( ClientInvite clientInvite : clientInvites){
            if(!clientInvite.getMediaType().equals(MediaType.broadcast)){
                continue;
            }
            if(!clientInvite.isInviteAck()){
                continue;
            }
            InviteDto inviteDto = new InviteDto();
            inviteDtos.add(inviteDto);
            BeanUtil.copyProperties(clientInvite,inviteDto);
            inviteDto.setInviteType(clientInvite.getMediaType().toString());
            MediaClient mediaClient = clientInvite.getMediaClient();
            if(mediaClient != null){
                BeanUtil.copyProperties(mediaClient, inviteDto);
                inviteDto.setRxRate(mediaClient.getRxRate());
                inviteDto.setTxRate(mediaClient.getTxRate());
            }
            // 级联上级的推流
            ServerInvite v = clientInvite.getServerInvite();
            if(v != null){
                InviteDto v1 = new InviteDto();
                BeanUtil.copyProperties(v, v1);
                v1.setInviteType(v.getMediaType().toString());
                MediaServer mediaServer = v.getMediaServer();
                if (mediaServer != null) {
                    BeanUtil.copyProperties(mediaServer, v1);
                    v1.setRxRate(mediaServer.getRxRate());
                    v1.setTxRate(mediaServer.getTxRate());
                }
                inviteDto.setPlatformInvites(List.of(v1));
            }
            inviteDto.setPlatformInvites(new ArrayList<>());
        }
        inviteDtos = inviteDtos.stream().filter(v -> {
                boolean match = true;
                if (ObjectUtil.isNotEmpty(param.getCallId())) {
                    match &= param.getCallId().equals(v.getCallId());
                }
                if (ObjectUtil.isNotEmpty(param.getSsrc())) {
                    match &= param.getSsrc().equals(v.getSsrc());
                }
                if (ObjectUtil.isNotEmpty(param.getDeviceId())) {
                    match &= param.getDeviceId().equals(v.getDeviceId());
                }
                if (ObjectUtil.isNotEmpty(param.getChannelId())) {
                    match &= param.getChannelId().equals(v.getChannelId());
                }
                return match;
            }).toList();
        return inviteDtos;
    }

    @Override
    public void stopInvite(QueryParam param) {
        SipSessionManger.getInstance().getAllServerInvite().stream().filter(v->param.getCallId().equals(v.getCallId()))
                .findFirst().ifPresent(v->{
                    SipResult<?> sipResult = sipDeviceSend.stopServerInvite(v.getSsrc());
                    if(!sipResult.isSuccess()){
                        throw new AppException(sipResult.getMessage());
                    }
                });
        SipSessionManger.getInstance().getAllClientInvite().stream().filter(v->param.getCallId().equals(v.getCallId()))
                .findFirst().ifPresent(v->{
                    SipResult<?> sipResult = sipDeviceSend.stopClientInvite(v.getCallId());
                    if(!sipResult.isSuccess()){
                        throw new AppException(sipResult.getMessage());
                    }
                });
    }

    @Override
    public Page<Platform> pagePlatform(QueryParam param) {
        Specification<Platform> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getDeviceId())){
                predicates.add(criteriaBuilder.like(root.get("platformId"),"%" + param.getDeviceId() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return platformRepository.findAll(specification,page);
    }


    @Override
    public void savePlatform(Platform platform) {
        if(ObjectUtil.isEmpty(platform.getId())){
            platform.setSipTransport("UDP");
            platform.setCharset("GB2312");
            platform.setExpires(3600);
            platform.setKeepTimeout(10);
            platform.setKeepCount(3);
            platform.setOnline(0);
            platform.setEnable(0);
        }else {
            Platform old = platformRepository.findById(platform.getId())
                    .orElseThrow(()->new AppException("no platform"));
            old.setName(platform.getName());
            old.setPlatformId(platform.getPlatformId());
            old.setSipDomain(platform.getSipDomain());
            old.setSipIp(platform.getSipIp());
            old.setSipPort(platform.getSipPort());
            old.setPassword(platform.getPassword());
            platform = old;
        }
        platformRepository.save(platform);
    }

    @Override
    public void deletePlatform(Platform platform) {
        Platform old = platformRepository.findByPlatformId(platform.getPlatformId());
        // 只能删除未启用的
        if(platform.getEnable() == 1){
            throw new AppException("platform is enable");
        }
        platformRepository.delete(old);
    }

    @Override
    public void enablePlatform(Platform platform) {
        Platform old = platformRepository.findByPlatformId(platform.getPlatformId());
        old.setEnable(platform.getEnable());
        platformRepository.save(old);
        // 注册上级平台
        if(platform.getEnable() == 1){
            SipResult<?> sipResult = sipPlatformSend.register(platform.getPlatformId());
        }else if(platform.getOnline() == 1){
            // 取消注册上级平台
            SipResult<?> sipResult = sipPlatformSend.unRegister(platform.getPlatformId());
            // 关闭所有推流
            List<ClientInvite> clientInvites = SipSessionManger.getInstance().getAllClientInvite();
            for (ClientInvite clientInvite : clientInvites){
                if(platform.getPlatformId().equals(clientInvite.getPlatformId())){
                    sipDeviceSend.stopClientInvite(clientInvite.getCallId());
                }
            }
            List<ServerInvite> serverInvites = SipSessionManger.getInstance().getAllServerInvite();
            for (ServerInvite serverInvite : serverInvites){
                if(platform.getPlatformId().equals(serverInvite.getPlatformId())){
                    sipDeviceSend.stopServerInvite(serverInvite.getSsrc());
                }
            }
        }
    }

    @Override
    public void savePlatformChannelList(Platform platform) {
        String platformId = platform.getPlatformId();
        platform.getPlatformChannelList().forEach(v->v.setPlatformId(platformId));
        List<PlatformChannel> newList = platform.getPlatformChannelList();
        List<PlatformChannel> oldList = platformChannelRepository.findByPlatformId(platformId);
        platformChannelRepository.deleteAll(oldList);
        platformChannelRepository.saveAll(newList);
        // 找出新增和删除
        List<PlatformChannel> addList = newList.stream()
                .filter(v -> oldList.stream()
                        .noneMatch(v1 -> v1.getChannelId().equals(v.getChannelId()) && v1.getDeviceId().equals(v.getDeviceId())))
                .collect(Collectors.toList());
        SpringUtil.publishEvent(new CatalogEvent(this, platformId, addList, "ADD"));
        List<PlatformChannel> delList = oldList.stream()
                .filter(v -> newList.stream()
                        .noneMatch(v1 -> v1.getChannelId().equals(v.getChannelId()) && v1.getDeviceId().equals(v.getDeviceId())))
                .collect(Collectors.toList());
        SpringUtil.publishEvent(new CatalogEvent(this, platformId, delList, "DEL"));
    }

    @Override
    public List<PlatformChannel> listPlatformChannel(QueryParam param) {
        return platformChannelRepository.findByPlatformId(param.getPlatformId());
    }

}
