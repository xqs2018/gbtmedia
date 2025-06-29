package cn.gbtmedia.jtt808.service;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.common.exception.AppException;
import cn.gbtmedia.common.extra.LocalFileHandler;
import cn.gbtmedia.common.aop.SyncWait;
import cn.gbtmedia.common.extra.FFmpegExec;
import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.gbtmedia.jtt808.dto.ClientMediaDto;
import cn.gbtmedia.jtt808.dto.CmdResult;
import cn.gbtmedia.jtt808.dto.InfoDto;
import cn.gbtmedia.jtt808.dto.QueryParam;
import cn.gbtmedia.jtt808.dto.DownloadRecordTask;
import cn.gbtmedia.jtt808.dto.RecordDto;
import cn.gbtmedia.jtt808.dto.RecordFileDto;
import cn.gbtmedia.jtt808.dto.StreamDto;
import cn.gbtmedia.jtt808.repository.ClientLocationRepository;
import cn.gbtmedia.jtt808.server.cmd.event.T1206Event;
import cn.gbtmedia.jtt808.server.cmd.message.T0001;
import cn.gbtmedia.jtt808.server.cmd.message.T1205;
import cn.gbtmedia.jtt808.server.cmd.message.T9101;
import cn.gbtmedia.jtt808.server.cmd.message.T9102;
import cn.gbtmedia.jtt808.server.cmd.message.T9201;
import cn.gbtmedia.jtt808.server.cmd.message.T9205;
import cn.gbtmedia.jtt808.server.cmd.message.T9206;
import cn.gbtmedia.jtt808.server.cmd.message.T9207;
import cn.gbtmedia.jtt808.server.cmd.send.Jtt1078Send;
import cn.gbtmedia.jtt808.server.cmd.session.ClientMedia;
import cn.gbtmedia.jtt808.server.cmd.session.Jtt808SessionManager;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import cn.gbtmedia.jtt808.server.media.MediaManger;
import cn.gbtmedia.jtt808.server.media.MediaParam;
import cn.gbtmedia.jtt808.server.media.server.MediaServer;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.progress.Progress;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
@Service("jtt808Service")
public class Jtt808ServiceImpl implements Jtt808Service {

    @Resource
    private Jtt1078Send jt1078Send;
    @Resource
    private ClientRepository clientRepository;
    @Resource
    private ClientLocationRepository clientLocationRepository;
    @Resource
    private LocalFileHandler localFileHandler;

