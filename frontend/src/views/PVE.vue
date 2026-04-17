<template>
  <div class="pve-container">
    <GameTopNav />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">ADVENTURE MODE</p>
        <h1>关卡挑战</h1>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="levels-list">
        <!-- 从数据库读取的关卡 -->
        <div v-for="level in levels" :key="level.id" :class="['level-card', { 'locked': level.id > userCurrentLevel }]">
          <div class="level-info">
            <h3>关卡 {{ level.id }}</h3>
            <p class="level-name">{{ level.levelName }}</p>
            <div class="rewards">
              <p class="level-reward">经验奖励: <span>{{ level.rewardExp }}</span></p>
              <p class="level-reward">金币奖励: <span>{{ level.rewardGold || 10 }}</span></p>
            </div>
            <!-- 显示星级和评分 -->
            <div v-if="userLevelStars[level.id]" class="level-stars">
              <div class="stars">
                <span v-for="i in 3" :key="i" :class="['star', { 'active': i <= userLevelStars[level.id].star }]">
                  ★
                </span>
              </div>
              <p class="score">评分: {{ userLevelStars[level.id].score }}</p>
            </div>
          </div>
          <div v-if="level.id > userCurrentLevel" class="locked-overlay">
            <p>未解锁</p>
          </div>
          <button v-else class="action-btn primary-btn" @click="openBattleModal(level.id)">进入关卡</button>
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
              <div class="elf-card__halo"></div>
              <img src="../assets/photo/sasuke/佐助.jpg" alt="精灵 1" v-if="elf.elfId === 1" />
              <img src="../assets/photo/zhaomeiming/照美冥.webp" alt="精灵 2" v-else-if="elf.elfId === 2" />
              <img src="../assets/photo/qianshouzhujian/千手柱间.jpg" alt="精灵 3" v-else-if="elf.elfId === 3" />
              <img src="../assets/hero.png" alt="精灵" v-else />
            </div>
            <div class="elf-info">
              <h4>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h4>
              <p>等级: <span>{{ elf.level }}</span></p>
              <p>HP: <span>{{ elf.maxHp }}</span></p>
              <p>MP: <span>{{ elf.maxMp }}</span></p>
              <div class="deployment-tag">出战顺序: {{ elf.fightOrder }}</div>
            </div>
          </div>
          <div v-if="battleElves.length === 0" class="no-elves">
             <p>您还没有设置出战精灵</p>
            <button class="action-btn primary-btn" @click="navigateTo('/elves')">去设置</button>
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
          <button @click="cancelBattle" class="action-btn secondary-btn">取消</button>
          <button @click="confirmBattle" class="action-btn confirm-btn">确定出战</button>
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
.pve-container {
  min-height: 100vh;
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
}

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

.loading, .error {
  text-align: center;
  padding: 2rem;
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.8);
}

.error {
  color: #ff5252;
}

.levels-list {
  display: flex;
  flex-wrap: nowrap;
  gap: 50px;
  padding: 1.5rem 1rem 4rem 1rem;
  overflow-x: auto;
  align-items: center;
}

.level-card:first-child {
  margin-left: auto;
}

.level-card:last-child {
  margin-right: auto;
}

.levels-list::-webkit-scrollbar {
  height: 10px;
}

.levels-list::-webkit-scrollbar-thumb {
  background: rgba(255, 156, 58, 0.4);
  border-radius: 5px;
}

.levels-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 5px;
}

.level-card {
  position: relative;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.96), rgba(16, 9, 3, 0.96));
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.7);
  border-radius: 20px;
  padding: 1.3rem;
  text-align: center;
  box-shadow: 0 16px 28px rgba(255, 140, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  overflow: visible;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 210px;
  min-height: 230px;
  flex-shrink: 0;
  z-index: 1;
}

.level-card::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 100%;
  width: 50px;
  height: 4px;
  background: linear-gradient(90deg, #ff9c3a, rgba(255, 156, 58, 0.2));
  z-index: -1;
  transform: translateY(-50%);
}

.level-card:last-child::after {
  display: none;
}

.level-card:hover:not(.locked) {
  transform: translateY(-8px);
  box-shadow: 0 24px 40px rgba(255, 140, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 195, 112, 0.5);
}

.level-card h3 {
  margin-top: 0;
  color: #ff9c3a;
  font-size: 1.3rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
  text-shadow: 0 2px 4px rgba(255, 140, 0, 0.2);
}

.level-name {
  margin: 0.5rem 0 1rem;
  color: #fff4df;
  font-size: 1.05rem;
  font-weight: bold;
}

.rewards {
  background: rgba(255, 169, 79, 0.05);
  border-radius: 12px;
  padding: 10px;
  margin-bottom: 15px;
  border: 1px solid rgba(255, 195, 112, 0.1);
}

.level-reward {
  margin: 0.3rem 0;
  color: rgba(247, 239, 224, 0.72);
  font-size: 0.85rem;
  display: flex;
  justify-content: space-between;
}

.level-reward span {
  color: #ffc107;
  font-weight: 700;
  text-shadow: 0 0 5px rgba(255, 193, 7, 0.3);
}

.level-card.locked {
  opacity: 0.7;
  background: rgba(15, 10, 5, 0.9);
  border-color: rgba(255, 152, 0, 0.1);
  border-top-color: rgba(255, 152, 0, 0.2);
}

