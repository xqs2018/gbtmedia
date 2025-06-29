package cn.gbtmedia.common.extra;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


/**
 * 监控过去10分钟cpu 内存 磁盘 网络
 * @author xqs
 */
@Slf4j
@Getter
@Component
public class SystemMonitor{

    public static SystemMonitor getInstance(){
        return SpringUtil.getBean(SystemMonitor.class);
    }

    // 最多保存10条历史记录
    private static final int MAX_HISTORY = 10;

    // 60秒统计一次
    private static final int PERIOD = 1000 * 60;

    private final Info monitorInfo = new Info();

    @PostConstruct
    void init() throws Exception {
        log.info("init system monitor ...");
        // Initialize system monitoring data
        monitorInfo.setCpu(new Info.Cpu());
        monitorInfo.setMemory(new Info.Memory());
        monitorInfo.setDisk(new Info.Disk());
        monitorInfo.setNetwork(new Info.Network());

        // Initialize history lists
        monitorInfo.getCpu().setHistory(new LinkedList<>());
        monitorInfo.getMemory().setHistory(new LinkedList<>());
        monitorInfo.getDisk().setHistory(new LinkedList<>());
        monitorInfo.getNetwork().setHistory(new LinkedList<>());

        // 定时更新系统监控
        SchedulerTask.getInstance().startPeriod("updateSystemMonitor", this::updateSystemMonitor, PERIOD);
    }

    private void updateSystemMonitor(){
        // 读取系统监控数据
        SystemInfo systemInfo = new SystemInfo();
        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // CPU 监控
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // Wait a second...
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long[] ticks = processor.getSystemCpuLoadTicks();
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;
        double cpuLoad = 100.0 * (totalCpu - idle) / totalCpu;
        // Sensors sensors = hardware.getSensors();
        // double cpuTemp = sensors.getCpuTemperature();
        double cpuTemp = 0.0;
        // 内存监控
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsage = (usedMemory * 100.0) / totalMemory;

        // 磁盘监控，这是所有的
        long totalDiskSpace = 0;
        long availableDiskSpace = 0;
        long usedDiskSpace = 0;
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        for (OSFileStore fs : fileStores) {
            totalDiskSpace += fs.getTotalSpace();
            availableDiskSpace += fs.getUsableSpace();
            long used = fs.getTotalSpace() - fs.getUsableSpace();
            usedDiskSpace += used;
        }
        // 改成读取指定目录所在的磁盘
        String storagePath = ServerConfig.getInstance().getGbt28181().getRecordPathCloud();
        long freeSpace = new File(storagePath).getFreeSpace();
        totalDiskSpace = new File(storagePath).getTotalSpace();
        usedDiskSpace = totalDiskSpace - freeSpace;
        double diskUsage = (usedDiskSpace * 100.0) / totalDiskSpace;

        // 网络监控
        List<NetworkIF> beforeRecvNetworkIFs = hardware.getNetworkIFs();
        long beforeRecv = beforeRecvNetworkIFs.stream().mapToLong(NetworkIF::getBytesRecv).sum();
        long beforeSend = beforeRecvNetworkIFs.stream().mapToLong(NetworkIF::getBytesSent).sum();
        // Wait a second...
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<NetworkIF> afterNetworkIFs = hardware.getNetworkIFs();
        long afterRecv = afterNetworkIFs.stream().mapToLong(NetworkIF::getBytesRecv).sum();
        long afterSend = afterNetworkIFs.stream().mapToLong(NetworkIF::getBytesSent).sum();
        int uploadSpeed = (int) (afterRecv-beforeRecv) / (1024 * 1024); // 转换为MB
        int downloadSpeed = (int) (afterSend-beforeSend) /(1024 * 1024); // 转换为MB

        // 更新当前值
        monitorInfo.getCpu().setUsage((int) cpuLoad);
        monitorInfo.getCpu().setTemp((int) cpuTemp);

        monitorInfo.getMemory().setUsed((int) (usedMemory / (1024 * 1024 * 1024))); // 转换为GB
        monitorInfo.getMemory().setTotal((int) (totalMemory / (1024 * 1024 * 1024))); // 转换为GB

        monitorInfo.getDisk().setUsed((int) (usedDiskSpace / (1024 * 1024 * 1024))); // 转换为GB
        monitorInfo.getDisk().setTotal((int) (totalDiskSpace / (1024 * 1024 * 1024))); // 转换为GB

        monitorInfo.getNetwork().setUpload(uploadSpeed);
        monitorInfo.getNetwork().setDownload(downloadSpeed);

        // 更新历史数据
        Info.HistoryItem cpuHistoryItem = new Info.HistoryItem();
        cpuHistoryItem.setTime(timeStr);
        cpuHistoryItem.setValue((int) cpuLoad);
        updateHistory(monitorInfo.getCpu().getHistory(), cpuHistoryItem);

        Info.HistoryItem memoryHistoryItem = new Info.HistoryItem();
        memoryHistoryItem.setTime(timeStr);
        memoryHistoryItem.setValue((int) memoryUsage);
        updateHistory(monitorInfo.getMemory().getHistory(), memoryHistoryItem);

        Info.HistoryItem diskHistoryItem = new Info.HistoryItem();
        diskHistoryItem.setTime(timeStr);
        diskHistoryItem.setValue((int) diskUsage);
        updateHistory(monitorInfo.getDisk().getHistory(), diskHistoryItem);

        Info.NetworkHistoryItem networkHistoryItem = new Info.NetworkHistoryItem();
        networkHistoryItem.setTime(timeStr);
        networkHistoryItem.setUpload(uploadSpeed);
        networkHistoryItem.setDownload(downloadSpeed);
        updateHistory(monitorInfo.getNetwork().getHistory(), networkHistoryItem);
    }

    private static <T> void updateHistory(List<T> history, T newData) {
        history.add(newData);
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
    }


    @Data
    public static class Info{

        private Cpu cpu;

        private Memory memory;

        private Disk disk;

        private Network network;

        @Data
        public static class Cpu {
            private int usage;  // CPU使用率百分比
            private int temp;   // CPU温度
            private List<Info.HistoryItem> history;  // 历史数据
        }

        @Data
        public static class Memory {
            private int used;   // 已用内存(GB)
            private int total;  // 总内存(GB)
            private List<Info.HistoryItem> history;  // 历史数据
        }

        @Data
        public static class Disk {
            private int used;   // 已用磁盘空间(GB)
            private int total;  // 总磁盘空间(GB)
            private List<Info.HistoryItem> history;  // 历史数据
        }

        @Data
        public static class Network {
            private int upload;    // 上传速度(MB/s)
            private int download;  // 下载速度(MB/s)
            private List<Info.NetworkHistoryItem> history;  // 历史数据
        }

        @Data
        public static class HistoryItem {
            private String time;  // 时间戳，格式：HH:mm:ss
            private int value;    // 数值
        }

        @Data
        public static class NetworkHistoryItem {
            private String time;    // 时间戳，格式：HH:mm:ss
            private int upload;     // 上传速度(MB/s)
            private int download;   // 下载速度(MB/s)
            private int speed;      // 网络接口速度(Mbps)
        }
    }
}
