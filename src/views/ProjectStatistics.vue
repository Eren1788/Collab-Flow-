<template>
  <div class="statistics-container">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <span>项目成员完成情况</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      <el-collapse v-model="activeNames" accordion>
        <el-collapse-item v-for="member in memberList" :key="member.userId" :name="member.userId">
          <template #title>
            <div class="member-title">
              <el-avatar :size="32" :src="getAvatarUrl(member.avatar)">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="member-name">{{ member.nickname || member.username }}</span>
              <el-progress :percentage="member.progress" :status="member.progress === 100 ? 'success' : ''" style="width: 200px; margin-left: 20px;" />
            </div>
          </template>
          <div class="member-detail">
            <div class="task-summary">
              任务总数：{{ member.totalTasks }}，已完成：{{ member.completedTasks }}
            </div>
            <el-table :data="member.files" border stripe>
              <el-table-column prop="taskTitle" label="所属任务" />
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column prop="fileSize" label="大小" width="120">
                <template #default="scope">
                  {{ formatFileSize(scope.row.fileSize) }}
                </template>
              </el-table-column>
              <el-table-column prop="uploadTime" label="上传时间" width="180" />
              <el-table-column label="操作" width="100">
                <template #default="scope">
                  <el-button link type="primary" @click="downloadFile(scope.row.id)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="member.files.length === 0" description="未上传任何文件" />
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="memberList.length === 0" description="暂无成员" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const projectId = route.params.id
const memberList = ref<any[]>([])
const activeNames = ref([])

const getAvatarUrl = (avatar: string) => {
  if (avatar && avatar.startsWith('/uploads')) {
    return `/api${avatar}?t=${Date.now()}`
  }
  return ''
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

const loadStatistics = async () => {
  try {
    const res: any = await request({ url: `/project/statistics/${projectId}`, method: 'get' })
    memberList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

const downloadFile = async (id: number) => {
  try {
    const res = await request({
      url: `/file/download/${id}`,
      method: 'get',
      responseType: 'blob'
    })
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const contentDisposition = res.headers['content-disposition']
    let fileName = 'file'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*=UTF-8''(.+)/)
      if (match) fileName = decodeURIComponent(match[1])
      else {
        const match2 = contentDisposition.match(/filename="(.+)"/)
        if (match2) fileName = match2[1]
      }
    }
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.member-title {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.member-name {
  font-weight: bold;
  font-size: 16px;
  min-width: 100px;
}
.member-detail {
  padding: 10px;
}
.task-summary {
  margin-bottom: 15px;
  color: #666;
}
</style>