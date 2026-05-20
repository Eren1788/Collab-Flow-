import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Home from '../views/Home.vue'

import UserList from '../views/UserList.vue'
import ProjectList from '../views/ProjectList.vue'
import TaskList from '../views/TaskList.vue'
import CommentList from '../views/CommentList.vue'
import FileList from '../views/FileList.vue'

const routes = [

  {
    path:'/login',
    component:Login
  },

  {
    path:'/',
    component:Home,

    redirect:'/users',

    children:[

      {
        path:'users',
        component:UserList
      },

      {
        path:'projects',
        component:ProjectList
      },

      {
        path:'tasks',
        component:TaskList
      },

      {
        path:'comments',
        component:CommentList
      },

      // 文件模块
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

export default router