<template>
  <div class="detail-container">
    <!-- 任务信息 -->
    <el-card shadow="never">
      <div class="header">
        <div>
          <div class="title">{{ taskInfo.title }}</div>
          <div class="content">{{ taskInfo.content || '暂无描述' }}</div>
        </div>
        <div class="right">
          <el-tag v-if="taskInfo.status === 0" type="info">待开始</el-tag>
          <el-tag v-else-if="taskInfo.status === 1" type="primary">进行中</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 统计数据 -->
    <div class="statistics">
      <el-card shadow="hover">
        <div class="label">优先级</div>
        <div class="value">
          <el-tag v-if="taskInfo.priority === 3" type="danger">高</el-tag>
          <el-tag v-else-if="taskInfo.priority === 2" type="warning">中</el-tag>
          <el-tag v-else>低</el-tag>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="label">执行人</div>
        <div class="value">
          <!-- 支持多执行人显示 -->
          <span v-if="taskInfo.executorNames && taskInfo.executorNames.length">
            {{ taskInfo.executorNames.join('、') }}
          </span>
          <span v-else>未指派</span>
        </div>
      </el-card>
      <el-card shadow="hover">
        <div class="label">创建人</div>
        <div class="value">{{ taskInfo.creatorName || '-' }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="label">创建时间</div>
        <div class="value">{{ formatDateTime(taskInfo.createTime) }}</div>
      </el-card>
      <el-card shadow="hover">
        <div class="label">截止时间</div>
        <div class="value">{{ formatDateTime(taskInfo.deadline) }}</div>
      </el-card>
    </div>

    <!-- 任务文件区域 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>任务文件</span>
          <span class="hint-text">完成后把文件上传进来</span>
        </div>
      </template>
      <div class="file-upload">
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          multiple
        >
          <el-button type="primary">上传文件</el-button>
        </el-upload>
      </div>
      <el-table :data="fileList" border stripe style="margin-top:20px">
        <el-table-column prop="name" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button link type="primary" @click="downloadFile(scope.row.id)">下载</el-button>
            <el-button link type="danger" @click="deleteFile(scope.row.id)" v-if="canManage">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="fileList.length === 0" description="暂无文件" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../store/user'

const route = useRoute()
const userStore = useUserStore()
const taskId = Number(route.params.id)

const canManage = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员' || info?.roleId === 2 || info?.roleName === '项目经理'
})

const taskInfo = reactive<any>({
  title: '',
  content: '',
  status: 0,
  priority: 1,
  executorNames: [],      // 改为数组
  creatorName: '',
  createTime: null,
  deadline: null,
  projectId: null         // 新增
})

const fileList = ref<any[]>([])

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '暂无'
  return dateTime.replace('T', ' ')
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

const beforeUpload = (file: File) => {
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    ElMessage.error('文件大小不能超过20MB')
    return false
  }
  return true
}

const loadTaskDetail = async () => {
  const res: any = await request({ url: `/task/detail/${taskId}`, method: 'get' })
  Object.assign(taskInfo, res.data)
}

const loadFileList = async () => {
  try {
    const res: any = await request({ url: '/file/list', method: 'get', params: { taskId } })
    fileList.value = res.data || []
  } catch (error) {
    fileList.value = []
  }
}

// 上传文件（传递 projectId）
const handleUpload = async (options: any) => {
  if (!taskInfo.projectId) {
    ElMessage.warning('无法获取项目信息，请刷新页面后重试')
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('taskId', String(taskId))
  formData.append('projectId', String(taskInfo.projectId))
  try {
    const res: any = await request({
      url: '/file/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 200) {
      ElMessage.success('上传成功')
      loadFileList()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('上传失败，请稍后重试')
  }
}

const deleteFile = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该文件吗？', '提示', { type: 'warning' })
    await request({ url: `/file/delete/${id}`, method: 'delete' })
    ElMessage.success('删除成功')
    loadFileList()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

// 修改下载方法：使用 axios 携带 token，避免 401
const downloadFile = async (id: number) => {
  try {
    const res = await request({
      url: `/file/download/${id}`,
      method: 'get',
      responseType: 'blob'
    })
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    // 从响应头获取文件名，若无则使用默认名
    const contentDisposition = res.headers['content-disposition']
    let fileName = 'download'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
    }
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('下载失败，请稍后重试')
  }
}

onMounted(() => {
  loadTaskDetail()
  loadFileList()
})
</script>

<style scoped>
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 15px;
}
.content {
  color: #666;
  line-height: 28px;
}
.statistics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}
.label {
  color: #999;
  margin-bottom: 15px;
}
.value {
  font-size: 20px;
  font-weight: bold;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.hint-text {
  font-size: 12px;
  color: #909399;
}
.file-upload {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}
</style>