<template>
  <div>
    <a-form layout="inline" :model="searchForm" class="search-form" style="margin-bottom: 16px;">
      <a-form-item label="时间范围">
        <a-range-picker 
          v-model:value="searchForm.dateRange" 
          :placeholder="['开始时间', '结束时间']" 
          :locale="zhCN"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
        />
      </a-form-item>
      <a-form-item>
        <a-button @click="handleSearch">查询</a-button>
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
        <template v-if="column.key === 'action'">
            <a :href="`https://uri.amap.com/marker?position=${record.longitude/1000000},${record.latitude/1000000}`" target="_blank">定位</a>
            <a-button v-if="record.alarmName" @click="handleAlarmFile(record)" style="margin-left: 12px;" :loading="record.loading">报警附件</a-button>
        </template>
        <template v-else-if="column.key === 'statusBitName' || column.key === 'warnBitName'">
          <a-tooltip :title="record[column.key]">
            <span class="ellipsis-text">{{ record[column.key] }}</span>
          </a-tooltip>
        </template>
      </template>
    </a-table>
    <a-modal
          :visible="alarmFileVisible"
          title="报警附件"
          @cancel="()=>alarmFileVisible=false"
          :footer="null"
          width="780px"
    >
      <div class="media-container">
        <div v-for="item in alarmFileNames" :key="item" class="media-item">
          <div class="media-content">
            <img
                v-if="item.endsWith('jpg')"
                :src="`${VITE_APP_BASE_API}/backend/jtt808/downloadAlarmFile/${item}`"
                alt=""
            />
            <video
                v-if="item.endsWith('mp4')"
                :src="`${VITE_APP_BASE_API}/backend/jtt808/downloadAlarmFile/${item}`"
                alt=""
            ></video>
          </div>
          <div class="filename">
            <a :href="`${VITE_APP_BASE_API}/backend/jtt808/downloadAlarmFile/${item}`" 
               target="_blank" 
               style="color: black">{{ item }}</a>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue';
import { message } from 'ant-design-vue';
import {deleteClient, pageClientLocation, updateClient} from "@/api/jtt808";
import {useRoute} from "vue-router";
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import zhCN from 'ant-design-vue/es/date-picker/locale/zh_CN';

dayjs.locale('zh-cn');

// 表格列配置
const columns = [
  {
    title: '终端手机号',
    dataIndex: 'clientId',
    key: 'clientId',
  },
  {
      title: '纬度',
      dataIndex: 'latitude',
      key: 'latitude',
  },
  {
      title: '经度',
      dataIndex: 'longitude',
      key: 'longitude',
  },
  {
      title: '速度',
      dataIndex: 'speed',
      key: 'speed',
  },
  {
      title: '状态标志',
      dataIndex: 'statusBitName',
      key: 'statusBitName',
      width: 200
  },
  {
      title: '报警标志',
      dataIndex: 'warnBitName',
      key: 'warnBitName',
      width: 200
  },
  {
      title: '苏标报警',
      dataIndex: 'alarmName',
      key: 'alarmName',
  },
  {
      title: '设备时间',
      dataIndex: 'deviceTime',
      key: 'deviceTime',
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

// 搜索表单
const searchForm = ref({
  dateRange: [] as Dayjs[],
});

// 处理搜索
const handleSearch = () => {
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 处理重置
const handleReset = () => {
  searchForm.value = {
    dateRange: [],
  };
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  try {
    isTableLoading.value = true;
    const param = {
      pageNo: page - 1,
      pageSize: pageSize,
      clientId:clientId.value,
      startTime: searchForm.value.dateRange[0]?.format('YYYY-MM-DD HH:mm:ss'),
      endTime: searchForm.value.dateRange[1]?.format('YYYY-MM-DD HH:mm:ss'),
    };
    const response = await pageClientLocation(param);
    // 更新表格数据
    tableData.value = response.data.content;
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
// 定义 clientId 变量
const clientId = ref('');

// 组件挂载时获取第一页数据
onMounted(() => {
    // 从路由的 query 参数中获取 deviceId
    clientId.value = route.query.clientId as string;
  fetchData(pagination.value.current, pagination.value.pageSize);
});

const alarmFileVisible = ref(false);

const alarmFileNames = ref<string[]>([]);

interface LocationRecord {
  alarmFileName: string;
  [key: string]: any;
}

const handleAlarmFile = (record: LocationRecord) => {
    alarmFileNames.value = record.alarmFileName.split(",");
    alarmFileVisible.value = true;
};

// 拼接字符串
const combinedUrl = `${window.location.protocol}//${window.location.host}${import.meta.env.VITE_APP_BASE_API}`;
// 去掉末尾的斜杠
const trimmedUrl = combinedUrl.replace(/\/$/, '');

const VITE_APP_BASE_API = ref(trimmedUrl);

</script>
<style scoped>
/* 可根据需要添加样式 */
.media-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px; /* 元素之间的间距 */
}

.media-item {
  width: 200px; /* 每个媒体项的宽度 */
  text-align: center; /* 使文件名居中 */
}

.media-content {
  width: 100%;
  height: 150px; /* 媒体内容的高度 */
}

.media-content img,
.media-content video {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持比例并覆盖容器 */
}

.filename {
  margin-top: 5px; /* 文件名与媒体内容之间的间距 */
  text-align: center;
}

:deep(.ellipsis-text) {
  display: inline-block;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}
</style>
