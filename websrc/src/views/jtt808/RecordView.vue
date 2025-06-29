<template>
  <div class="search-form" style="margin-bottom: 16px;">
    <a-form layout="inline">
      <a-form-item label="终端手机号">
        <a-input v-model:value="searchParams.clientId" placeholder="请输入终端手机号" />
      </a-form-item>
      <a-form-item label="通道号">
        <a-input v-model:value="searchParams.channelNo" placeholder="请输入通道号" />
      </a-form-item>
      <a-form-item>
        <a-button @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>
  </div>
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
        <a :href="`${VITE_APP_BASE_API}/backend/jtt808/downloadRecordFile/${record.fileName}`" style="margin-right: 10px;" :download="record.fileName" >下载</a>
        <a-button @click="handlePlay(record)" style="margin-right: 10px;">播放</a-button>
        <a-button danger @click="handleDelete(record)" style="margin-right: 10px;" :loading="record.loading">删除</a-button>
      </template>
    </template>
  </a-table>

  <a-modal
    v-model:visible="isVideoModalVisible"
    :title="`在线播放 - ${currentFileName}`"
    :footer="null"
    width="800px"
    @cancel="handleVideoModalClose"
  >
    <video
      v-if="currentVideoUrl"
      :src="currentVideoUrl"
      controls
      muted
      style="width: 100%"
    ></video>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  deleteRecordFile,
  listRecordFile,
} from "@/api/jtt808";
import { message } from 'ant-design-vue';

// 表格列配置
const columns = [
  {
    title: '终端手机号',
    dataIndex: 'clientId',
    key: 'clientId',
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
    title: '文件大小',
    dataIndex: 'fileSize',
    key: 'fileSize',
  },
  {
    title: '文件名称',
    dataIndex: 'fileName',
    key: 'fileName',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
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

// 搜索参数
const searchParams = ref({
  clientId: '',
  channelNo: ''
});

// 处理搜索
const handleSearch = () => {
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 处理重置
const handleReset = () => {
  searchParams.value = {
    clientId: '',
    channelNo: ''
  };
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  try {
    isTableLoading.value = true;
    const param = {
      pageNo: page - 1, // 假设后端页码从 0 开始
      pageSize: pageSize,
      clientId: searchParams.value.clientId,
      channelNo: searchParams.value.channelNo
    };
    const response = await listRecordFile(param);
    // 更新表格数据
    tableData.value = response.data.map((item) => ({
      ...item,
      loading: false,
    }));
    pagination.value.total = response.data.length;
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

// 组件挂载时获取第一页数据
onMounted(() => {
  fetchData(pagination.value.current, pagination.value.pageSize);
});

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

// 视频播放相关状态
const isVideoModalVisible = ref(false);
const currentVideoUrl = ref('');
const currentFileName = ref('');

// 处理播放
const handlePlay = (record: any) => {
  currentVideoUrl.value = `${VITE_APP_BASE_API.value}/backend/jtt808/downloadRecordFile/${record.fileName}`;
  currentFileName.value = record.fileName;
  isVideoModalVisible.value = true;
};

// 处理视频模态框关闭
const handleVideoModalClose = () => {
  isVideoModalVisible.value = false;
  currentVideoUrl.value = '';
  currentFileName.value = '';
};
</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
