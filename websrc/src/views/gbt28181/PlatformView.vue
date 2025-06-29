<template>
    <a-button class="editable-add-btn" style="margin-bottom: 16px" @click="handleAdd()">新增</a-button>
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
            <template v-if="column.key === 'status'">
              <span :style="{ color: record.online == '1'? 'green' :'red' }">
                {{ record.online == '1'? '在线' : '离线' }}
              </span>
            </template>
            <template v-if="column.key === 'used'">
              <span :style="{ color: record.enable == '1'? 'green' :'red' }">
                {{ record.enable == '1'? '启用' : '禁用' }}
              </span>
            </template>
            <template v-if="column.key === 'action'">
                <a-button @click="handleUpdate(record)" style="margin-right: 10px;">更新</a-button>
                <a-button @click="handleChannel(record)" style="margin-right: 10px;">共享通道</a-button>
                <a-button danger @click="handleDelete(record)" style="margin-right: 10px;">删除</a-button>
                <a-button v-if="record.enable == '0'" @click="handleEnable(record, '1')" style="margin-right: 12px;" :loading="record.loading">
                    启用
                </a-button>
                <a-button danger v-if="record.enable == '1'" @click="handleEnable(record, '0')" style="margin-right: 10px;" :loading="record.loading">
                    禁用
                </a-button>
            </template>
        </template>
    </a-table>
    <a-modal
            :visible="modalVisible"
            title="上级平台"
            @ok="handleModalOk"
            @cancel="handleModalCancel"
    >
          <a-form :model="formData" ref="formRef">
              <a-form-item label="名称" name="name" :rules="[{ required: true, message: '请输入名称!' }]">
                  <a-input v-model:value="formData.name" />
              </a-form-item>
              <a-form-item label="上级平台ID" name="platformId" :rules="[{ required: true, message: '请输入上级平台ID!' }]">
                  <a-input v-model:value="formData.platformId" />
              </a-form-item>
              <a-form-item label="上级平台域" name="sipDomain" :rules="[{ required: true, message: '请输入上级平台域!' }]">
                  <a-input v-model:value="formData.sipDomain" />
              </a-form-item>
              <a-form-item label="信令IP" name="sipIp" :rules="[{ required: true, message: '请输入信令IP!' }]">
                  <a-input v-model:value="formData.sipIp" />
              </a-form-item>
              <a-form-item label="信令端口" name="sipPort" :rules="[{ required: true, message: '请输入信令端口!' }]">
                  <a-input v-model:value="formData.sipPort" />
              </a-form-item>
              <a-form-item label="密码" name="password">
                  <a-input v-model:value="formData.password" />
              </a-form-item>
          </a-form>
    </a-modal>
  <!-- 原有代码保持不变 -->
    <a-modal
            v-if="channelModalVisible"
            :visible="channelModalVisible"
            title="设备通道树"
            @ok="handleChannelModalOk"
            @cancel="handleChannelModalCancel"
            :width="600"
            :bodyStyle="{ height: '500px', overflow: 'auto' }"
    >
        <a-tree
                :tree-data="channelTreeData"
                @check="onTreeCheck"
                :checkable="true"
                v-model:expandedKeys="expandedKeys"
                v-model:selectedKeys="selectedKeys"
                v-model:checkedKeys="checkedKeys"
        />
    </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import {
  enablePlatform,
  pagePlatform,
  play,
  savePlatform,
  deletePlatform, pageDevice, pageDeviceChannel, savePlatformChannelList, listPlatformChannel, channelTree
} from '@/api/gbt28181';
import { message } from 'ant-design-vue';
import { FormInstance } from 'ant-design-vue/es/form';

