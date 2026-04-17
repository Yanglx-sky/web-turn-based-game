<template>
  <div class="achievement-container">
    <GameTopNav />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">ACHIEVEMENTS</p>
        <h1>成就系统</h1>
      </div>
      
      <!-- 成就列表 -->
      <div class="achievement-content">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="achievements.length === 0" class="no-data">暂无成就</div>
        <div v-else class="achievement-grid">
          <div 
            v-for="achievement in achievements" 
            :key="achievement.achievementId" 
            class="achievement-card"
            :class="{ 'completed': achievement.status === 1 }"
          >
            <div class="achievement-icon">
              <img :src="getAchievementIcon(achievement.achievementId)" :alt="getAchievementName(achievement.achievementId)" />
            </div>
            <div class="achievement-info">
              <h3>{{ getAchievementName(achievement.achievementId) }}</h3>
              <p class="achievement-description">{{ getAchievementDescription(achievement.achievementId) }}</p>
              <div class="achievement-progress">
                <div class="progress-bar">
                  <div 
                    class="progress-fill" 
                    :style="{ width: getProgressPercentage(achievement) + '%' }"
                  ></div>
                </div>
                <span class="progress-text">{{ achievement.currentValue }}/{{ getAchievementTarget(achievement.achievementId) }}</span>
              </div>
              <span v-if="achievement.status === 1" class="completed-text">已完成</span>
              <span v-else class="uncompleted-text">进行中</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- 成就完成弹窗 -->
  <div v-if="showAchievementToast && currentAchievement" class="achievement-toast">
    <div class="toast-content">
      <div class="toast-icon">
        <img :src="getAchievementIcon(currentAchievement.achievementId)" :alt="getAchievementName(currentAchievement.achievementId)" />
      </div>
      <div class="toast-text">
        <h4>成就达成！</h4>
        <p>{{ getAchievementName(currentAchievement.achievementId) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { achievementApi } from '../api/achievement'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()
const user = ref(null)
const achievements = ref([])
const achievementConfigs = ref([])
const loading = ref(false)
const showAchievementToast = ref(false)
const currentAchievement = ref(null)

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('user')
  localStorage.removeItem('token')
  router.push('/auth')
}

// 加载成就配置
const loadAchievementConfigs = async () => {
  try {
    const response = await achievementApi.getConfigs()
    if (response && response.code === 200 && response.data) {
      achievementConfigs.value = response.data
    }
  } catch (error) {
    console.error('加载成就配置失败:', error)
  }
}

// 检查成就状态变化，显示完成弹窗
const checkAchievementStatus = (oldAchievements, newAchievements) => {
  // 从localStorage获取已显示过弹窗的成就ID
  const shownAchievements = JSON.parse(localStorage.getItem('shownAchievements') || '[]')
  
  for (let i = 0; i < newAchievements.length; i++) {
    const newAchievement = newAchievements[i]
    const oldAchievement = oldAchievements.find(a => a.achievementId === newAchievement.achievementId)
    
    // 检查成就是否从未完成变为已完成，且未显示过弹窗
    if (newAchievement.status === 1 && (!oldAchievement || oldAchievement.status === 0) && !shownAchievements.includes(newAchievement.achievementId)) {
      // 显示成就完成弹窗
      currentAchievement.value = newAchievement
      showAchievementToast.value = true
      
      // 将该成就ID添加到已显示列表
      shownAchievements.push(newAchievement.achievementId)
      localStorage.setItem('shownAchievements', JSON.stringify(shownAchievements))
      
      // 3秒后自动关闭弹窗
      setTimeout(() => {
        showAchievementToast.value = false
        currentAchievement.value = null
      }, 3000)
    }
  }
}

// 加载用户成就
const loadUserAchievements = async () => {
  if (!user.value) return
  loading.value = true
  try {
    const response = await achievementApi.getUserAchievements()
    
    console.log('=== 成就调试信息 ===')
    console.log('后端返回的原始数据:', response)
    
    if (response && response.code === 200) {
      console.log('用户成就数据:', response.data)
      
      // 将用户成就数据转换为以achievementId为键的对象
      const userAchievementsMap = {}
      if (response.data) {
        response.data.forEach(achievement => {
          console.log('单个成就:', achievement)
          userAchievementsMap[achievement.achievementId] = {
            ...achievement
          }
        })
      }
      
      console.log('成就配置:', achievementConfigs.value)
      
      // 为每个成就配置创建或更新成就记录
      const allAchievements = achievementConfigs.value.map(config => {
        const userAchievement = userAchievementsMap[config.id]
        console.log(`配置ID ${config.id} 对应的用户成就:`, userAchievement)
        if (userAchievement) {
          return userAchievement
        } else {
          // 创建新的成就记录，初始值为0
          return {
            achievementId: config.id,
            userId: user.value.id,
            currentValue: 0,
            status: 0
          }
        }
      })
      
      console.log('最终成就列表:', allAchievements)
      console.log('=== 调试结束 ===')
      
      // 检查成就状态变化
      checkAchievementStatus([...achievements.value], allAchievements)
      
      achievements.value = allAchievements
    }
  } catch (error) {
    console.error('加载用户成就失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取成就名称
const getAchievementName = (achievementId) => {
  const config = achievementConfigs.value.find(config => config.id === achievementId)
  return config ? config.name : '未知成就'
}

// 获取成就描述
const getAchievementDescription = (achievementId) => {
  const config = achievementConfigs.value.find(config => config.id === achievementId)
  return config ? config.description : '未知描述'
}

// 获取成就目标值
const getAchievementTarget = (achievementId) => {
  const config = achievementConfigs.value.find(config => config.id === achievementId)
  return config ? config.conditionTarget : 0
}

// 获取成就图标
const getAchievementIcon = (achievementId) => {
  const config = achievementConfigs.value.find(config => config.id === achievementId)
  return config && config.icon ? config.icon : '/src/assets/hero.png'
}

// 获取进度百分比
const getProgressPercentage = (achievement) => {
  const target = getAchievementTarget(achievement.achievementId)
  if (target === 0) return 0
  const percentage = (achievement.currentValue / target) * 100
  return Math.min(percentage, 100)
}



onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
    await loadAchievementConfigs()
    await loadUserAchievements()
  } else {
    router.push('/auth')
  }
})
</script>

<style scoped>
/* 全局样式 */
.achievement-container {
  min-height: 100vh;
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
}

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 22px auto 0;
  padding: 0 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  color: #fff4df;
  font-weight: 800;
  font-size: 2.8rem;
  letter-spacing: -0.02em;
  text-shadow: 0 4px 12px rgba(255, 140, 0, 0.4);
}

