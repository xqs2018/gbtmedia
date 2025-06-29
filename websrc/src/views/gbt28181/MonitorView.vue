<template>
  <div class="home-container">
    <a-row :gutter="[24, 24]" class="monitoring-cards">
      <!-- Register Info Card -->
      <a-col :span="8">
        <a-card title="Register Info" class="register-info-card">
          <div class="card-content">
            <div class="card-main">
              <a-descriptions :column="1" size="small">
                <a-descriptions-item label="GB28181 ID">
                  <span>{{ gb28181Id }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="GB28181 Domain">
                  <span>{{ gb28181Domain }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="IP Address">
                  <span>{{ ipAddress }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Port">
                  <span>{{ port }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Password">
                  <span>{{ password }}</span>
                </a-descriptions-item>
              </a-descriptions>
            </div>
            <div class="monitoring-buttons">
              <a-space style="width: 80%" size="small">
                <a-button type="primary" block size="small" @click="$router.push('/GBT28181/MonitorViewJessibuca')">
                  <template #icon><PlaySquareOutlined /></template>
                  Jessibuca
                </a-button>
                <a-button type="primary" block size="small" @click="$router.push('/GBT28181/MonitorViewMpegts')">
                  <template #icon><PlaySquareOutlined /></template>
                  MPEGTS
                </a-button>
              </a-space>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- Device & Channel Status Card -->
      <a-col :span="8">
        <a-card title="Device & Channel Status" class="device-stats-card">
          <div class="card-content">
            <div class="status-charts">
              <div class="chart-wrapper">
                <div ref="devicePieChart" style="height: 100%"></div>
                <div class="chart-title">Device Status (Total: {{ totalDevices }})</div>
              </div>
              <div class="chart-wrapper">
                <div ref="channelPieChart" style="height: 100%"></div>
                <div class="chart-title">Channel Status (Total: {{ totalChannels }})</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { PlaySquareOutlined } from '@ant-design/icons-vue'
import { info } from "@/api/gbt28181";
import * as echarts from 'echarts'
import { message } from 'ant-design-vue';

// System monitoring data
const gb28181Id = ref('')
const gb28181Domain = ref('')
const ipAddress = ref('')
const port = ref('')
const password = ref('')

// Device and channel statistics
const totalDevices = ref(0)
const onlineDevices = ref(0)
const offlineDevices = ref(0)
const totalChannels = ref(0)
const onlineChannels = ref(0)
const offlineChannels = ref(0)

// Chart refs
const devicePieChart = ref<HTMLElement>()
const channelPieChart = ref<HTMLElement>()

// Chart instances
let devicePieChartInstance: echarts.ECharts | null = null
let channelPieChartInstance: echarts.ECharts | null = null

// Fetch info data
const fetchInfoData = async () => {
  try {
    const infoRes = await info({});
    if (infoRes.data) {
      // Update GB28181 info
      if (infoRes.data.gbt28181) {
        const { sipId, sipDomain, accessIp, sipPort, sipPassword } = infoRes.data.gbt28181;
        gb28181Id.value = sipId;
        gb28181Domain.value = sipDomain;
        ipAddress.value = accessIp;
        port.value = sipPort;
        password.value = sipPassword;
      }
      
      // Update device and channel statistics
      if (infoRes.data.devices && infoRes.data.channels) {
        updateDeviceStats({
          devices: infoRes.data.devices,
          channels: infoRes.data.channels
        });
      }
    }
  } catch (error) {
    console.error('Failed to fetch info data:', error);
    message.error('Failed to load system information');
  }
}

// Initialize pie charts
const initPieCharts = () => {
  if (devicePieChart.value) {
    devicePieChartInstance = echarts.init(devicePieChart.value)
    devicePieChartInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: '10%',
        top: 'middle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: {
          fontSize: 12
        }
      },
      series: [{
        name: 'Device Status',
        type: 'pie',
        radius: '50%',
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'center',
          formatter: '{d}%'
        },
        emphasis: {
          label: {
            show: true
          }
        },
        data: [
          { value: 0, name: 'Offline' },
          { value: 0, name: 'Online' }
        ]
      }]
    })
  }

  if (channelPieChart.value) {
    channelPieChartInstance = echarts.init(channelPieChart.value)
    channelPieChartInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: '10%',
        top: 'middle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: {
          fontSize: 12
        }
      },
      series: [{
        name: 'Channel Status',
        type: 'pie',
        radius: '50%',
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'center',
          formatter: '{d}%'
        },
        emphasis: {
          label: {
            show: true
          }
        },
        data: [
          { value: 0, name: 'Offline' },
          { value: 0, name: 'Online' }
        ]
      }]
    })
  }
}

