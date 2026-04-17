<template>
  <div class="home-container">
    <GameTopNav />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <h1>关卡挑战</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="levels-list">
        <!-- 从数据库读取的关卡 -->
        <div v-for="level in levels" :key="level.id" :class="['level-card', { 'locked': level.id > userCurrentLevel }]">
          <h3>关卡 {{ level.id }}</h3>
          <p>{{ level.levelName }}</p>
          <p>经验奖励: {{ level.rewardExp }}</p>
          <p>金币奖励: {{ level.rewardGold || 10 }}</p>
          <!-- 显示星级和评分 -->
          <div v-if="userLevelStars[level.id]" class="level-stars">
            <div class="stars">
              <span v-for="i in 3" :key="i" :class="['star', { 'active': i <= userLevelStars[level.id].star }]">
                ★
              </span>
            </div>
            <p class="score">评分: {{ userLevelStars[level.id].score }}</p>
          </div>
          <div v-if="level.id > userCurrentLevel" class="locked-overlay">
            <p>未解锁</p>
          </div>
          <button v-else @click="openBattleModal(level.id)">进入关卡</button>
        </div>
      </div>
    </div>
    
    <!-- 出战精灵配置弹窗 -->
    <div v-if="showBattleModal" class="modal-overlay">
      <div class="modal-content">
        <h2>出战精灵配置</h2>
        <div v-if="battleElvesLoading" class="loading">加载中...</div>
        <div v-else-if="battleElvesError" class="error">{{ battleElvesError }}</div>
        <div v-else class="battle-elves-list">
          <div v-for="elf in sortedBattleElves" :key="elf.id" class="elf-card">
            <div class="elf-image">
              <img src="../assets/photo/sasuke/佐助.jpg" alt="精灵 1" v-if="elf.elfId === 1" />
              <img src="../assets/photo/zhaomeiming/照美冥.webp" alt="精灵 2" v-else-if="elf.elfId === 2" />
              <img src="../assets/photo/qianshouzhujian/千手柱间.jpg" alt="精灵 3" v-else-if="elf.elfId === 3" />
              <img src="../assets/hero.png" alt="精灵" v-else />
            </div>
            <h4>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h4>
            <p>等级: {{ elf.level }}</p>
            <p>HP: {{ elf.maxHp }}</p>
            <p>MP: {{ elf.maxMp }}</p>
            <p>出战顺序: {{ elf.fightOrder }}</p>
          </div>
          <div v-if="battleElves.length === 0" class="no-elves">
            <p>您还没有设置出战精灵</p>
            <button @click="navigateTo('/elves')">去设置</button>
          </div>
        </div>
        
        <!-- 战斗策略推荐 -->
        <div class="strategy-section" v-if="battleElves.length > 0">
          <h3>AI战斗策略推荐</h3>
          <div v-if="strategyLoading" class="loading">加载中...</div>
          <div v-else-if="strategyError" class="error">{{ strategyError }}</div>
          <div v-else-if="strategyRecommendation" class="strategy-content">
            <p>{{ strategyRecommendation }}</p>
          </div>
          <div v-else class="no-strategy">
            <p>无法生成战斗策略推荐</p>
          </div>
        </div>
        <div class="modal-buttons">
          <button @click="cancelBattle" class="cancel-btn">取消</button>
          <button @click="confirmBattle" class="confirm-btn">确定出战</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { levelApi } from '../api/level'
import { userElfApi } from '../api/userElf'
import { battleApi } from '../api/battle'
import api from '../api/axios'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/auth')
}

const levels = ref([])
const loading = ref(true)
const error = ref('')
const userCurrentLevel = ref(1) // 用户当前解锁的关卡（= 已通过关卡 + 1）
const userLevelStars = ref({}) // 用户关卡星级和评分

// 出战精灵配置弹窗相关
const showBattleModal = ref(false)
const battleElves = ref([])
const battleElvesLoading = ref(false)
const battleElvesError = ref('')
const selectedLevelId = ref(null)
// 战斗策略推荐相关
const strategyRecommendation = ref('')
const strategyLoading = ref(false)
const strategyError = ref('')

// 按战斗顺序排序的出战精灵
const sortedBattleElves = computed(() => {
  return [...battleElves.value].sort((a, b) => a.fightOrder - b.fightOrder)
})

const fetchLevels = async () => {
  try {
    loading.value = true
    // 获取关卡列表
    const response = await levelApi.getLevelList()
    if (response.code === 200) {
      levels.value = response.data
    }
    
    // 获取用户当前关卡信息
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (user.currentLevel !== undefined && user.currentLevel !== null) {
        // currentLevel是已通过的关卡，解锁关卡 = currentLevel + 1
        userCurrentLevel.value = user.currentLevel + 1
      }
      
      // 获取用户关卡星级和评分
      try {
        const levelStarsResponse = await api.get('/api/users/me/level-stars')
        if (levelStarsResponse.code === 200) {
          // 将数据转换为以关卡ID为键的对象
          const starsMap = {}
          levelStarsResponse.data.forEach(item => {
            starsMap[item.levelId] = {
              star: item.star,
              score: item.maxScore
            }
          })
          userLevelStars.value = starsMap
        }
      } catch (err) {
        console.error('获取用户关卡星级失败:', err)
      }
    }
  } catch (err) {
    error.value = '获取关卡列表失败'
    console.error('获取关卡列表失败:', err)
  } finally {
    loading.value = false
  }
}

