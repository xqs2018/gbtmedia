package cn.gbtmedia.jtt808.service;

import cn.gbtmedia.common.extra.SchedulerTask;
import cn.gbtmedia.jtt808.dto.QueryParam;
import cn.gbtmedia.jtt808.entity.Client;
import cn.gbtmedia.jtt808.entity.ClientLocation;
import cn.gbtmedia.jtt808.repository.ClientLocationRepository;
import cn.gbtmedia.jtt808.repository.ClientRepository;
import cn.gbtmedia.jtt808.server.alarm.event.T1212Event;
import cn.gbtmedia.jtt808.server.cmd.event.T0200Event;
import cn.gbtmedia.jtt808.server.cmd.message.T0200;
import cn.hutool.core.bean.BeanUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * @author xqs
 */
@Slf4j
@Component
public class Jtt808LocationService {

    @Lazy
    @Resource
    private Jtt808Service jtt808Service;

    @Resource
    private ClientRepository clientRepository;

    @Resource
    private ClientLocationRepository clientLocationRepository;

    private final Cache<String, Client> clientCache = Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build();

    {
        SchedulerTask.getInstance().startPeriod("locationClientCache", clientCache::cleanUp,1000 * 100);
    }

    @Async
    @EventListener
    public void t0200Event(T0200Event event){
        String clientId = event.getClientId();
        Client client = clientCache.get(clientId, clientRepository::findByClientId);
        T0200 t0200 = event.getT0200();
        ClientLocation clientLocation = new ClientLocation();
        clientLocation.setClientId(clientId);
        clientLocation.setPlateNo(client.getPlateNo());
        BeanUtil.copyProperties(t0200,clientLocation);
        clientLocation.setWarnBitName(getWarnBitName(t0200.getWarnBit()));
        clientLocation.setStatusBitName(getStatusBitName(t0200.getStatusBit()));
        clientLocation.setTotalMileage(t0200.getAttr0x01()/10);
        clientLocation.setOil(t0200.getAttr0x02()/10);
        // 苏标报警名称转换
        if(t0200.getAttr0x64() != null){
            T0200.Attr0x64 attr0x64 = t0200.getAttr0x64();
            String platformAlarmId = attr0x64.getPlatformAlarmId();
            clientLocation.setPlatformAlarmId(platformAlarmId);
            clientLocation.setAlarmName(getAlarmName("0x64-"+attr0x64.getType()));
        }
        if(t0200.getAttr0x65() != null){
            T0200.Attr0x65 attr0x65 = t0200.getAttr0x65();
            String platformAlarmId = attr0x65.getPlatformAlarmId();
            clientLocation.setPlatformAlarmId(platformAlarmId);
            clientLocation.setAlarmName(getAlarmName("0x65-"+attr0x65.getType()));
        }
        if(t0200.getAttr0x66() != null){
            T0200.Attr0x66 attr0x66 = t0200.getAttr0x66();
            String platformAlarmId = attr0x66.getPlatformAlarmId();
            clientLocation.setPlatformAlarmId(platformAlarmId);
            clientLocation.setAlarmName(getAlarmName("0x66"));
        }
        if(t0200.getAttr0x67() != null){
            T0200.Attr0x67 attr0x67 = t0200.getAttr0x67();
            String platformAlarmId = attr0x67.getPlatformAlarmId();
            clientLocation.setPlatformAlarmId(platformAlarmId);
            clientLocation.setAlarmName(getAlarmName("0x67-"+attr0x67.getType()));
        }
        clientLocationRepository.saveBatch(List.of(clientLocation));
    }

    @Async
    @EventListener
    public void t1212Event(T1212Event event){
        // 报警文件上传完成了，更新对应的文件名，延迟1秒钟等待位置数据入库
        LockSupport.parkNanos(1000 * 1000000L);
        String platformAlarmId = event.getPlatformAlarmId();
        QueryParam param = new QueryParam();
        param.setPlatformAlarmId(platformAlarmId);
        List<String> alarmFileName = jtt808Service.listAlarmFileName(param);
        ClientLocation clientLocation = clientLocationRepository.findByPlatformAlarmId(platformAlarmId);
        if(clientLocation == null){
            log.error("update alarmFileNames is null platformAlarmId {}",platformAlarmId);
            return;
        }
        clientLocation.setAlarmFileName(String.join(",", alarmFileName));
        clientLocationRepository.save(clientLocation);
    }

