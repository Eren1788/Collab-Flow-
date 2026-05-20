<template>

  <div class="comment-container">

    <!-- 查询区域 -->

    <el-card shadow="never">

      <div class="toolbar">

        <div class="left">

          <el-input
            v-model="taskId"
            placeholder="请输入任务ID"
            style="width:220px"
          />

          <el-button
            type="primary"
            @click="loadCommentList"
          >
            查询评论
          </el-button>

        </div>

        <div class="right">

          <el-button
            type="primary"
            @click="openAddDialog"
          >
            新增评论
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- 评论表格 -->

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
          prop="taskId"
          label="任务ID"
          width="100"
        />

        <el-table-column
          prop="userId"
          label="用户ID"
          width="100"
        />

        <el-table-column
          prop="content"
          label="评论内容"
          min-width="400"
        />

        <el-table-column
          prop="createTime"
          label="评论时间"
          width="180"
        />

        <el-table-column
          label="操作"
          width="120"
        >

          <template #default="scope">

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

    </el-card>

    <!-- 新增评论 -->

    <el-dialog
      v-model="dialogVisible"
      title="新增评论"
      width="600px"
    >

      <el-form
        :model="form"
        label-width="100px"
      >

        <el-form-item label="任务ID">

          <el-input v-model="form.taskId" />

        </el-form-item>

        <el-form-item label="用户ID">

          <el-input v-model="form.userId" />

        </el-form-item>

        <el-form-item label="评论内容">

          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
          />

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
          发布评论
        </el-button>

      </template>

    </el-dialog>

  </div>

</template>

<script setup lang="ts">

import {

  ref,
  reactive

} from 'vue'

import {

  ElMessage,
  ElMessageBox

} from 'element-plus'

import request from '../utils/request'

const tableData = ref([])

const taskId = ref('')

const dialogVisible = ref(false)

const form = reactive<any>({

  taskId:'',

  userId:'',

  content:''
})

/**
 * 评论列表
 */
const loadCommentList = async ()=>{

  if(!taskId.value){

    ElMessage.warning('请输入任务ID')

    return
  }

  try{

    const res:any = await request({

      url:`/comment/list/${taskId.value}`,

      method:'get'
    })

    tableData.value = res.data || []

  }catch(error){

    console.log(error)
  }
}

/**
 * 打开新增弹窗
 */
const openAddDialog = ()=>{

  resetForm()

  dialogVisible.value = true
}

/**
 * 提交评论
 */
const submitForm = async ()=>{

  try{

    await request({

      url:'/comment/add',

      method:'post',

      data:form
    })

    ElMessage.success('评论成功')

    dialogVisible.value = false

    // 自动刷新
    if(taskId.value){

      loadCommentList()
    }

  }catch(error){

    console.log(error)
  }
}

/**
 * 删除评论
 */
const handleDelete = (id:number)=>{

  ElMessageBox.confirm(

    '确认删除该评论吗？',

    '提示',

    {

      type:'warning'
    }

  ).then(async ()=>{

    await request({

      url:`/comment/delete/${id}`,

      method:'delete'
    })

    ElMessage.success('删除成功')

    loadCommentList()
  })
}

/**
 * 重置
 */
const resetForm = ()=>{

  form.taskId = ''

  form.userId = ''

  form.content = ''
}

</script>

<style scoped>

.comment-container{

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

</style>