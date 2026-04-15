import api from './axios'

export const skillApi = {
  // 获取技能列表
  getSkillList: () => api.get('/skill/list'),
  // 获取精灵技能
  getElfSkills: (elfId) => api.get(`/skill/elf-skills?elfId=${elfId}`),
  // 解锁技能
  unlockSkill: (elfId, skillId) => api.get(`/skill/unlock?elfId=${elfId}&skillId=${skillId}`),
  // 获取已解锁技能
  getUnlockedSkills: (elfId) => api.get(`/skill/unlocked?elfId=${elfId}`)
}