    public String getWarnBitName(Integer warnBit) {
        if (warnBit == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        String binary32 = String.format("%32s", Integer.toBinaryString(warnBit)).replace(' ', '0');
        for (int i = 0; i < 32; i++) {
            int bitPosition = 31 - i;
            boolean isSet = binary32.charAt(i) == '1';
            if (!isSet) {
                continue;
            }
            switch (bitPosition) {
                case 0: names.add("超速报警"); break;
                case 1: names.add("疲劳驾驶"); break;
                case 2: names.add("驾驶员行为异常"); break;
                case 3: names.add("预警"); break;
                case 4: names.add("GNSS模块发生故障"); break;
                case 5: names.add("GNSS天线未接或被剪断"); break;
                case 6: names.add("GNSS天线短路"); break;
                case 7: names.add("终端主电源欠压"); break;
                case 8: names.add("终端主电源掉电"); break;
                case 9: names.add("终端LCD或显示器故障"); break;
                case 10: names.add("TTS模块故障"); break;
                case 11: names.add("摄像头故障"); break;
                case 12: names.add(""); break;
                case 13: names.add(""); break;
                case 14: names.add(""); break;
                case 15: names.add(""); break;
                case 16: names.add(""); break;
                case 17: names.add(""); break;
                case 18: names.add("当天累计驾驶超时"); break;
                case 19: names.add("超时停车"); break;
                case 20: names.add("进出区域"); break;
                case 21: names.add("进出路线"); break;
                case 22: names.add("路段行驶时间不足/过长"); break;
                case 23: names.add("路线偏离报警"); break;
                case 24: names.add("车辆VSS故障"); break;
                case 25: names.add("车辆油量异常"); break;
                case 26: names.add("车辆被盗"); break;
                case 27: names.add("车辆非法点火"); break;
                case 28: names.add("车辆非法位移"); break;
                case 29: names.add("碰撞侧翻报警"); break;
                case 30: names.add(""); break;
                case 31: names.add(""); break;
            }
        }
        return names.reversed().stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    public String getStatusBitName(Integer statusBit) {
        if (statusBit == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        String binary32 = String.format("%32s", Integer.toBinaryString(statusBit)).replace(' ', '0');
        for (int i = 0; i < 32; i++) {
            int bitPosition = 31 - i;
            boolean isSet = binary32.charAt(i) == '1';
            switch (bitPosition) {
                case 0: names.add(isSet ? "ACC开" : "ACC关");break;
                case 1: names.add(isSet ? "已定位" : "未定位");break;
                case 12: names.add(isSet ? "车门解锁" : "车门闭锁");break;
                case 18: names.add(isSet ? "GPS定位" : "");break;
                case 19: names.add(isSet ? "北斗定位" : "");break;
                case 20: names.add(isSet ? "GLONASS定位" : "");break;
                case 21: names.add(isSet ? "Galileo定位" : "");break;
                case 22: names.add(isSet ? "行驶状态" : "停止状态");break;
            }
        }
        return names.reversed().stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    public String getAlarmName(String type) {
        return switch (type) {
            // 0x64: ADAS辅助驾驶报警大类
            case "0x64-1"  -> "前向碰撞报警";
            case "0x64-2"  -> "车道偏离报警";
            case "0x64-3"  -> "车距过近报警";
            case "0x64-4"  -> "行人碰撞报警";
            case "0x64-5"  -> "频繁变道报警";
            case "0x64-6"  -> "道路标识超限报警";
            case "0x64-7"  -> "障碍物报警";
            case "0x64-16" -> "道路标志识别事件";
            case "0x64-17" -> "主动抓拍事件";
            // 0x65: DSM驾驶员状态报警大类
            case "0x65-1"  -> "疲劳驾驶报警";
            case "0x65-2"  -> "接打电话报警";
            case "0x65-3"  -> "抽烟报警";
            case "0x65-4"  -> "分神驾驶报警";
            case "0x65-5"  -> "驾驶员异常报警";
            case "0x65-7"  -> "用户自定义报警";
            case "0x65-16" -> "自动抓拍事件";
            case "0x65-17" -> "驾驶员变更事件";
            // 0x66: TPMS轮胎状态报警
            case "0x66"    -> "轮胎状态报警";
            // 0x67: BSD盲点监测报警
            case "0x67-1"  -> "后方接近报警";
            case "0x67-2"  -> "左侧后方接近报警";
            case "0x67-3"  -> "右侧后方接近报警";
            default -> type;
        };
    }
}
