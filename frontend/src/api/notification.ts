import request from '../utils/request'

/**
 * 我的通知
 */
export const getMyNotifications = () => {

  return request({

    url:'/notification/my',

    method:'get'
  })
}

/**
 * 未读数量
 */
export const getUnreadCount = () => {

  return request({

    url:'/notification/unread/count',

    method:'get'
  })
}

/**
 * 已读
 */
export const readNotification = (id:number) => {

  return request({

    url:`/notification/read/${id}`,

    method:'put'
  })
}

/**
 * 全部已读
 */
export const readAllNotification = () => {

  return request({

    url:'/notification/read/all',

    method:'put'
  })
}