<template>
  <a-table
      :columns="columns"
      :data-source="tableData"
      :pagination="pagination"
      @change="handleTableChange"
      :loading="isTableLoading"
      :scroll="{ x: 'max-content' }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'online'">
       <span :style="{ color: record.online == '1'? 'green' :'red' }">
        {{ record.online == '1'? '在线' : '离线' }}
      </span>
      </template>
      <template v-if="column.key === 'action'">
        <router-link :to="'/GBT28181/DeviceChannelRecordView?deviceId='+deviceId+'&channelId='+record.channelId" style="margin-right: 10px;">录像</router-link>
        <router-link :to="'/GBT28181/DeviceChannelRecordFileView?deviceId='+deviceId+'&channelId='+record.channelId" style="margin-right: 10px;">文件</router-link>
        <a-button @click="handlePlay(record)" style="margin-right: 12px;" :loading="record.loading">播放</a-button>
        <a-button danger v-if="record.playCallId" @click="handleStop(record)" style="margin-right: 12px;" :loading="record.loading2">停止</a-button>
        <a-button @click="handleUpdate(record)" style="margin-right: 10px;">更新</a-button>
        <a-button danger @click="handleDelete(record)" style="margin-right: 10px;" :loading="record.loading1">删除</a-button>
      </template>
    </template>
  </a-table>
  <a-modal
      :visible="updateModalVisible"
      title="通道信息"
      @ok="handleUpdateModalOk"
      @cancel="handleUpdateModalCancel"
  >
    <a-form :form="formRef" :model="formData">
      <a-form-item label="自定义名称">
        <a-input v-model:value="formData.customName" />
      </a-form-item>
      <a-form-item label="云端录像">
        <a-select v-model:value="formData.cloudRecord">
          <a-select-option value="0">关闭</a-select-option>
          <a-select-option value="1">开启</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
  <FlvPlayerModal
      :visible="modalVisible"
      :hasAudio="true"
      :flvUrl="flvUrl"
      @cancel="flvPlayerModalCancel()"
      @update:visible="modalVisible = $event"
  >
    <div class="outer-container">
      <div class="square-container">
        <div class="triangle triangle-up" @click="handlecontrolPtz({'upDown':1})"></div>
        <div class="triangle triangle-down" @click="handlecontrolPtz({'upDown':2})"></div>
        <div class="triangle triangle-left" @click="handlecontrolPtz({'leftRight':1})"></div>
        <div class="triangle triangle-right" @click="handlecontrolPtz({'leftRight':2})"></div>
        <div class="circle circle-stop" @click="handlecontrolPtz({})"></div>
      </div>
      <div class="square-container square-container-two">
        <div class="circle circle-plus" @click="handlecontrolPtz({'inOut':2})">+</div>
        <div class="circle circle-minus" @click="handlecontrolPtz({'inOut':1})">-</div>
      </div>
      <div class="audio-container" style="pointer-events: none;">
        <div class="audio-item">
           <span>对讲</span>
          <div>
            <AudioOutlined v-if="audioType == 'talk'" :class="'audio-icon ' + audioLoadingClass" @click="handleAudio('talk')"/>
            <AudioMutedOutlined v-if="audioType != 'talk'"  class="audio-icon" @click="handleAudio('talk')"/>
          </div>
        </div>
        <div class="audio-item">
          <span>广播</span>
          <div>
            <AudioOutlined v-if="audioType == 'broadcast'" :class="'audio-icon ' + audioLoadingClass" @click="handleAudio('broadcast')"/>
            <AudioMutedOutlined v-if="audioType != 'broadcast'" class="audio-icon" @click="handleAudio('broadcast')"/>
          </div>
        </div>
      </div>
    </div>
  </FlvPlayerModal>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {
  broadcast,
  controlPtzCmd,
  deleteDevice,
  deleteDeviceChannel,
  pageDeviceChannel,
  play,
  queryCatalog, stopInvite, talk, updateDevice, updateDeviceChannel
} from "@/api/gbt28181";
import {useRoute} from "vue-router";
import {message} from "ant-design-vue";
import FlvPlayerModal from "@/components/FlvPlayerModal.vue";
import {AudioOutlined,AudioMutedOutlined } from '@ant-design/icons-vue';
import MediaStreamRecorder from 'msr';
import {FormInstance} from "ant-design-vue/es/form";

// 表格列配置
const columns = [
  {
    title: '设备ID',
    dataIndex: 'deviceId',
    key: 'deviceId',
    customRender:()=>{
      return deviceId.value
    }
  },
  {
    title: '通道ID',
    dataIndex: 'channelId',
    key: 'channelId',
  },
  {
    title: '名称',
    dataIndex: 'name',
    customRender: ({ record }) => record.customName || record.name
  },
  {
    title: '制造厂商',
    dataIndex: 'manufacturer',
    key: 'manufacturer',
  },
  {
    title: '型号',
    dataIndex: 'model',
    key: 'model',
  },
  {
    title: '状态',
    key: 'online',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',  // 添加固定右侧配置
    width: 400       // 设置固定列宽度
  }
];

