<template>
  <div class="container" >
      <!-- 左侧目录区域 -->
      <div class="sidebar">
        <div class="layout-switcher">
          <select v-model="currentLayout" class="layout-select" @change="switchLayout(currentLayout)">
            <option v-for="layout in layouts" :key="layout.value" :value="layout.value">
              {{ layout.label }}
            </option>
          </select>
          <select v-model="pollInterval" class="layout-select" @change="switchPollInterval(pollInterval)">
            <option v-for="interval in pollIntervals" :key="interval.value" :value="interval.value">
              {{ interval.label }}
            </option>
          </select>
        </div>
        <div class="menu-scroll-container">
          <div
                  v-for="(item, index) in menuData"
                  :key="index"
                  class="menu-item"
          >
              <div class="first-level" :class="{ 'offline': item.online !== 1 }" @click="toggleExpand(index)">
                  {{ item.label }}
                  <span class="arrow">
                      {{ item.isExpanded ? '▼' : '▶' }}
                  </span>
              </div>
              <div
                      v-show="item.isExpanded"
                      class="second-level"
              >
                  <div
                          v-for="(subItem, subIndex) in item.children"
                          :key="subIndex"
                          class="sub-item"
                          :class="{ 'offline': subItem.online !== 1 }"
                          @click="handleSubItemClick(index, subIndex)"
                  >
                      <span class="sequence-number">{{ subIndex + 1 }}</span>
                      {{ subItem.label }}
                  </div>
              </div>
          </div>
        </div>
      </div>

      <!-- 右侧视频容器区域 -->
      <div class="content" ref="contentRef" @dblclick="handleContentDblClick">
          <div class="video-grid" :data-layout="currentLayout">
              <div
                      v-for="(item, index) in videoItems"
                      :key="index"
                      class="video-container"
                      :class="{ 'selected': selectedVideoIndex === index }"
                      @click="handleVideoContainerClick(index)"
              >
                  <div class="video-box">
                    <video width="100%" height="100%" autoplay muted controls :ref="el => refVideoContainer[index] = el"></video>
                      <div v-if="item.label" class="menu-label">
                          {{ item.label }}
                      </div>
                      <div v-else-if="!item.isPlaying" class="video-signal">
                          视频信号{{ index + 1 }}
                      </div>
                  </div>
              </div>
          </div>
      </div>
  </div>
</template>

<script setup>
import {nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'
import {channelTree, play} from "@/api/gbt28181";

const layouts = [
  { label: '1分屏', value: 1 },
  { label: '4分屏', value: 4 },
  { label: '6分屏', value: 6 },
  { label: '9分屏', value: 9 }
];

const pollIntervals = [
  { label: '关闭', value: 0 },
  { label: '1分钟', value: 60 },
  { label: '5分钟', value: 300 },
  { label: '10分钟', value: 600 },
  { label: '无限制', value: 86400 }
];


const currentLayout = ref(parseInt(window.localStorage.getItem("MpegtsIndexLayout")) || 4); // 默认4分屏
const pollInterval = ref(parseInt(window.localStorage.getItem("MpegtsPollInterval")) || 0); // 默认关闭
const currentPollIndex = ref(0); // 当前轮询到的视频索引
const pollTimer = ref(null); // 轮询定时器

const menuData = reactive([])

const videoItems = reactive(
    Array.from({ length: currentLayout.value }, () => ({ label: '', isPlaying: false }))
)

const refVideoContainer = reactive(Array.from({ length: currentLayout.value }, () => null));

let videoPlayer =[];

const selectedVideoIndex = ref(-1); // 当前选中的视频容器索引

// 组件挂载时获取第一页数据
onMounted(async () => {
    const param = {};
    const r1 = await channelTree(param);
    let tree = r1.data
    tree = tree.map(v=>{
        let children =v.children.map(v2=>{
            return {
                "label": v2.customName,
                "key":v.gbId + "_" + v2.gbId,
                "online": v2.online
            }
        })
        return  {
            "label": v.customName,
             "online":v.online,
            "isExpanded":false,
             "children":children
        }
    })
    menuData.push(...tree);

    // 如果设置了轮询，开始轮询
    const pollInterval = parseInt(window.localStorage.getItem("MpegtsPollInterval")) || 0;
    if (pollInterval > 0) {
        startPolling();
    }
});

onBeforeUnmount(() => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
  }
  console.log("destroy videoPlayer ...",videoPlayer)
  videoPlayer.forEach(v=>v.destroy())
    // 强制重新加载页面
  window.location.reload();
})

