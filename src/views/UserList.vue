<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="list">
      <el-table-column prop="id" label="ID"/>
      <el-table-column prop="username" label="用户名"/>
      <el-table-column prop="nickname" label="昵称"/>
      <el-table-column prop="role" label="角色"/>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { requestWithType } from '../utils/request'

interface User {
  id: number
  username: string
  nickname: string
  role: string
}

const list = ref<User[]>([])

onMounted(async () => {
  try {
    const res = await requestWithType<{ code:number, data: User[], message:string }>({
      url: '/user/list',
      method: 'get'
    })
    if(res.code === 200) list.value = res.data
  } catch (err) {
    console.error(err)
  }
})
</script>