// 表格数据
const tableData = ref<any[]>([]);

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
});

const isTableLoading = ref(false);

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  try {
      isTableLoading.value = true
    const param = {
      pageNo: page - 1, // 假设后端页码从 0 开始
      pageSize: pageSize,
      deviceId:deviceId.value,
    };
    const response = await pageDeviceChannel(param);
    // 更新表格数据
    tableData.value = response.data.content.map((item) => ({
      ...item,
      loading: false,
      loading1: false,
      loading2: false,
    }));
    pagination.value.total = response.data.page.totalElements;
    pagination.value.current = page;
  } catch (error) {
    console.error('获取数据失败:', error);
  }finally {
      isTableLoading.value = false
  }
};

// 表格分页、排序、筛选变化时触发
const handleTableChange = (pagination: any) => {
  const { current, pageSize } = pagination;
  fetchData(current, pageSize);
};

// 获取当前路由信息
const route = useRoute();
// 定义 deviceId 变量
const deviceId = ref('');

// 组件挂载时获取第一页数据
onMounted(() => {
  // 从路由的 query 参数中获取 deviceId
  deviceId.value = route.query.deviceId as string;
  fetchData(pagination.value.current, pagination.value.pageSize);
});

const modalVisible = ref(false);
const flvUrl = ref('');

let nowRecord
let nowPlay
const handlePlay = async (record) => {
  nowRecord = record
  record.loading = true;
  try {
    const v = await play({deviceId: record.deviceId, channelId: record.channelId});
    nowPlay = v.data
    flvUrl.value = v.data.httpFlv;
    modalVisible.value = true;
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
};

const handleStop = async (record) => {
  record.loading2 = true;
  try {
    await stopInvite({"callId": record.playCallId})
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading2 = false;
  }
}

const flvPlayerModalCancel = async () => {
  modalVisible.value = false
  audioType.value = ''
  stopRecording()
  if (lastAudioCallId) {
    await stopInvite({"callId": lastAudioCallId})
  }
}

const handleDelete = async (record) => {
  record.loading1 = true;
  try {
    const v = await deleteDeviceChannel({deviceId: record.deviceId, channelId: record.channelId});
    message.success(v.message);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading1 = false;
  }
  await fetchData(pagination.value.current, pagination.value.pageSize);
}

const updateModalVisible = ref(false);
const formRef = ref<FormInstance>();
const formData = ref({});

const handleUpdate = (record) => {
  formData.value = {
    ...record,
    cloudRecord: String(record.cloudRecord) // 确保是字符串
  };
  updateModalVisible.value = true;
};

const handleUpdateModalOk = async () => {
  await formRef.value?.validate();
  try {
    // 提取纯数据
    const pureFormData = JSON.parse(JSON.stringify(formData.value));
    const v = await updateDeviceChannel(pureFormData);
    message.success(v.message);
    await fetchData(pagination.value.current, pagination.value.pageSize);
    updateModalVisible.value = false;
  } catch (error) {
    console.error('操作失败:', error);
  }
};

const handleUpdateModalCancel = () => {
  updateModalVisible.value = false;
};

const handlecontrolPtz = async (param) => {
  try {
    param['deviceId'] = nowRecord.deviceId;
    param['channelId'] = nowRecord.channelId;
    const v = await controlPtzCmd(param);
  } catch (error) {
    console.log(error)
  } finally {
  }
}

const audioType = ref('');
let lastAudioCallId
const audioLoadingClass = ref('');
const handleAudio = async (type) => {
  try {
    if( audioType.value == type){
        audioType.value = ''
        stopRecording()
    }else {
      audioType.value = type
    }
    audioLoadingClass.value = 'audioLoading'
    // 先停止旧的语音或广播
    if(lastAudioCallId){
      await stopInvite({"callId":lastAudioCallId})
    }
    let httpWs
    if(audioType.value == 'talk'){
      // TODO 有些设备只有一路 要先关闭实时播放
      // await stopInvite({"callId": nowPlay.callId})
      const v = await talk({deviceId: nowRecord.deviceId, channelId: nowRecord.channelId});
      httpWs = v.data.httpWs
      lastAudioCallId = v.data.callId
      // 更新flv播放器地址为对讲的流
      // flvUrl.value = v.data.httpFlv;
      // modalVisible.value = false;
      // modalVisible.value = true;
    }
    if(audioType.value == 'broadcast'){
      const v = await broadcast({deviceId: nowRecord.deviceId, channelId: nowRecord.channelId});
      httpWs = v.data.httpWs
      lastAudioCallId = v.data.callId
    }
    if(!httpWs){
      audioType.value = ''
      audioLoadingClass.value = ''
      return
    }
    audioLoadingClass.value = 'audioLoadingOk'
    message.success(type + " " + httpWs)
    // TODO 连接ws 发送和播放音频
    await startRecording(httpWs);
  } catch (error) {
    audioType.value = ''
    audioLoadingClass.value = ''
    console.log(error)
  } finally {
  }
}

let mediaRecorder;
let ws;
let streamDevice;

// 开始录音
const startRecording = async (url) => {
  try {
    // 开启 WebSocket 连接
    ws = new WebSocket(url);

    ws.onopen = () => {
      console.log('WebSocket 连接已建立');
    };

    ws.onmessage = async (event) => {
      // 处理接收到的 PCM 数据并播放
      const pcmData = await getPCMDataFromMessage(event.data);
      playPCMData(pcmData);
    };

    ws.onerror = (error) => {
      console.error('WebSocket 发生错误:', error);
    };

    ws.onclose = () => {
      console.log('WebSocket 连接已关闭');
    };

    // 获取麦克风权限
    streamDevice = await navigator.mediaDevices.getUserMedia({ audio: true });

    // 创建 MediaRecorder 实例
    mediaRecorder = new MediaStreamRecorder(streamDevice);
    mediaRecorder.mimeType = 'audio/pcm'
    mediaRecorder.audioChannels = 1; // 单声道
    mediaRecorder.sampleRate = 8000;

    mediaRecorder.ondataavailable = async (data) => {
      //console.log('Received all audio data from start to stop:', data);
      if(ws && ws.readyState === WebSocket.OPEN){
        ws.send(await data.arrayBuffer());
      }
    };

    // 开始录音
    mediaRecorder.start(30);
  } catch (error) {
    console.error('获取麦克风权限失败:', error);
  }
};

// 停止录音
const stopRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop();
  }
  if(ws){
    ws.close();
  }
  if(streamDevice){
    streamDevice.getTracks().forEach((track) => track.stop());
  }
};