const clearLabel = (index) => {
    videoItems[index].label = ''
}

const toggleExpand = (index) => {
  menuData[index].isExpanded = !menuData[index].isExpanded
}

// 在script中添加以下代码
const contentRef = ref(null);

const handleContentDblClick = (event) => {
// 阻止事件冒泡到父元素
event.stopPropagation();

// 检查是否子元素触发的双击
if (!event.target.classList.contains('video-container')) {
  toggleFullScreen();
}
};

const toggleFullScreen = () => {
if (!document.fullscreenElement) {
  const element = contentRef.value;

  // 多浏览器兼容
  const requestMethod =
      element.reMpegtsullscreen ||
      element.webkitRequestFullscreen ||
      element.mozRequestFullScreen ||
      element.msRequestFullscreen;

  if (requestMethod) {
    requestMethod.call(element).catch(err => {
      console.error('全屏请求失败:', err);
    });
  }

  // 添加全屏样式
  element.classList.add('fullscreen-active');
} else {
  document.exitFullscreen();
}
};

const switchLayout = (layout) => {
  // 更新视频容器数量
  const oldLength = videoItems.length;
  const newLength = layout;
  
  // 如果需要减少容器，先销毁多余的播放器
  if (newLength < oldLength) {
    for (let i = newLength; i < oldLength; i++) {
      if (videoPlayer[i]) {
        videoPlayer[i].destroy();
        videoPlayer[i] = null;
      }
    }
    // 移除多余的容器
    videoItems.splice(newLength);
    refVideoContainer.splice(newLength);
  } else if (newLength > oldLength) {
    // 如果需要增加容器，添加新的容器
    for (let i = oldLength; i < newLength; i++) {
      videoItems.push({ label: '', isPlaying: false });
      refVideoContainer.push(null);
    }
  }
  
  currentLayout.value = layout;
  // 保存到 localStorage
  window.localStorage.setItem("MpegtsIndexLayout", layout.toString());
  // 强制重新加载页面
  window.location.reload();
};

const switchPollInterval = (interval) => {
  pollInterval.value = interval;
  // 保存到 localStorage
  window.localStorage.setItem("MpegtsPollInterval", interval.toString());
  // 强制重新加载页面
  window.location.reload();
};

// 获取所有在线的视频项
const getOnlineVideos = () => {
  const onlineVideos = [];
  menuData.forEach(item => {
    if (item.online === 1) {
      item.children.forEach(child => {
        if(child.online === 1){
          onlineVideos.push({
          label: child.label,
          key: child.key
        });
        }
      });
    }
  });
  return onlineVideos;
};

// 播放单个视频
const playVideo = async (videoIndex, videoData) => {
  return new Promise((resolve) => {
    try {
      // 先销毁当前播放器
      if (videoPlayer[videoIndex]) {
        videoPlayer[videoIndex].pause();
        videoPlayer[videoIndex].unload();
        videoPlayer[videoIndex].detachMediaElement();
        videoPlayer[videoIndex].destroy();
        videoPlayer[videoIndex] = null;
      }

      videoItems[videoIndex].label = videoData.label + " 播放中..."
      videoItems[videoIndex].isPlaying = false

      // 重建播放器
      let deviceId = videoData.key.split("_")[0]
      let channelId = videoData.key.split("_")[1]
      play({deviceId: deviceId, channelId: channelId}).then(v => {
        videoPlayer[videoIndex] = mpegts.createPlayer({
          type: 'flv',
          url: v.data.httpFlv.replace("http","ws"),
          hasAudio:false
        }, {
          isLive: true,
          // 开启自动追帧
          liveSync: true,
          liveSyncPlaybackRate: 2.0
        });
        videoPlayer[videoIndex].attachMediaElement(refVideoContainer[videoIndex]);
        videoPlayer[videoIndex].load();

        // 确保视频元素是静音的
        refVideoContainer[videoIndex].muted = true;

        // 尝试播放视频
        const playPromise = refVideoContainer[videoIndex].play();
        if (playPromise !== undefined) {
          playPromise.then(() => {
            videoItems[videoIndex].label = ""
            videoItems[videoIndex].isPlaying = true
            resolve(true);
          }).catch(error => {
            console.error('播放失败:', error);
            clearLabel(videoIndex);
            resolve(false);
          });
        }

        videoPlayer[videoIndex].on(mpegts.Events.LOADING_COMPLETE, function (res) {
              console.log("检测到播放结束，重新开始播放",videoIndex,res)
              playVideo(videoIndex, videoData);
        }); 

        // 设置超时
        setTimeout(() => {
          if (!videoItems[videoIndex].isPlaying) {
            console.error('播放超时')
            clearLabel(videoIndex)
            resolve(false);
          }
        }, 5000);

      }).catch(e => {
        console.error('播放失败', e)
        clearLabel(videoIndex)
        resolve(false);
      });
    } catch (e) {
      console.error('播放失败', e)
      clearLabel(videoIndex)
      resolve(false);
    }
  });
};

