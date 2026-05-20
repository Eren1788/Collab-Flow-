<template>

  <div class="user-container">

    <!-- 顶部工具栏 -->

    <el-card shadow="never">

      <div class="toolbar">

        <div class="left">

          <el-input
            v-model="keyword"
            placeholder="请输入用户名"
            clearable
            style="width:220px"
            @keyup.enter="loadUserList"
          />

          <el-button
            type="primary"
            @click="loadUserList"
          >
            搜索
          </el-button>

        </div>

        <div class="right">

          <el-button
            type="primary"
            @click="openAddDialog"
          >
            新增用户
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- 用户表格 -->

    <el-card shadow="never">

      <el-table
        :data="tableData"
        border
        stripe
        style="width:100%"
      >

        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />

        <el-table-column
          prop="username"
          label="用户名"
        />

        <el-table-column
          prop="nickname"
          label="昵称"
        />

        <el-table-column
          prop="email"
          label="邮箱"
        />

        <el-table-column
          prop="phone"
          label="手机号"
        />

        <el-table-column
          prop="role"
          label="角色"
          width="120"
        >

          <template #default="scope">

            <el-tag
              v-if="scope.row.role === 'admin'"
              type="danger"
            >
              管理员
            </el-tag>

            <el-tag v-else>
              普通用户
            </el-tag>

          </template>

        </el-table-column>

        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >

          <template #default="scope">

            <el-tag
              v-if="scope.row.status === 1"
              type="success"
            >
              正常
            </el-tag>

            <el-tag
              v-else
              type="danger"
            >
              禁用
            </el-tag>

          </template>

        </el-table-column>

        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        />

        <el-table-column
          label="操作"
          width="220"
          fixed="right"
        >

          <template #default="scope">

            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
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

    <!-- 新增/编辑弹窗 -->

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
    >

      <el-form
        :model="form"
        label-width="90px"
      >

        <el-form-item label="用户名">

          <el-input
            v-model="form.username"
            :disabled="isEdit"
          />

        </el-form-item>

        <el-form-item
          label="密码"
          v-if="!isEdit"
        >

          <el-input
            v-model="form.password"
            type="password"
            show-password
          />

        </el-form-item>

        <el-form-item label="昵称">

          <el-input v-model="form.nickname" />

        </el-form-item>

        <el-form-item label="邮箱">

          <el-input v-model="form.email" />

        </el-form-item>

        <el-form-item label="手机号">

          <el-input v-model="form.phone" />

        </el-form-item>

        <el-form-item label="角色">

          <el-select
            v-model="form.role"
            style="width:100%"
          >

            <el-option
              label="管理员"
              value="admin"
            />

            <el-option
              label="普通用户"
              value="user"
            />

          </el-select>

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="dialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          @click="submitForm"
        >
          确定
        </el-button>

      </template>

    </el-dialog>

  </div>

</template>

<script setup lang="ts">

import {

  ref,
  reactive,
  onMounted

} from 'vue'

import {

  ElMessage,
  ElMessageBox

} from 'element-plus'

import request from '../utils/request'

const tableData = ref([])

const total = ref(0)

const pageNum = ref(1)

const pageSize = ref(10)

const keyword = ref('')

const dialogVisible = ref(false)

const isEdit = ref(false)

const form = reactive<any>({

  id:null,

  username:'',

  password:'',

  nickname:'',

  email:'',

  phone:'',

  role:'user'
})

/**
 * 加载用户分页
 */
const loadUserList = async ()=>{

  try{

    const res:any = await request({

      url:'/user/page',

      method:'get',

      params:{

        pageNum:pageNum.value,

        pageSize:pageSize.value,

        keyword:keyword.value
      }
    })

    tableData.value = res.data.records || []

    total.value = res.data.total || 0

  }catch(error){

    console.log(error)
  }
}

/**
 * 分页切换
 */
const handlePageChange = (page:number)=>{

  pageNum.value = page

  loadUserList()
}

/**
 * 打开新增弹窗
 */
const openAddDialog = ()=>{

  isEdit.value = false

  resetForm()

  dialogVisible.value = true
}

/**
 * 编辑
 */
const handleEdit = (row:any)=>{

  isEdit.value = true

  Object.assign(form,row)

  dialogVisible.value = true
}

/**
 * 提交表单
 */
const submitForm = async ()=>{

  try{

    // 编辑
    if(isEdit.value){

      await request({

        url:'/user/update',

        method:'put',

        data:form
      })

      ElMessage.success('修改成功')

    }else{

      // 新增用户
      await request({

        url:'/user/register',

        method:'post',

        data:form
      })

      ElMessage.success('新增成功')
    }

    dialogVisible.value = false

    loadUserList()

  }catch(error){

    console.log(error)
  }
}

/**
 * 删除用户
 */
const handleDelete = (id:number)=>{

  ElMessageBox.confirm(

    '确认删除该用户吗？',

    '提示',

    {

      type:'warning'
    }

  ).then(async ()=>{

    await request({

      url:`/user/delete/${id}`,

      method:'delete'
    })

    ElMessage.success('删除成功')

    loadUserList()
  })
}

/**
 * 重置表单
 */
const resetForm = ()=>{

  form.id = null

  form.username = ''

  form.password = ''

  form.nickname = ''

  form.email = ''

  form.phone = ''

  form.role = 'user'
}

onMounted(()=>{

  loadUserList()
})

</script>

<style scoped>

.user-container{

  display:flex;

  flex-direction:column;

  gap:20px;
}

.toolbar{

  display:flex;

  justify-content:space-between;

  align-items:center;
}

.left{

  display:flex;

  gap:10px;
}

.pagination{

  margin-top:20px;

  display:flex;

  justify-content:flex-end;
}

</style>