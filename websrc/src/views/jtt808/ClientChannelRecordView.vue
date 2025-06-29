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
      <template v-if="column.key === 'action'">
        <a-button @click="handleOpen(record,'PlayBack')" style="margin-right: 8px;">回放</a-button>
        <a-button v-if="record.progress == 0" @click="handleOpen(record,'Download')" style="margin-right: 8px;">下载</a-button>

        <a v-if="record.fileName" :href="`${VITE_APP_BASE_API}/backend/jtt808/downloadRecordFile/${record.fileName}`" style="margin-right: 10px;">下载文件</a>
        <a-button v-if="record.fileName" danger @click="handleDelete(record)" style="margin-right: 10px;" :loading="record.loading">删除</a-button>
      </template>
    </template>
  </a-table>
  <a-modal
      :visible="isModalVisible"
      :title="handleType"
      @ok="handleRecord"
      @cancel="() => { isModalVisible = false }"
      :confirmLoading="confirmLoading"
      :width="400"
  >
  <div style="padding: 20px;">
    <!-- 使用Flex布局来排列元素 -->
    <div style="display: flex; flex-direction: column; gap: 15px;">
      <a-range-picker
          v-model:value="recordTime"
          format="YYYY-MM-DD HH:mm:ss"
          show-time
          :allowClear="false"
      />
    </div>
  </div>
  </a-modal>
  <FlvPlayerModal
      :visible="flvModalVisible"
      :flvUrl="flvUrl"
      @cancel="flvPlayerCancel"
      @update:visible="flvModalVisible = $event"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  deleteRecordFile,
  downloadRecord,
  playback,
  queryRecordInfo,
  stopClientMedia
} from "@/api/jtt808";
import { message } from 'ant-design-vue';
import {useRoute} from "vue-router";
import moment from 'moment';
import FlvPlayerModal from "@/components/FlvPlayerModal.vue";
import dayjs, { Dayjs } from 'dayjs';
import {stopInvite} from "@/api/gbt28181";

// 表格列配置
const columns = [
  {
    title: '终端手机号',
    dataIndex: 'clientId',
    key: 'clientId',
    customRender:()=>{
      return clientId.value
    }
  },
  {
    title: '通道序号',
    dataIndex: 'channelNo',
    key: 'channelNo',
  },
  {
    title: '开始时间',
    dataIndex: 'startTime',
    key: 'startTime',
  },
  {
    title: '结束时间',
    dataIndex: 'endTime',
    key: 'endTime',
  },
  {
    title: '大小',
    dataIndex: 'fileSize',
    key: 'fileSize',
  },
  {
    title: '下载进度',
    dataIndex: 'progress',
    key: 'progress',
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',  // 添加固定右侧配置
    width: 350       // 设置固定列宽度
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
      clientId:clientId.value,
      channelNo:channelNo.value,
    };
    const response = await queryRecordInfo(param);
    tableData.value = response.data.recordList;
    pagination.value.total = response.data.recordList.length;
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
const clientId = ref('');
const channelNo = ref('');

// 组件挂载时获取第一页数据
onMounted(() => {
  clientId.value = route.query.clientId as string;
  channelNo.value = route.query.channelNo as string;
  fetchData(pagination.value.current, pagination.value.pageSize);
});


// 模态框显示状态
const isModalVisible = ref(false);
// 定义确认按钮加载状态
const confirmLoading = ref(false);
// moment().subtract(1, 'hours').startOf('hour'), moment()
//const recordTime = ref( []);
const recordSpeed = ref(4);
const handleType = ref('');
const flvModalVisible = ref(false);
const flvUrl = ref('');

// 定义日期格式
const dateFormat = 'YYYY-MM-DD HH:00:00';
// 使用 moment 创建日期对象并设置默认值
const recordTime = ref<[Dayjs, Dayjs]>([
  dayjs().subtract(1, 'hour').minute(0).second(0),
  dayjs().subtract(0, 'hour').minute(0).second(0)
])

const handleOpen = (record,type) => {
  isModalVisible.value=true
  handleType.value = type
  recordTime.value[0] = dayjs(record.startTime)
  recordTime.value[1] = dayjs(record.endTime)
  nowRecord = record
}

let nowRecord

// 点击确定按钮的处理函数
const handleRecord = async () => {
  // 设置确认按钮为加载状态
  confirmLoading.value = true;
  try {
    let startTime = moment(recordTime.value[0].valueOf()).format('YYYY-MM-DD HH:mm:ss');
    let endTime = moment(recordTime.value[1].valueOf()).format('YYYY-MM-DD HH:mm:ss');
    if(handleType.value === 'Download'){
      const v = await downloadRecord({
        clientId: clientId.value, channelNo: channelNo.value,
        startTime: startTime, endTime: endTime,
        mediaType: nowRecord.mediaType, streamType: nowRecord.streamType,storageType:nowRecord.storageType,
        size:nowRecord.size,
      })
      message.success(v.message);
      await fetchData(pagination.value.current, pagination.value.pageSize);
    }
    if(handleType.value === 'PlayBack'){
      const v = await playback({
        clientId: clientId.value, channelNo: channelNo.value,
        startTime: startTime, endTime: endTime,
        mediaType: nowRecord.mediaType, streamType: nowRecord.streamType,storageType:nowRecord.storageType,
        size:nowRecord.size,
      })
      isModalVisible.value = false;
      flvUrl.value = v.data.httpFlv;
      nowPlayMediaKey = v.data.mediaKey
      flvModalVisible.value = true;
    }
  } catch (error) {
    console.log(error)
  } finally {
    confirmLoading.value = false;
    isModalVisible.value = false;
  }
};

let nowPlayMediaKey;

const  flvPlayerCancel = async () => {
  try {
    const v = await stopClientMedia({mediaKey: nowPlayMediaKey})
    message.success(v.message);
  } catch (error) {
    console.log(error)
  } finally {
    flvModalVisible.value = false
  }
}


// 拼接字符串
const combinedUrl = `${window.location.protocol}//${window.location.host}${import.meta.env.VITE_APP_BASE_API}`;
// 去掉末尾的斜杠
const trimmedUrl = combinedUrl.replace(/\/$/, '');

const VITE_APP_BASE_API = ref(trimmedUrl);

const handleDelete = async (record) => {
    record.loading = true;
    try {
        const v = await deleteRecordFile({fileName: record.fileName})
        message.success(v.message);
        await fetchData(pagination.value.current, pagination.value.pageSize);
    } catch (error) {
        console.log(error)
    } finally {
        record.loading = false;
    }
}
</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