.level-card.locked::after {
  background: rgba(255, 152, 0, 0.2);
}

.locked-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(10, 5, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(3px);
  z-index: 10;
  border-radius: 20px;
}

.locked-overlay p {
  color: rgba(255, 230, 200, 0.9);
  font-size: 1.1rem;
  font-weight: bold;
  letter-spacing: 0.2em;
  background: rgba(40, 20, 5, 0.8);
  padding: 10px 20px;
  border-radius: 30px;
  border: 1px solid rgba(255, 152, 0, 0.3);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.5);
}

.level-stars {
  margin: 10px 0 15px;
}

.stars {
  display: flex;
  justify-content: center;
  margin-bottom: 5px;
}

.star {
  font-size: 1.4rem;
  color: rgba(255, 255, 255, 0.1);
  margin: 0 4px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.star.active {
  color: #ffd700;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
}

.score {
  font-size: 0.85rem;
  color: rgba(247, 239, 224, 0.8);
  margin: 0;
  font-weight: 600;
}

.action-btn {
  border: none;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease;
  min-height: 48px;
  padding: 0 24px;
  border-radius: 16px;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  margin-top: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.action-btn:active {
  transform: translateY(1px);
}

.primary-btn {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  box-shadow: 0 12px 24px rgba(255, 132, 29, 0.2);
}

.primary-btn:hover {
  box-shadow: 0 16px 28px rgba(255, 132, 29, 0.35);
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.confirm-btn {
  background: linear-gradient(135deg, #a2ffc4, #3aff75 55%, #1aff53);
  color: #0a2d12;
  box-shadow: 0 12px 24px rgba(29, 255, 83, 0.2);
}

.confirm-btn:hover {
  box-shadow: 0 16px 28px rgba(29, 255, 83, 0.35);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(4, 8, 15, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: linear-gradient(180deg, rgba(15, 19, 31, 0.96), rgba(9, 12, 19, 0.96));
  border: 1px solid rgba(255, 195, 112, 0.14);
  border-radius: 28px;
  padding: 35px;
  max-width: 900px;
  width: 95%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 50px rgba(4, 8, 15, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  text-align: center;
}

.modal-content::-webkit-scrollbar {
  width: 8px;
}

.modal-content::-webkit-scrollbar-thumb {
  background: rgba(255, 195, 112, 0.3);
  border-radius: 4px;
}

.modal-content h2 {
  color: #fff4df;
  margin-top: 0;
  margin-bottom: 25px;
  font-size: 2rem;
  text-shadow: 0 2px 8px rgba(255, 140, 0, 0.3);
}

.battle-elves-list {
  margin: 20px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: center;
}

.elf-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 194, 107, 0.12);
  border-radius: 20px;
  padding: 20px;
  text-align: center;
  min-width: 250px;
  flex: 1;
  max-width: 280px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.elf-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.3);
  border-color: rgba(255, 194, 107, 0.3);
}

.elf-image {
  margin-bottom: 15px;
  position: relative;
  display: flex;
  justify-content: center;
}

.elf-card__halo {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 169, 79, 0.2), transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.elf-image img {
  width: 110px;
  height: 110px;
  object-fit: cover;
  border-radius: 50%;
  border: 3px solid rgba(255, 169, 79, 0.8);
  box-shadow: 0 8px 16px rgba(255, 129, 35, 0.2);
  position: relative;
  z-index: 1;
}

.elf-info h4 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #fff4df;
  font-size: 1.2rem;
}

.elf-info p {
  margin: 6px 0;
  font-size: 0.95rem;
  color: rgba(247, 239, 224, 0.7);
  display: flex;
  justify-content: space-between;
  padding: 0 10%;
}

.elf-info p span {
  color: #fff;
  font-weight: bold;
}

.deployment-tag {
  margin-top: 15px;
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(255, 169, 79, 0.15);
  color: #ffc107;
  font-size: 0.85rem;
  font-weight: bold;
  border: 1px solid rgba(255, 169, 79, 0.3);
}

.no-elves {
  padding: 40px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px dashed rgba(255, 194, 107, 0.3);
  border-radius: 20px;
  width: 100%;
}

.no-elves p {
  color: rgba(247, 239, 224, 0.8);
  margin-bottom: 20px;
  font-size: 1.1rem;
}

.no-elves .action-btn {
  margin: 0 auto;
  max-width: 200px;
}

.modal-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 35px;
}

.modal-buttons .action-btn {
  width: auto;
  margin: 0;
  min-width: 140px;
}

/* 战斗策略推荐样式 */
.strategy-section {
  margin-top: 35px;
  padding: 25px;
  background: rgba(16, 26, 45, 0.6);
  border-radius: 20px;
  border: 1px solid rgba(79, 145, 255, 0.2);
  position: relative;
  overflow: hidden;
}

.strategy-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, transparent, #4f91ff, transparent);
}

.strategy-section h3 {
  color: #8ab4f8;
  margin-top: 0;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.strategy-section h3::before {
  content: '✨';
}

.strategy-content {
  background: rgba(0, 0, 0, 0.3);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.strategy-content p {
  margin: 0;
  line-height: 1.6;
  color: #e8eaed;
  font-size: 1.05rem;
  text-align: left;
}

.no-strategy {
  padding: 20px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.5);
  font-style: italic;
}
</style>
