<template>
  <div class="record-view-container">
    <!-- Left side: Video files list -->
    <div class="video-list">
      <div class="date-picker-container">
        <a-date-picker
          v-model:value="selectedDate"
          :disabledDate="disabledDate"
          @change="handleDateChange"
          style="width: 100%"
          :locale="locale"
        />
      </div>
      <a-spin :spinning="isTableLoading">
        <a-list
          class="video-list-content"
          :data-source="filteredRecords"
          :bordered="false"
        >
          <template #renderItem="{ item: record, index }">
            <a-list-item
              class="video-item"
              :class="{ 'video-item-active': currentIndex === index }"
              @click="handlePlay(record, index)"
            >
              <div class="video-item-content">
                <span class="video-index">{{ index + 1 }}</span>
                <span class="video-time">{{ formatTime(record.startTime) }} - {{ formatTime(record.endTime) }}</span>
                <span class="video-type" :class="record.type === 1 ? 'type-device' : 'type-cloud'">
                  {{ record.type === 1 ? '设备' : record.type === 2 ? '云端' : record.type }}
                </span>
                <span class="video-size">{{ record.fileSize }}</span>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </a-spin>
    </div>

    <!-- Right side: Video player -->
    <div class="video-player-container">
      <div v-if="currentVideoUrl" class="video-player">
        <div class="video-container">
          <div v-if="isVideoLoading" class="video-loading">
            <a-spin size="large" />
            <span class="loading-text">视频加载中...</span>
          </div>
          <video
            ref="videoRef"
            :src="currentVideoUrl"
            controls
            muted
            style="width: 100%; height: 100%"
            @loadstart="isVideoLoading = true"
            @canplay="handleVideoCanPlay"
            @error="handleVideoError"
          ></video>
        </div>
        <div class="video-actions">
          <a-button 
            class="action-button" 
            @click="handleDownload"
            :disabled="!currentFileName"
          >
            <download-outlined /> 下载
          </a-button>
          <a-button danger @click="handleDelete(currentRecord)" :loading="currentRecord?.loading">
            <delete-outlined /> 删除
          </a-button>
        </div>
      </div>
      <div v-else class="no-video-selected">
        请从左侧选择要播放的视频
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { deleteRecordFile, listRecordFile } from "@/api/gbt28181";
import { message } from 'ant-design-vue';
import { DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';

// 设置 dayjs 语言为中文
dayjs.locale('zh-cn');

// API基础URL
const baseApiUrl = `${window.location.protocol}//${window.location.host}${import.meta.env.VITE_APP_BASE_API}`.replace(/\/$/, '');;

// 日期选择相关
const selectedDate = ref<dayjs.Dayjs>(dayjs());
const allRecords = ref<any[]>([]);
const isTableLoading = ref(false);

// 禁用未来日期
const disabledDate = (current: dayjs.Dayjs) => {
  return current && current > dayjs().endOf('day');
};

// 格式化时间显示
const formatTime = (timeStr: string) => {
  return dayjs(timeStr).format('HH:mm:ss');
};

// 获取数据
const fetchData = async () => {
  isTableLoading.value = true;
  try {
    const param = {
      pageNo: 0,
      pageSize: 1000,
      deviceId: deviceId.value,
      channelId: channelId.value
    };
    const response = await listRecordFile(param);
    if (response.data) {
      allRecords.value = response.data.map((item: any) => ({
        ...item,
        loading: false,
      }));
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    message.error('获取数据失败');
  } finally {
    isTableLoading.value = false;
  }
};

// 根据选择的日期过滤记录
const filteredRecords = computed(() => {
  if (!selectedDate.value || !allRecords.value.length) return [];
  
  const startOfDay = selectedDate.value.startOf('day');
  const endOfDay = selectedDate.value.endOf('day');
  
  return allRecords.value.filter(record => {
    const recordDate = dayjs(record.startTime);
    return recordDate.isAfter(startOfDay) && recordDate.isBefore(endOfDay);
  });
});

// 处理日期变化
const handleDateChange = () => {
  fetchData();
};

// 视频播放相关状态
const currentVideoUrl = ref('');
const currentFileName = ref('');
const currentRecord = ref<any>(null);
const currentIndex = ref(-1);
const isVideoLoading = ref(false);
const videoRef = ref<HTMLVideoElement | null>(null);

// Add this after the other refs
const currentFetchController = ref<AbortController | null>(null);

// 处理播放
const handlePlay = async (record: any, index: number) => {
  try {
    isVideoLoading.value = true;
    
    // 取消之前的请求
    if (currentFetchController.value) {
      currentFetchController.value.abort();
      currentFetchController.value = null;
    }
    
    // 先清理当前视频
    if (videoRef.value) {
      videoRef.value.pause();
      videoRef.value.removeAttribute('src');
      videoRef.value.load();
    }
    
    // 重置状态
    currentVideoUrl.value = '';
    currentFileName.value = '';
    currentRecord.value = null;
    currentIndex.value = -1;
    
    // 等待DOM更新
    await nextTick();
    
    // 设置新的视频源
    const newVideoUrl = `${baseApiUrl}/backend/gbt28181/downloadRecordFile/${record.fileName}`;
    
    // 创建新的 AbortController
    currentFetchController.value = new AbortController();
    
    // 预加载视频
    const response = await fetch(newVideoUrl, {
      signal: currentFetchController.value.signal,
      cache: 'no-store'
    });
    
    if (!response.ok) {
      throw new Error('视频加载失败');
    }
    
    // 设置新的视频信息
    currentVideoUrl.value = newVideoUrl;
    currentFileName.value = record.fileName;
    currentRecord.value = record;
    currentIndex.value = index;
    
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('视频加载已取消');
      return;
    }
    console.error('视频切换失败:', error);
    message.error('视频加载失败，请重试');
    isVideoLoading.value = false;
  }
};

// 处理视频可以播放
const handleVideoCanPlay = () => {
  isVideoLoading.value = false;
  if (videoRef.value) {
    videoRef.value.play().catch(error => {
      console.error('自动播放失败:', error);
      message.error('自动播放失败，请手动点击播放');
    });
  }
};

// 处理视频加载错误
const handleVideoError = (error: any) => {
  console.error('视频加载错误:', error);
  isVideoLoading.value = false;
  message.error('视频加载失败，请重试');
  
  // 清理错误的视频源
  if (videoRef.value) {
    videoRef.value.pause();
    videoRef.value.removeAttribute('src');
    videoRef.value.load();
  }
  currentVideoUrl.value = '';
  currentFileName.value = '';
  currentRecord.value = null;
  currentIndex.value = -1;
};

// 删除记录
const handleDelete = async (record: any) => {
  record.loading = true;
  try {
    await deleteRecordFile({ fileName: record.fileName });
    message.success('删除成功');
    await fetchData();
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  } finally {
    record.loading = false;
  }
};

// 获取当前路由信息
const route = useRoute();
const deviceId = ref('');
const channelId = ref('');

// 组件挂载时获取数据
onMounted(() => {
  deviceId.value = route.query.deviceId as string;
  channelId.value = route.query.channelId as string;
  fetchData();
});

// 组件卸载时清理
onUnmounted(() => {
  if (videoRef.value) {
    videoRef.value.pause();
    videoRef.value.removeAttribute('src');
  }
  
  // 取消任何未完成的请求
  if (currentFetchController.value) {
    currentFetchController.value.abort();
    currentFetchController.value = null;
  }
});

// Add this function before onMounted
const handleDownload = () => {
  if (!currentFileName.value) {
    message.error('没有可下载的文件');
    return;
  }
  
  const downloadUrl = `${baseApiUrl}/backend/gbt28181/downloadRecordFile/${currentFileName.value}`;
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.download = currentFileName.value;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
</script>

<style scoped>
.record-view-container {
  display: flex;
  height: calc(100vh - 220px);
  gap: 20px;
  padding: 20px;
  overflow: hidden;
}

.video-list {
  width: 300px;
  border-right: 1px solid #f0f0f0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.date-picker-container {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.video-list-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.video-list-content::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.video-item {
  padding: 8px 16px !important;
  cursor: pointer;
  transition: all 0.3s;
}

.video-item:hover {
  background-color: #f5f5f5;
}

.video-item-active {
  background-color: #f0f7ff;
  border-right: 3px solid #3498db;
}

.video-item-active .video-index {
  background-color: #3498db;
  color: #fff;
}

.video-item-active .video-time {
  color: #3498db;
}

.video-item-active .video-type.type-device {
  background-color: #f5f6fa;
  color: #2c3e50;
  border-color: #dcdde1;
}

.video-item-active .video-type.type-cloud {
  background-color: #f6ffed;
  color: #52c41a;
  border-color: #b7eb8f;
}

.video-item-active .video-size {
  color: #1890ff;
}

.video-item-content {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.video-index {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.video-time {
  width: 125px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
  font-feature-settings: "tnum" 
}

.video-type {
  width: 36px;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 12px;
  text-align: center;
  flex-shrink: 0;
}

.type-device {
  background-color: #f5f6fa;
  color: #2c3e50;
  border: 1px solid #dcdde1;
}

.type-cloud {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.video-size {
  width: 65px;
  color: #999;
  font-size: 12px;
  text-align: right;
  flex-shrink: 0;
}

.video-player-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.video-player {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #000;
  height: 100%;
}

.video-container {
  flex: 1;
  position: relative;
  background: #000;
  min-height: 0;
}

.video-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  z-index: 1;
}

.loading-text {
  color: #fff;
  margin-top: 16px;
  font-size: 16px;
}

.video-actions {
  padding: 16px;
  display: flex;
  gap: 12px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.action-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 15px;
  height: 32px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  color: rgba(0, 0, 0, 0.85);
  cursor: pointer;
  transition: all 0.3s;
}

.action-button:hover {
  color: #40a9ff;
  border-color: #40a9ff;
}

.no-video-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 16px;
}
</style>
  