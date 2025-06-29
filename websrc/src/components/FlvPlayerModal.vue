<template>
  <a-modal
    :visible="visible"
    @cancel="handleCancel"
    :footer="null"
    width="780px"
  >
    <template #title>
        <span>
          FLV视频播放器 {{ flvUrl }}
          <a :href="'/jessibuca/demo.html?flvUrl='+flvUrl" style="margin-left: 10px" target="_blank">标准版本</a>
          <a :href="'/jessibuca-pro/demo.html?flvUrl='+flvUrl" style="margin-left: 10px" target="_blank">Pro版本</a>
        </span>
    </template>
    <div ref="playerContainer" style="width: 100%; height: 450px;"></div>
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

// 创建 ref
const playerContainer = ref(null);
const player = ref(null);

// 初始化播放器
const initPlayer = () => {
    if (player.value) {
      return;
    }
    player.value = new Jessibuca({
      container: playerContainer.value,
      videoBuffer: 0.2, // 缓存时长
      isResize: true,
      text: "",
      // 视频加载转圈时的提示文字
      loadingText: "加载中",
      decoder: "/jessibuca/decoder.js",
      // 是否有音频，如果设置false，则不对音频数据解码，提升性能。
      hasAudio:props.hasAudio,
      //  是否开启控制台调试打印
      debug: false,
      // 是否显示网速
      showBandwidth: true,
      // fullscreen 是否显示全屏按钮
      // screenshot 是否显示截图按钮
      // play 是否显示播放暂停按钮
      // audio 是否显示声音按钮
      // record 是否显示录制按钮
      operateBtns: {
        fullscreen: true,
        screenshot: true,
        play: true,
        audio: props.hasAudio,
      },
      vod: true,
      // 是否不使用离屏模式（提升渲染能力）
      forceNoOffscreen: true,
      // 是否开启声音，默认是关闭声音播放的。
      isNotMute: props.hasAudio,
    });
    player.value.play(props.flvUrl);
};

const playbackSpeed = (speed) => {
  console.log('playbackSpeed:', speed);
  // 播放器不支持倍速
}

defineExpose({ playbackSpeed })

// 销毁播放器
const destroyPlayer = () => {
  if (player.value) {
    player.value.destroy();
    player.value = null;
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
