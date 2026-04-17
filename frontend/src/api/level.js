import api from './axios'

export const levelApi = {
  // 获取关卡列表
  getLevelList: () => api.get('/api/levels'),
  // 获取关卡信息
  getLevelInfo: (levelId) => api.get(`/api/levels/${levelId}`),
  // 进入关卡（自动使用玩家配置的出战精灵列表）
  enterLevel: (levelId) => api.post('/api/levels/enter', { levelId })
}