// 开始轮询
const startPolling = () => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
  }

  const onlineVideos = getOnlineVideos();
  if (onlineVideos.length === 0) return;

  const playNextVideo = async () => {
    // 按照窗口顺序（1到9）播放视频
    for (let windowIndex = 0; windowIndex < 9; windowIndex++) {
      // 如果当前窗口索引小于布局数量，则播放视频
      if (windowIndex < currentLayout.value) {
        const videoData = onlineVideos[currentPollIndex.value % onlineVideos.length];
        const success = await playVideo(windowIndex, videoData);
        if (success) {
          currentPollIndex.value++;
        }
        // 无论成功失败都等待一小段时间再播放下一个视频
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    }
  };

  // 执行一轮轮询
  const executePollingRound = async () => {
    await playNextVideo();
    // 如果设置了轮询间隔，等待指定时间后开始下一轮
    if (pollInterval.value > 0) {
      setTimeout(executePollingRound, pollInterval.value * 1000);
    }
  };

  // 开始第一轮轮询
  executePollingRound();
};

const handleVideoContainerClick = (index) => {
  // 如果点击的是当前选中的容器，则取消选中
  if (selectedVideoIndex.value === index) {
    selectedVideoIndex.value = -1;
  } else {
    // 否则选中新容器
    selectedVideoIndex.value = index;
  }
};

const handleSubItemClick = async (parentIndex, subIndex) => {
  if (selectedVideoIndex.value === -1) {
    return; // 如果没有选中视频容器，直接返回
  }

  const subItem = menuData[parentIndex].children[subIndex];
  try {
    // 先销毁当前播放器
    if (videoPlayer[selectedVideoIndex.value]) {
      videoPlayer[selectedVideoIndex.value].pause();
      videoPlayer[selectedVideoIndex.value].unload();
      videoPlayer[selectedVideoIndex.value].detachMediaElement();
      videoPlayer[selectedVideoIndex.value].destroy();
      videoPlayer[selectedVideoIndex.value] = null;
    }

    videoItems[selectedVideoIndex.value].label = subItem.label + " 播放中..."
    videoItems[selectedVideoIndex.value].isPlaying = false

    // 重建播放器
    let deviceId = subItem.key.split("_")[0]
    let channelId = subItem.key.split("_")[1]
    const v = await play({deviceId: deviceId, channelId: channelId});
    videoPlayer[selectedVideoIndex.value] = mpegts.createPlayer({
      type: 'flv',
      url: v.data.httpFlv.replace("http","ws"),
      hasAudio:false
    }, {
      isLive: true,
      liveSync: true,
      liveSyncPlaybackRate: 2.0
    });
    videoPlayer[selectedVideoIndex.value].attachMediaElement(refVideoContainer[selectedVideoIndex.value]);
    videoPlayer[selectedVideoIndex.value].load();

    videoPlayer[selectedVideoIndex.value].play();

    /*videoPlayer[selectedVideoIndex.value].on(mpegts.Events.ERROR, (errorType, errorDetail, errorInfo) => {
		    console.log("errorType:", errorType);
        console.log("errorDetail:", errorDetail);
        console.log("errorInfo:", errorInfo);
    })*/


    /*videoPlayer[selectedVideoIndex.value].on("statistics_info", function (res) {
      console.log("statistics_info",res)
     });*/

     videoPlayer[selectedVideoIndex.value].on(mpegts.Events.LOADING_COMPLETE, function (res) {
      console.log("LOADING_COMPLETE",res)
      // 重新播放当前窗口的视频
      const currentVideoData = {
        label: subItem.label,
        key: subItem.key
      };
      playVideo(selectedVideoIndex.value, currentVideoData);
     });


    videoItems[selectedVideoIndex.value].label = ""
    videoItems[selectedVideoIndex.value].isPlaying = true
    selectedVideoIndex.value = -1; // 播放后清除选中状态
  } catch (e) {
    console.error('播放失败', e)
    clearLabel(selectedVideoIndex.value)
    selectedVideoIndex.value = -1; // 播放失败也清除选中状态
  }
};

