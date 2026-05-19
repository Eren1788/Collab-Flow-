<template>
  <el-container style="height:100vh">
    <!-- 左侧菜单 -->
    <el-aside width="200px">
      <Sidebar />
    </el-aside>

    <!-- 内容区 -->
    <el-container>
      <Header />
      <el-main>
        <router-view />
      </el-main>
      <Footer />
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Header from '../components/Layout/Header.vue'
import Sidebar from '../components/Layout/Sidebar.vue'
import Footer from '../components/Layout/Footer.vue'
</script>