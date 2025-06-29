<template>
  <div class="home-container">
    <div class="split-layout">
      <!-- Left Panel (25%) -->
      <div class="left-panel">
        <!-- Register Info Card -->
        <div class="info-card">
          <div class="card-header">
            <span class="title">Register Info</span>
          </div>
          <div class="card-content">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="Access IP">
                <span>{{ accessIp }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="Command Port">
                <span>{{ cmdPort }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </div>

        <!-- Vehicle List -->
        <div class="vehicle-list">
          <div class="list-header">
            <span class="title">车辆列表 <span class="count">{{ onlineCount }}/{{ vehicleList.length }}</span></span>
            <a-input
              v-model:value="searchKeyword"
              placeholder="输入车牌号或终端号"
              style="width: 180px; margin-left: 8px;"
              size="small"
              @change="handleSearch"
            />
          </div>
          <div class="list-content">
            <a-list
              :data-source="filteredVehicleList"
              :loading="loading"
              size="small"
              :virtual="true"
              :item-height="40"
              :height="listHeight"
              :row-key="listItemKey"
            >
              <template #renderItem="{ item }">
                <a-list-item
                  @click="handleVehicleClick(item)"
                  :class="{ 'active': selectedVehicle?.clientId === item.clientId }"
                >
                  <div class="vehicle-item">
                    <span class="status-dot" :style="{ backgroundColor: getStatusColor(item.clientStatus) }"></span>
                    <span class="vehicle-info">
                      <span class="truncate-text client-id">{{ item.clientId }}</span> - 
                      <span class="truncate-text plate-no">{{ item.plateNo }}</span>
                      <span class="create-time">{{ item.createTime }}</span>
                    </span>
                  </div>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </div>
      </div>

      <!-- Right Panel (75%) -->
      <div class="right-panel">
        <div ref="mapRef" class="map"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { info, lastClientLocation } from "@/api/jtt808";
import { message } from 'ant-design-vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import debounce from 'lodash/debounce';

// System monitoring data
const accessIp = ref('')
const cmdPort = ref('')
const mapRef = ref<HTMLElement | null>(null)
let map: any = null
let markers: Map<string, any> = new Map()
let pollingTimer: number | null = null
let AMap: any = null
const isTracking = ref(false)
const isFirstLoad = ref(true)

// Vehicle list data
const vehicleList = ref<any[]>([])
const loading = ref(false)
const selectedVehicle = ref<any>(null)
const searchKeyword = ref('')

// Add computed property for online count
const onlineCount = computed(() => {
  return vehicleList.value.filter(vehicle => vehicle.clientStatus === 1).length
})

// Filtered vehicle list
const filteredVehicleList = computed(() => {
  if (!searchKeyword.value) {
    return vehicleList.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return vehicleList.value.filter(vehicle => 
    vehicle.clientId.toLowerCase().includes(keyword) ||
    (vehicle.plateNo && vehicle.plateNo.toLowerCase().includes(keyword))
  )
})

// Handle search
const handleSearch = () => {
  // The filtering is handled automatically by the computed property
}

// Fetch info data
const fetchInfoData = async () => {
  try {
    const infoRes = await info({});
    if (infoRes.data) {
      if (infoRes.data.jtt808) {
        const { accessIp: ip, cmdPort: port } = infoRes.data.jtt808;
        accessIp.value = ip;
        cmdPort.value = port;
      }
    }
  } catch (error) {
    console.error('Failed to fetch info data:', error);
    message.error('Failed to load system information');
  }
}

// Add new refs for performance optimization
const previousVehicleData = ref<Map<string, any>>(new Map())
const isUpdating = ref(false)

// Optimize updateMarkers function
const updateMarkers = debounce((vehicles: any[]) => {
  if (!map || !AMap || isUpdating.value) return
  
  isUpdating.value = true
  
  try {
    vehicles.forEach(vehicle => {
      if (!vehicle) return

      const { latitude, longitude, clientStatus, alarmName, clientId } = vehicle
      const lat = latitude / 1000000
      const lng = longitude / 1000000

      // Check if data has changed
      const prevData = previousVehicleData.value.get(clientId)
      const isSelected = selectedVehicle.value?.clientId === clientId
      const needsUpdate = !prevData || 
        prevData.latitude !== latitude || 
        prevData.longitude !== longitude || 
        prevData.clientStatus !== clientStatus ||
        prevData.alarmName !== alarmName ||
        prevData.isSelected !== isSelected

      if (!needsUpdate) return

      // Update previous data
      previousVehicleData.value.set(clientId, { 
        ...vehicle,
        isSelected 
      })

      if (markers.has(clientId)) {
        // Update existing marker
        const marker = markers.get(clientId)
        marker.setPosition([lng, lat])
        
        // Update marker content based on status and selection
        const markerContent = document.createElement('div')
        markerContent.className = `custom-marker ${getStatusClass(clientStatus)} ${isSelected ? 'selected' : ''}`
        marker.setContent(markerContent)
        
        // Update label content
        const labelContent = alarmName ? 
          `${vehicle.plateNo || vehicle.clientId}\n${alarmName}` : 
          vehicle.plateNo || vehicle.clientId
        marker.setLabel({
          content: labelContent,
          direction: 'top',
          offset: new AMap.Pixel(0, -10)
        })
        
        // If this is the tracked vehicle, update map center
        if (isTracking.value && isSelected) {
          map.setCenter([lng, lat])
        }
      } else {
        // Create new marker
        const markerContent = document.createElement('div')
        markerContent.className = `custom-marker ${getStatusClass(clientStatus)} ${isSelected ? 'selected' : ''}`
        
        const labelContent = alarmName ? 
          `${vehicle.plateNo || vehicle.clientId}\n${alarmName}` : 
          vehicle.plateNo || vehicle.clientId
        
        const marker = new AMap.Marker({
          position: [lng, lat],
          content: markerContent,
          label: {
            content: labelContent,
            direction: 'top',
            offset: new AMap.Pixel(0, -10)
          }
        })
        map.add(marker)
        markers.set(clientId, marker)
      }
    })
  } finally {
    isUpdating.value = false
  }
}, 100) // Debounce for 100ms

// Optimize fetchClientLocation
const fetchClientLocation = async () => {
  try {
    loading.value = true
    const response = await lastClientLocation({});
    if (response.data && response.data.length > 0) {
      // Only update if data has changed
      const hasChanged = response.data.some((newVehicle: any) => {
        const oldVehicle = vehicleList.value.find(v => v.clientId === newVehicle.clientId)
        return !oldVehicle || 
          oldVehicle.latitude !== newVehicle.latitude ||
          oldVehicle.longitude !== newVehicle.longitude ||
          oldVehicle.clientStatus !== newVehicle.clientStatus ||
          oldVehicle.alarmName !== newVehicle.alarmName
      })

      if (hasChanged) {
        vehicleList.value = response.data
        updateMarkers(response.data)
        
        // Find the latest vehicle based on createTime
        const latestVehicle = response.data.reduce((latest: any, current: any) => {
          return new Date(current.createTime) > new Date(latest.createTime) ? current : latest;
        }, response.data[0]);
        
        // Set initial center point
        const { latitude, longitude } = latestVehicle;
        initialCenter = [longitude / 1000000, latitude / 1000000];
        
        // Set selected vehicle
        selectedVehicle.value = latestVehicle;
        isTracking.value = true;
      }
    }
  } catch (error) {
    console.error('Failed to fetch client location:', error);
  } finally {
    loading.value = false
  }
}

// Handle vehicle click
const handleVehicleClick = (vehicle: any) => {
  if (selectedVehicle.value?.clientId === vehicle.clientId) {
    // If clicking the same vehicle, toggle tracking
    selectedVehicle.value = null
    isTracking.value = false
    // Update all markers to remove selection
    updateMarkers(vehicleList.value)
  } else {
    // Select new vehicle and start tracking
    selectedVehicle.value = vehicle
    isTracking.value = true
    if (vehicle) {
      const { latitude, longitude } = vehicle
      const lat = latitude / 1000000
      const lng = longitude / 1000000
      // 只更新中心点，不改变缩放级别
      map.setCenter([lng, lat])
      // Update all markers to show selection
      updateMarkers(vehicleList.value)
    }
  }
}

// Helper function to get status color
const getStatusColor = (status: number) => {
  switch (status) {
    case 0: return '#999999' // 离线
    case 1: return '#52c41a' // 在线
    case 2: return '#ff4d4f' // 报警
    default: return '#999999'
  }
}

// Helper function to get status class
const getStatusClass = (status: number) => {
  switch (status) {
    case 0: return 'offline'
    case 1: return 'normal'
    case 2: return 'alarm'
    default: return 'offline'
  }
}

// Initialize AMap
const initMap = async () => {
  if (mapRef.value) {
    try {
      AMap = await AMapLoader.load({
        key: 'a5c8f98239681769eb5f134a8017cc43',
        securityJsCode: '9a47acddedb3e97db340c99156e27c15',
        version: '2.0',
        plugins: ['AMap.ToolBar', 'AMap.Scale', 'AMap.HawkEye', 'AMap.MapType', 'AMap.Geolocation']
      } as any);

      // 先获取车辆数据
      const response = await lastClientLocation({});
      let initialCenter: [number, number] = [116.397428, 39.90923]; // 默认天安门
      let initialZoom = 15;

      if (response.data && response.data.length > 0) {
        vehicleList.value = response.data;
        
        // Find the latest vehicle based on createTime
        const latestVehicle = response.data.reduce((latest: any, current: any) => {
          return new Date(current.createTime) > new Date(latest.createTime) ? current : latest;
        }, response.data[0]);
        
        // Set initial center point
        const { latitude, longitude } = latestVehicle;
        initialCenter = [longitude / 1000000, latitude / 1000000];
        
        // Set selected vehicle
        selectedVehicle.value = latestVehicle;
        isTracking.value = true;
      }

      map = new AMap.Map(mapRef.value, {
        zoom: initialZoom,
        center: initialCenter,
        viewMode: '3D'
      });

      // Add controls
      map.addControl(new AMap.ToolBar());
      map.addControl(new AMap.Scale());
      map.addControl(new AMap.HawkEye({isOpen: true}));
      map.addControl(new AMap.MapType());
      map.addControl(new AMap.Geolocation());

      map.on('complete', () => {
        map.setZoomAndCenter(initialZoom, initialCenter, true);
        // 更新标记以显示选中状态
        updateMarkers(vehicleList.value);
        startPolling();
      });

      // 监听地图缩放变化
      map.on('zoomend', () => {
        // 移除不必要的日志
      });

    } catch (error) {
      console.error('Failed to load AMap:', error);
      message.error('地图加载失败');
    }
  }
}

// Start polling
const startPolling = () => {
  // 移除初始获取，因为已经在initMap中获取了
  pollingTimer = window.setInterval(fetchClientLocation, 10000); // Poll every 10 seconds
}

// Add list height ref
const listHeight = ref(0)

// Update list height on mount and resize
const updateListHeight = () => {
  const listContent = document.querySelector('.list-content')
  if (listContent) {
    listHeight.value = listContent.clientHeight
  }
}

onMounted(async () => {
  await fetchInfoData();
  await initMap();
  updateListHeight();
  window.addEventListener('resize', updateListHeight);
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
  }
  window.removeEventListener('resize', updateListHeight);
})

// Optimize list rendering
const listItemKey = (item: any) => item.clientId
</script>

<style scoped>
.home-container {
  padding: 4px;
  height: 78vh;
}

.split-layout {
  display: flex;
  width: 100%;
  height: 100%;
  gap: 16px;
}

.left-panel {
  width: 25%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  width: 75%;
  position: relative;
}

.map {
  width: 100%;
  height: 100%;
  border-radius: 4px;
}

.info-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.vehicle-list {
  flex: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header,
.list-header {
  padding: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header .title,
.list-header .title {
  font-size: 16px;
  font-weight: 500;
}

.card-content {
  padding: 12px;
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px 8px 8px;
}

/* Custom scrollbar styles */
.list-content::-webkit-scrollbar {
  width: 6px;
}

.list-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.list-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.list-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

:deep(.ant-list) {
  padding: 0;
}

:deep(.ant-list-item) {
  padding: 8px 4px !important;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}

:deep(.ant-list-item:hover) {
  background: rgba(0, 0, 0, 0.04);
}

:deep(.ant-list-item.active) {
  background-color: #e6f7ff;
}

:deep(.ant-list-item.active:hover) {
  background-color: #bae7ff;
}

:deep(.ant-list-item-meta) {
  padding: 0;
  margin: 0;
}

:deep(.ant-list-item-meta-content) {
  padding: 0;
  margin: 0;
}

:deep(.ant-list-item-meta-title) {
  margin: 0;
  padding: 0;
}

:deep(.ant-list-item-meta-description) {
  margin: 0;
  padding: 0;
}

:deep(.ant-descriptions) {
  margin-bottom: 0;
}

:deep(.ant-descriptions-item-label) {
  padding-bottom: 4px;
  font-size: 14px;
}

:deep(.ant-descriptions-item-content) {
  font-size: 14px;
}

:deep(.ant-input) {
  border-radius: 4px;
}

:deep(.ant-input:hover),
:deep(.ant-input:focus) {
  border-color: #40a9ff;
}

@media screen and (max-width: 1600px) {
  .vehicle-info {
    font-size: 14px;
  }
  
  .client-id,
  .plate-no {
    font-size: 14px;
  }
  
  .create-time {
    font-size: 12px;
  }
  
  .count {
    font-size: 14px;
  }
}

@media screen and (max-width: 1200px) {
  .vehicle-info {
    font-size: 14px;
  }
  
  .client-id,
  .plate-no {
    font-size: 14px;
  }
  
  .create-time {
    font-size: 12px;
  }
  
  .count {
    font-size: 14px;
  }
}

@media screen and (max-width: 768px) {
  .split-layout {
    flex-direction: column;
  }
  
  .left-panel,
  .right-panel {
    width: 100%;
  }
  
  .left-panel {
    height: 40%;
  }
  
  .right-panel {
    height: 60%;
  }
  
  .vehicle-info {
    font-size: 12px;
  }
  
  .client-id,
  .plate-no {
    font-size: 11px;
  }
  
  .create-time {
    font-size: 10px;
  }
  
  .count {
    font-size: 11px;
  }
  
  .card-header .title,
  .list-header .title {
    font-size: 14px;
  }
  
  :deep(.ant-descriptions-item-label),
  :deep(.ant-descriptions-item-content) {
    font-size: 12px;
  }
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.truncate-text {
  display: inline-block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
}

:deep(.ant-list-item-meta) {
  padding-right: 4px;
}

:deep(.ant-list-item-meta.active) {
  background-color: #e6f7ff;
  border-radius: 4px;
  padding: 4px;
  margin: -4px -4px;
}

:deep(.ant-list-item-meta.active:hover) {
  background-color: #bae7ff;
}

/* Custom marker styles */
:deep(.custom-marker) {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  transition: all 0.3s ease;
}

:deep(.custom-marker.selected) {
  transform: scale(1.3);
  z-index: 100;
  box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.3);
}

:deep(.custom-marker.selected::after) {
  border-top-color: #1890ff;
}

:deep(.custom-marker.normal) {
  background-color: #52c41a;
}

:deep(.custom-marker.alarm) {
  background-color: #ff4d4f;
}

:deep(.custom-marker.offline) {
  background-color: #999999;
}

:deep(.custom-marker::after) {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid #fff;
  transition: all 0.3s ease;
}

:deep(.custom-marker::before) {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #fff;
}

.vehicle-info {
  font-size: 14px;
  font-feature-settings: tnum;
  font-variant-numeric: tabular-nums;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol', 'Noto Color Emoji';
  padding-right: 4px;
}

.client-id {
  max-width: 100px;
}

.plate-no {
  max-width: 70px;
}

.create-time {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.count {
  font-size: 14px;
  color: #666;
  margin-left: 4px;
}

.vehicle-item {
  display: flex;
  align-items: center;
  padding: 0 4px;
}
</style>
  