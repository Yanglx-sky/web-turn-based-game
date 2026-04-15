import api from './axios'

export const userApi = {
  // 注册
  register: (data) => api.post('/api/users/register', data),
  // 登录
  login: (data) => api.post('/api/users/login', data),
  // 获取用户信息
  getUserInfo: () => api.get('/api/users/me'),
  // 修改密码
  updatePassword: (data) => api.put('/api/users/me/password', data),
  // 修改资料
  updateUserInfo: (data) => api.put('/api/users/me', data),
  // 获取用户精灵数量
  getElfCount: () => api.get('/api/users/me/elves/count'),
  // 获取用户资产
  getUserAsset: () => api.get('/api/users/me/assets')
}