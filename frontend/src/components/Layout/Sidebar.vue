<template>

  <el-menu
    router
    class="sidebar"
    :default-active="activeMenu"
  >

    <!-- 用户管理 -->

    <el-menu-item
      v-if="userStore.hasPermission('user:list')"
      index="/users"
    >
      用户管理
    </el-menu-item>

    <!-- ========================= -->
    <!-- 【新增】项目管理权限控制 -->
    <!-- ========================= -->

    <el-menu-item
      v-if="userStore.hasPermission('project:list')"
      index="/projects"
    >
      项目管理
    </el-menu-item>

    <!-- ========================= -->
    <!-- 【新增】任务管理权限控制 -->
    <!-- ========================= -->

    <el-menu-item
      v-if="userStore.hasPermission('task:list')"
      index="/tasks"
    >
      任务管理
    </el-menu-item>

  </el-menu>

</template>

<script setup lang="ts">

import {

  ref,
  onMounted

} from 'vue'

import { useRoute } from 'vue-router'

import { useUserStore } from '../../store/user'

const route = useRoute()

const activeMenu = ref('')

const userStore = useUserStore()

onMounted(()=>{

  activeMenu.value = route.path
})

</script>

<style scoped>

.sidebar{

  height:100%;

  background-color:#ffffff;
}

</style>