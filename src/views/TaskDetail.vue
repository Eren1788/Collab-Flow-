<template>

  <div class="detail-container">

    <!-- 任务信息 -->

    <el-card shadow="never">

      <div class="header">

        <div>

          <div class="title">
            {{ taskInfo.title }}
          </div>

          <div class="content">
            {{ taskInfo.content || '暂无描述' }}
          </div>

        </div>

        <div class="right">

          <el-tag
            v-if="taskInfo.status === 0"
            type="info"
          >
            待开始
          </el-tag>

          <el-tag
            v-else-if="taskInfo.status === 1"
            type="primary"
          >
            进行中
          </el-tag>

          <el-tag
            v-else
            type="success"
          >
            已完成
          </el-tag>

        </div>

      </div>

    </el-card>

    <!-- 统计 -->

    <div class="statistics">

      <el-card shadow="hover">

        <div class="label">优先级</div>

        <div class="value">

          <el-tag
            v-if="taskInfo.priority === 3"
            type="danger"
          >
            高
          </el-tag>

          <el-tag
            v-else-if="taskInfo.priority === 2"
            type="warning"
          >
            中
          </el-tag>

          <el-tag v-else>
            低
          </el-tag>

        </div>

      </el-card>

      <el-card shadow="hover">

        <div class="label">执行人</div>

        <div class="value">
          {{ taskInfo.executorName || '暂无' }}
        </div>

      </el-card>

      <el-card shadow="hover">

        <div class="label">创建时间</div>

        <div class="value">
          {{ taskInfo.createTime || '-' }}
        </div>

      </el-card>

      <el-card shadow="hover">

        <div class="label">截止时间</div>

        <div class="value">
          {{ taskInfo.endTime || '-' }}
        </div>

      </el-card>

    </div>

    <!-- 状态修改 -->

    <el-card shadow="never">

      <div class="status-toolbar">

        <el-select
          v-model="statusForm.status"
          style="width:200px"
        >

          <el-option label="待开始" :value="0" />

          <el-option label="进行中" :value="1" />

          <el-option label="已完成" :value="2" />

        </el-select>

        <el-button
          type="primary"
          @click="updateStatus"
        >
          修改状态
        </el-button>

      </div>

    </el-card>

    <!-- Tabs -->

    <el-card shadow="never">

      <el-tabs v-model="activeTab">

        <!-- 评论 -->

        <el-tab-pane
          label="任务评论"
          name="comment"
        >

          <div class="comment-toolbar">

            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="4"
              placeholder="请输入评论内容"
            />

            <el-button
              type="primary"
              @click="submitComment"
            >
              发表评论
            </el-button>

          </div>

          <div
            v-for="item in commentList"
            :key="item.id"
            class="comment-item"
          >

            <div class="comment-user">
              {{ item.username || '用户' }}
            </div>

            <div class="comment-content">
              {{ item.content }}
            </div>

            <div class="comment-time">
              {{ item.createTime }}
            </div>

          </div>

          <el-empty
            v-if="commentList.length === 0"
            description="暂无评论"
          />

        </el-tab-pane>

        <!-- 文件 -->

        <el-tab-pane
          label="任务文件"
          name="file"
        >

          <el-upload
            :action="uploadUrl"
            :headers="headers"
            :data="{ taskId }"
            multiple
            :on-success="handleUploadSuccess"
          >

            <el-button type="primary">
              上传文件
            </el-button>

          </el-upload>

          <el-table
            :data="fileList"
            border
            stripe
            style="margin-top:20px"
          >

            <el-table-column
              prop="fileName"
              label="文件名"
            />

            <el-table-column
              prop="createTime"
              label="上传时间"
              width="180"
            />

            <el-table-column
              label="操作"
              width="120"
            >

              <template #default="scope">

                <el-link
                  type="primary"
                  :href="scope.row.fileUrl"
                  target="_blank"
                >
                  下载
                </el-link>

              </template>

            </el-table-column>

          </el-table>

          <el-empty
            v-if="fileList.length === 0"
            description="暂无文件"
          />

        </el-tab-pane>

      </el-tabs>

    </el-card>

  </div>

</template>

<script setup lang="ts">

import {

  ref,
  reactive,
  onMounted

} from 'vue'

import {

  useRoute

} from 'vue-router'

import {

  ElMessage

} from 'element-plus'

import request from '../utils/request'

const route = useRoute()

const taskId = route.params.id

const activeTab = ref('comment')

const taskInfo = reactive<any>({})

const commentList = ref<any[]>([])

const fileList = ref<any[]>([])

const commentContent = ref('')

const uploadUrl = 'http://localhost:8080/file/upload'

const headers = {
  Authorization:`Bearer ${localStorage.getItem('token')}`
}

const statusForm = reactive({
  taskId:Number(taskId),
  status:0
})

/**
 * 加载任务详情
 */
const loadTaskDetail = async ()=>{

  const res:any = await request({

    url:`/task/detail/${taskId}`,

    method:'get'
  })

  Object.assign(taskInfo,res.data)

  statusForm.status = res.data.status
}

/**
 * 加载评论
 */
const loadCommentList = async ()=>{

  try{

    const res:any = await request({

      url:`/comment/task/${taskId}`,

      method:'get'
    })

    commentList.value = res.data || []

  }catch(error){

    commentList.value = []
  }
}

/**
 * 加载文件
 */
const loadFileList = async ()=>{

  try{

    const res:any = await request({

      url:`/file/task/${taskId}`,

      method:'get'
    })

    fileList.value = res.data || []

  }catch(error){

    fileList.value = []
  }
}

/**
 * 修改状态
 */
const updateStatus = async ()=>{

  await request({

    url:'/task/status',

    method:'put',

    data:statusForm
  })

  ElMessage.success('状态修改成功')

  loadTaskDetail()
}

/**
 * 发表评论
 */
const submitComment = async ()=>{

  if(!commentContent.value){

    ElMessage.warning('请输入评论内容')

    return
  }

  await request({

    url:'/comment/add',

    method:'post',

    data:{
      taskId:Number(taskId),
      content:commentContent.value
    }
  })

  ElMessage.success('评论成功')

  commentContent.value = ''

  loadCommentList()
}

/**
 * 上传成功
 */
const handleUploadSuccess = ()=>{

  ElMessage.success('上传成功')

  loadFileList()
}

onMounted(()=>{

  loadTaskDetail()

  loadCommentList()

  loadFileList()
})

</script>

<style scoped>

.detail-container{

  display:flex;

  flex-direction:column;

  gap:20px;
}

.header{

  display:flex;

  justify-content:space-between;

  align-items:flex-start;
}

.title{

  font-size:28px;

  font-weight:bold;

  margin-bottom:15px;
}

.content{

  color:#666;

  line-height:28px;
}

.statistics{

  display:grid;

  grid-template-columns:repeat(4,1fr);

  gap:20px;
}

.label{

  color:#999;

  margin-bottom:15px;
}

.value{

  font-size:20px;

  font-weight:bold;
}

.status-toolbar{

  display:flex;

  gap:15px;

  align-items:center;
}

.comment-toolbar{

  display:flex;

  flex-direction:column;

  gap:15px;

  margin-bottom:20px;
}

.comment-item{

  border:1px solid #eee;

  border-radius:8px;

  padding:15px;

  margin-bottom:15px;
}

.comment-user{

  font-weight:bold;

  margin-bottom:10px;
}

.comment-content{

  line-height:24px;

  margin-bottom:10px;
}

.comment-time{

  color:#999;

  font-size:13px;
}

</style>