import api from './axios'

export const potionApi = {
  // 获取所有药品
  getAllPotions: () => {
    return api.get('/api/potion/all')
  },
  
  // 获取药品详情
  getPotionById: (id) => {
    return api.get(`/api/potion/detail/${id}`)
  },
  
  // 获取用户拥有的药品
  getUserPotions: () => {
    return api.get('/api/potion/user')
  },
  
  // 使用药品
  usePotion: (elfId, potionConfigId) => {
    return api.post('/api/potion/use', {}, {
      params: {
        elfId,
        potionId: potionConfigId
      }
    })
  },
  
  // 增加用户药品数量
  addUserPotion: (potionConfigId, count) => {
    return api.post('/api/potion/add', {}, {
      params: {
        potionConfigId,
        count
      }
    })
  },
  
  // 减少用户药品数量
  reduceUserPotion: (potionConfigId, count) => {
    return api.post('/api/potion/reduce', {}, {
      params: {
        potionConfigId,
        count
      }
    })
  }
}