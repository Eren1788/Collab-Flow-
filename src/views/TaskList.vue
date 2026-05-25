<template>
  <div class="task-container">

    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="title">任务总数</div>
            <div class="value">{{ statistics.total || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="title">进行中</div>
            <div class="value">{{ statistics.doing || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="title">已完成</div>
            <div class="value">{{ statistics.done || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="title">已逾期</div>
            <div class="value">{{ statistics.expired || 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索区域 -->
    <el-card shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-input
            v-model="keyword"
            placeholder="请输入任务名称"
            clearable
            style="width:220px"
          />
          <el-select
            v-model="status"
            placeholder="任务状态"
            clearable
            style="width:140px"
          >
            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
          <el-select
            v-model="priority"
            placeholder="优先级"
            clearable
            style="width:140px"
          >
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
          <el-button type="primary" @click="loadTaskList">搜索</el-button>
        </div>
        <!-- 移除新增任务按钮 -->
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" border stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="任务标题" min-width="200" />
        <el-table-column prop="projectName" label="所属项目" width="180" />
        <el-table-column prop="executorName" label="执行人" width="140" />
        <el-table-column prop="priority" label="优先级" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.priority === 3" type="danger">高</el-tag>
            <el-tag v-else-if="scope.row.priority === 2" type="warning">中</el-tag>
            <el-tag v-else>低</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="info">待开始</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="primary">进行中</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="success">已完成</el-tag>
          </template>
        </el-table-column>
        <!-- 截止时间列 -->
        <el-table-column label="截止时间" width="180">
          <template #default="scope">
            {{ scope.row.deadline ? scope.row.deadline.replace('T', ' ') : '未设置' }}
          </template>
        </el-table-column>
        <!-- 新增进度列 -->
        <el-table-column label="进度" width="180">
          <template #default="scope">
            <el-progress
              :percentage="getTaskProgress(scope.row.status)"
              :status="getProgressStatus(scope.row.status)"
              :show-text="true"
            />
          </template>
        </el-table-column>
        <!-- 操作列已完全移除 -->
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 以下弹窗全部移除（新增/编辑、指派、状态弹窗不再需要） -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getTaskPageApi } from '../api/task'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

// 判断当前用户是否是超级管理员（仅用于数据过滤，不再影响操作按钮）
const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

const currentUserId = computed(() => userStore.info?.id)

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const status = ref()
const priority = ref()
const statistics = ref<any>({})

/**
 * 根据任务状态获取进度百分比
 */
const getTaskProgress = (taskStatus: number) => {
  switch (taskStatus) {
    case 0: return 0      // 待开始
    case 1: return 50     // 进行中
    case 2: return 100    // 已完成
    default: return 0
  }
}

/**
 * 根据任务状态获取进度条状态样式
 */
const getProgressStatus = (taskStatus: number) => {
  if (taskStatus === 2) return 'success'
  if (taskStatus === 0) return ''
  return ''
}

/**
 * 计算逾期任务数量
 */
const calculateExpired = (tasks: any[]) => {
  const now = new Date().getTime()
  return tasks.filter(task => {
    return task.deadline && new Date(task.deadline).getTime() < now && task.status !== 2
  }).length
}

/**
 * 加载任务列表（权限过滤保持不变）
 */
const loadTaskList = async () => {
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: status.value,
      priority: priority.value
    }
    // 非超级管理员只能看到自己执行的任务
    if (!isSuperAdmin.value && currentUserId.value) {
      params.executorId = currentUserId.value
    }
    const res: any = await getTaskPageApi(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    await loadStatistics()
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  }
}

/**
 * 加载统计（权限过滤保持一致）
 */
const loadStatistics = async () => {
  try {
    const params: any = {}
    if (!isSuperAdmin.value && currentUserId.value) {
      params.executorId = currentUserId.value
    }
    const res: any = await request({ url: '/task/statistics', method: 'get', params })
    statistics.value = res.data || {}
    if (statistics.value.expired === undefined) {
      statistics.value.expired = calculateExpired(tableData.value)
    }
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

/**
 * 分页切换
 */
const handlePageChange = (page: number) => {
  pageNum.value = page
  loadTaskList()
}

onMounted(() => {
  loadTaskList()
})
</script>

<style scoped>
.task-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.stat-card {
  text-align: center;
}
.stat-card .title {
  color: #999;
  margin-bottom: 10px;
}
.stat-card .value {
  font-size: 30px;
  font-weight: bold;
  color: #409EFF;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left {
  display: flex;
  gap: 10px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>