import api from './axios'

export const achievementApi = {
  // 获取成就配置
  getConfigs: () => api.get('/api/achievement/configs'),
  // 获取用户成就
  getUserAchievements: () => api.get('/api/achievement/user'),
  // 更新成就进度
  updateProgress: (achievementType, increment) => api.put('/api/achievement', null, { params: { achievementType, increment } })
}