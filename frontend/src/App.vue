<template>

  <router-view />

</template>

<script setup lang="ts">

import { onMounted } from 'vue'

import { useUserStore } from './store/user'

const userStore = useUserStore()

/**
 * 页面刷新后恢复连接
 */
onMounted(() => {

  if (
    userStore.token
    &&
    userStore.info?.id
  ) {

    userStore.connectWebSocket()
  }
})

</script>