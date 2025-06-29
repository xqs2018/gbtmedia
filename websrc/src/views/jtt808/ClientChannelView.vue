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
        <router-link :to="'/JTT808/ClientChannelRecordView?clientId='+clientId+'&channelNo='+record.channelNo" style="margin-right: 10px;">录像</router-link>
        <a-button @click="handlePlay(record)" style="margin-right: 12px;" :loading="record.loading">播放</a-button>
      </template>
    </template>
  </a-table>
  <FlvPlayerModal
    :visible="modalVisible"
    :hasAudio="true"
    :flvUrl="flvUrl"
    @cancel="flvPlayerModalCancel()"
    @update:visible="modalVisible = $event"
  >
  </FlvPlayerModal>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from "vue-router";
import {message} from "ant-design-vue";
import FlvPlayerModal from "@/components/FlvPlayerModal.vue";
import {pageClient, play} from "@/api/jtt808";


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
    };
    const response = await pageClient(param);
    let max = response.data.content[0].maxVideoChannels;
    let list = [];
    for(let i=1;i<max+1;i++){
      list.push({"channelNo":i})
    }
    // 更新表格数据
    tableData.value = list.map((item) => ({
      ...item,
      loading: false,
      loading1: false,
    }));
    pagination.value.total = list.length
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

const modalVisible = ref(false);
const flvUrl = ref('');

let nowRecord
let nowPlay
const handlePlay = async (record) => {
  nowRecord = record
  record.loading = true;
  try {
    const v = await play({clientId: clientId.value, channelNo: record.channelNo});
    nowPlay = v.data
    flvUrl.value = v.data.httpFlv;
    modalVisible.value = true;
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
};

const flvPlayerModalCancel = async () => {
  modalVisible.value = false
}


</script>

<style scoped>

</style>
