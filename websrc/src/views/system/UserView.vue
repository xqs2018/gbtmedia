<template>
  <div>
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" @click="handleChangePassword(record)">修改密码</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:visible="passwordModalVisible"
      title="修改密码"
      @ok="handlePasswordSubmit"
      @cancel="handlePasswordCancel"
    >
      <a-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        layout="vertical"
      >
        <a-form-item label="新密码" name="password">
          <a-input-password v-model:value="passwordForm.password" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import type { FormInstance } from 'ant-design-vue';
import { userPage, updatePassword } from '@/api/system';
import type { AxiosResponse } from 'axios';
import { message } from 'ant-design-vue';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  code: number;
  message: string;
  traceId: string | null;
  timestamp: number;
}

interface PageData<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

interface UserData {
  id: number;
  username: string;
  password: string;
  createTime: string;
  updateTime: string;
}

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    key: 'updateTime',
  },
  {
    title: '操作',
    key: 'action',
  },
];

const dataSource = ref<UserData[]>([]);

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const loading = ref(false);

const passwordModalVisible = ref(false);
const passwordFormRef = ref<FormInstance>();
const currentUser = ref<any>(null);

const passwordForm = ref({
  password: '',
  confirmPassword: '',
});

const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string) => {
        if (value !== passwordForm.value.password) {
          throw new Error('两次输入的密码不一致');
        }
      },
      trigger: 'blur',
    },
  ],
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.value.current - 1, // Convert to 0-based index
      pageSize: pagination.value.pageSize,
    };
    const res = (await userPage(params)) as unknown as ApiResponse<PageData<UserData>>;
    if (res.code === 200 && res.success) {
      dataSource.value = res.data.content.map((item) => ({
        ...item,
        key: item.id,
      }));
      pagination.value.total = res.data.page.totalElements;
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag: any) => {
  pagination.value = pag;
  fetchData();
};

onMounted(() => {
  fetchData();
});

const handleAdd = () => {
  // TODO: 实现新增用户逻辑
};

const handleEdit = (record: any) => {
  // TODO: 实现编辑用户逻辑
};

const handleDelete = (record: any) => {
  // TODO: 实现删除用户逻辑
};

const handleChangePassword = (record: any) => {
  currentUser.value = record;
  passwordModalVisible.value = true;
};

const handlePasswordSubmit = async () => {
  try {
    await passwordFormRef.value?.validate();
    const params = {
      username: currentUser.value.username,
      password: passwordForm.value.password
    };
    const res = (await updatePassword(params)) as unknown as ApiResponse<any>;
    if (res.code === 200 && res.success) {
      message.success('密码修改成功');
      passwordModalVisible.value = false;
      passwordForm.value = {
        password: '',
        confirmPassword: '',
      };
    } else {
      message.error(res.message || '密码修改失败');
    }
  } catch (error) {
    console.error('密码修改失败:', error);
    message.error('密码修改失败');
  }
};

const handlePasswordCancel = () => {
  passwordModalVisible.value = false;
  passwordForm.value = {
    password: '',
    confirmPassword: '',
  };
};
</script>

<style scoped>
.user-management {
  padding: 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}
</style> 