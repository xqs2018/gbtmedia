package cn.gbtmedia.jtt808.service;

import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.gbtmedia.jtt808.dto.ClientMediaDto;
import cn.gbtmedia.jtt808.dto.InfoDto;
import cn.gbtmedia.jtt808.dto.QueryParam;
import cn.gbtmedia.jtt808.dto.DownloadRecordTask;
import cn.gbtmedia.jtt808.dto.RecordDto;
import cn.gbtmedia.jtt808.dto.RecordFileDto;
import cn.gbtmedia.jtt808.dto.StreamDto;
import cn.gbtmedia.jtt808.entity.Client;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author xqs
 */
public interface Jtt808Service {

    /**
     * 配置信息
     */
    InfoDto info(QueryParam param);

    /**
     * 分页查询终端
     */
    Page<Client> pageClient(QueryParam param);

    /**
     * 更新终端
     */
    void updateClient(Client client);

    /**
     * 删除终端
     */
    void deleteDevice(Client client);

    /**
     * 分页查询终端位置信息
     */
    Page<ClientLocation> pageClientLocation(QueryParam param);

    /**
     * 查询终端最后一次位置信息
     */
    List<ClientLocation> lastClientLocation(QueryParam param);

    /**
     * 实时播放
     */
    StreamDto play(QueryParam param);

    /**
     * 停止实时播放
     */
    void stopPlay(QueryParam param);

    /**
     * 查询录像
     */
    RecordDto queryRecordInfo(QueryParam param);

    /**
     * 录像播放
     */
    StreamDto playback(QueryParam param);

    /**
     * 停止录像播放
     */
    void stopPlayback(QueryParam param);

    /**
     * 下载录像
     */
    DownloadRecordTask downloadRecord(QueryParam param);

    /**
     * 所有下载任务
     */
    List<DownloadRecordTask> listDownloadRecordTask(QueryParam param);

    /**
     * 清理下载任务
     */
    void cleanDownloadTask(QueryParam param);

    /**
     * 检查下载任务
     */
    DownloadRecordTask checkDownloadTask(QueryParam param);

    /**
     * 下载录像控制，暂停/取消
     */
    void downloadRecordControl(QueryParam param);

    /**
     * 查询录像文件
     */
    List<RecordFileDto> listRecordFile(QueryParam param);

    /**
     *下载录像文件
     */
    void downloadRecordFile(HttpServletRequest request, HttpServletResponse response, String fileName);

    /**
     *删除录像文件
     */
    void deleteRecordFile(RecordFileDto recordFileDto);

    /**
     * 获取报警文件名称
     */
    List<String> listAlarmFileName(QueryParam param);

    /**
     *下载报警文件
     */
    void downloadAlarmFile(HttpServletRequest request, HttpServletResponse response, String fileName);

    /**
     * 停止媒体播放
     */
    void stopClientMedia(String mediaKey);

    /**
     * 获取所有的推流
     */
    List<ClientMediaDto> listClientMedia(QueryParam param);

}
