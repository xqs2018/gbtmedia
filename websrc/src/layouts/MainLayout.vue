<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" :width="220" :trigger="null" collapsible>
      <div class="logo">
        <span class="logo-text" :class="{ 'collapsed': collapsed }">GBTMedia</span>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys" theme="dark" mode="inline">
        <template v-for="menu in menus" :key="menu.path">
          <a-sub-menu v-if="menu.type === 'menu'" :key="menu.path" @click="!menu.children && router.push(menu.path)">
            <template #title>
              <ApiOutlined v-if="menu.icon === 'ApiOutlined'" />
              <VideoCameraOutlined v-if="menu.icon === 'VideoCameraOutlined'" />
              <CarOutlined v-if="menu.icon === 'CarOutlined'" />
              <HomeOutlined v-if="menu.icon === 'HomeOutlined'" />
              <SettingOutlined v-if="menu.icon === 'SettingOutlined'" />
              <span>{{ menu.name }}</span>
            </template>
            <template v-if="menu.children" v-for="child in menu.children" :key="child.path">
              <a-menu-item v-if="child.type === 'menu'" :key="child.path">
                <router-link :to="child.path">{{ child.name }}</router-link>
              </a-menu-item>
            </template>
          </a-sub-menu>
        </template>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <menu-unfold-outlined
          v-if="collapsed"
          class="trigger"
          @click="() => (collapsed = !collapsed)"
        />
        <menu-fold-outlined
          v-else
          class="trigger"
          @click="() => (collapsed = !collapsed)"
        />
        <span class="header-text"></span>
        <div class="header-right">
          <a-dropdown>
            <a class="user-dropdown">
              <UserOutlined class="user-icon" />
              <span class="username">Admin</span>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <UserOutlined />
                  <span>个人中心</span>
                </a-menu-item>
                <a-menu-item key="settings">
                  <SettingOutlined />
                  <span>设置</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content style="margin: 0 16px; height: calc(100vh - 50px - 64px); overflow: hidden;">
        <a-breadcrumb style="margin: 16px 0">
          <template v-for="(item, index) in prevPath" :key="item.path">
            <a-breadcrumb-item v-if="index < prevPath.length - 1">
              <router-link :to="item.path">{{ item.name }}</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item v-else>{{ item.name }}</a-breadcrumb-item>
          </template>
        </a-breadcrumb>
        <div :style="{ padding: '16px', background: '#fff', height: 'calc(100% - 32px)', overflow: 'auto' }">
          <router-view></router-view>
        </div>
      </a-layout-content>
      <a-layout-footer style="text-align: center">
        Ant Design ©2018 Created by Ant UED
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script lang="ts" setup>
import { ref, watch, computed } from 'vue';
import { useRoute, useRouter } from "vue-router";
import { MenuUnfoldOutlined, MenuFoldOutlined, ApiOutlined, VideoCameraOutlined,CarOutlined, HomeOutlined, UserOutlined, SettingOutlined, LogoutOutlined } from '@ant-design/icons-vue';

const route = useRoute();
const router = useRouter();
const collapsed = ref<boolean>(false);
const selectedKeys = ref<string[]>([]);
const openKeys = ref<string[]>([]);

