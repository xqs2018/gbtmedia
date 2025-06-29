package cn.gbtmedia.jtt808.controller;

import cn.gbtmedia.common.vo.Result;
import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.gbtmedia.jtt808.dto.InfoDto;
import cn.gbtmedia.jtt808.service.Jtt808Service;
import cn.gbtmedia.jtt808.dto.ClientMediaDto;
import cn.gbtmedia.jtt808.dto.DownloadRecordTask;
import cn.gbtmedia.jtt808.dto.QueryParam;
import cn.gbtmedia.jtt808.dto.RecordDto;
import cn.gbtmedia.jtt808.dto.RecordFileDto;
import cn.gbtmedia.jtt808.dto.StreamDto;
import cn.gbtmedia.jtt808.entity.Client;
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
@RequestMapping("/backend/jtt808")
@RestController
public class Jtt808Controller {

    @Resource
    private Jtt808Service jtt808Service;

    @PostMapping("/info")
    public Result<InfoDto> info(@RequestBody QueryParam param){
        InfoDto info = jtt808Service.info(param);
        return Result.success(info);
    }

    @PostMapping("/pageClient")
    public Result<Page<Client>> pageDevice(@RequestBody QueryParam param){
        Page<Client> page = jtt808Service.pageClient(param);
        return Result.success(page);
    }

    @PostMapping("/updateClient")
    public Result<?> updateClient(@RequestBody Client client){
        jtt808Service.updateClient(client);
        return Result.success();
    }

    @PostMapping("/deleteClient")
    public Result<?> deleteDevice(@RequestBody Client client){
        jtt808Service.deleteDevice(client);
        return Result.success();
    }

    @PostMapping("/pageClientLocation")
    public Result<Page<ClientLocation>> pageClientLocation(@RequestBody QueryParam param){
        Page<ClientLocation> page = jtt808Service.pageClientLocation(param);
        return Result.success(page);
    }

    @PostMapping("/lastClientLocation")
    public Result<List<ClientLocation>> lastClientLocation(@RequestBody QueryParam param){
        List<ClientLocation> clientLocations = jtt808Service.lastClientLocation(param);
        return Result.success(clientLocations);
    }

    @PostMapping("/play")
    public Result<StreamDto> play(@RequestBody QueryParam param){
        StreamDto streamDto = jtt808Service.play(param);
        return Result.success(streamDto);
    }

    @PostMapping("/stopPlay")
    public Result<?> stopPlay(@RequestBody QueryParam param){
         jtt808Service.stopPlay(param);
        return Result.success();
    }

    @PostMapping("/queryRecordInfo")
    public Result<RecordDto> queryRecordInfo(@RequestBody QueryParam param){
        RecordDto recordInfo = jtt808Service.queryRecordInfo(param);
        return Result.success(recordInfo);
    }

    @PostMapping("/playback")
    public Result<StreamDto> playback(@RequestBody QueryParam param){
        StreamDto streamDto = jtt808Service.playback(param);
        return Result.success(streamDto);
    }

    @PostMapping("/stopPlayback")
    public Result<?> stopPlayback(@RequestBody QueryParam param){
        jtt808Service.stopPlayback(param);
        return Result.success();
    }

    @PostMapping("/downloadRecord")
    public Result<DownloadRecordTask> downloadRecord(@RequestBody QueryParam param){
        DownloadRecordTask task = jtt808Service.downloadRecord(param);
        return Result.success(task);
    }

    @PostMapping("/downloadRecordControl")
    public Result<?> downloadRecordControl(@RequestBody QueryParam param){
        jtt808Service.downloadRecordControl(param);
        return Result.success();
    }

    @PostMapping("/listRecordFile")
    public Result<List<RecordFileDto>> listRecordFile(@RequestBody QueryParam param){
        List<RecordFileDto> fileDtos = jtt808Service.listRecordFile(param);
        return Result.success(fileDtos);
    }

    @GetMapping("/downloadRecordFile/{fileName}")
    public void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String fileName){
        jtt808Service.downloadRecordFile(request,response,fileName);
    }

    @PostMapping("/deleteRecordFile")
    public Result<?> deleteRecordFile(@RequestBody RecordFileDto recordFileDto){
        jtt808Service.deleteRecordFile(recordFileDto);
        return Result.success();
    }

    @PostMapping("/listAlarmFileName")
    public Result<List<String>> listAlarmFileName(@RequestBody QueryParam param){
        List<String> alarmFileNames = jtt808Service.listAlarmFileName(param);
        return Result.success(alarmFileNames);
    }

    @GetMapping("/downloadAlarmFile/{fileName}")
    public void downloadAlarmFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String fileName){
        jtt808Service.downloadAlarmFile(request, response,fileName);
    }

    @PostMapping("/stopClientMedia")
    public Result<?> stopClientMedia(@RequestBody QueryParam param){
        jtt808Service.stopClientMedia(param.getMediaKey());
        return Result.success();
    }

    @PostMapping("/listClientMedia")
    public Result<List<ClientMediaDto>> listClientMedia(@RequestBody QueryParam param){
        List<ClientMediaDto> clientMediaDtos = jtt808Service.listClientMedia(param);
        return Result.success(clientMediaDtos);
    }
}