</script>

<style scoped>
.container {
    display: flex;
    height: 100vh;
    background: #1a1a1a;
    overflow: hidden;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
}

/* 防止白屏闪烁 */
:root {
    background-color: #1a1a1a;
}

body {
    background-color: #1a1a1a;
    margin: 0;
    padding: 0;
    overflow: hidden;
}

/* 左侧目录样式 */
.sidebar {
    width: 18%;
    min-width: 240px;
    max-width: 320px;
    background: #2d2d2d;
    border-right: 1px solid #404040;
    display: flex;
    flex-direction: column;
    height: 100%;
    color: #ccc;
    flex-shrink: 0;
    overflow: hidden;
}

.layout-switcher {
  padding: 10px;
  display: flex;
  gap: 8px;
  background: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.layout-select {
  flex: 1;
  padding: 8px;
  background: #3a3a3a;
  border: 1px solid #404040;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
  min-width: 0;
}

.layout-select:hover {
  background: #4a4a4a;
}

.layout-select:focus {
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
}

.layout-select option {
  background: #3a3a3a;
  color: #fff;
  padding: 8px;
}

.menu-scroll-container {
    overflow-y: auto;
    flex: 1;
    padding: 10px;
    height: 100%;
}

.menu-scroll-container::-webkit-scrollbar {
    width: 6px;
    background-color: #2d2d2d;
}

.menu-scroll-container::-webkit-scrollbar-thumb {
    border-radius: 3px;
    background: #555;
}

.menu-item {
    margin-bottom: 4px;
    border-radius: 4px;
    overflow: hidden;
}

.first-level {
    padding: 12px;
    cursor: pointer;
    background: #3a3a3a;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: background 0.2s;
    position: relative;
}

.first-level::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 16px;
    border-radius: 2px;
    background-color: #4CAF50;
    transition: background-color 0.3s;
}

.first-level.offline::before {
    background-color: #f44336;
}

.arrow {
    font-size: 12px;
    color: #4CAF50;
    transition: color 0.3s;
}

.first-level.offline .arrow {
    color: #f44336;
}

.second-level {
    margin-top: 2px;
    background: #353535;
}

.sub-item {
    padding: 10px 16px;
    cursor: pointer;
    background: #404040;
    margin: 2px 0;
    transition: all 0.2s;
    font-size: 14px;
    color: #aaa;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    position: relative;
}

.sub-item::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #4CAF50;
    transition: background-color 0.3s;
    margin-left: 8px;
}

/* 增加选择器优先级 */
.sub-item.offline::before,
.sub-item[class*="offline"]::before {
    background-color: #f44336 !important;
}

.sub-item:hover {
    background: #4a4a4a;
}

.sequence-number {
    margin-right: 8px;
    color: #666;
    font-size: 14px;
    min-width: 20px;
    text-align: center;
}

.sub-item:hover .sequence-number {
    color: #fff;
}

/* 右侧视频容器 */
.content {
    flex: 1;
    padding: 2px;
    background: #1a1a1a;
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: stretch;
    justify-content: stretch;
    height: 100%;
}

.video-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(3, 1fr);
    width: 100%;
    height: 100%;
    gap: 2px;
    margin: 0;
    padding: 2px;
    box-sizing: border-box;
}

