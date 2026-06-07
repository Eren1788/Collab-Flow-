<template>
  <div class="file-container">
    <!-- 左侧项目列表 -->
    <div class="file-sidebar">
      <div class="sidebar-header">
        <span>项目列表</span>
        <el-button size="small" text @click="loadProjectList">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
      <div class="project-list">
        <div
          v-for="proj in projectList"
          :key="proj.id"
          class="project-item"
          :class="{ active: selectedProjectId === proj.id }"
          @click="selectProject(proj.id)"
        >
          <el-icon><FolderOpened /></el-icon>
          <span class="project-name">{{ proj.name }}</span>
        </div>
        <el-empty v-if="projectList.length === 0" description="暂无项目" :image-size="60" />
      </div>
    </div>

    <!-- 右侧文件区域 -->
    <div class="file-main">
      <!-- 顶部操作栏（只保留刷新按钮） -->
      <el-card shadow="never">
        <div class="toolbar">
          <div class="left">
            <template v-if="selectedProjectId">
              <span class="selected-project-label">
                当前项目：<strong>{{ currentProjectName }}</strong>
              </span>
            </template>
            <span v-else class="placeholder-text">请从左侧选择一个项目查看文件</span>
          </div>
          <div class="right">
            <el-button type="success" @click="loadFileList">刷新列表</el-button>
          </div>
        </div>
      </el-card>

      <!-- 文件表格 -->
      <el-card shadow="never">
        <el-table
          v-if="selectedProjectId"
          :data="tableData"
          border
          stripe
          style="width:100%"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="文件名称" min-width="220">
            <template #default="scope">
              <div class="file-name-cell">
                <el-icon><Document /></el-icon>
                {{ scope.row.name }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="文件大小" width="120">
            <template #default="scope">
              {{ formatFileSize(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="文件类型" width="130" />
          <el-table-column prop="uploaderName" label="上传人" width="120" />
          <el-table-column label="上传时间" width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.uploadTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="downloadFile(scope.row.id)">
                下载
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!selectedProjectId"
          description="请在左侧选择项目"
          :image-size="80"
        />
        <el-empty
          v-if="selectedProjectId && tableData.length === 0"
          description="该项目暂无文件"
          :image-size="80"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, FolderOpened, Document } from '@element-plus/icons-vue'
import request from '../utils/request'

// ---- 状态 ----
const projectList = ref<any[]>([])
const selectedProjectId = ref<number | null>(null)
const tableData = ref<any[]>([])

// ---- 计算属性 ----
const currentProjectName = computed(() => {
  const proj = projectList.value.find(p => p.id === selectedProjectId.value)
  return proj ? proj.name : ''
})

// ---- 项目列表 ----
const loadProjectList = async () => {
  try {
    const res: any = await request({
      url: '/file/project-list',
      method: 'get'
    })
    projectList.value = res.data || []
  } catch (error) {
    console.log(error)
  }
}

// ---- 选择项目 ----
const selectProject = (id: number) => {
  selectedProjectId.value = id
  loadFileList()
}

// ---- 文件列表 ----
const loadFileList = async () => {
  if (!selectedProjectId.value) return
  try {
    const res: any = await request({
      url: '/file/list',
      method: 'get',
      params: { projectId: selectedProjectId.value }
    })
    tableData.value = res.data || []
  } catch (error) {
    console.log(error)
  }
}

// ---- 下载文件 ----
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
    const contentDisposition = res.headers['content-disposition']
    let fileName = 'download'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
      }
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

// ---- 删除文件 ----
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确认删除该文件吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request({
        url: `/file/delete/${id}`,
        method: 'delete'
      })
      ElMessage.success('删除成功')
      loadFileList()
    })
    .catch(() => {})
}

// ---- 日期时间格式化 ----
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ')
}

// ---- 文件大小格式化 ----
const formatFileSize = (size: number) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

// ---- 初始化 ----
onMounted(() => {
  loadProjectList()
})
</script>

<style scoped>
.file-container {
  display: flex;
  gap: 24px;
  height: calc(100vh - 112px);
  min-height: 500px;
}

/* 左侧项目列表 */
.file-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--cf-surface);
  border-radius: var(--cf-radius-md);
  border: 1px solid var(--cf-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--cf-shadow-sm);
  transition: var(--cf-transition);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid var(--cf-border);
  color: var(--cf-text-heading);
}

.project-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.project-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--cf-text);
  transition: var(--cf-transition);
  font-size: 13px;
}
.project-item:hover {
  background: var(--cf-primary-bg);
  color: var(--cf-primary);
}
.project-item.active {
  background: var(--cf-primary-bg);
  color: var(--cf-primary);
  font-weight: 600;
  border-right: 3px solid var(--cf-primary);
}
.project-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧主区域 */
.file-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.left { display: flex; align-items: center; gap: 12px; }
.selected-project-label { font-size: 14px; color: var(--cf-text); font-weight: 500; }
.placeholder-text { color: var(--cf-text-muted); font-size: 14px; }
.file-name-cell { display: flex; align-items: center; gap: 6px; }

:deep(.el-table) { border-radius: var(--cf-radius-md); overflow: hidden; }
:deep(.el-table td) { padding: 10px 0; }
:deep(.el-table th.el-table__cell) { padding: 10px 0; }
:deep(.el-card) { transition: var(--cf-transition-slow); }
</style>