<template>
  <div>
    <div class="search-container" style="margin-bottom: 16px;">
      <a-form layout="inline">
      <a-form-item label="设备ID">
        <a-input v-model:value="searchParams.deviceId" placeholder="请输入设备ID" allowClear />
      </a-form-item>
      <a-form-item label="通道ID">
        <a-input v-model:value="searchParams.channelId" placeholder="请输入通道ID" allowClear />
      </a-form-item>
      <a-form-item>
        <a-button @click="handleSearch">查询</a-button>
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
          <a :href="`${baseApiUrl}/backend/gbt28181/downloadRecordFile/${record.fileName}`" style="margin-right: 10px;" :download="record.fileName">下载</a>
          <a-button @click="handlePlay(record)" style="margin-right: 10px;">播放</a-button>
          <a-button danger @click="handleDelete(record)" :loading="record.loading">删除</a-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { deleteRecordFile, listRecordFile } from "@/api/gbt28181";
import { message } from 'ant-design-vue';

// 搜索参数
const searchParams = ref({
  deviceId: '',
  channelId: ''
});

// 表格列配置
const columns = [
  {
    title: '设备ID',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '通道ID',
    dataIndex: 'channelId',
    key: 'channelId',
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
    title: '录像类型',
    dataIndex: 'type',
    key: 'type',
    customRender: ({ text }: { text: number }) => {
      return text === 1 ? '设备' : text === 2 ? '云端' : text;
    }
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    key: 'fileSize',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 350
  }
];

// 表格数据
const tableData = ref<any[]>([]);
const isTableLoading = ref(false);

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
});

// API基础URL
const baseApiUrl = `${window.location.protocol}//${window.location.host}${import.meta.env.VITE_APP_BASE_API}`.replace(/\/$/, '');;

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  isTableLoading.value = true;
  try {
    const param = {
      pageNo: page - 1,
      pageSize: pageSize,
      deviceId: searchParams.value.deviceId,
      channelId: searchParams.value.channelId
    };
    const response = await listRecordFile(param);
    tableData.value = response.data.map((item: any) => ({
      ...item,
      loading: false,
    }));
    pagination.value.total = response.data.length;
    pagination.value.current = page;
  } catch (error) {
    console.error('获取数据失败:', error);
    message.error('获取数据失败');
  } finally {
    isTableLoading.value = false;
  }
};

// 表格分页变化时触发
const handleTableChange = (pagination: any) => {
  const { current, pageSize } = pagination;
  fetchData(current, pageSize);
};

// 删除记录
const handleDelete = async (record: any) => {
  record.loading = true;
  try {
    const response = await deleteRecordFile({ fileName: record.fileName });
    message.success('删除成功');
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  } finally {
    record.loading = false;
  }
};

// 视频播放相关状态
const isVideoModalVisible = ref(false);
const currentVideoUrl = ref('');
const currentFileName = ref('');

// 处理播放
const handlePlay = (record: any) => {
  currentVideoUrl.value = `${baseApiUrl}/backend/gbt28181/downloadRecordFile/${record.fileName}`;
  currentFileName.value = record.fileName;
  isVideoModalVisible.value = true;
};

// 处理视频模态框关闭
const handleVideoModalClose = () => {
  isVideoModalVisible.value = false;
  currentVideoUrl.value = '';
  currentFileName.value = '';
};

// 处理搜索
const handleSearch = () => {
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 处理重置
const handleReset = () => {
  searchParams.value = {
    deviceId: '',
    channelId: ''
  };
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 组件挂载时获取第一页数据
onMounted(() => {
  fetchData(pagination.value.current, pagination.value.pageSize);
});
</script>

<style scoped>
</style>