.video-container {
    position: relative;
    width: 100%;
    height: 100%;
    background: #000;
    overflow: hidden;
    border: 1px solid #333;
    transition: border-color 0.3s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.video-box {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

/* 全屏样式 */
.content:fullscreen {
    background: #000;
    padding: 2px !important;
    display: flex !important;
    justify-content: stretch;
    align-items: stretch;
    height: 100vh;
    width: 100vw;
}

.content:fullscreen .video-grid {
    width: 100%;
    height: 100%;
    margin: 0;
    gap: 2px;
    padding: 2px;
    box-sizing: border-box;
}

/* 根据布局设置全屏网格 */
.content:fullscreen .video-grid[data-layout="1"] {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
}

.content:fullscreen .video-grid[data-layout="4"] {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
}

.content:fullscreen .video-grid[data-layout="6"] {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(2, 1fr);
}

.content:fullscreen .video-grid[data-layout="9"] {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(3, 1fr);
}

/* 宽屏适配（宽度 > 4:3比例） */
@media (min-aspect-ratio: 4/3) {
    .content:fullscreen .video-grid {
        width: 100%;
        height: 100%;
    }
}

/* 窄屏适配（高度 > 4:3比例） */
@media (max-aspect-ratio: 4/3) {
    .content:fullscreen .video-grid {
        width: 100%;
        height: 100%;
    }
}

/* 每个视频容器保持4:3 */
.content:fullscreen .video-container {
    width: 100%;
    height: 100%;
    border-width: 1px;
}

/* 浏览器前缀兼容 */
.content:-webkit-full-screen {
    padding: 2px !important;
}

/* 播放器控件适配 */
:deep(.jessibuca-operate) {
    background: linear-gradient(to top, rgba(0,0,0,0.7), transparent) !important;
    padding: 8px 12px !important;
}

:deep(.jessibuca-icons) {
    filter: brightness(1.2);
}

/* 文字和装饰样式 */
.menu-label {
    color: rgba(255, 255, 255, 0.9);
    font-size: 16px;
    text-shadow: 0 2px 4px rgba(0,0,0,0.5);
    animation: fadeIn 0.3s;
    padding: 10px 16px;
    background: rgba(0,0,0,0.4);
    border-radius: 4px;
    position: absolute;
    z-index: 10;
}

.video-signal {
    color: rgba(255, 255, 255, 0.6);
    font-size: 16px;
    text-shadow: 0 2px 4px rgba(0,0,0,0.5);
    animation: fadeIn 0.3s;
    padding: 10px 16px;
    background: rgba(0,0,0,0.4);
    border-radius: 4px;
    position: absolute;
    z-index: 10;
}

.play-line {
position: relative;
width: 80%;
height: 2px;
opacity: 0.6;
}

.play-line::before,
.play-line::after {
content: '';
position: absolute;
top: 50%;
left: 50%;
transform: translate(-50%, -50%);
}

.play-line::before {
width: 100%;
height: 2px;
background: linear-gradient(
90deg,
rgba(0,0,0,0) 10%,
rgba(255,255,255,1) 50%,
rgba(0,0,0,0) 90%
);
}

.play-line::after {
width: 150%;
height: 30px;
background: radial-gradient(
circle,
rgba(255,255,255,0.4) 0%,
rgba(255,255,255,0) 70%
);
filter: blur(10px);
}


@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(5px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 隐藏视频默认控件 */
video::-webkit-media-controls {
    display: none !important;
}

video::-webkit-media-controls-panel {
    display: none !important;
}

video::-webkit-media-controls-play-button {
    display: none !important;
}

video::-webkit-media-controls-timeline {
    display: none !important;
}

video::-webkit-media-controls-current-time-display {
    display: none !important;
}

video::-webkit-media-controls-time-remaining-display {
    display: none !important;
}

video::-webkit-media-controls-mute-button {
    display: none !important;
}

video::-webkit-media-controls-volume-slider {
    display: none !important;
}

video::-webkit-media-controls-fullscreen-button {
    display: none !important;
}

video::-webkit-media-controls-rewind-button {
    display: none !important;
}

video::-webkit-media-controls-return-to-realtime-button {
    display: none !important;
}

video::-webkit-media-controls-toggle-closed-captions-button {
    display: none !important;
}

/* 移除视频标签的默认样式 */
video {
    -webkit-appearance: none;
    -moz-appearance: none;
    appearance: none;
    outline: none;
    border: none;
    background: transparent;
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: fill;
    background: #000;
}

/* 根据布局设置网格 */
.video-grid[data-layout="1"] {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
}

.video-grid[data-layout="4"] {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
}

.video-grid[data-layout="6"] {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(2, 1fr);
}

.video-grid[data-layout="9"] {
    grid-template-columns: repeat(3, 1fr);
    grid-template-rows: repeat(3, 1fr);
}

.video-container.selected {
    border: 2px solid #4CAF50;
    box-shadow: 0 0 10px rgba(76, 175, 80, 0.3);
}
</style>