    private static final ExecutorService JTT808_SERVICE_POOL =
            Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("vt-jtt808-service-", 0)
                            .uncaughtExceptionHandler((t, e) -> log.error("jtt808 service pool ex t {}", t, e))
                            .factory());

    @Override
    public InfoDto info(QueryParam param) {
        InfoDto info = new InfoDto();
        InfoDto.Jtt808 jtt808 = new InfoDto.Jtt808();
        info.setJtt808(jtt808);
        jtt808.setAccessIp(ServerConfig.getInstance().getAccessIp());
        jtt808.setCmdPort(String.valueOf(ServerConfig.getInstance().getJtt808().getCmdPort()));
        return info;
    }

    @Override
    public Page<Client> pageClient(QueryParam param) {
        Specification<Client> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getClientId())){
                predicates.add(criteriaBuilder.like(root.get("clientId"),"%" + param.getClientId() + "%"));
            }
            if(ObjectUtil.isNotEmpty(param.getClientIp())){
                predicates.add(criteriaBuilder.like(root.get("clientIp"),"%" + param.getClientIp() + "%"));
            }
            if(ObjectUtil.isNotEmpty(param.getOnline())){
                predicates.add(criteriaBuilder.equal(root.get("online"),param.getOnline()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return clientRepository.findAll(specification,page);
    }

    @Override
    public void updateClient(Client client) {
        Client old = clientRepository.findByClientId(client.getClientId());
        old.setMaxVideoChannels(client.getMaxVideoChannels());
        clientRepository.save(old);
    }

    @Override
    public void deleteDevice(Client client) {
        Client old = clientRepository.findByClientId(client.getClientId());
        clientRepository.delete(old);
        // 停止所有推流
        List<ClientMedia> clientMediaList = Jtt808SessionManager.getInstance().getAllClientMedia();
        for(ClientMedia clientMedia: clientMediaList){
            if(old.getClientId().equals(clientMedia.getClientId())){
                stopClientMedia(clientMedia.getMediaKey());
            }
        }
    }

    @Override
    public Page<ClientLocation> pageClientLocation(QueryParam param) {
        Specification<ClientLocation> specification = (root, query, criteriaBuilder)->{
            List<Predicate> predicates = new ArrayList<>();
            if(ObjectUtil.isNotEmpty(param.getClientId())){
                predicates.add(criteriaBuilder.equal(root.get("clientId"), param.getClientId()));
            }
            if(ObjectUtil.isNotEmpty(param.getStartTime()) && ObjectUtil.isNotEmpty(param.getEndTime())){
                predicates.add(criteriaBuilder.between(root.get("createTime"), param.getStartTime(), param.getEndTime()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest page = PageRequest.of(param.getPageNo(), param.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return clientLocationRepository.findAll(specification, page);
    }

    @Override
    public List<ClientLocation> lastClientLocation(QueryParam param) {
        List<ClientLocation> result = new ArrayList<>();
        List<ClientLocation> lastClientLocation = clientLocationRepository.findLastClientLocation();
        List<Client> clientList = clientRepository.findAll();
        for(Client client : clientList){
            ClientLocation clientLocation = lastClientLocation.stream()
                    .filter(v -> v.getClientId().equals(client.getClientId())).findFirst().orElse(null);
            // 没有上报过位置，生成默认的位置
            if(clientLocation == null){
                clientLocation = new ClientLocation();
                BeanUtil.copyProperties(client,clientLocation);
                clientLocation.setClientStatus(client.getOnline());
            }else {
                clientLocation.setClientStatus(client.getOnline()==0?0:clientLocation.getAlarmName()==null?1:2);
            }
            result.add(clientLocation);
        }
        return result;
    }

    @Override
    @SyncWait(key = "#param.clientId")
    public StreamDto play(QueryParam param){
        log.info("play clientId {}",param.getClientId());
        try {
            ServerConfig config = ServerConfig.getInstance();
            String mediaIp = config.getAccessIp();
            String clientId = param.getClientId();
            Integer channelNo = param.getChannelNo();
            // 已经在播放，直接返回
            String mediaKey = String.format("%s_%s_%s","play",clientId,channelNo);
            ClientMedia clientMedia = Jtt808SessionManager.getInstance().getClientMedia(mediaKey);
            if(clientMedia != null){
                StreamDto streamDto = new StreamDto();
                BeanUtil.copyProperties(clientMedia,streamDto);
                return streamDto;
            }
            // 开始播放
            clientMedia = new ClientMedia();
            clientMedia.setMediaType("play");
            clientMedia.setClientId(clientId);
            clientMedia.setChannelNo(channelNo);
            clientMedia.setMediaKey(mediaKey);
            Jtt808SessionManager.getInstance().putClientMedia(clientMedia);
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaKey(mediaKey);
            mediaParam.setMediaType(clientMedia.getMediaType());
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new AppException("media err");
            }
            clientMedia.setHttpFlv(mediaServer.getHttFlv());
            clientMedia.setMediaServer(mediaServer);
            mediaServer.setMediaIp(mediaIp);
            mediaServer.setMediaKey(mediaKey);
            log.info("play mediaKey {} mediaIp {} mediaPort {} ", mediaKey, mediaServer.getMediaIp(),mediaServer.getMediaPort());
            // 给设备下发推流指令
            T9101 t9101 = new T9101();
            t9101.setClientId(clientId);
            t9101.setIpLength(mediaIp.length());
            t9101.setIp(mediaIp);
            t9101.setTcpPort(mediaServer.getMediaPort());
            t9101.setUdpPort(mediaServer.getMediaPort());
            t9101.setChannelNo(channelNo);
            t9101.setMediaType(0);
            t9101.setStreamType(0);
            // 某些终端不返回
            JTT808_SERVICE_POOL.execute(()->{
                CmdResult<T0001> result = jt1078Send.sendT9101(t9101);
            });
            boolean await = clientMedia.getMediaServer().awaitStream();
            if(!await){
                throw new AppException("await stream timeOut");
            }
            StreamDto streamDto = new StreamDto();
            BeanUtil.copyProperties(clientMedia,streamDto);
            return streamDto;
        }catch (Exception ex){
            log.error("play ex clientId {} channelNo {}",param.getClientId(), param.getChannelNo(),ex);
            stopPlay(param);
            throw new AppException(ex.getMessage());
        }
    }

    @Override
    public void stopPlay(QueryParam param) {
        String clientId = param.getClientId();
        Integer channelNo = param.getChannelNo();
        String mediaKey = String.format("%s_%s_%s","play",clientId,channelNo);
        param.setMediaKey(mediaKey);
        stopClientMedia(mediaKey);
    }

    @Override
    @SyncWait(key = "#param.clientId")
    public RecordDto queryRecordInfo(QueryParam param) {
        log.info("queryRecordInfo clientId {}",param.getClientId());

        String clientId = param.getClientId();
        // 默认时间范围，今天
        if(param.getStartTime() == null){
            param.setStartTime(new DateTime(new DateTime().toDateStr()));
        }
        if(param.getEndTime() == null){
            param.setEndTime(new DateTime());
        }
        T9205 t9205 = new T9205();
        t9205.setClientId(clientId);
        t9205.setChannelNo(param.getChannelNo());
        t9205.setStartTime(new DateTime(param.getStartTime()).toString("yyMMddHHmmss"));
        t9205.setEndTime(new DateTime(param.getEndTime()).toString("yyMMddHHmmss"));
        StopWatch stopWatch = new StopWatch("queryRecordInfo");
        // 发送指令
        stopWatch.start("sendT9205");
        CmdResult<T1205> result = jt1078Send.sendT9205(t9205);
        if(!result.isSuccess()){
            throw new AppException(result.getMessage());
        }
        stopWatch.stop();
        // 设置id
        RecordDto dto = new RecordDto();
        dto.setClientId(clientId);
        dto.setRecordList(new ArrayList<>());
        List<RecordDto.Item> recordList = dto.getRecordList();
        // 获取下载进度
        stopWatch.start("getDownloadProgress");
        for(T1205.Item v: result.getData().getItems()){
            RecordDto.Item item = new RecordDto.Item();
            BeanUtil.copyProperties(v, item);
            // 查看对应文件是否在下载中
            String tempPath = getRecordTempPath(clientId, item.getChannelNo(), item.getWarnBit1(), item.getWarnBit2(),
                    item.getMediaType(), item.getStreamType(), item.getStorageType(), item.getStartTime(), item.getEndTime());
            param.setTempPath(tempPath);
            DownloadRecordTask task = checkDownloadTask(param);
            if(task != null){
                item.setProgress(task.getProgress());
                item.setDownloadSerialNo(task.getSerialNo());
                item.setFileName(task.getFileName());
            }
            item.setFileSize(FileUtil.readableFileSize(item.getSize()));
            recordList.add(item);
        }
        stopWatch.stop();
        // 修改时间格式
        recordList.forEach(v->{
            try {
                v.setStartTime(DateUtil.parse(v.getStartTime(),"yyMMddHHmmss").toString());
                v.setEndTime(DateUtil.parse(v.getEndTime(),"yyMMddHHmmss").toString());
            }catch (Exception ex){
                log.error("parse time ex",ex);
            }
        });
        // 时间排序
        recordList.sort((v1, v2) -> {
            try {
                return DateUtil.parse(v1.getStartTime()).compareTo(DateUtil.parse(v2.getStartTime()));
            } catch (Exception ex) {
                log.error("parse time ex",ex);
                return 0;
            }
        });
        log.info("queryRecordInfo clientId {} cost {}",param.getClientId(), stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
        return dto;
    }

    @Override
    @SyncWait(key = "#param.clientId")
    public StreamDto playback(QueryParam param) {
        log.info("playback clientId {}",param.getClientId());
        try {
            ServerConfig config = ServerConfig.getInstance();
            String mediaIp = config.getAccessIp();
            String clientId = param.getClientId();
            Integer channelNo = param.getChannelNo();
            // 回放最大取流限制1
            String mediaKey = String.format("%s_%s_%s","playback",clientId,channelNo);
            ClientMedia clientMedia = Jtt808SessionManager.getInstance().getClientMedia(mediaKey);
            if(clientMedia != null){
                log.warn("playback maxPlaybackStream 1");
                stopPlayback(param);
            }
            // 开始播放
            clientMedia = new ClientMedia();
            clientMedia.setMediaType("playback");
            clientMedia.setClientId(clientId);
            clientMedia.setChannelNo(channelNo);
            clientMedia.setMediaKey(mediaKey);
            Jtt808SessionManager.getInstance().putClientMedia(clientMedia);
            // 创建流媒体服务器
            MediaParam mediaParam = new MediaParam();
            mediaParam.setMediaKey(mediaKey);
            mediaParam.setMediaType(clientMedia.getMediaType());
            MediaServer mediaServer = MediaManger.getInstance().createServer(mediaParam);
            if(mediaServer == null){
                throw new AppException("media err");
            }
            clientMedia.setHttpFlv(mediaServer.getHttFlv());
            clientMedia.setMediaServer(mediaServer);
            mediaServer.setMediaIp(mediaIp);
            mediaServer.setMediaKey(mediaKey);
            log.info("playback mediaKey {} mediaIp {} mediaPort {} ", mediaKey, mediaServer.getMediaIp(),mediaServer.getMediaPort());
            // 给设备下发推流指令
            T9201 t9201 = new T9201();
            t9201.setClientId(clientId);
            t9201.setIpLength(mediaIp.length());
            t9201.setIp(mediaIp);
            t9201.setTcpPort(mediaServer.getMediaPort());
            t9201.setUdpPort(mediaServer.getMediaPort());
            t9201.setChannelNo(channelNo);
            t9201.setStreamType(param.getStreamType());
            t9201.setMediaType(param.getMediaType());
            t9201.setStorageType(param.getStorageType());;
            t9201.setPlaybackMode(0);
            t9201.setPlaybackSpeed(0);
            t9201.setStartTime(new DateTime(param.getStartTime()).toString("yyMMddHHmmss"));
            t9201.setEndTime(new DateTime(param.getEndTime()).toString("yyMMddHHmmss"));
            // 某些终端不返回
            JTT808_SERVICE_POOL.execute(()->{
                CmdResult<T1205> result = jt1078Send.sendT9201(t9201);
            });
            boolean await = clientMedia.getMediaServer().awaitStream();
            if(!await){
                throw new AppException("await stream timeOut");
            }
            StreamDto streamDto = new StreamDto();
            BeanUtil.copyProperties(clientMedia,streamDto);
            return streamDto;
        }catch (Exception ex){
            log.error("playback ex clientId {} channelNo {}",param.getClientId(), param.getChannelNo(),ex);
            stopPlayback(param);
            throw new AppException(ex.getMessage());
        }
    }

    @Override
    public void stopPlayback(QueryParam param) {
        String clientId = param.getClientId();
        Integer channelNo = param.getChannelNo();
        String mediaKey = String.format("%s_%s_%s","playback",clientId,channelNo);
        param.setMediaKey(mediaKey);
        stopClientMedia(mediaKey);
    }

    @Override
    @SyncWait(key = "#param.clientId")
    public DownloadRecordTask downloadRecord(QueryParam param) {
        log.info("downloadRecord clientId {}",param.getClientId());
        try {
            String clientId = param.getClientId();
            ServerConfig config = ServerConfig.getInstance();
            String ftpIp = config.getAccessIp();
            int port = config.getFtp().getPort();
            String username = config.getFtp().getUsername();
            String password = config.getFtp().getPassword();
            T9206 t9206 = new T9206();
            t9206.setClientId(clientId);
            t9206.setChannelNo(param.getChannelNo());
            t9206.setStreamType(param.getStreamType());
            t9206.setMediaType(param.getMediaType());
            t9206.setStorageType(param.getStorageType());
            t9206.setIpLength(ftpIp.length());
            t9206.setIp(ftpIp);
            t9206.setPort(port);
            t9206.setUsernameLength(username.length());
            t9206.setUsername(username);
            t9206.setPasswordLength(password.length());
            t9206.setPassword(password);
            t9206.setStartTime(new DateTime(param.getStartTime()).toString("yyMMddHHmmss"));
            t9206.setEndTime(new DateTime(param.getEndTime()).toString("yyMMddHHmmss"));
            // 先上传到ftp的目录，上传通知完成后，合并这个目录下的所有文件，再移动到录像文件目录
            String tempPath = getRecordTempPath(t9206.getClientId(), t9206.getChannelNo(), t9206.getWarnBit1(), t9206.getWarnBit2(),
                    t9206.getMediaType(), t9206.getStreamType(), t9206.getStorageType(), t9206.getStartTime(), t9206.getEndTime());
            t9206.setPathLength(tempPath.length());
            t9206.setPath(tempPath);
            param.setTempPath(tempPath);
            // 单个终端同时只能下载一个
            if(downloadTasks.stream().anyMatch(v -> v.getClientId().equals(clientId))){
                throw new AppException("同时只能下载一个");
            }
            String tempFullPtah = config.getFtp().getPath() + tempPath;
            String recordFullPath = config.getJtt808().getRecordPath() + tempPath.replace(FTP_RECORD_TEMP_DIR,"");
            // 创建完整的目录 ，先删除临时目录和存储目录
            FileUtil.del(tempFullPtah);
            FileUtil.del(recordFullPath);
            FileUtil.mkdir(tempFullPtah);
            log.info("downloadRecord ftpIp {} port {} tempPath {}",ftpIp, port,tempPath);
            CmdResult<T0001> result = jt1078Send.sendT9206(t9206);
            if(!result.isSuccess()){
                FileUtil.del(tempFullPtah);
                throw new AppException(result.getMessage());
            }
            // 后续下载取消，控制，完成通知都用的到
            int serialNo = result.getData().getResponseSerialNo();
            DownloadRecordTask task = new DownloadRecordTask();
            task.setTaskId(IdUtil.fastSimpleUUID());
            task.setClientId(clientId);
            task.setSerialNo(serialNo);
            task.setTempPath(tempPath);
            task.setProgress(1);
            task.setSize(param.getSize());
            task.setTaskStartTime(new Date());
            task.setRecordStartTime(param.getStartTime());
            task.setRecordEndTime(param.getEndTime());
            downloadTasks.add(task);
            log.info("downloadRecord task add {}",task);
            return task;
        }catch (Exception ex){
            log.error("downloadRecord ex clientId {} channelNo {}",param.getClientId(), param.getChannelNo(),ex);
            throw new AppException(ex.getMessage());
        }
    }

    private final List<DownloadRecordTask> downloadTasks = new CopyOnWriteArrayList<>();

    /**
     * 录像临时ftp路径
     */
    private static final String FTP_RECORD_TEMP_DIR = "/jtt808RecordTemp";
    private static String getRecordTempPath(String clientId,int channelNo,int warnBit1,int warnBit2, int mediaType,
                                            int streamType,int storageType,String startTime,String endTime){
        // /clientId_channelNo_warnBit1_warnBit2_mediaType_streamType_storageType_startTIme_endTIme
        return String.format(FTP_RECORD_TEMP_DIR + "/%s_%s_%s_%s_%s_%s_%s_%s_%s",
                clientId, channelNo, warnBit1, warnBit2, mediaType,streamType, storageType, startTime, endTime);
    }

    @Override
    public List<DownloadRecordTask> listDownloadRecordTask(QueryParam param) {
        return downloadTasks.stream().toList();
    }

    @Override
    public void cleanDownloadTask(QueryParam param) {
        String clientId = param.getClientId();
        downloadTasks.forEach(v->{
            if(ObjectUtil.isEmpty(clientId) || clientId.equals(v.getClientId())){
                param.setCommand(2);
                param.setTempPath(v.getTempPath());
                try {
                    downloadRecordControl(param);
                }catch (Exception ex){
                    log.error("cleanDownloadTask ex",ex);
                }
            }
        });
        // 没有指定客户端，删除所有临时文件
        ServerConfig config = ServerConfig.getInstance();
        String ftpPath = config.getFtp().getPath();
        if(ObjectUtil.isEmpty(clientId)){
            FileUtil.del(ftpPath + FTP_RECORD_TEMP_DIR);
        }
    }

    @Override
    public DownloadRecordTask checkDownloadTask(QueryParam param) {
        String tempPath = param.getTempPath();
        if(ObjectUtil.isEmpty(tempPath)){
            DownloadRecordTask task = downloadTasks.stream()
                    .filter(v -> v.getClientId().equals(param.getClientId()))
                    .filter(v -> v.getSerialNo() == param.getSerialNo())
                    .findFirst().orElse(null);
            if(task != null){
                tempPath = task.getTempPath();
            }
        }
        ServerConfig config = ServerConfig.getInstance();
        String ftpPath = config.getFtp().getPath();
        String recordPath = config.getJtt808().getRecordPath();
        String tempFullPath = ftpPath + tempPath;
        String recordFullPath = recordPath + tempPath.replace(FTP_RECORD_TEMP_DIR,"");
        DownloadRecordTask task;
        // 已经下载完成了
        if(FileUtil.isDirectory(recordFullPath) && !FileUtil.loopFiles(recordFullPath).isEmpty()){
            task = new DownloadRecordTask();
            task.setProgress(100);
            task.setRecordFullPath(recordFullPath);
            task.setFileName(FileUtil.loopFiles(recordFullPath).get(0).getName());
            return task;
        }
        // 还在下载中
        String finalTempPath = tempPath;
        task = downloadTasks.stream().filter(v -> v.getTempPath().equals(finalTempPath)).findFirst().orElse(null);
        if(task == null){
            return null;
        }
        if(FileUtil.isDirectory(tempFullPath)){
            long totalSize = FileUtil.size(new File(tempFullPath));
            int percentage = (int) Math.round((double) totalSize / task.getSize() * 100);
            task.setLastProgress(task.getProgress());
            task.setProgress(Math.min(99, percentage>0?percentage:1));
            task.setTempFullPath(tempFullPath);
        }
        return task;
    }

    @Async
    @EventListener
    public void t1206Event(T1206Event event){
        QueryParam param = new QueryParam();
        param.setClientId(event.getClientId());
        param.setSerialNo(event.getT1206().getResponseSerialNo());
        DownloadRecordTask task = checkDownloadTask(param);
        if(task == null){
            log.error("t1206Event task is null clientId {} t206 {}",event.getClientId(),event.getT1206());
            return;
        }
        log.info("t1206Event downloadRecord task {}",task);
        downloadTasks.remove(task);
        ServerConfig config = ServerConfig.getInstance();
        String ftpPath = config.getFtp().getPath();
        String recordPath = config.getJtt808().getRecordPath();
        String tempFullPath = ftpPath + task.getTempPath();
        String recordFullPath = recordPath + task.getTempPath().replace(FTP_RECORD_TEMP_DIR,"");
        List<File> fileList = FileUtil.loopFiles(tempFullPath);
        if(fileList.isEmpty()){
            log.error("t1206Event tempFullPath File isEmpty  {}",tempFullPath);
        }
        // 创建保存目录
        FileUtil.del(recordFullPath);
        FileUtil.mkdir(recordFullPath);
        if(fileList.size() == 1){
            long startTime = System.currentTimeMillis();
            FileUtil.rename(fileList.get(0), IdUtil.fastSimpleUUID() + "." + FileUtil.getSuffix(fileList.get(0)),true);
            FileUtil.copy(FileUtil.loopFiles(tempFullPath).get(0).getAbsolutePath(),recordFullPath,true);
            // 删除临时目录
            FileUtil.del(tempFullPath);
            log.info("end copy file taskId {} cost {} ",task.getTaskId(), (System.currentTimeMillis() - startTime)/1000);
        }else if (fileList.size() > 1){
            // 多个需要合并
            try {
                log.info("record tempFile taskId {} size {} start merge",task.getTaskId(), fileList.size());
                long startTime = System.currentTimeMillis();
                FFmpegExec.getInstance().merge(fileList, (progress, outputFile) -> {
                    if(progress.status.equals(Progress.Status.END)){
                        // 合并完成，复制到存储目录
                        log.info("ffmpeg merge file taskId {} cost {} ",task.getTaskId(), (System.currentTimeMillis() - startTime)/1000);
                        FileUtil.copy(outputFile.getAbsolutePath(), recordFullPath,true);
                        // 删除临时目录
                        FileUtil.del(outputFile);
                        FileUtil.del(tempFullPath);
                    }else {
                        long duration = task.getRecordEndTime().getTime() - task.getTaskStartTime().getTime();
                        long percentage = progress.out_time_ns /1000 / duration;
                        log.info("ffmpeg merge file taskId {} percentage {}",task.getTaskId(), percentage);
                    }
                });
            }catch (Exception ex){
                log.error("ffmpeg merge file ex taskId {}",task.getTaskId(), ex);
            }
        }
    }

    @Override
    public void downloadRecordControl(QueryParam param) {
        log.info("downloadRecordControl clientId {} command {}",param.getClientId(),param.getCommand());
        DownloadRecordTask task = checkDownloadTask(param);
        if(task == null){
            return;
        }
        T9207 t9207 = new T9207();
        t9207.setClientId(task.getClientId());
        t9207.setResponseSerialNo(task.getSerialNo());
        t9207.setCommand(param.getCommand());
        try {
            CmdResult<T0001> result = jt1078Send.sendT9207(t9207);
            if(!result.isSuccess()){
                throw new AppException(result.getMessage());
            }
        }finally {
            if(param.getCommand() == 2){
                downloadTasks.remove(task);
                // 删除临时目录
                FileUtil.del(task.getTempFullPath());
            }
        }
    }

    @Override
    public List<RecordFileDto> listRecordFile(QueryParam param) {
        String recordPath = ServerConfig.getInstance().getJtt808().getRecordPath();
        List<File> saveDir = Arrays.stream(FileUtil.ls(recordPath)).filter(File::isDirectory).toList();
        List<RecordFileDto> fileDtoList = new ArrayList<>();
        for(File subDir : saveDir) {
            String dirName = subDir.getName();
            String[] parts = dirName.split("_");
            if (parts.length == 9) {
                File[] files = subDir.listFiles((file, name) -> !name.equals("temp"));
                if (files != null) {
                    for (File file : files) {
                        RecordFileDto fileDto = new RecordFileDto();
                        fileDto.setClientId(parts[0]);
                        fileDto.setChannelNo(Integer.parseInt(parts[1]));
                        fileDto.setWarnBit1(Integer.parseInt(parts[2]));
                        fileDto.setWarnBit2(Integer.parseInt(parts[3]));
                        fileDto.setMediaType(Integer.parseInt(parts[4]));
                        fileDto.setStreamType(Integer.parseInt(parts[5]));
                        fileDto.setStorageType(Integer.parseInt(parts[6]));
                        fileDto.setStartTime(parts[7]);
                        fileDto.setEndTime(parts[8]);
                        fileDto.setFileName(file.getName());
                        fileDto.setFileSize(FileUtil.readableFileSize(file));
                        fileDto.setCreateTime(new Date(file.lastModified()));
                        // 修改展示的日期格式
                        try {
                            fileDto.setStartTime(DateUtil.parse(fileDto.getStartTime(),"yyMMddHHmmss").toString());
                            fileDto.setEndTime(DateUtil.parse(fileDto.getEndTime(),"yyMMddHHmmss").toString());
                        }catch (Exception ignored){}
                        fileDtoList.add(fileDto);
                    }
                }
            }
        }
        fileDtoList.sort(Comparator.comparing(RecordFileDto::getCreateTime).reversed());
        if(ObjectUtil.isNotEmpty(param.getClientId())){
            fileDtoList = fileDtoList.stream().filter(v->param.getClientId().equals(v.getClientId())).toList();
        }
        if(ObjectUtil.isNotEmpty(param.getChannelNo())){
            fileDtoList = fileDtoList.stream().filter(v->param.getChannelNo().equals(v.getChannelNo())).toList();
        }
        return fileDtoList;
    }

    @SneakyThrows
    @Override
    public void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, String fileName) {
        String recordPath =  ServerConfig.getInstance().getJtt808().getRecordPath();
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
        String recordPath =  ServerConfig.getInstance().getJtt808().getRecordPath();
        List<File> files = FileUtil.loopFiles(recordPath);
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
    public List<String> listAlarmFileName(QueryParam param) {
        String platformAlarmId = param.getPlatformAlarmId();
        String alarmFilePath = ServerConfig.getInstance().getJtt808().getAlarmFilePath();
        String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
        File saveDir = new File(savePath);
        if(saveDir.exists()){
            return FileUtil.loopFiles(saveDir).stream().map(File::getName).toList();
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    @Override
    public void downloadAlarmFile(HttpServletRequest request, HttpServletResponse response, String fileName) {
        String alarmFilePath = ServerConfig.getInstance().getJtt808().getAlarmFilePath();
        String platformAlarmId = fileName.split("_")[4];
        platformAlarmId =platformAlarmId.substring(0,platformAlarmId.lastIndexOf("."));
        String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
        List<File> files = FileUtil.loopFiles(savePath);
        File file = files.stream().filter(v -> v.getName().equals(fileName)).findFirst().orElse(null);
        if(file == null){
            response.setStatus(404);
            return;
        }
        request.setAttribute(LocalFileHandler.FILE_PATH, file.getAbsolutePath());
        localFileHandler.handleRequest(request,response);
    }

    @Override
    @SyncWait(key = "#mediaKey")
    public void stopClientMedia(String mediaKey) {
        log.info("stopMedia mediaKey {}",mediaKey);
        try {
            ClientMedia clientMedia = Jtt808SessionManager.getInstance().getClientMedia(mediaKey);
            if(clientMedia == null){
                return;
            }
            // 发送指令
            T9102 t9102 = new T9102();
            t9102.setClientId(clientMedia.getClientId());
            t9102.setChannelNo(clientMedia.getChannelNo());
            t9102.setCommand(0);
            t9102.setCloseType(0);
            CmdResult<T0001> result = jt1078Send.sendT9102(t9102);
            MediaServer mediaServer = clientMedia.getMediaServer();
            if(!result.isSuccess()){
                log.error("stopClientMedia error {}",result.getMessage());
            }
            // 关闭流媒体
            if(mediaServer != null){
                mediaServer.stop();
            }
            // 移除缓存
            Jtt808SessionManager.getInstance().removeClientMedia(clientMedia);
        }
        catch (Exception ex){
            log.error("stopClientMedia ex",ex);
        }
    }

    @Override
    public List<ClientMediaDto> listClientMedia(QueryParam param) {
        List<ClientMedia> allClientMedia = Jtt808SessionManager.getInstance().getAllClientMedia();
        List<ClientMediaDto> clientMediaDtos = new ArrayList<>();
        for(ClientMedia clientMedia : allClientMedia){
            ClientMediaDto mediaDto = new ClientMediaDto();
            clientMediaDtos.add(mediaDto);
            BeanUtil.copyProperties(clientMedia,mediaDto);
            mediaDto.setType(clientMedia.getMediaType());
            mediaDto.setMediaKey(clientMedia.getMediaKey());
            MediaServer mediaServer = clientMedia.getMediaServer();
            if(mediaServer != null){
                mediaDto.setRxRate(mediaServer.getRxRate());
                mediaDto.setTxRate(mediaServer.getTxRate());
                mediaDto.setViewNum(mediaServer.getViewNum());
            }
        }
        if(ObjectUtil.isNotEmpty(param.getClientId())){
            clientMediaDtos = clientMediaDtos.stream().filter(v->param.getClientId().equals(v.getClientId())).toList();
        }
        if(ObjectUtil.isNotEmpty(param.getChannelNo())){
            clientMediaDtos = clientMediaDtos.stream().filter(v->param.getChannelNo().equals(v.getChannelNo())).toList();
        }
        return clientMediaDtos;
    }
}