// 表格列配置
const columns = [
    {
        title: '名称',
        dataIndex: 'name',
        key: 'name',
    },
    {
        title: '上级平台ID',
        dataIndex: 'platformId',
        key: 'platformId',
    },
  {
    title: '上级平台域',
    dataIndex: 'sipDomain',
    key: 'sipDomain',
  },
    {
        title: '信令地址',
        dataIndex: 'sipAddress',
        key: 'sipAddress',
    },
    {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
    },
    {
        title: '使用',
        dataIndex: 'used',
        key: 'used',
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
      width: 400       // 设置固定列宽度
    },
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
            pageNo: page - 1, // 假设后端页码从 0 开始
            pageSize: pageSize,
        };
        const response = await pagePlatform(param);
        // 更新表格数据
        tableData.value = response.data.content.map((item) => ({
            ...item,
            loading: false,
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

const modalVisible = ref(false);
const formRef = ref<FormInstance>();
const formData = ref({});

const handleAdd = () => {
    formData.value = {}
    modalVisible.value = true;
};

const handleUpdate = (record) => {
    formData.value = JSON.parse(JSON.stringify(record))
    modalVisible.value = true;
};

const handleModalOk = async () => {
    await formRef.value?.validate();
    try {
        // 提取纯数据
        const pureFormData = JSON.parse(JSON.stringify(formData.value));
        const v = await savePlatform(pureFormData);
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

const handleDelete = async (record) => {
    record.loading = true;
    try {
        // 提取纯数据
        const pureFormData = JSON.parse(JSON.stringify(record));
        const v = await deletePlatform(pureFormData);
        message.success(v.message);
        await fetchData(pagination.value.current, pagination.value.pageSize);
    } catch (error) {
        console.log(error);
    } finally {
        record.loading = false;
    }
}
const handleEnable = async (record, type) => {
    record.loading = true;
    try {
        // 提取纯数据
        const pureFormData = JSON.parse(JSON.stringify(record));
        pureFormData.enable = type;
        const v = await enablePlatform(pureFormData);
        message.success(v.message);
        await fetchData(pagination.value.current, pagination.value.pageSize);
    } catch (error) {
        console.log(error);
    } finally {
        record.loading = false;
    }
};
// 新增响应式变量
const channelModalVisible = ref(false);
const channelTreeData = ref<any[]>([]);
const checkedKeys = ref<any[]>([]);
const defaultCheckedKeys = ref<any[]>([]);
const expandedKeys = ref<string[]>([]);
const selectedKeys = ref<string[]>([]);
let recordNow
const handleChannel = async (record) => {
    recordNow = record;
    record.loading = true;
    try {
        const param = {};
        const r1 = await channelTree(param);
        let tree = r1.data
         tree = tree.map(v=>{
            let children =v.children.map(v2=>{
                return {"title":v2.gbId + "("+ v2.customName+")", "key":v.gbId + "_" + v2.gbId}}
            )
            return  {
                "title":v.gbId + "("+ v.customName+")",
                "key":v.gbId,
                "children":children
            }
        })
        channelTreeData.value = tree;

        checkedKeys.value = []
        expandedKeys.value = []
        expandedKeys.value = []
        const r3 = await listPlatformChannel({"platformId": recordNow.platformId})
        r3.data.forEach(v=>{
            checkedKeys.value.push(v.deviceId + "_" + v.channelId);
            expandedKeys.value.push(v.deviceId + "_" + v.channelId);
            selectedKeys.value.push(v.deviceId + "_" + v.channelId);
        })
        channelModalVisible.value = true; // 打开弹窗
    } catch (error) {
        console.error('获取数据失败:', error);
    } finally {
        record.loading = false;
    }
}

const handleChannelModalOk = async () => {
    // 处理确认操作，这里可以使用 checkedKeys.value
    try {
        let params = checkedKeys.value.filter(v => v.indexOf("_") > 0).map(v => {
            return {"deviceId": v.split("_")[0], "channelId": v.split("_")[1]}
        });
        const r1 = await savePlatformChannelList({"platformId": recordNow.platformId, platformChannelList:params})
        message.success(r1.message)
    } catch (error) {
        console.error('获取数据失败:', error);
    }
    channelModalVisible.value = false;
};
const handleChannelModalCancel = () => {
    channelModalVisible.value = false;
};
const onTreeCheck = (checkedKeysValue: any[]) => {
    checkedKeys.value = checkedKeysValue;
    //console.log('Checked keys changed:', checkedKeysValue);
};

</script>

<style scoped>
/* 可根据需要添加样式 */
</style>
