<template>
  <template v-if="isLoginPage">
    <router-view></router-view>
  </template>
  <template v-else-if="isStandaloneView">
    <router-view></router-view>
  </template>
  <main-layout v-else />
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useRoute } from "vue-router";
import MainLayout from './layouts/MainLayout.vue';

const route = useRoute();
const isMonitorJessibucaView = computed(() => route.path === '/GBT28181/MonitorViewJessibuca');
const isMonitorMpegtsView = computed(() => route.path === '/GBT28181/MonitorViewMpegts'); 
const isLoginPage = computed(() => route.path === '/login');
const isStandaloneView = computed(() => isMonitorJessibucaView.value || isMonitorMpegtsView.value);
</script>

<style scoped>
</style>