// Update device stats
const updateDeviceStats = (data: any) => {
  totalDevices.value = data.devices.total
  onlineDevices.value = data.devices.online
  offlineDevices.value = data.devices.offline
  totalChannels.value = data.channels.total
  onlineChannels.value = data.channels.online
  offlineChannels.value = data.channels.offline

  // Update pie charts
  if (devicePieChartInstance) {
    devicePieChartInstance.setOption({
      series: [{
        data: [
          { value: offlineDevices.value, name: 'Offline' },
          { value: onlineDevices.value, name: 'Online' }
        ]
      }]
    })
  }

  if (channelPieChartInstance) {
    channelPieChartInstance.setOption({
      series: [{
        data: [
          { value: offlineChannels.value, name: 'Offline' },
          { value: onlineChannels.value, name: 'Online' }
        ]
      }]
    })
  }
}

onMounted(async () => {
  initPieCharts()
  await fetchInfoData()
})

onUnmounted(() => {
  // Dispose charts
  devicePieChartInstance?.dispose()
  channelPieChartInstance?.dispose()
})
</script>

<style scoped>
.home-container {

}

.monitoring-cards {
  height: 100%;
  padding-bottom: 24px;
}

:deep(.ant-row) {
  margin: 0 !important;
  width: 100%;
  height: 100%;
}

:deep(.ant-col) {
  padding: 0 !important;
  height: 100%;
}

:deep(.ant-card) {
  height: 300px;
  display: flex;
  flex-direction: column;
  margin: 0 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

.card-main {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.card-main::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.monitoring-buttons {
  flex-shrink: 0;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  width: 100%;
}

:deep(.ant-descriptions) {
  margin-bottom: 8px;
  padding: 0 8px;
}

:deep(.ant-card-head) {
  min-height: 36px;
  padding: 0 12px;
}

:deep(.ant-card-head-title) {
  padding: 8px 0;
  font-size: 14px;
}

:deep(.ant-descriptions-item-label) {
  padding-bottom: 4px;
  font-size: 14px;
}

:deep(.ant-descriptions-item-content) {
  font-size: 14px;
}

:deep(.ant-statistic-title) {
  margin-bottom: 8px;
  font-size: 14px;
}

:deep(.ant-statistic-content) {
  font-size: 16px;
}

:deep(.ant-tag) {
  margin-left: 4px;
}

:deep(.ant-btn) {
  height: auto;
  min-height: 32px;
  padding: 4px 8px;
  white-space: normal;
  word-break: break-word;
  display: block;
  width: 100%;
}

:deep(.ant-space) {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

:deep(.ant-space-item) {
  flex: 1;
  margin-right: 8px;
}

:deep(.ant-space-item:last-child) {
  margin-right: 0;
}

.status-charts {
  display: flex;
  justify-content: space-around;
  align-items: stretch;
  padding: 8px;
  height: 100%;
  min-height: 0;
  flex: 1;
}

.chart-wrapper {
  text-align: center;
  width: 45%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}

.chart-wrapper > div:first-child {
  flex: 1;
  min-height: 0;
}

.chart-title {
  margin-top: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  line-height: 1.4;
}

/* 响应式布局调整 */
@media screen and (max-width: 1200px) {
  .home-container {
    padding: 16px;
  }
  
  :deep(.ant-descriptions-item-label) {
    font-size: 12px;
  }
  
  :deep(.ant-descriptions-item-content) {
    font-size: 12px;
  }
  
  :deep(.ant-btn) {
    font-size: 12px;
    padding: 4px 6px;
    min-height: 28px;
  }
  
  .monitoring-buttons {
    padding-top: 12px;
  }
}

@media screen and (max-width: 768px) {
  .home-container {
    padding: 12px;
  }
  
  :deep(.ant-descriptions-item-label) {
    font-size: 11px;
  }
  
  :deep(.ant-descriptions-item-content) {
    font-size: 11px;
  }
  
  :deep(.ant-btn) {
    font-size: 11px;
    padding: 4px 4px;
    min-height: 24px;
  }
  
  .monitoring-buttons {
    padding-top: 8px;
  }
  
  :deep(.ant-row) {
    height: auto;
  }
  
  :deep(.ant-col) {
    height: 300px;
  }
  
  .status-charts {
    flex-direction: column;
  }
  
  .chart-wrapper {
    width: 100%;
    height: 50%;
  }
}
</style>
  