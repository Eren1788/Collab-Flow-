import request from '../utils/request'

export function loginApi(data:any){

  return request({

    url:'/user/login',

    method:'post',

    data
  })
}

export function getUserListApi(){

  return request({

    url:'/user/list',

    method:'get'
  })
}