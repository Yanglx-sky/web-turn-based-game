<template>
  <div class="rank-container">
    <GameTopNav />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">LEADERBOARD</p>
        <h1>排行榜</h1>
      </div>
      
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
      
      <div class="rank-panels-wrapper" :class="{ 'has-sidebar': userRankData }">
        <!-- 排行榜数据 -->
        <div class="rank-content stage-panel">
          <div class="panel-heading">
            <div>
              <h3>排名详情</h3>
            </div>
          </div>
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="rankData.length === 0" class="no-data">暂无数据</div>
          <div v-else class="rank-list">
            <div
              v-for="(item, index) in rankData"
              :key="item.userId"
              class="rank-item"
              :class="[
                { 'current-user': item.userId === user?.id },
                getRankPositionClass(index + 1)
              ]"
            >
              <div class="rank-number" :class="getRankPositionClass(index + 1)">{{ index + 1 }}</div>
              <div class="rank-info">
                <div class="rank-user">{{ item.nickname || '未知用户' }}</div>
              </div>
              <div class="rank-score-container">
                <span class="score-label">分数</span>
                <div class="rank-score">{{ item.score || 0 }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 个人排名 -->
        <div class="user-rank stage-panel" v-if="userRankData">
          <div class="panel-heading">
            <div>
              <p class="section-eyebrow">MY RANKING</p>
              <h3>我的排名</h3>
            </div>
          </div>
          <div class="user-rank-info">
            <div class="user-rank-number-glass">
              <span class="label">当前名次</span>
              <div class="user-rank-number">{{ userRankData.rankNum }}</div>
            </div>
          </div>
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
import GameTopNav from '../components/GameTopNav.vue'

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
      if (rankConfigs.value.length > 0 && !activeRankType.value) {
        activeRankType.value = rankConfigs.value[0].rankType
        await loadRankData(activeRankType.value)
        await loadUserRankData(activeRankType.value)
      }
    }
  } catch (error) {
    console.error('加载排行榜配置失败:', error)
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
/* 全局样式匹配 Home/PVE */
.rank-container {
  min-height: 100vh;
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
  font-family: 'Arial', sans-serif;
}

.main-content {
  max-width: 1200px;
  margin: 22px auto 0;
  padding: 0 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 标题区 */
.page-header {
  text-align: center;
  margin-bottom: 0.5rem;
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

/* 排行榜类型选择 Tab */
.rank-tabs {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.rank-tab {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 194, 107, 0.12);
  padding: 0.8rem 2rem;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(247, 239, 224, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.rank-tab:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
  border-color: rgba(255, 194, 107, 0.3);
}

.rank-tab.active {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  border-color: transparent;
  box-shadow: 0 8px 16px rgba(255, 132, 29, 0.3);
}

/* 内容面板布局 */
.rank-panels-wrapper {
  display: grid;
  gap: 20px;
  align-items: start;
}

.rank-panels-wrapper.has-sidebar {
  grid-template-columns: minmax(0, 1fr) 350px;
}

.stage-panel {
  position: relative;
  padding: 26px;
  border-radius: 28px;
  border: 1px solid rgba(255, 191, 112, 0.14);
  background: rgba(11, 15, 24, 0.86);
  box-shadow: 0 16px 28px rgba(4, 8, 15, 0.24);
  overflow: hidden;
}

.panel-heading {
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 191, 112, 0.08);
  padding-bottom: 12px;
}

.panel-heading h3 {
  margin: 0;
  color: #fff4df;
  font-size: 1.4rem;
}

/* 列表区 */
.loading, .no-data {
  text-align: center;
  padding: 3rem;
  color: rgba(255, 255, 255, 0.6);
  font-size: 1.1rem;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  border: 1px solid rgba(255, 194, 107, 0.08);
  transition: all 0.3s ease;
}

.rank-item:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 194, 107, 0.2);
}

.rank-item.current-user {
  background: rgba(255, 156, 58, 0.1);
  border-color: rgba(255, 156, 58, 0.4);
  box-shadow: 0 4px 12px rgba(255, 156, 58, 0.15);
}

.rank-item.position-gold {
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.15), rgba(255, 215, 0, 0.02));
  border-left: 4px solid #ffd700;
  border-color: rgba(255, 215, 0, 0.3) rgba(255, 215, 0, 0.1) rgba(255, 215, 0, 0.1) #ffd700;
}

.rank-item.position-silver {
  background: linear-gradient(90deg, rgba(192, 192, 192, 0.15), rgba(192, 192, 192, 0.02));
  border-left: 4px solid #c0c0c0;
  border-color: rgba(192, 192, 192, 0.3) rgba(192, 192, 192, 0.1) rgba(192, 192, 192, 0.1) #c0c0c0;
}

.rank-item.position-bronze {
  background: linear-gradient(90deg, rgba(205, 127, 50, 0.15), rgba(205, 127, 50, 0.02));
  border-left: 4px solid #cd7f32;
  border-color: rgba(205, 127, 50, 0.3) rgba(205, 127, 50, 0.1) rgba(205, 127, 50, 0.1) #cd7f32;
}

.rank-number {
  width: 50px;
  text-align: center;
  font-size: 1.4rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.5);
  font-family: 'Arial', sans-serif;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.rank-number.position-gold { color: #ffd700; text-shadow: 0 0 10px rgba(255, 215, 0, 0.5); }
.rank-number.position-silver { color: #c0c0c0; text-shadow: 0 0 10px rgba(192, 192, 192, 0.5); }
.rank-number.position-bronze { color: #cd7f32; text-shadow: 0 0 10px rgba(205, 127, 50, 0.5); }

.rank-info {
  flex: 1;
  padding-left: 10px;
}

.rank-user {
  font-size: 1.15rem;
  font-weight: 700;
  color: #fff4df;
  letter-spacing: 0.02em;
}

.rank-score-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.score-label {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.4);
  text-transform: uppercase;
  margin-bottom: 2px;
}

.rank-score {
  font-size: 1.25rem;
  font-weight: 800;
  color: #ff9c3a;
  text-shadow: 0 2px 4px rgba(255, 140, 0, 0.2);
}

/* 个人排名 */
.user-rank .panel-heading {
  text-align: center;
  border-bottom: none;
  margin-bottom: 10px;
}

.user-rank .panel-heading h3 {
  color: #ff9c3a;
  font-size: 1.8rem;
  margin-top: 5px;
}

.user-rank-info {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}

.user-rank-number-glass {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 194, 107, 0.2);
  border-radius: 20px;
  padding: 30px 40px;
  text-align: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 0 20px rgba(255, 156, 58, 0.05);
  width: 100%;
}

.user-rank-number-glass .label {
  display: block;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 10px;
  letter-spacing: 0.1em;
}

.user-rank-number {
  font-size: 3.5rem;
  font-weight: 900;
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 4px 8px rgba(255, 140, 0, 0.3));
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .rank-panels-wrapper.has-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>