const menus = [
  {
    name: '首页',
    type: 'menu',
    icon: 'HomeOutlined',
    path: '/home'
  },
  {
    name: 'GBT28181',
    type: 'menu',
    icon: 'VideoCameraOutlined',
    path: '/GBT28181',
    children: [
      {
        name: '监控信息',
        path: '/GBT28181/MonitorView',
        type: 'menu'
      },
      {
        name: '国标设备',
        path: '/GBT28181/DeviceView',
        type: 'menu',
        children: [
          {
            name: '设备通道',
            path: '/GBT28181/DeviceChannelView',
            type: 'button',
            children: [
              {
                name: '设备录像',
                path: '/GBT28181/DeviceChannelRecordView',
                type: 'button',
              },
              {
                name: '录像文件',
                path: '/GBT28181/DeviceChannelRecordFileView',
                type: 'button',
              },
            ]
          },
        ],
      },
      {
        name: '设备推流',
        path: '/GBT28181/InviteView',
        type: 'menu'
      },
      {
        name: '录像文件',
        path: '/GBT28181/RecordView',
        type: 'menu'
      },
      {
        name: '国标级联',
        path: '/GBT28181/PlatformView',
        type: 'menu'
      },
    ],
  },
  {
    name: 'JTT808',
    type: 'menu',
    icon: 'CarOutlined',
    path: '/JTT808',
    children: [
    {
        name: '监控信息',
        path: '/JTT808/MonitorView',
        type: 'menu'
      },
      {
        name: '部标终端',
        path: '/JTT808/ClientView',
        type: 'menu',
        children: [
          {
            name: '终端通道',
            path: '/JTT808/ClientChannelView',
            type: 'button',
            children: [
              {
                name: '终端录像',
                path: '/JTT808/ClientChannelRecordView',
                type: 'button',
              },
            ]
          },
          {
            name: '终端位置',
            path: '/JTT808/ClientLocationView',
            type: 'button'
          }
        ],
      },
      {
        name: '终端推流',
        path: '/JTT808/ClientMediaView',
        type: 'menu'
      },
      {
        name: '录像文件',
        path: '/JTT808/RecordView',
        type: 'menu'
      },
    ]
  },
  {
    name: '系统管理',
    type: 'menu',
    icon: 'SettingOutlined',
    path: '/system',
    children: [
      {
        name: '用户管理',
        path: '/system/user',
        type: 'menu'
      },
      {
        name: '日志管理',
        path: '/system/log',
        type: 'menu'
      }
    ]
  }
];

// 从 localStorage 获取初始路径，如果没有则使用默认值
const getInitialPath = () => {
  const savedPath = localStorage.getItem('prevPath');
  return savedPath ? JSON.parse(savedPath) : [{name:'GBT28181',path:'/'},{name:'国标设备',path:'/GBT28181/DeviceView'}];
};

const prevPath = ref<any>(getInitialPath());

interface MenuItem {
  name: string;
  path: string;
  type: string;
  children?: MenuItem[];
}

// 递归查找菜单项及其父级菜单项
function findMenuItem(menuList: MenuItem[], path: string, parents: any[] = []): any[] | null {
  for (const menu of menuList) {
    const currentPath = path.includes('?') ? path.split('?')[0] : path;
    if (menu.path === currentPath) {
      // 保持原始路径中的查询参数
      return [...parents, { name: menu.name, path: path }];
    }
    if (menu.children) {
      const result = findMenuItem(menu.children, path, [...parents, { name: menu.name, path: menu.path }]);
      if (result) {
        return result;
      }
    }
  }
  return null;
}

// 保持路径参数一致的函数
function keepParamsConsistent(newPaths: any[], oldPaths: any[]) {
  return newPaths.map((newPathObj) => {
    if (!newPathObj.path) {
      return newPathObj;
    }
    const baseNewPath = newPathObj.path.split('?')[0];
    const oldMatch = oldPaths.find((oldPathObj) => {
      return oldPathObj.path && oldPathObj.path.split('?')[0] === baseNewPath;
    });
    if (oldMatch && oldMatch.path && oldMatch.path.includes('?')) {
      const params = oldMatch.path.split('?')[1];
      return { ...newPathObj, path: `${baseNewPath}?${params}` };
    }
    return newPathObj;
  });
}

