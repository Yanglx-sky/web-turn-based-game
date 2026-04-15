import api from './axios'

export const trainApi = {
  // 创建训练人偶
  createMannequin: (attack, defense, hp, mp, type, isAttack) => api.post(`/api/train/create`, { attack, defense, hp, mp, type, isAttack }),
  // 开始训练
  startTrain: (mannequinId) => api.post(`/api/train/start`, { mannequinId }),
  // 普通攻击
  normalAttack: () => api.post(`/api/train/attack`),
  // 使用技能
  useSkill: (skillId) => api.post(`/api/train/skill`, { skillId }),
  // 训练逃跑
  flee: () => api.post(`/api/train/flee`),
  // 切换精灵
  switchElf: (elfId) => api.post(`/api/train/switch`, { elfId }),
  // 训练结算
  trainSettlement: () => api.post(`/api/train/settlement`),
  // 获取训练记录
  getTrainRecords: () => api.get(`/api/train/records`)
}