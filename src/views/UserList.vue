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
        <!-- [MODIFIED] 移除“新增用户”按钮，改为显示当前登录用户信息 -->
        <div class="welcome-info">
          <el-icon><User /></el-icon>
          <span>欢迎使用 Collab Flow 智能任务协作系统，当前用户：{{ currentUserDisplay }} ({{ currentUserRole }})</span>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="userList" border stripe>
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

        <!-- 仅当用户不是项目经理时显示状态列 -->
        <el-table-column v-if="!isProjectManager" label="状态" width="140">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px">
              <span>{{ scope.row.status === 1 ? '正常' : '禁用' }}</span>
              <el-switch
                :model-value="scope.row.status === 1"
                @change="changeStatus(scope.row)"
                v-if="userStore.hasPermission('user:status')"
              />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="openEditDialog(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="userStore.hasPermission('user:delete')"
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

    <!-- 新增用户弹窗已完全移除，不再需要 -->
    <!-- 编辑用户弹窗保留 -->
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
        <el-form-item label="职位">
          <el-select
            v-model="editForm.roleId"
            placeholder="请选择职位"
            style="width: 100%"
            :disabled="!userStore.hasPermission('user:status')"
          >
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
          <div
            v-if="!userStore.hasPermission('user:status')"
            style="color: #909399; font-size: 12px; margin-top: 5px;"
          >
            只有管理员可以修改用户职位
          </div>
        </el-form-item>
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
import { User } from '@element-plus/icons-vue'  // [MODIFIED] 导入用户图标
import { getUserPageApi, registerApi, deleteUserApi, updateUserApi, updateUserStatusApi, getRoleListApi } from '../api/user'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

// 判断当前登录用户是否是项目经理
const isProjectManager = computed(() => userStore.info?.roleName === '项目经理')

// [MODIFIED] 计算当前用户显示名称（昵称优先，无昵称则用户名）
const currentUserDisplay = computed(() => {
  const info = userStore.info
  if (info?.nickname) return info.nickname
  if (info?.username) return info.username
  return '未知用户'
})

// [MODIFIED] 计算当前用户角色
const currentUserRole = computed(() => {
  const info = userStore.info
  if (info?.roleName) return info.roleName
  return '用户'
})

// 搜索表单
const searchForm = reactive({ username: '' })

// 用户列表
const userList = ref<any[]>([])

// 角色列表
const roleList = ref<any[]>([])

// 分页参数
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 弹窗控制
const editDialogVisible = ref(false)

// 表单（移除了addForm相关变量）
const editForm = reactive({ id: null, username: '', nickname: '', email: '', phone: '', roleId: null })

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
    const res: any = await getRoleListApi(); 
    roleList.value = res.data || [] 
  } catch { 
    ElMessage.error('获取角色列表失败') 
  } 
}

// 弹窗操作（移除了新增相关的openAddDialog）
const openEditDialog = (row: any) => { 
  Object.assign(editForm, { ...row }); 
  editDialogVisible.value = true 
}

// 编辑用户
const handleUpdateUser = async () => { 
  try { 
    const updateData: any = { ...editForm }; 
    if (!userStore.hasPermission('user:status')) delete updateData.roleId; 
    await updateUserApi(updateData); 
    ElMessage.success('修改成功'); 
    editDialogVisible.value = false; 
    loadUserList() 
  } catch { 
    ElMessage.error('修改失败') 
  } 
}

// 删除用户
const handleDelete = async (id: number) => { 
  try { 
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' }); 
    await deleteUserApi(id); 
    ElMessage.success('删除成功'); 
    loadUserList() 
  } catch {} 
}

// 修改状态
const changeStatus = async (row: any) => { 
  try { 
    const newStatus = row.status === 1 ? 0 : 1; 
    await updateUserStatusApi(row.id, newStatus); 
    row.status = newStatus; 
    ElMessage.success('状态修改成功') 
  } catch { 
    ElMessage.error('状态修改失败') 
  } 
}

onMounted(() => { 
  loadUserList(); 
  loadRoleList();
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

/* [MODIFIED] 欢迎信息样式 */
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
</style>