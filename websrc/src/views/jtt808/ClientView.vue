<template>
  <div>
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item label="终端手机号">
        <a-input v-model:value="searchForm.clientId" placeholder="请输入终端手机号" allowClear />
      </a-form-item>
      <a-form-item label="终端IP">
        <a-input v-model:value="searchForm.clientIp" placeholder="请输入终端IP" allowClear />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="searchForm.online" placeholder="请选择状态" allowClear style="width: 120px" :default-value="undefined" >
          <a-select-option value="1">在线</a-select-option>
          <a-select-option value="0">离线</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button @click="handleSearch" :loading="isTableLoading">查询</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>
    <a-table
      :columns="columns"
      :data-source="tableData"
      :pagination="pagination"
      @change="handleTableChange"
      :loading="isTableLoading"
      :scroll="{ x: 'max-content' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'clientAddress'">
          {{ record.clientIp }}:{{ record.clientPort }}
        </template>
        <template v-if="column.key === 'maxVideoChannels'">
          <a-select
            ref="select"
            v-model:value="record.maxVideoChannels"
            style="width: 120px"
            @change="(value) => clientChange(record, value)">
            <a-select-option value="1">1</a-select-option>
            <a-select-option value="2">2</a-select-option>
            <a-select-option value="3">3</a-select-option>
            <a-select-option value="4">4</a-select-option>
          </a-select>
        </template>
        <template v-if="column.key === 'status'">
        <span :style="{ color: record.online == '1'? 'green' :'red' }">
          {{ record.online == '1'? '在线' : '离线' }}
        </span>
        </template>
        <template v-if="column.key === 'action'">
          <router-link :to="'/JTT808/ClientChannelView?clientId='+record.clientId" style="margin-right: 10px;">通道</router-link>
          <router-link :to="'/JTT808/ClientLocationView?clientId='+record.clientId" style="margin-right: 10px;">位置</router-link>
          <a-button danger @click="handleDelete(record)" style="margin-right: 10px;" :loading="record.loading1">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import {deleteClient, pageClient, updateClient} from "@/api/jtt808";

// 表格列配置
const columns = [
  {
    title: '终端手机号',
    dataIndex: 'clientId',
    key: 'clientId',
  },
  {
    title: '车辆标识',
    dataIndex: 'plateNo',
    key: 'plateNo',
  },
  {
    title: '版本',
    dataIndex: 'version',
    key: 'version',
  },
  {
    title: '终端型号',
    dataIndex: 'deviceModel',
    key: 'deviceModel',
  },
  {
    title: '终端ID',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '终端地址',
    key: 'clientAddress',
  },
  {
    title: '通道数量',
    dataIndex: 'maxVideoChannels',
    key: 'maxVideoChannels',
  },
  {
    title: '状态',
    key: 'status',
  },
  {
    title: '心跳时间',
    dataIndex: 'keepaliveTime',
    key: 'keepaliveTime',
  },
  {
    title: '注册时间',
    dataIndex: 'registTime',
    key: 'registTime',
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

// 搜索表单数据
const searchForm = ref({
  clientId: '',
  clientIp: '',
  online: undefined
});

const isTableLoading = ref(false);

// 重置搜索
const handleReset = () => {
  searchForm.value = {
    clientId: '',
    clientIp: '',
    online: undefined
  };
  fetchData(1, pagination.value.pageSize);
};

// 搜索
const handleSearch = () => {
  pagination.value.current = 1;
  fetchData(1, pagination.value.pageSize);
};

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  try {
    isTableLoading.value = true;
    const param = {
      pageNo: page - 1,
      pageSize: pageSize,
      clientId: searchForm.value.clientId,
      clientIp: searchForm.value.clientIp,
      online: searchForm.value.online
    };
    const response = await pageClient(param);
    // 更新表格数据
    tableData.value = response.data.content.map((item: any) => ({
      ...item,
      loading: false,
      loading1: false,
    }));
    pagination.value.total = response.data.page.totalElements;
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

const clientChange = (record, value)=> {
  record['maxVideoChannels'] = value;
  updateClient(record).then(v=>message.success(v.message))
}

const handleDelete = async (record) => {
  record.loading1 = true;
  try {
    const v = await deleteClient({clientId: record.clientId});
    message.success(v.message);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading1 = false;
  }
  await fetchData(pagination.value.current, pagination.value.pageSize);
}

</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
