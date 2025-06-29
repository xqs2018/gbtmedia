// @ts-nocheck

import { createRouter, createWebHistory ,createWebHashHistory} from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'index',
      redirect: to => "/GBT28181/DeviceView",
      meta: { requiresAuth: true }
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/GBT28181',
      name: 'GBT28181',
      redirect: to => "/GBT28181/DeviceView"
    },
    {
      path: '/GBT28181/MonitorView',
      name: 'GBT28181MonitorView',
      component: () => import('../views/gbt28181/MonitorView.vue'),
    },
    {
      path: '/GBT28181/MonitorViewJessibuca',
      name: 'GBT28181MonitorViewJessibuca',
      component: () => import('../views/gbt28181/MonitorViewJessibuca.vue'),
    },
    {
      path: '/GBT28181/MonitorViewMpegts',
      name: 'GBT28181MonitorViewMpegts',
      component: () => import('../views/gbt28181/MonitorViewMpegts.vue'),
    },
    {
      path: '/GBT28181/DeviceView',
      name: 'GBT28181DeviceView',
      component: () => import('../views/gbt28181/DeviceView.vue'),
    },
    {
      path: '/GBT28181/DeviceChannelView',
      name: 'GBT28181DeviceChannelView',
      component: () => import('../views/gbt28181/DeviceChannelView.vue'),
    },
    {
      path: '/GBT28181/DeviceChannelRecordView',
      name: 'GBT28181DeviceChannelRecordView',
      component: () => import('../views/gbt28181/DeviceChannelRecordView.vue'),
    },
    {
      path: '/GBT28181/DeviceChannelRecordFileView',
      name: 'GBT28181DeviceChannelRecordFileView',
      component: () => import('../views/gbt28181/DeviceChannelRecordFileView.vue'),
    },
    {
      path: '/GBT28181/PlatformView',
      name: 'GBT28181PlatformView',
      component: () => import('../views/gbt28181/PlatformView.vue'),
    },
    {
      path: '/GBT28181/RecordView',
      name: 'GBT28181RecordView',
      component: () => import('../views/gbt28181/RecordView.vue'),
    },
    {
      path: '/GBT28181/InviteView',
      name: 'GBT28181InviteView',
      component: () => import('../views/gbt28181/InviteView.vue'),
    },
    {
      path: '/JTT808',
      name: 'JTT808',
      redirect: to => "/JTT808/ClientView"
    },
    {
      path: '/JTT808/MonitorView',
      name: 'JTT808MonitorView',
      component: () => import('../views/JTT808/MonitorView.vue'),
    },
    {
      path: '/JTT808/ClientView',
      name: 'JTT808ClientView',
      component: () => import('../views/jtt808/ClientView.vue'),
    },
    {
      path: '/JTT808/ClientLocationView',
      name: 'JTT808ClientLocationView',
      component: () => import('../views/jtt808/ClientLocationView.vue'),
    },
    {
      path: '/JTT808/ClientChannelView',
      name: 'JTT808ClientChannelView',
      component: () => import('../views/jtt808/ClientChannelView.vue'),
    },
    {
      path: '/JTT808/ClientChannelRecordView',
      name: 'JTT808ClientChannelRecordView',
      component: () => import('../views/jtt808/ClientChannelRecordView.vue'),
    },
    {
      path: '/JTT808/ClientMediaView',
      name: 'JTT808ClientMediaView',
      component: () => import('../views/jtt808/ClientMediaView.vue'),
    },
    {
      path: '/JTT808/RecordView',
      name: 'JTT808RecordView',
      component: () => import('../views/jtt808/RecordView.vue'),
    },
    {
      path: '/system/user',
      name: 'systemUser',
      component: () => import('../views/system/UserView.vue'),
    },
    {
      path: '/system/log',
      name: 'systemLog',
      component: () => import('../views/system/LogView.vue'),
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem('token')
  const hasHttpBase = localStorage.getItem('httpbase')
  
  if (to.meta.requiresAuth && (!isAuthenticated || !hasHttpBase)) {
    next('/login')
  } else {
    next()
  }
})

export default router
