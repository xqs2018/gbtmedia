<template>
  <div class="home-container">

    <a-row :gutter="[16, 16]" class="monitoring-cards">
      
      <a-col :span="8">
        <a-card title="国标设备" :bordered="false" class="stats-card">
          <div class="stats">
            <div class="stat-item">
              <span class="label">设备总数</span>
              <span class="value">100</span>
            </div>
            <div class="stat-item">
              <span class="label">在线设备</span>
              <span class="value">100</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card title="部标终端" :bordered="false" class="stats-card">
          <div class="stats">
            <div class="stat-item">
              <span class="label">终端总数</span>
              <span class="value">100</span>
            </div>
            <div class="stat-item">
              <span class="label">在线终端</span>
              <span class="value">100</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- CPU Usage Card -->
      <a-col :span="8">
        <a-card title="CPU Usage" :bordered="false" class="stats-card">
          <div class="card-content">
            <div class="chart-container">
              <div ref="cpuChart" style="height: 100%"></div>
            </div>
            <div class="stats-info">
              <span>Usage: {{ cpuUsage }}%</span>
              <span>Temperature: {{ cpuTemp }}°C</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- Memory Usage Card -->
      <a-col :span="8">
        <a-card title="Memory Usage" :bordered="false" class="stats-card">
          <div class="card-content">
            <div class="chart-container">
              <div ref="memoryChart" style="height: 100%"></div>
            </div>
            <div class="stats-info">
              <span>Used: {{ memoryUsed }}GB</span>
              <span>Total: {{ memoryTotal }}GB</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- Disk Usage Card -->
      <a-col :span="8">
        <a-card title="Disk Usage" :bordered="false" class="stats-card">
          <div class="card-content">
            <div class="chart-container">
              <div ref="diskChart" style="height: 100%"></div>
            </div>
            <div class="stats-info">
              <span>Used: {{ diskUsed }}GB</span>
              <span>Total: {{ diskTotal }}GB</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- Network Traffic Card -->
      <a-col :span="8">
        <a-card title="Network Traffic" :bordered="false" class="stats-card">
          <div class="card-content">
            <div class="chart-container">
              <div ref="networkChart" style="height: 100%"></div>
            </div>
            <div class="stats-info">
              <span>Upload: {{ networkUpload }}KB/s</span>
              <span>Download: {{ networkDownload }}KB/s</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { monitor } from "@/api/system";
import { message } from 'ant-design-vue';

const router = useRouter();

// System monitoring data
const cpuUsage = ref(0);
const cpuTemp = ref(0);
const memoryUsed = ref(0);
const memoryTotal = ref(0);
const diskUsed = ref(0);
const diskTotal = ref(0);
const networkUpload = ref(0);
const networkDownload = ref(0);

// Historical data arrays
const cpuHistory = ref<{ time: string; value: number }[]>([]);
const memoryHistory = ref<{ time: string; value: number }[]>([]);
const diskHistory = ref<{ time: string; value: number }[]>([]);
const networkHistory = ref<{ time: string; value: number; upload: number; download: number }[]>([]);

// Chart refs
const cpuChart = ref<HTMLElement>();
const memoryChart = ref<HTMLElement>();
const diskChart = ref<HTMLElement>();
const networkChart = ref<HTMLElement>();

// Chart instances
let cpuChartInstance: echarts.ECharts | null = null;
let memoryChartInstance: echarts.ECharts | null = null;
let diskChartInstance: echarts.ECharts | null = null;
let networkChartInstance: echarts.ECharts | null = null;

// Initialize charts
const initCharts = () => {
  if (cpuChart.value) {
    cpuChartInstance = echarts.init(cpuChart.value);
    cpuChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br />Usage: {c}%'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '20%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: {
          interval: 1,
          rotate: 0
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [{
        name: 'CPU Usage',
        type: 'line',
        data: [],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        lineStyle: {
          width: 2
        }
      }]
    });
  }

  if (memoryChart.value) {
    memoryChartInstance = echarts.init(memoryChart.value);
    memoryChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br />Usage: {c}%'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '20%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: {
          interval: 1,
          rotate: 0
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [{
        name: 'Memory Usage',
        type: 'line',
        data: [],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        lineStyle: {
          width: 2
        }
      }]
    });
  }

  if (diskChart.value) {
    diskChartInstance = echarts.init(diskChart.value);
    diskChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br />Usage: {c}%'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '20%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: {
          interval: 1,
          rotate: 0
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [{
        name: 'Disk Usage',
        type: 'line',
        data: [],
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        lineStyle: {
          width: 2
        }
      }]
    });
  }

  if (networkChart.value) {
    networkChartInstance = echarts.init(networkChart.value);
    networkChartInstance.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: function(params: any) {
          const time = params[0].axisValue;
          const upload = params[0].value;
          const download = params[1].value;
          return `${time}<br />Upload: ${upload.toFixed(2)}MB/s<br />Download: ${download.toFixed(2)}MB/s`;
        }
      },
      legend: {
        data: ['Upload', 'Download']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '20%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: {
          interval: 1,
          rotate: 0
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 10,
        interval: 2,
        axisLabel: {
          formatter: '{value}MB/s'
        }
      },
      series: [
        {
          name: 'Upload',
          type: 'line',
          data: [],
          smooth: true,
          areaStyle: {
            opacity: 0.3
          },
          lineStyle: {
            width: 2
          }
        },
        {
          name: 'Download',
          type: 'line',
          data: [],
          smooth: true,
          areaStyle: {
            opacity: 0.3
          },
          lineStyle: {
            width: 2
          }
        }
      ]
    });
  }
};