// 监听路由变化 每次变化后修改prevPath  按顺序来 如果path中有参数
watch(
    () => route.fullPath,
    (newPath, oldPath) => {
      const newPrevPath = findMenuItem(menus, newPath);
      if (newPrevPath) {
        const updatedPrevPath = keepParamsConsistent(newPrevPath, prevPath.value);
        prevPath.value = updatedPrevPath;
        // 保存到 localStorage
        localStorage.setItem('prevPath', JSON.stringify(updatedPrevPath));
        openKeys.value = [prevPath.value[0].path]
        if(prevPath.value[1]){
          selectedKeys.value = [prevPath.value[1].path]
        }else{
          openKeys.value = []
          selectedKeys.value = []
        }
      }
    }
);

const handleLogout = () => {
  // 这里添加退出登录的逻辑
  window.location.href = "/logout"
};
</script>

<style scoped>

.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}

.site-layout .site-layout-background {
  background: #fff;
}
[data-theme='dark'] .site-layout .site-layout-background {
  background: #141414;
}
:deep(.ant-table) {
  .ant-table-tbody > tr > td {
    padding: 8px 6px;
    font-size: 14px;
  }
  .ant-table-thead > tr > th {
    padding: 8px 6px;
    font-size: 14px;
  }
}

.logo {
  height: 50px;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #367fa9;
  overflow: hidden;
}

.logo-text {
  color: white;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  transition: all 0.3s;
}

.logo-text.collapsed {
  font-size: 14px;
}

.header {
  background: #3c8dbc;
  padding: 0 16px;
  display: flex;
  align-items: center;
  height: 50px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  color: white;
  margin-right: 16px;
}

.trigger:hover {
  color: #1890ff;
}

.header-text {
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.site-layout .site-layout-background {
  background: #fff;
}
[data-theme='dark'] .site-layout .site-layout-background {
  background: #141414;
}

:deep(.ant-layout-sider) {
  background: #34495e !important;
}

:deep(.ant-menu) {
  background: #34495e !important;
}

:deep(.ant-menu-item) {
  background: #34495e !important;
  transition: none !important;
}

:deep(.ant-menu-item:hover:not(.ant-menu-item-selected)) {
  background: #34495e !important;
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-item-selected) {
  background: #3498db !important;
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-title) {
  transition: none !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title) {
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title .anticon) {
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-title:hover:not(.ant-menu-submenu-selected > .ant-menu-submenu-title)) {
  background: transparent !important;
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-title:hover:not(.ant-menu-submenu-selected > .ant-menu-submenu-title) .anticon) {
  color: #ffffff !important;
  transition: none !important;
}

:deep(.ant-menu-item .anticon) {
  margin-right: 10px;
  font-size: 16px;
}

:deep(.ant-menu-submenu-title .anticon) {
  margin-right: 10px;
  font-size: 16px;
}

:deep(.ant-menu-submenu) {
  background: #34495e !important;
  transition: none !important;
}

:deep(.ant-menu-submenu:hover) {
  background: #34495e !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-active) {
  background: #34495e !important;
  transition: none !important;
}

:deep(.ant-menu-submenu-arrow) {
  color: #ffffff !important;
}

:deep(.ant-menu-submenu-arrow::before),
:deep(.ant-menu-submenu-arrow::after) {
  background: #ffffff !important;
}

:deep(.ant-menu-submenu:hover > .ant-menu-submenu-title > .ant-menu-submenu-arrow) {
  color: #ffffff !important;
}

:deep(.ant-menu-submenu:hover > .ant-menu-submenu-title > .ant-menu-submenu-arrow::before),
:deep(.ant-menu-submenu:hover > .ant-menu-submenu-title > .ant-menu-submenu-arrow::after) {
  background: #ffffff !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title > .ant-menu-submenu-arrow) {
  color: #ffffff !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title > .ant-menu-submenu-arrow::before),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title > .ant-menu-submenu-arrow::after) {
  background: #ffffff !important;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.user-dropdown {
  color: white;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 12px;
  height: 100%;
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-icon {
  font-size: 16px;
  margin-right: 8px;
}

.username {
  font-size: 14px;
}

</style> 