.section-eyebrow {
  margin: 0;
  color: rgba(255, 220, 162, 0.78);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

/* 成就内容 */
.achievement-content {
  background: transparent;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
  backdrop-filter: none;
  border: none;
}

.loading, .no-data {
  text-align: center;
  padding: 3rem;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.2rem;
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
  padding-bottom: 40px;
}

.achievement-card {
  position: relative;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.96), rgba(16, 9, 3, 0.96));
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.4);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  overflow: visible;
  display: flex;
  gap: 1.2rem;
  align-items: center;
}

.achievement-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 24px 40px rgba(255, 140, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 195, 112, 0.4);
}

/* 成就完成状态 - 金色荣誉 */
.achievement-card.completed {
  border-top-color: #ffd700;
  border-color: rgba(255, 215, 0, 0.4);
  background: linear-gradient(180deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.96));
  box-shadow: 0 16px 28px rgba(255, 215, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.achievement-card.completed:hover {
  box-shadow: 0 24px 40px rgba(255, 215, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 215, 0, 0.6);
}

.achievement-icon {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 169, 79, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 129, 35, 0.3);
  position: relative;
  background: rgba(0, 0, 0, 0.5);
}

.achievement-card.completed .achievement-icon {
  border-color: #ffd700;
  box-shadow: 0 0 15px rgba(255, 215, 0, 0.4);
}

.achievement-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.achievement-info {
  flex: 1;
}

.achievement-info h3 {
  color: #ff9c3a;
  margin: 0 0 0.4rem 0;
  font-size: 1.25rem;
  font-weight: 800;
  text-shadow: 0 2px 4px rgba(255, 140, 0, 0.2);
}

.achievement-card.completed .achievement-info h3 {
  color: #ffd700;
  text-shadow: 0 2px 4px rgba(255, 215, 0, 0.3);
}

.achievement-description {
  color: rgba(247, 239, 224, 0.75);
  margin: 0 0 1rem 0;
  font-size: 0.9rem;
  line-height: 1.4;
}

.achievement-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 0.8rem;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9c3a, #ff7a1a);
  border-radius: 4px;
  transition: width 0.5s ease;
  box-shadow: 0 0 8px rgba(255, 122, 26, 0.6);
}

/* 成就完成进度条 - 金色 */
.achievement-card.completed .progress-fill {
  background: linear-gradient(90deg, #fada5e, #ffd700);
  box-shadow: 0 0 10px rgba(255, 215, 0, 0.6);
}

.progress-text {
  font-size: 0.8rem;
  color: rgba(247, 239, 224, 0.6);
  font-weight: 600;
  align-self: flex-end;
  letter-spacing: 0.05em;
}

.completed-text {
  color: #ffd700;
  font-weight: 700;
  font-size: 0.9rem;
  display: inline-block;
  padding: 4px 10px;
  background: rgba(255, 215, 0, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(255, 215, 0, 0.2);
  text-shadow: 0 0 5px rgba(255, 215, 0, 0.3);
}

.uncompleted-text {
  color: #ff9c3a;
  font-weight: 700;
  font-size: 0.9rem;
  display: inline-block;
  padding: 4px 10px;
  background: rgba(255, 156, 58, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(255, 156, 58, 0.2);
}

/* 成就完成弹窗 */
.achievement-toast {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 1000;
  animation: slideInToast 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

.toast-content {
  background: linear-gradient(135deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.98));
  color: #fff4df;
  padding: 15px 20px;
  border-radius: 16px;
  border: 1px solid rgba(255, 215, 0, 0.4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5), 0 0 15px rgba(255, 215, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 15px;
  min-width: 250px;
  max-width: 350px;
}

.toast-icon {
  width: 48px;
  height: 48px;
  border: 2px solid #ffd700;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 0 10px rgba(255, 215, 0, 0.4);
}

.toast-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.toast-text {
  flex: 1;
}

.toast-text h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 800;
  color: #ffd700;
  text-shadow: 0 0 5px rgba(255, 215, 0, 0.4);
  letter-spacing: 0.05em;
}

.toast-text p {
  margin: 0;
  font-size: 13px;
  color: rgba(247, 239, 224, 0.8);
}

@keyframes slideInToast {
  0% {
    transform: translateX(120%);
    opacity: 0;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .main-content {
    padding: 1rem;
    margin-top: 10px;
  }
  
  .page-header h1 {
    font-size: 2.2rem;
  }
  
  .achievement-grid {
    grid-template-columns: 1fr;
  }
  
  .achievement-card {
    flex-direction: row;
    align-items: flex-start;
  }
  
  .achievement-toast {
    top: 70px;
    right: 10px;
    left: 10px;
  }
  
  .toast-content {
    max-width: 100%;
  }
}
</style>