// 从消息中获取 PCM 数据
const getPCMDataFromMessage = async (data) => {
  const arrayBuffer = await data.arrayBuffer();
  return new Int16Array(arrayBuffer);
};

// 播放接收到的 PCM 数据
const playPCMData = (pcmData) => {
  const audioContext = new (window.AudioContext || window.webkitAudioContext)();
  const buffer = audioContext.createBuffer(1, pcmData.length, audioContext.sampleRate);
  const channelData = buffer.getChannelData(0);
  channelData.set(pcmData);

  const source = audioContext.createBufferSource();
  source.buffer = buffer;
  source.connect(audioContext.destination);
  source.start();
};

</script>

<style scoped>
/* 外层容器样式 */
.outer-container {
  width: 100%;
  height: 100px;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  padding-top: 15px;
}

/* 定义容器样式 */
.square-container {
  position: relative;
  width: 100px;
  height: 100px;
}

/* 定义三角形样式 */
.triangle {
  position: absolute;
  width: 0;
  height: 0;
  border-style: solid;
  cursor: pointer;
}

/* 上方三角形 */
.triangle-up {
  top: 4px;
  left: 38px;
  border-width: 0 12px 12px 12px;
  border-color: transparent transparent #08c transparent;
}

/* 下方三角形 */
.triangle-down {
  bottom: 4px;
  left: 38px;
  border-width: 12px 12px 0 12px;
  border-color: #08c transparent transparent transparent;
}

/* 左方三角形 */
.triangle-left {
  top: 38px;
  left: 4px;
  border-width: 12px 12px 12px 0;
  border-color: transparent #08c transparent transparent;
}

/* 右方三角形 */
.triangle-right {
  top: 38px;
  right: 4px;
  border-width: 12px 0 12px 12px;
  border-color: transparent transparent transparent #08c;
}

/* 圆形包裹的加减符号容器 */
.circle {
  position: absolute;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #08c;
  color: white;
  font-size: 20px;
  text-align: center;
  line-height: 24px;
  cursor: pointer;
}

.circle-stop {
  top: 38px;
  left: 38px;
  background-color: #FF000084;
}

.square-container-two {
  width: 50px;
}

/* 上方圆形加号 */
.circle-plus {
  top: 21px;
  left: 8px;
}

/* 下方圆形减号 */
.circle-minus {
  bottom: 21px;
  left: 8px;
}

/* 并排容器样式 */
.square-container,
.audio-container {
  margin: 0 10px;
}

.audio-container {
  display: flex;
  justify-content: space-around;
}

.audio-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 15px;
}

.audio-item span {
  margin-bottom: 8px;
}

.audio-icon {
  font-size: 30px;
  color: #08c;
  cursor: pointer;
}

.audioLoading {
  animation: colorGradient 0.7s linear infinite;
}

@keyframes colorGradient {
  0% {
    color: #ff0000;
  }
  50% {
    color: #FF0000AD;
  }
  100% {
    color: #FF000062;
  }
}

.audioLoadingOk {
  color: #00cc29;
}
</style>
