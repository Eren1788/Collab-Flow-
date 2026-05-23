<template>
  <div class="user-container">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <div class="search-bar">
        <el-input
          v-model="searchForm.username"
          placeholder="请输入用户名"
          clearable
          style="width: 300px"
        />
        <el-button type="primary" @click="loadUserList">
          搜索
        </el-button>
        <div class="welcome-info">
          <el-icon><User /></el-icon>
          <span>欢迎使用 Collab Flow 智能任务协作系统，当前用户：{{ currentUserDisplay }} ({{ currentUserRole }})</span>
        </div>
      </div>
    </el-card>

    <!-- 当前用户个人信息卡片（置顶显示） -->
    <el-card class="my-info-card" shadow="hover" v-if="myInfo">
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 我的信息</span>
          <el-button type="primary" size="small" @click="openEditDialog(myInfo)">编辑我的信息</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ myInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ myInfo.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ myInfo.roleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ myInfo.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ myInfo.phone || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 员工列表标题 -->
    <div class="employee-list-title">
      <el-divider content-position="left">
        <span style="font-size: 16px; font-weight: bold; color: #409eff;">
          当前系统的所有员工信息
        </span>
      </el-divider>
    </div>

    <!-- 用户表格（不包含当前登录用户） -->
    <el-card>
      <el-table :data="filteredUserList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="roleName" label="职位">
          <template #default="scope">
            <el-tag type="warning">
              {{ scope.row.roleName || '暂无职位' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />

        <!-- 状态列：仅超级管理员可见 -->
        <el-table-column v-if="isSuperAdmin" label="状态" width="140">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px">
              <span>{{ scope.row.status === 1 ? '正常' : '禁用' }}</span>
              <el-switch
                :model-value="scope.row.status === 1"
                @change="changeStatus(scope.row)"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 操作列：仅超级管理员可见（修改点：添加 v-if="isSuperAdmin"） -->
        <el-table-column label="操作" width="220" v-if="isSuperAdmin">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="openEditDialog(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        style="margin-top: 20px; text-align: right"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 编辑用户弹窗（复用） -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="职位" v-if="isSuperAdmin">
          <el-select
            v-model="editForm.roleId"
            placeholder="请选择职位"
            style="width: 100%"
          >
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <div v-if="!isSuperAdmin" style="color: #909399; font-size: 12px; margin-top: -10px; margin-bottom: 10px;">
          （只能修改自己的昵称、邮箱、手机号）
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { getUserPageApi, deleteUserApi, updateUserApi, updateUserStatusApi, getRoleListApi, getUserInfoApi } from '../api/user'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

// 判断当前登录用户是否是超级管理员
const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

// 当前用户显示名称
const currentUserDisplay = computed(() => {
  const info = userStore.info
  if (info?.nickname) return info.nickname
  if (info?.username) return info.username
  return '未知用户'
})

// 当前用户角色
const currentUserRole = computed(() => {
  const info = userStore.info
  if (info?.roleName) return info.roleName
  return '用户'
})

// 当前用户个人信息（用于顶部卡片）
const myInfo = computed(() => userStore.info)

// 搜索表单
const searchForm = reactive({ username: '' })

// 用户列表（原始数据，包含自己）
const userList = ref<any[]>([])

// 角色列表
const roleList = ref<any[]>([])

// 分页参数
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 弹窗控制
const editDialogVisible = ref(false)

// 编辑表单
const editForm = reactive({
  id: null as number | null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  roleId: null as number | null,
  status: null as number | null
})

// 过滤掉当前登录用户后的列表
const filteredUserList = computed(() => {
  const currentUserId = userStore.info?.id
  if (!currentUserId) return userList.value
  return userList.value.filter(user => user.id !== currentUserId)
})

// 获取用户列表
const loadUserList = async () => {
  try {
    const res: any = await getUserPageApi({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: searchForm.username })
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

// 分页处理
const handlePageChange = (newPage: number) => { pageNum.value = newPage; loadUserList() }

// 获取角色列表
const loadRoleList = async () => {
  try {
    const res: any = await getRoleListApi()
    roleList.value = res.data || []
  } catch {
    ElMessage.error('获取角色列表失败')
  }
}

// 打开编辑弹窗
const openEditDialog = (row: any) => {
  editForm.id = row.id
  editForm.username = row.username
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.phone = row.phone || ''

  if (isSuperAdmin.value) {
    editForm.roleId = row.roleId || null
    editForm.status = row.status
  } else {
    editForm.roleId = null
    editForm.status = null
  }
  editDialogVisible.value = true
}

// 保存编辑
const handleUpdateUser = async () => {
  try {
    const updateData: any = {
      id: editForm.id,
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone
    }
    if (isSuperAdmin.value) {
      if (editForm.roleId !== null) updateData.roleId = editForm.roleId
      if (editForm.status !== null) updateData.status = editForm.status
    }
    await updateUserApi(updateData)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    await loadUserList()  // 刷新列表

    // 如果修改的是自己，同时更新 store 中的用户信息
    if (editForm.id === userStore.info.id) {
      const res: any = await getUserInfoApi()
      userStore.setUserInfo(res.data)
    }
  } catch (error: any) {
    console.error('修改失败:', error)
  }
}

// 删除用户
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch {}
}

// 修改状态（仅超级管理员）
const changeStatus = async (row: any) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateUserStatusApi(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('状态修改成功')
  } catch {
    ElMessage.error('状态修改失败')
  }
}

onMounted(() => {
  loadUserList()
  loadRoleList()
})
</script>

<style scoped>
.user-container { padding: 20px }
.search-card { margin-bottom: 20px }
.search-bar { 
  display: flex; 
  align-items: center; 
  gap: 20px;
  flex-wrap: wrap;
}
.welcome-info {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 0 12px;
  height: 32px;
  border-radius: 16px;
  font-size: 14px;
}
.welcome-info .el-icon {
  font-size: 16px;
}

/* 我的信息卡片样式 */
.my-info-card {
  margin-bottom: 20px;
  border-left: 4px solid #409eff;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header span {
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 员工列表标题样式 */
.employee-list-title {
  margin: 8px 0 16px 0;
}
</style>