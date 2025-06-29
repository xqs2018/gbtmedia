<template>
  <a-modal
    :visible="visible"
    @cancel="handleCancel"
    :footer="null"
    width="780px"
  >
    <template #title>
        <span>
          FLV-JS视频播放器 {{ flvUrl }}
        </span>
    </template>
    <video autoplay controls width="100%" height="500" id="myVideo"></video>
    <slot></slot>
  </a-modal>
</template>

<script setup>
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import { Modal } from 'ant-design-vue';

// 接收 props
const props = defineProps({
  flvUrl: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: false
  },
  hasAudio: {
    type: Boolean,
    default: false
  }
});

// 定义 emit
const emit = defineEmits(['cancel']);

const flvPlayer = ref(null);

// 初始化播放器
const initPlayer = () => {
    if (flvPlayer.value) {
        return;
    }
  // mpegts.js是 flv.js的升级版本更好支持追逐直播延迟
  // https://github.com/xqq/mpegts.js
  if (mpegts.isSupported()) {
    var videoElement = document.getElementById('myVideo');
    flvPlayer.value = mpegts.createPlayer({
          type: 'flv',
          url: props.flvUrl,
          hasAudio:props.hasAudio
        },
        {
          isLive: true,
          // 自动追帧，回放不要开启，影响手动倍速
          //liveSync: true,
          //liveSyncPlaybackRate: 2.0,
        });

    flvPlayer.value.attachMediaElement(videoElement);
    flvPlayer.value.load();
    flvPlayer.value.play();

    flvPlayer.value.on(flvjs.Events.STATISTICS_INFO, function(stats) {
      // 计算当前播放延迟
      var latency = (Date.now() / 1000) - stats.currentTime;

      // 如果延迟超过一定阈值，则执行追帧策略
      if (latency > 2) { // 假设阈值为2秒
        console.warn('High latency detected:', latency);

        // 快进到最近的关键帧，或使用倍速播放
        flvPlayer.value().currentTime = stats.currentTime + latency;
        // flvPlayer.playbackRate = 1.5; // 或者使用倍速播放
      }
    })
  }
};

const playbackSpeed = (speed) => {
  console.log('playbackSpeed:', speed);
  var videoElement = document.getElementById('myVideo');
   videoElement.playbackRate = speed;
  //flvPlayer.value.playbackRate = speed;
}

defineExpose({ playbackSpeed })

// 销毁播放器
const destroyPlayer = () => {
    if (flvPlayer.value) {
      flvPlayer.value.unload();
      flvPlayer.value.detachMediaElement();
      flvPlayer.value.destroy();
      flvPlayer.value = undefined;
    }
};

// 处理取消事件
const handleCancel = () => {
  emit('cancel');
};

// 监听 visible 的变化
watch(() => props.visible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      initPlayer();
    });
  } else {
    destroyPlayer();
  }
});

// 组件销毁前销毁播放器
onBeforeUnmount(() => {
  destroyPlayer();
});
</script>

<style scoped>
/* 可以添加一些样式来美化播放器 */
</style>
