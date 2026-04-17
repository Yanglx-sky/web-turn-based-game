<template>
  <div class="rank-container">
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
        <button class="nav-btn active" @click="navigateTo('/rank')">排行榜</button>
        <button class="nav-btn" @click="navigateTo('/ai')">AI助手</button>
        <button class="nav-btn" @click="navigateTo('/chat')">聊天</button>
        <button class="nav-btn" @click="navigateTo('/achievement')">成就</button>
        <button class="nav-btn" @click="logout">退出</button>
      </div>
    </nav>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <h2 class="page-title">排行榜</h2>
      
      <!-- 排行榜类型选择 -->
      <div class="rank-tabs">
        <button 
          v-for="config in rankConfigs" 
          :key="config.id" 
          class="rank-tab" 
          :class="{ active: activeRankType === config.rankType }"
          @click="selectRankType(config.rankType)"
        >
          {{ config.rankName }}
        </button>
      </div>
      
      <!-- 排行榜数据 -->
      <div class="rank-content">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="rankData.length === 0" class="no-data">暂无数据</div>
        <div v-else class="rank-list">
          <div
            v-for="(item, index) in rankData"
            :key="item.id"
            class="rank-item"
            :class="[
              { 'current-user': item.userId === user?.id },
              getRankPositionClass(index + 1)
            ]"
          >
            <div class="rank-number" :class="getRankPositionClass(index + 1)">{{ index + 1 }}</div>
            <div class="rank-user">{{ item.nickname }}</div>
            <div class="rank-score">{{ item.score }}</div>
          </div>
        </div>
      </div>
      
      <!-- 个人排名 -->
      <div class="user-rank" v-if="userRankData">
        <h3>我的排名</h3>
        <div class="user-rank-info">
          <div class="user-rank-number">{{ userRankData.rankNum }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { rankApi } from '../api/rank'
import { userApi } from '../api/user'

const router = useRouter()
const user = ref(null)
const rankConfigs = ref([])
const rankData = ref([])
const userRankData = ref(null)
const activeRankType = ref('')
const loading = ref(false)

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

// 根据排名位置获取CSS类名
const getRankPositionClass = (position) => {
  if (position === 1) return 'position-gold'
  if (position === 2) return 'position-silver'
  if (position === 3) return 'position-bronze'
  return ''
}

// 选择排行榜类型
const selectRankType = async (rankType) => {
  activeRankType.value = rankType
  await loadRankData(rankType)
  await loadUserRankData(rankType)
}

// 加载排行榜配置
const loadRankConfigs = async () => {
  try {
    const response = await rankApi.getConfigs()
    if (response && response.code === 200 && response.data) {
      rankConfigs.value = response.data
      if (response.data.length > 0 && !activeRankType.value) {
        activeRankType.value = response.data[0].rankType
        await loadRankData(activeRankType.value)
        await loadUserRankData(activeRankType.value)
      }
    }
  } catch (error) {
    console.error('加载排行榜配置失败:', error)
  }
}

// 加载用户信息
const loadUserInfo = async (userId) => {
  try {
    // 注意：这里可能需要修改，因为userApi.getUserInfo()现在不接受userId参数
    // 暂时返回默认值，实际项目中可能需要调整API设计
    return '用户'
  } catch (error) {
    console.error('加载用户信息失败:', error)
    return '未知用户'
  }
}

// 加载排行榜数据
const loadRankData = async (rankType) => {
  loading.value = true
  try {
    const response = await rankApi.getRankList(rankType)
    if (response && response.code === 200 && response.data) {
      rankData.value = response.data
    }
  } catch (error) {
    console.error('加载排行榜数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载用户排名数据
const loadUserRankData = async (rankType) => {
  if (!user.value) return
  try {
    const response = await rankApi.getUserRank(rankType)
    if (response && response.code === 200 && response.data) {
      userRankData.value = response.data
    }
  } catch (error) {
    console.error('加载用户排名数据失败:', error)
  }
}

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
    await loadRankConfigs()
  } else {
    router.push('/auth')
  }
})
</script>

<style scoped>
/* 全局样式 */
.rank-container {
  min-height: 100vh;
  background-image: url('https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=colorful%20fantasy%20game%20background%20with%20magical%20elements%20and%20floating%20islands&image_size=landscape_16_9');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Arial', sans-serif;
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

/* 排行榜类型选择 */
.rank-tabs {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.rank-tab {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #ff6b00;
  padding: 0.8rem 1.5rem;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.rank-tab:hover {
  background: #ff6b00;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.4);
}

.rank-tab.active {
  background: #ff6b00;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.4);
}

/* 排行榜内容 */
.rank-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  margin-bottom: 2rem;
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

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  background: white;
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.rank-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.rank-item.current-user {
  background: var(--color-brand-bg);
  border-color: var(--color-brand);
}

/* 排名位置颜色 - 金银铜 */
.rank-item.position-gold {
  background: var(--color-gold-bg);
  border-color: var(--color-gold);
  box-shadow: var(--shadow-md), 0 0 20px oklch(0.80 0.15 85 / 0.3);
}

.rank-item.position-silver {
  background: var(--color-silver-bg);
  border-color: var(--color-silver);
}

.rank-item.position-bronze {
  background: var(--color-bronze-bg);
  border-color: var(--color-bronze);
}

.rank-number {
  width: 50px;
  text-align: center;
  font-size: 1.2rem;
  font-weight: bold;
  color: var(--color-brand);
}

.rank-number.position-gold {
  color: var(--color-gold);
}

.rank-number.position-silver {
  color: var(--color-silver);
}

.rank-number.position-bronze {
  color: var(--color-bronze);
}

.rank-user {
  flex: 1;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-neutral-700);
}

.rank-score {
  width: 100px;
  text-align: right;
  font-size: 1.2rem;
  font-weight: bold;
  color: #4CAF50;
}

/* 个人排名 */
.user-rank {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(76, 175, 80, 0.3);
  text-align: center;
}

.user-rank h3 {
  color: #4CAF50;
  margin-bottom: 1rem;
  font-size: 1.5rem;
}

.user-rank-info {
  display: flex;
  justify-content: center;
  gap: 2rem;
  align-items: center;
}

.user-rank-number {
  font-size: 2rem;
  font-weight: bold;
  color: #ff6b00;
}

.user-rank-score {
  font-size: 2rem;
  font-weight: bold;
  color: #4CAF50;
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
  
  .rank-tabs {
    flex-direction: column;
    align-items: center;
  }
  
  .rank-tab {
    width: 100%;
    max-width: 300px;
    text-align: center;
  }
  
  .rank-item {
    flex-direction: column;
    gap: 0.5rem;
    text-align: center;
  }
  
  .rank-number, .rank-score {
    width: 100%;
    text-align: center;
  }
  
  .user-rank-info {
    flex-direction: column;
    gap: 1rem;
  }
}
</style>