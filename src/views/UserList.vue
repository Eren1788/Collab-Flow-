<template>
  <div class="user-container">
    <!-- 当前用户个人信息卡片（置顶显示） -->
    <el-card class="my-info-card" shadow="hover" v-if="myInfo">
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 我的信息</span>
          <el-button type="primary" size="small" @click="openEditDialog(myInfo)">编辑我的信息</el-button>
        </div>
      </template>
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="uploadAvatar"
          accept="image/png, image/jpeg, image/jpg"
        >
          <el-avatar :size="80" :src="avatarUrl" class="user-avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="avatar-tip">点击更换头像</div>
        </el-upload>
        <el-descriptions :column="2" border class="user-info-desc">
          <el-descriptions-item label="用户名">{{ myInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ myInfo.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{ myInfo.roleName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ myInfo.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ myInfo.phone || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

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
      </div>
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
            <el-tag type="warning">{{ scope.row.roleName || '暂无职位' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />

        <el-table-column v-if="isSuperAdmin" label="状态" width="140">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px">
              <span>{{ scope.row.status === 1 ? '正常' : '禁用' }}</span>
              <el-switch :model-value="scope.row.status === 1" @change="changeStatus(scope.row)" />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" v-if="isSuperAdmin">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 20px; text-align: right"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 编辑用户弹窗 -->
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
          <el-select v-model="editForm.roleId" placeholder="请选择职位" style="width: 100%">
            <el-option v-for="item in roleList" :key="item.id" :label="item.roleName" :value="item.id" />
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
import {
  getUserPageApi,
  deleteUserApi,
  updateUserApi,
  updateUserStatusApi,
  getRoleListApi,
  getUserInfoApi,
  uploadAvatarApi
} from '../api/user'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const isSuperAdmin = computed(() => {
  const info = userStore.info
  return info?.roleId === 1 || info?.roleName === '超级管理员'
})

const myInfo = computed(() => userStore.info)

const avatarUrl = computed(() => {
  const avatar = userStore.info?.avatar
  if (avatar && avatar.startsWith('/uploads')) {
    return `/api${avatar}?t=${Date.now()}`
  }
  return ''
})

const searchForm = reactive({ username: '' })
const userList = ref<any[]>([])
const roleList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editDialogVisible = ref(false)
const editForm = reactive({
  id: null as number | null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  roleId: null as number | null,
  status: null as number | null
})

const filteredUserList = computed(() => {
  const currentUserId = userStore.info?.id
  if (!currentUserId) return userList.value
  return userList.value.filter(user => user.id !== currentUserId)
})

const loadUserList = async () => {
  try {
    const res: any = await getUserPageApi({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: searchForm.username })
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

const handlePageChange = (newPage: number) => { pageNum.value = newPage; loadUserList() }

const loadRoleList = async () => {
  try {
    const res: any = await getRoleListApi()
    roleList.value = res.data || []
  } catch {
    ElMessage.error('获取角色列表失败')
  }
}

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

const handleUpdateUser = async () => {
  try {
    const updateData: any = { id: editForm.id, nickname: editForm.nickname, email: editForm.email, phone: editForm.phone }
    if (isSuperAdmin.value) {
      if (editForm.roleId !== null) updateData.roleId = editForm.roleId
      if (editForm.status !== null) updateData.status = editForm.status
    }
    await updateUserApi(updateData)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    await loadUserList()
    if (editForm.id === userStore.info.id) {
      const res: any = await getUserInfoApi()
      userStore.setUserInfo(res.data)
    }
  } catch (error: any) {
    console.error('修改失败:', error)
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch {}
}

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

const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过5MB')
    return false
  }
  return true
}

const uploadAvatar = async (options: any) => {
  try {
    const res: any = await uploadAvatarApi(options.file)
    if (res.code === 200 && res.data?.avatarUrl) {
      ElMessage.success('头像更新成功')
      const userInfoRes: any = await getUserInfoApi()
      userStore.setUserInfo(userInfoRes.data)
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  }
}

onMounted(() => {
  loadUserList()
  loadRoleList()
})
</script>

<style scoped>
.user-container { padding: 0; }

.search-card { margin-bottom: 24px; }
.search-bar { 
  display: flex; 
  align-items: center; 
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

/* 我的信息卡片 */
.my-info-card {
  margin-bottom: 24px;
  border-left: 3px solid var(--cf-primary) !important;
  border-radius: var(--cf-radius-md) !important;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header span {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--cf-text-heading);
}
.avatar-section {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  flex-wrap: wrap;
}
.avatar-uploader {
  cursor: pointer;
  text-align: center;
}
.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--cf-primary-border);
  transition: var(--cf-transition);
}
.user-avatar:hover { opacity: 0.8; }
.avatar-tip {
  font-size: 12px;
  color: var(--cf-text-muted);
  margin-top: 8px;
}
.user-info-desc { flex: 1; }

.employee-list-title { margin: 4px 0 12px 0; }

/* 表格内标签美化 */
:deep(.el-table) { border-radius: var(--cf-radius-md); overflow: hidden; }
:deep(.el-table td) { padding: 10px 0; }
:deep(.el-table th.el-table__cell) { padding: 10px 0; }

:deep(.el-card) { transition: var(--cf-transition-slow); }
</style>