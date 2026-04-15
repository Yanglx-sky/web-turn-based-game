import api from './axios'

export const shopApi = {
  getAllItems: () => {
    return api.get('/api/equips')
  },
  
  getItemsByType: (type) => {
    return api.get(`/api/equips/type/${type}`)
  },
  
  buyItem: (shopItemId) => {
    return api.post('/api/equips', null, {
      params: {
        equipId: shopItemId
      }
    })
  },
  
  getUserEquipsByType: (userId, type) => {
    return api.get('/api/equips/user/type', {
      params: {
        type
      }
    })
  }
}