const fetchBattleElves = async () => {
  try {
    battleElvesLoading.value = true
    battleElvesError.value = ''
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      battleElvesError.value = '请先登录'
      return
    }
    const user = JSON.parse(userStr)
    const response = await userElfApi.getBattleElves()
    if (response.code === 200) {
      battleElves.value = response.data
      // 获取战斗策略推荐
      await fetchStrategyRecommendation(selectedLevelId.value)
    } else {
      battleElvesError.value = '获取出战精灵失败'
    }
  } catch (err) {
    battleElvesError.value = '网络错误，获取出战精灵失败'
    console.error('获取出战精灵失败:', err)
  } finally {
    battleElvesLoading.value = false
  }
}

// 获取战斗策略推荐
const fetchStrategyRecommendation = async (levelId) => {
  try {
    strategyLoading.value = true
    strategyError.value = ''
    const response = await battleApi.getStrategyRecommendation(levelId)
    if (response.code === 200) {
      strategyRecommendation.value = response.data.recommendation
    } else {
      strategyError.value = '获取战斗策略推荐失败'
    }
  } catch (err) {
    strategyError.value = '网络错误，获取战斗策略推荐失败'
    console.error('获取战斗策略推荐失败:', err)
  } finally {
    strategyLoading.value = false
  }
}

const openBattleModal = (levelId) => {
  selectedLevelId.value = levelId
  fetchBattleElves()
  showBattleModal.value = true
}

const cancelBattle = () => {
  showBattleModal.value = false
  selectedLevelId.value = null
}

const confirmBattle = () => {
  // 检查是否有出战精灵
  if (battleElves.value.length === 0) {
    alert('请先设置出战精灵')
    return
  }
  // 进入选定的关卡（后端会自动读取玩家的出战精灵列表）
  if (!selectedLevelId.value) {
    alert('参数错误，请重试')
    return
  }
  showBattleModal.value = false
  router.push(`/battle/${selectedLevelId.value}`)
  selectedLevelId.value = null
}

const enterLevel = (levelId) => {
  router.push(`/battle/${levelId}`)
}

onMounted(() => {
  fetchLevels()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: white;
  padding: 20px;
}

.main-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

h1 {
  text-align: center;
  margin-bottom: 2rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.loading, .error {
  text-align: center;
  padding: 2rem;
  font-size: 1.2rem;
}

.error {
  color: #f44336;
}

.levels-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.level-card {
  background: var(--color-neutral-100);
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
  text-align: center;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  overflow: hidden;
  min-height: 250px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.level-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

/* 难度颜色 - 根据关卡ID递增 */
.level-card:nth-child(3n+1) {
  border-color: var(--color-success);
}

.level-card:nth-child(3n+2) {
  border-color: var(--color-warning);
}

.level-card:nth-child(3n+3) {
  border-color: var(--color-error);
}

.level-card.locked {
  opacity: 0.5;
  background: var(--color-neutral-300);
  border-color: var(--color-neutral-400);
}

.level-card.locked:hover {
  transform: none;
  box-shadow: var(--shadow-sm);
}

.locked-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 8px;
  backdrop-filter: blur(2px);
}

.locked-overlay p {
  color: white;
  font-size: 1.5rem;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
  background: rgba(100, 100, 100, 0.8);
  padding: 10px 20px;
  border-radius: 20px;
  border: 2px solid white;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.level-card h3 {
  margin-top: 0;
  color: #333;
  font-size: 1.2rem;
}

.level-card p {
  margin: 0.5rem 0;
  color: #666;
  font-size: 0.9rem;
}

.level-stars {
  margin: 10px 0;
}

.stars {
  display: flex;
  justify-content: center;
  margin-bottom: 5px;
}

.star {
  font-size: 1.2rem;
  color: #ddd;
  margin: 0 2px;
}

.star.active {
  color: #ffd700;
}

.score {
  font-size: 0.8rem;
  color: #666;
  margin: 0;
}

button {
  margin-top: 1rem;
  padding: 0.75rem 1.5rem;
  background-color: #ff8c00;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease, transform 0.1s ease;
}

button:hover {
  background-color: #ff7000;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.3);
}

button:active {
  transform: translateY(0);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 15px;
  padding: 30px;
  max-width: 1000px;
  width: 95%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  text-align: center;
}

.modal-content h2 {
  color: #ff8c00;
  margin-bottom: 20px;
}

.battle-elves-list {
  margin: 20px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  justify-content: center;
}

.elf-image {
  margin-bottom: 10px;
  text-align: center;
}

.elf-image img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  border: 2px solid #ff8c00;
  background: white;
  padding: 5px;
}

.elf-card {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  min-width: 250px;
  flex: 1;
  max-width: 300px;
}

.elf-card h4 {
  margin-top: 0;
  color: #333;
}

.elf-card p {
  margin: 5px 0;
  font-size: 0.9rem;
  color: #666;
}

.no-elves {
  padding: 30px;
  background: #f5f5f5;
  border-radius: 8px;
  margin: 20px 0;
}

.modal-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.cancel-btn {
  background-color: #9e9e9e;
}

.cancel-btn:hover {
  background-color: #757575;
}

.confirm-btn {
  background-color: #4caf50;
}

.confirm-btn:hover {
  background-color: #43a047;
}

/* 战斗策略推荐样式 */
.strategy-section {
  margin-top: 30px;
  padding: 20px;
  background: #f0f8ff;
  border-radius: 8px;
  border: 1px solid #add8e6;
}

.strategy-section h3 {
  color: #1e90ff;
  margin-bottom: 15px;
}

.strategy-content {
  background: white;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  min-height: 100px;
}

.strategy-content p {
  margin: 0;
  line-height: 1.5;
  color: #333;
}

.no-strategy {
  padding: 20px;
  background: #f5f5f5;
  border-radius: 4px;
  text-align: center;
  color: #666;
}
</style>
