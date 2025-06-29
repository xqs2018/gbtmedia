<template>
  <div class="search-form" style="margin-bottom: 16px;">
    <a-form layout="inline">
      <a-form-item label="设备ID">
        <a-input v-model:value="searchForm.deviceId" placeholder="请输入设备ID" allowClear />
      </a-form-item>
      <a-form-item label="通道ID">
        <a-input v-model:value="searchForm.channelId" placeholder="请输入通道ID" allowClear />
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
      <a-button @click="handlePlay(record)" style="margin-right: 10px;" :loading="handlePlayLoading">Play</a-button>
      <a-button @click="handlePlatformInvite(record)" style="margin-right: 10px;" >级联推流[{{record.platformInvites.length}}]</a-button>
      <a-button danger @click="handleStopInvite(record)" style="margin-right: 10px;" :loading="record.loading">停止</a-button>
    </template>
    </template>
  </a-table>
  <FlvPlayerModal
      :visible="modalVisible"
      :flvUrl="flvUrl"
      @cancel="()=>modalVisible=false"
      @update:visible="modalVisible = $event"
  />
  <a-modal
      :visible="platformInviteVisible"
      title="级联推流"
      @cancel="handlePlatformInviteCancel"
      :footer="null"
      width="780px"
  >
    <a-table
        :columns="platformInviteColumns"
        :data-source="platformInviteTableData"
        :pagination="platformInvitePagination"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button danger @click="handleStopPlatformInvite(record)" style="margin-right: 12px;" :loading="record.loading">停止</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {listInvite, play, stopInvite,} from "@/api/gbt28181";
import { message } from 'ant-design-vue';
import moment from 'moment';
import FlvPlayerModal from "@/components/FlvPlayerModal.vue";

// 搜索表单
const searchForm = ref({
  deviceId: '',
  channelId: ''
});

// 处理搜索
const handleSearch = () => {
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 处理重置
const handleReset = () => {
  searchForm.value = {
    deviceId: '',
    channelId: ''
  };
  handleSearch();
};

// 表格列配置
const columns = [
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    customRender:(r)=>{
      return moment(r.value).format('YYYY-MM-DD HH:mm:ss');
    }
  },
  {
    title: '类型',
    dataIndex: 'inviteType',
    key: 'inviteType',
  },
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
    title: '播放地址',
    dataIndex: 'httpFlv',
    key: 'httpFlv',
  },
  {
    title: '接收速率',
    dataIndex: 'rxRate',
    key: 'rxRate',
  },
  {
    title: '扩展信息',
    dataIndex: 'extInfo',
    key: 'extInfo',
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
    const param = {
      pageNo: page - 1,
      pageSize: pageSize,
      deviceId: searchForm.value.deviceId,
      channelId: searchForm.value.channelId
    };
    const response = await listInvite(param);
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

const modalVisible = ref(false);
const flvUrl = ref('');
const handlePlayLoading = ref<boolean>(false);
const handlePlay = async (record) => {
  handlePlayLoading.value = true;
  try {
    flvUrl.value = record.httpFlv;
    modalVisible.value = true;
  } catch (error) {
    console.log(error)
  } finally {
    handlePlayLoading.value = false;
  }
};

const handleStopInvite = async (record) => {
  record.loading = true;
  try {
    const v = await stopInvite({callId:record.callId})
    message.success(v.message);
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
};

const platformInviteVisible = ref(false);

const platformInviteColumns = [
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    customRender:(r)=>{
      return moment(r.value).format('YYYY-MM-DD HH:mm:ss');
    }
  },
  {
    title: '上级平台ID',
    dataIndex: 'platformId',
    key: 'platformId',
  },
  {
    title: '发送速率',
    dataIndex: 'txRate',
    key: 'txRate',
  },
  {
    title: '操作',
    key: 'action',
  }
];

// 表格数据
const platformInviteTableData = ref<any[]>([]);

// 分页配置
const platformInvitePagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
});

const handlePlatformInvite = (record) => {
  platformInviteVisible.value = true;
  // 更新表格数据
  platformInviteTableData.value = record.platformInvites.map((item) => ({
    ...item,
    loading: false,
  }));
  platformInvitePagination.value = record.platformInvites.length;
}

const handleStopPlatformInvite = async (record) => {
  record.loading = true;
  try {
    const v = await stopInvite({callId: record.callId})
    message.success(v.message);
    platformInviteTableData.value = platformInviteTableData.value.filter(v=>v.callId!=record.callId);
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
}

const handlePlatformInviteCancel = () => {
  fetchData(pagination.value.current, pagination.value.pageSize);
  platformInviteVisible.value = false
}


</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
