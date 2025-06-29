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
        <a-button @click="handlePlay(record)" style="margin-right: 10px;" :loading="handlePlayLoading">Play</a-button>
        <a-button danger @click="handleStop(record)" style="margin-right: 10px;" :loading="record.loading">停止</a-button>
      </template>
    </template>
  </a-table>
  <FlvPlayerModal
      :visible="modalVisible"
      :flvUrl="flvUrl"
      @cancel="()=>modalVisible=false"
      @update:visible="modalVisible = $event"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import moment from 'moment';
import FlvPlayerModal from "@/components/FlvPlayerModal.vue";
import {listClientMedia, stopClientMedia} from "@/api/jtt808";

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
    dataIndex: 'type',
    key: 'type',
  },
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

// 搜索参数
const searchParams = ref({
  clientId: '',
  channelNo: ''
});

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  isTableLoading.value = true;
  try {
    const param = {
      pageNo: page - 1,
      pageSize: pageSize,
      clientId: searchParams.value.clientId,
      channelNo: searchParams.value.channelNo
    };
    const response = await listClientMedia(param);
    // 更新表格数据
    tableData.value = response.data.map((item) => ({
      ...item,
      loading: false,
    }));
    pagination.value.total = response.data.length;
    pagination.value.current = page;
  } catch (error) {
    console.error('获取数据失败:', error);
  } finally {
    isTableLoading.value = false;
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

const handleStop = async (record) => {
  record.loading = true;
  try {
    const v = await stopClientMedia({mediaKey:record.mediaKey})
    message.success(v.message);
    await fetchData(pagination.value.current, pagination.value.pageSize);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
};

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

</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
