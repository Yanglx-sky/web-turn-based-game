import api from './axios'

export const elfApi = {
  // 获取精灵列表
  getElfList: () => api.get(`/user-elf/list`),
  // 设置出战精灵
  setBattleElf: (elfId) => api.get(`/user-elf/set-battle?elfId=${elfId}`),
  // 精灵升级
  upgradeElf: (elfId) => api.get(`/user-elf/upgrade?elfId=${elfId}`),
  // 查看精灵详情
  getElfDetail: (elfId) => api.get(`/user-elf/detail?elfId=${elfId}`),
  // 获取御三家精灵
  getStarterElves: () => api.get(`/elf/starter`)
}