<template>
  <div class="search-form" style="margin-bottom: 16px;">
    <a-form layout="inline">
      <a-form-item label="设备ID">
        <a-input v-model:value="searchParams.deviceId" placeholder="请输入设备ID" />
      </a-form-item>
      <a-form-item label="设备IP">
        <a-input v-model:value="searchParams.sipIp" placeholder="请输入设备IP" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select 
          v-model:value="searchParams.online"  placeholder="请选择状态" allowClear  style="width: 120px" :default-value="undefined" >
          <a-select-option value="1">在线</a-select-option>
          <a-select-option value="0">离线</a-select-option>
        </a-select>
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
    <template v-if="column.key === 'sipAddress'">
        {{ record.sipIp }}:{{ record.sipPort }}
    </template>
    <template v-if="column.key === 'mediaTransport'">
      <a-select
          ref="select"
          v-model:value="record.mediaTransport"
          style="width: 120px"
          @change="(value) => deviceChange(record, value)">
        <a-select-option value="tcpPassive">tcpPassive</a-select-option>
        <a-select-option value="tcpActive">tcpActive</a-select-option>
        <a-select-option value="udp">udp</a-select-option>
      </a-select>
    </template>
    <template v-if="column.key === 'online'">
      <span :style="{ color: record.online == '1'? 'green' :'red' }">
        {{ record.online == '1'? '在线' : '离线' }}
      </span>
    </template>
    <template v-if="column.key === 'action'">
      <router-link :to="'/GBT28181/DeviceChannelView?deviceId='+record.deviceId" style="margin-right: 10px;">通道</router-link>
      <a-button @click="handleSync(record)" style="margin-right: 10px;" :loading="record.loading">同步</a-button>
      <a-button @click="handleUpdate(record)" style="margin-right: 10px;">更新</a-button>
      <a-button danger @click="handleDelete(record)" style="margin-right: 10px;" :loading="record.loading1">删除</a-button>
    </template>
  </template>
  </a-table>

  <a-modal
      :visible="modalVisible"
      title="设备信息"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
  >
    <a-form 
      ref="formRef"
      :model="formData"
      :rules="rules"
    >
      <a-form-item label="自定义名称">
        <a-input v-model:value="formData.customName" />
      </a-form-item>
      <a-form-item label="最大播放路数" name="maxPlayStream">
        <a-input-number v-model:value="formData.maxPlayStream" :min="1" style="width: 100%" />
      </a-form-item>
      <a-form-item label="最大回放路数" name="maxPlaybackStream">
        <a-input-number v-model:value="formData.maxPlaybackStream" :min="1" style="width: 100%" />
      </a-form-item>
      <a-form-item label="最大下载路数" name="maxDownloadStream">
        <a-input-number v-model:value="formData.maxDownloadStream" :min="1" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>

</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  deleteDevice,
  pageDevice,
  queryCatalog,
  queryDeviceInfo, savePlatform,
  updateDevice
} from "@/api/gbt28181";
import { message } from 'ant-design-vue';
import {FormInstance} from "ant-design-vue/es/form";

// 表格列配置
const columns = [
  {
    title: '设备ID',
    dataIndex: 'deviceId',
    key: 'deviceId',
  },
  {
    title: '名称',
    dataIndex: 'name',
    key: 'name',
    customRender: ({ record }) => record.customName || record.name
  },
  {
    title: '制造厂商',
    dataIndex: 'manufacturer',
    key: 'manufacturer',
  },
  {
    title: '信令地址',
    key: 'sipAddress',
  },
  {
    title: '流媒体传输模式',
    dataIndex: 'mediaTransport',
    key: 'mediaTransport',
  },
  {
    title: '状态',
    key: 'online',
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


const isTableLoading = ref(false);

// 搜索参数
const searchParams = ref({
  deviceId: '',
  name: '',
  sipIp: '',
  online: undefined
});

// 搜索方法
const handleSearch = () => {
  pagination.value.current = 1;
  fetchData(pagination.value.current, pagination.value.pageSize);
};

// 重置搜索
const handleReset = () => {
  searchParams.value = {
    deviceId: '',
    name: '',
    sipIp: '',
    online: undefined
  };
  console.log('Reset searchParams:', searchParams.value);
  handleSearch();
};

// 获取表格数据
const fetchData = async (page: number, pageSize: number) => {
  try {
    const param = {
      pageNo: page - 1, // 假设后端页码从 0 开始
      pageSize: pageSize,
      ...searchParams.value
    };
    const response = await pageDevice(param);
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

const deviceChange = (record, value)=> {
  record['mediaTransport'] = value;
  updateDevice(record).then(v=>message.success(v.message))
}

const handleSync = async (record) => {
  record.loading = true;
  try {
    const v = await queryDeviceInfo({deviceId: record.deviceId});
    message.success("queryDeviceInfo");
    const v1 = await queryCatalog({deviceId: record.deviceId});
    // sumNum  nowNum
    message.success("queryCatalog sumNum: "+v1.data.sumNum + " nowNum：" + v1.data.nowNum);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading = false;
  }
}

const handleDelete = async (record) => {
  record.loading1 = true;
  try {
    const v = await deleteDevice({deviceId: record.deviceId});
    message.success(v.message);
  } catch (error) {
    console.log(error)
  } finally {
    record.loading1 = false;
  }
   await fetchData(pagination.value.current, pagination.value.pageSize);
}

const modalVisible = ref(false);
const formRef = ref<FormInstance>();
const formData = ref({
  customName: '',
  maxPlayStream: undefined,
  maxPlaybackStream: undefined,
  maxDownloadStream: undefined
});

const rules = {
  maxPlayStream: [{ required: true, message: '请输入最大播放路数' }],
  maxPlaybackStream: [{ required: true, message: '请输入最大回放路数' }],
  maxDownloadStream: [{ required: true, message: '请输入最大下载路数' }]
};

const handleUpdate = (record) => {
  formData.value = JSON.parse(JSON.stringify(record))
  modalVisible.value = true;
};

const handleModalOk = async () => {
  try {
    await formRef.value?.validate();
    // 提取纯数据
    const pureFormData = JSON.parse(JSON.stringify(formData.value));
    const v = await updateDevice(pureFormData);
    message.success(v.message);
    await fetchData(pagination.value.current, pagination.value.pageSize);
    modalVisible.value = false;
  } catch (error) {
    console.error('操作失败:', error);
  }
};

const handleModalCancel = () => {
  modalVisible.value = false;
};

</script>
<style scoped>
/* 可根据需要添加样式 */
</style>
