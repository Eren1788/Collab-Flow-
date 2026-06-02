import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Home from '../views/Home.vue'

import UserList from '../views/UserList.vue'
import ProjectList from '../views/ProjectList.vue'
import TaskList from '../views/TaskList.vue'
import CommentList from '../views/CommentList.vue'
import FileList from '../views/FileList.vue'

/* ========================= */
/* 【新增】导入项目详情页 */
/* ========================= */

import ProjectDetail from '../views/ProjectDetail.vue'

import { useUserStore } from '../store/user'

const routes = [

  /**
   * 登录页
   */
  {
    path:'/login',
    component:Login
  },

  /**
   * 首页布局
   */
  {
    path:'/',
    component:Home,

    redirect:'/users',

    children:[

      /**
       * 用户管理
       */
      {
        path:'users',
        component:UserList
      },

      /**
       * 项目管理
       */
      {
        path:'projects',
        component:ProjectList
      },

      /* ========================= */
      /* 【新增】项目详情页路由 */
      /* ========================= */

      {
        path:'project/detail/:id',
        component:ProjectDetail
      },

      /**
       * 项目成员完成情况统计页
       */
      {
        path:'project/statistics/:id',
        component:() => import('../views/ProjectStatistics.vue')
      },

      /**
       * 任务管理
       */
      {
        path:'tasks',
        component:TaskList
      },

      /**
       * 任务详情页
       */
      {
        path:'/task/detail/:id',
        component:()=>import('../views/TaskDetail.vue')
      },

      /**
       * 评论管理
       */
      {
        path:'comments',
        component:CommentList
      },

      /**
       * 文件管理
       */
      {
        path:'files',
        component:FileList
      }

    ]
  }

]

const router = createRouter({

  history:createWebHistory(),

  routes
})

/**
 * 路由守卫
 */
router.beforeEach((to, from, next) => {

  const userStore = useUserStore()

  /**
   * 未登录
   */
  if(

    to.path !== '/login'

    &&

    !userStore.token

  ){

    next('/login')

    return
  }

  /**
   * 已登录
   * 自动恢复WebSocket连接
   */
  if(

    userStore.token

    &&

    userStore.info?.id

  ){

    userStore.connectWebSocket()
  }

  next()
})

export default router