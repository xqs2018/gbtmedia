<template>
  <div>
    <a-form layout="inline" :model="searchForm" class="search-form" style="margin-bottom: 16px;">
      <a-form-item label="时间范围">
        <a-range-picker v-model:value="searchForm.dateRange" :placeholder="['开始时间', '结束时间']" :locale="zhCN" />
      </a-form-item>
      <a-form-item>
        <a-button  @click="handleSearch" :loading="loading">查询</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="getTypeColor(record.type)">
            {{ getTypeText(record.type) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import zhCN from 'ant-design-vue/es/date-picker/locale/zh_CN';
import { logPage } from '@/api/system';

dayjs.locale('zh-cn');

interface LogRecord {
  key: string;
  type: string;
  operator: string;
  ip: string;
  content: string;
  createTime: string;
}

interface SearchParams {
  type: string;
  dateRange: Dayjs[];
  pageNum: number;
  pageSize: number;
}

const loading = ref(false);
const searchForm = ref({
  type: '',
  dateRange: [] as Dayjs[],
});

const columns = [
  {
    title: '请求路径',
    dataIndex: 'uri',
    key: 'uri',
  },
  {
    title: '请求参数',
    dataIndex: 'params',
    key: 'params',
    ellipsis: true,
  },
  {
    title: '错误信息',
    dataIndex: 'errorMessage',
    key: 'errorMessage',
    ellipsis: true,
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    key: 'ip',
  },
  {
    title: '耗时',
    dataIndex: 'costTime',
    key: 'costTime',
  },
  {
    title: '操作时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
];

const dataSource = ref<LogRecord[]>([]);

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const fetchLogs = async (params: SearchParams) => {
  loading.value = true;
  try {
    const requestParams = {
      type: params.type,
      startTime: params.dateRange[0]?.format('YYYY-MM-DD HH:mm:ss'),
      endTime: params.dateRange[1]?.format('YYYY-MM-DD HH:mm:ss'),
      pageNo: params.pageNum,
      pageSize: params.pageSize,
    };
    
    const response = await logPage(requestParams);
    if (response.code === 200) {
      dataSource.value = response.data.content
      pagination.value.total = response.data.page.totalElements;
    }
  } catch (error) {
    console.error('Failed to fetch logs:', error);
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag: any) => {
  pagination.value = pag;
  fetchLogs({
    ...searchForm.value,
    pageNum: pag.current,
    pageSize: pag.pageSize,
  });
};

const handleSearch = () => {
  pagination.value.current = 1;
  fetchLogs({
    ...searchForm.value,
    pageNum: 1,
    pageSize: pagination.value.pageSize,
  });
};

const handleReset = () => {
  searchForm.value = {
    type: '',
    dateRange: [],
  };
  pagination.value.current = 1;
  fetchLogs({
    type: '',
    dateRange: [],
    pageNum: 1,
    pageSize: pagination.value.pageSize,
  });
};

const getTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    login: 'green',
    operation: 'blue',
    error: 'red',
  };
  return colorMap[type] || 'default';
};

const getTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    login: '登录',
    operation: '操作',
    error: '错误',
  };
  return textMap[type] || type;
};

onMounted(() => {
  fetchLogs({
    type: '',
    dateRange: [],
    pageNum: 1,
    pageSize: pagination.value.pageSize,
  });
});
</script>

<style scoped>
.log-management {
  padding: 24px;
}

.header {
  margin-bottom: 8px;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.search-form {
  margin-bottom: 8px;
}
</style> 