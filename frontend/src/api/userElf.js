import api from './axios'

export const userElfApi = {
  // 获取用户精灵列表
  list: () => {
    return api.get('/user-elf/list')
  },
  
  // 获取出战精灵列表
  getBattleElves: () => {
    return api.get('/user-elf/battle-elves')
  },
  
  // 设置出战精灵
  setActive: (elfId, fightOrder) => {
    return api.post('/user-elf/set-battle', { elfId, fightOrder })
  },
  
  // 精灵升级
  upgrade: (elfId) => {
    return api.post('/user-elf/upgrade', { elfId })
  },
  
  // 解锁技能
  unlockSkill: (elfId, skillId) => {
    return api.post('/user-elf/unlock-skill', { elfId, skillId })
  },
  
  // 获取精灵详情
  getDetail: (elfId) => {
    return api.get('/user-elf/detail', { params: { elfId } })
  },
  
  // 创建精灵
  create: (elfId) => {
    return api.post(`/user-elf/create?elfId=${elfId}`)
  }
}