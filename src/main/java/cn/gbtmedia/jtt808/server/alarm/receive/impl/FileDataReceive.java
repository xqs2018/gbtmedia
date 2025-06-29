package cn.gbtmedia.jtt808.server.alarm.receive.impl;

import cn.gbtmedia.common.config.ServerConfig;
import cn.gbtmedia.jtt808.server.alarm.codec.AlarmFileMessageData;
import cn.gbtmedia.jtt808.server.alarm.receive.IAlarmFileReceive;
import cn.gbtmedia.jtt808.server.alarm.session.AlarmFileSession;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author xqs
 */
@Slf4j
@Component
public class FileDataReceive implements IAlarmFileReceive {

    @Override
    public boolean support(Object request) {
        return request instanceof AlarmFileMessageData;
    }

    @Override
    public void handle(AlarmFileSession session, Object req){
        /*
        文件名称命名规则为：
        <文件类型>_<通道号>_<报警类型>_<序号>_<报警编号>.<后缀名>
                字段定义如下：
        文件类型：00——图片；01——音频；02——视频；03——文本；04——其它。
        通道号：0~37 表示 JT/T 1076 标准中表 2 定义的视频通道。
        64 表示 ADAS 模块视频通道。
        65 表示 DSM 模块视频通道。
        附件与通道无关，则直接填 0。
        报警类型：由外设 ID 和对应的模块报警类型组成的编码，例如，前向碰撞报警表示为“6401”。
        序号：用于区分相同通道、相同类型的文件编号。
        报警编号：平台为报警分配的唯一编号。
        后缀名：图片文件为 jpg 或 png，音频文件为 wav，视频文件为 h264，文本文件为 bin。
        */
        AlarmFileMessageData request = (AlarmFileMessageData) req;
        log.info("FileDataReceive clientId {} {}",session.getClientId(), request);

        StopWatch stopWatch = new StopWatch("FileDataReceive");
        stopWatch.start("findFile");
        ServerConfig conf = ServerConfig.getInstance();
        String alarmFilePath = conf.getJtt808().getAlarmFilePath();
        String platformAlarmIdO = request.getName().split("_")[4];
        // 上报的字数不足后面是空格
        String platformAlarmId = platformAlarmIdO.substring(0,platformAlarmIdO.indexOf(".")).trim();
        // 查找目录是否已经创建
        String savePath = alarmFilePath + "/" + platformAlarmId.substring(0,2) + "/" + platformAlarmId;
        File saveDir = new File(savePath);
        if(!saveDir.exists()){
            log.error("FileDataReceive saveDir is null saveDir {}",saveDir);
            return;
        }
        String saveFilePath = savePath + "/" + request.getName().trim();
        log.info("FileDataReceive clientId {} saveFilePath {} length {} ",session.getClientId(), saveFilePath,request.getLength());
        File saveFile = FileUtil.loopFiles(savePath).stream()
                .filter(v -> v.getName().equals(request.getName().trim()))
                .findFirst().orElse(FileUtil.touch(saveFilePath));
        stopWatch.stop();

        stopWatch.start("writeFile");
        FileUtil.writeBytes(request.getData(),saveFile,0,request.getData().length,true);
        stopWatch.stop();

        log.info("FileDataReceive clientId {} cost {}",session.getClientId(), stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
    }
}
