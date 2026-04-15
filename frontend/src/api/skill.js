import api from './axios'

export const skillApi = {
  // 获取技能列表
  getSkillList: () => api.get('/api/skill'),
  // 获取精灵技能
  getElfSkills: (elfId) => api.get(`/api/skill/elves/${elfId}`),
  // 解锁技能
  unlockSkill: (elfId, skillId) => api.post('/api/skill/unlock', null, { params: { elfId, skillId } }),
  // 获取已解锁技能
  getUnlockedSkills: (elfId) => api.get(`/api/skill/elves/${elfId}/unlocked`)
}