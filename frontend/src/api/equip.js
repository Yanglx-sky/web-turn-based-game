import api from './axios'

export const equipApi = {
  // 获取所有装备
  getAllEquips: () => {
    return api.get('/api/equip/all')
  },
  
  // 根据类型获取装备
  getEquipsByType: (type) => {
    return api.get(`/api/equip/type/${type}`)
  },
  
  // 获取装备详情
  getEquipById: (id) => {
    return api.get(`/api/equip/detail/${id}`)
  },
  
  // 购买装备
  buyEquip: (equipId) => {
    return api.post('/api/equip/buy', {}, {
      params: {
        equipId
      }
    })
  },
  
  // 装备武器
  equipWeapon: (userId, elfId, weaponId) => {
    return api.post('/api/equip/weapon/equip', {}, {
      params: {
        elfId,
        weaponId
      }
    })
  },
  
  // 装备防具
  equipArmor: (userId, elfId, armorId) => {
    return api.post('/api/equip/armor/equip', {}, {
      params: {
        elfId,
        userBagId: armorId
      }
    })
  },
  
  // 卸下武器
  unequipWeapon: (elfId) => {
    return api.post('/api/equip/weapon/unequip', {}, {
      params: {
        elfId
      }
    })
  },
  
  // 卸下防具
  unequipArmor: (elfId) => {
    return api.post('/api/equip/armor/unequip', {}, {
      params: {
        elfId
      }
    })
  },
  
  // 获取用户拥有的指定类型装备
  getUserEquipsByType: (type) => {
    return api.get('/api/equip/user/type', {
      params: {
        type
      }
    })
  }
}