// Update system monitoring data
const updateSystemStats = (data: any) => {
  // Update current values
  cpuUsage.value = data.cpu.usage;
  cpuTemp.value = data.cpu.temp;
  memoryUsed.value = data.memory.used;
  memoryTotal.value = data.memory.total;
  diskUsed.value = data.disk.used;
  diskTotal.value = data.disk.total;
  networkUpload.value = data.network.upload;
  networkDownload.value = data.network.download;

  // Update historical data
  cpuHistory.value = data.cpu.history;
  memoryHistory.value = data.memory.history;
  diskHistory.value = data.disk.history;
  networkHistory.value = data.network.history.map((item: any) => ({
    time: new Date(item.time).toLocaleTimeString(),
    value: item.upload,
    upload: item.upload,
    download: item.download
  }));

  // Update charts
  if (cpuChartInstance) {
    cpuChartInstance.setOption({
      xAxis: {
        data: cpuHistory.value.map(item => item.time)
      },
      series: [{ data: cpuHistory.value.map(item => item.value) }]
    });
  }
  if (memoryChartInstance) {
    memoryChartInstance.setOption({
      xAxis: {
        data: memoryHistory.value.map(item => item.time)
      },
      series: [{ data: memoryHistory.value.map(item => item.value) }]
    });
  }
  if (diskChartInstance) {
    diskChartInstance.setOption({
      xAxis: {
        data: diskHistory.value.map(item => item.time)
      },
      series: [{ data: diskHistory.value.map(item => item.value) }]
    });
  }
  if (networkChartInstance) {
    networkChartInstance.setOption({
      xAxis: {
        data: networkHistory.value.map(item => item.time)
      },
      series: [
        { 
          name: 'Upload',
          data: networkHistory.value.map(item => item.upload)
        },
        { 
          name: 'Download',
          data: networkHistory.value.map(item => item.download)
        }
      ]
    });
  }
};

// Start monitoring
let monitoringInterval: number | null = null;

onMounted(async () => {
  initCharts();
  
  // Fetch data immediately
  try {
    const monitorRes = await monitor({});
    if (monitorRes.data) {
      updateSystemStats(monitorRes.data);
    }
  } catch (error) {
    console.error('Failed to fetch monitor data:', error);
    message.error('Failed to load system monitoring data');
  }
  
  // Update stats every minute
  monitoringInterval = window.setInterval(async () => {
    try {
      const monitorRes = await monitor({});
      if (monitorRes.data) {
        updateSystemStats(monitorRes.data);
      }
    } catch (error) {
      console.error('Failed to fetch monitor data:', error);
    }
  }, 60000);
});

onUnmounted(() => {
  if (monitoringInterval) {
    clearInterval(monitoringInterval);
  }
  
  // Dispose charts
  cpuChartInstance?.dispose();
  memoryChartInstance?.dispose();
  diskChartInstance?.dispose();
  networkChartInstance?.dispose();
});
</script>

<style scoped>
.home-container {
  
}

h1 {
  color: #333;
  margin-bottom: 30px;
}

.content {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex: 1;
  cursor: pointer;
}

h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 18px;
}

.stats-container {
  display: flex;
  gap: 24px;
}

.stats-group {
  flex: 1;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stats-group h3 {
  color: #333;
  font-size: 16px;
  margin-bottom: 16px;
  text-align: center;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 6px;
}

.label {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.value {
  color: #367fa9;
  font-size: 24px;
  font-weight: bold;
}

.monitoring-cards {
  margin-top: 24px;
}

:deep(.ant-row) {
  margin: 0 !important;
  width: 100%;
}

:deep(.ant-col) {
  padding: 0 !important;
  height: 300px;
}

:deep(.ant-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  margin: 0 6px;
}

:deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  min-height: 0;
  overflow: hidden;
}

.card-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chart-container {
  flex: 1;
  min-height: 0;
  position: relative;
}

.stats-info {
  display: flex;
  justify-content: space-between;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
  padding: 0 8px;
}

/* 响应式布局调整 */
@media screen and (max-width: 1200px) {
  .home-container {
    padding: 16px;
  }
  
  :deep(.ant-col) {
    height: 250px;
  }
}

@media screen and (max-width: 768px) {
  .home-container {
    padding: 12px;
  }
  
  :deep(.ant-col) {
    height: 200px;
  }
  
  .content {
    flex-direction: column;
  }
  
  .card {
    margin-bottom: 16px;
  }
  
  .stats-container {
    flex-direction: column;
    gap: 16px;
  }
}

.content {
  margin-bottom: 24px;
}

.stats-card {
  height: 100%;
}

.stats-card :deep(.ant-card-head) {
  min-height: 48px;
  padding: 0 16px;
}

.stats-card :deep(.ant-card-head-title) {
  padding: 12px 0;
  font-size: 16px;
}

.stats-card :deep(.ant-card-body) {
  padding: 16px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 6px;
}

.label {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.value {
  color: #367fa9;
  font-size: 24px;
  font-weight: bold;
}

@media screen and (max-width: 768px) {
  .content {
    margin-bottom: 16px;
  }
}
</style> 