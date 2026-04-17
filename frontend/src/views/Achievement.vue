<template>
  <div class="achievement-container">
    <!-- 顶部导航栏 -->
    <nav class="nav-bar">
      <div class="nav-logo">洛克王国</div>
      <div class="nav-menu">
        <button class="nav-btn" @click="navigateTo('/')">首页</button>
        <button class="nav-btn" @click="navigateTo('/elves')">我的精灵</button>
        <button class="nav-btn" @click="navigateTo('/pve')">冒险</button>
        <button class="nav-btn" @click="navigateTo('/shop')">商店</button>
        <button class="nav-btn" @click="navigateTo('/bag')">背包</button>
        <button class="nav-btn" @click="navigateTo('/train')">训练</button>
        <button class="nav-btn" @click="navigateTo('/rank')">排行榜</button>
        <button class="nav-btn" @click="navigateTo('/ai')">AI助手</button>
        <button class="nav-btn" @click="navigateTo('/chat')">聊天</button>
        <button class="nav-btn active" @click="navigateTo('/achievement')">成就</button>
        <button class="nav-btn" @click="logout">退出</button>
      </div>
    </nav>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <h2 class="page-title">成就系统</h2>
      
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
  background-image: url('https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=colorful%20fantasy%20game%20background%20with%20magical%20elements%20and%20floating%20islands&image_size=landscape_16_9');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Arial', sans-serif;
}

/* 导航栏 */
.nav-bar {
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #ff6b00;
  text-shadow: 0 0 10px rgba(255, 107, 0, 0.5);
}

.nav-menu {
  display: flex;
  gap: 1rem;
}

.nav-btn {
  background: transparent;
  color: white;
  border: 1px solid #ff6b00;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
}

.nav-btn:hover {
  background: #ff6b00;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.4);
}

.nav-btn.active {
  background: #ff6b00;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.4);
}

/* 主内容区 */
.main-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
}

.page-title {
  color: #ff6b00;
  text-align: center;
  margin-bottom: 2rem;
  font-size: 2rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

/* 成就内容 */
.achievement-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 107, 0, 0.3);
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.2rem;
}

.no-data {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.2rem;
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.achievement-card {
  background: white;
  border-radius: 15px;
  padding: 1.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 107, 0, 0.2);
  display: flex;
  gap: 1rem;
}

.achievement-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(255, 107, 0, 0.3);
}

.achievement-card.completed {
  border-color: #4CAF50;
  background: rgba(76, 175, 80, 0.05);
}

.achievement-icon {
  width: 80px;
  height: 80px;
  border-radius: 0;
  overflow: hidden;
  border: 2px solid #ff6b00;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.achievement-icon img {
  width: 60px;
  height: 60px;
  object-fit: cover;
}

.achievement-info {
  flex: 1;
}

.achievement-info h3 {
  color: #333;
  margin-bottom: 0.5rem;
  font-size: 1.2rem;
}

.achievement-description {
  color: #666;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  line-height: 1.4;
}

.achievement-progress {
  margin-bottom: 1rem;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff6b00, #ff9e4f);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.achievement-card.completed .progress-fill {
  background: linear-gradient(90deg, #4CAF50, #81C784);
}

.progress-text {
  font-size: 0.8rem;
  color: #666;
  font-weight: 600;
}

.claim-btn {
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
  font-weight: 600;
}

.claim-btn:hover {
  background: linear-gradient(135deg, #388E3C 0%, #66BB6A 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.4);
}

.claimed-text {
  color: #4CAF50;
  font-weight: 600;
  font-size: 0.9rem;
}

.completed-text {
  color: #4CAF50;
  font-weight: 600;
  font-size: 0.9rem;
}

.uncompleted-text {
  color: #ff6b00;
  font-weight: 600;
  font-size: 0.9rem;
}

/* 成就完成弹窗 */
.achievement-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  animation: slideIn 0.5s ease-out;
}

.toast-content {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  padding: 15px 20px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
  display: flex;
  align-items: center;
  gap: 15px;
  min-width: 200px;
  max-width: 300px;
}

.toast-icon {
  width: 50px;
  height: 50px;
  border: 2px solid white;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.toast-icon img {
  width: 35px;
  height: 35px;
  object-fit: cover;
}

.toast-text {
  flex: 1;
}

.toast-text h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  font-weight: bold;
}

.toast-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
  
  .nav-menu {
    gap: 0.5rem;
  }
  
  .nav-btn {
    padding: 0.3rem 0.6rem;
    font-size: 0.8rem;
  }
  
  .achievement-grid {
    grid-template-columns: 1fr;
  }
  
  .achievement-card {
    flex-direction: column;
    text-align: center;
  }
  
  .achievement-icon {
    margin: 0 auto;
  }
  
  .achievement-toast {
    top: 10px;
    right: 10px;
    left: 10px;
  }
  
  .toast-content {
    max-width: 100%;
  }
}
</style>