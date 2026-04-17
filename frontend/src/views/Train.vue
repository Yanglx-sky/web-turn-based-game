<template>
  <div class="train-container">
    <GameTopNav />
    
    <div class="main-content">
      <!-- 头部标题 -->
      <div v-if="!inTrain && !showResult" class="page-header">
        <p class="section-eyebrow">TRAINING PREPARATION</p>
        <h1>训练参数</h1>
      </div>
      <div v-else-if="inTrain && !showResult" class="page-header">
        <p class="section-eyebrow">BATTLE SIMULATION</p>
        <h1>模拟战斗</h1>
      </div>
      <div v-if="showResult" class="page-header">
        <p class="section-eyebrow">TRAINING REPORT</p>
        <h1>训练报告</h1>
      </div>

      <!-- 训练人偶创建表单 -->
      <div v-if="!inTrain && !showResult" class="create-mannequin">
        <form @submit.prevent="createMannequin" class="mannequin-form">
          <div class="form-grid">
          <div class="form-group">
            <label>伤害</label>
            <input type="number" v-model.number="mannequinForm.attack" min="1" required>
          </div>
          <div class="form-group">
            <label>防御</label>
            <input type="number" v-model.number="mannequinForm.defense" min="1" required>
          </div>
          <div class="form-group">
            <label>生命值</label>
            <input type="number" v-model.number="mannequinForm.hp" min="10" required>
          </div>
          <div class="form-group">
            <label>蓝量</label>
            <input type="number" v-model.number="mannequinForm.mp" min="10" required>
          </div>
          <div class="form-group">
            <label>速度</label>
            <input type="number" v-model.number="mannequinForm.speed" min="1" required>
          </div>
          <div class="form-group">
            <label>系别</label>
            <select v-model.number="mannequinForm.type" required>
              <option value="1">火系</option>
              <option value="2">水系</option>
              <option value="3">草系</option>
            </select>
          </div>
          <div class="form-group">
            <label>是否主动攻击</label>
            <select v-model.number="mannequinForm.isAttack" required>
              <option value="0">否</option>
              <option value="1">是</option>
            </select>
          </div>
          </div>
          <div class="form-footer">
            <button type="submit" class="btn create-btn">创建并开始训练</button>
          </div>
        </form>
      </div>
      
      <!-- 训练战斗界面 -->
      <div v-else-if="inTrain && !showResult" class="train-battle">
        <div class="battle-info">
          <div class="player-info">
            <h3>你的精灵</h3>
            <p>{{ playerElf.elfName }}</p>
            <p>HP: {{ playerElfHp }}</p>
          </div>
          <div class="mannequin-info" :class="getElementColorClass(mannequin.type)">
            <h3>训练人偶</h3>
            <span class="element-badge" :class="getElementColorClass(mannequin.type)">{{ getMannequinTypeName(mannequin.type) }}</span>
            <p>HP: {{ mannequinHp }}</p>
          </div>
        </div>
        
        <div class="battle-log">
          <h3>训练日志</h3>
          <div class="log-content">
            <div v-for="(log, index) in trainLogs" :key="index" class="log-item">
              {{ log }}
            </div>
          </div>
        </div>
        
        <div class="action-buttons">
          <button @click="normalAttack" class="btn attack-btn">普通攻击</button>
          <div class="skills">
            <button 
              v-for="skill in playerElf.skills" 
              :key="skill.id" 
              @click="useSkill(skill.id)" 
              class="skill-btn"
              :disabled="playerElf.mp < skill.costMp"
            >
              {{ skill.skillName }} ({{ skill.costMp }}MP)
            </button>
          </div>
        </div>
      </div>
      
      <!-- 训练结果 -->
      <div v-if="showResult" class="train-result">
        <h3>训练结果</h3>
        <p class="result-text">{{ trainResult }}</p>
        <div class="ai-score">
          <h4>AI评分: {{ aiScore }}</h4>
        </div>
        <div class="ai-report">
          <h4>AI评分报告</h4>
          <p>{{ aiReport }}</p>
        </div>
        <div class="result-buttons">
          <button @click="backToTrain" class="btn">返回训练</button>
          <button @click="navigateTo('/ai')" class="btn">AI训练总结</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { trainApi } from '../api/train'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()

// 训练人偶表单
const mannequinForm = ref({
  attack: 50,
  defense: 30,
  hp: 200,
  mp: 100,
  speed: 50,
  type: 1,
  isAttack: 1
})

// 训练状态
const inTrain = ref(false)
const showResult = ref(false)
const trainResult = ref('')
const aiScore = ref(0)
const aiReport = ref('')

// 战斗信息
const trainLogs = ref([])
const playerElf = ref({})
const mannequin = ref({})
const playerElfHp = ref(0)
const mannequinHp = ref(0)

// 创建训练人偶并开始训练
const createMannequin = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    
    // 直接将训练人偶属性传递到Battle页面，不经过后端保存
    const params = new URLSearchParams({
      type: 'train',
      mannequinAttack: mannequinForm.value.attack,
      mannequinDefense: mannequinForm.value.defense,
      mannequinHp: mannequinForm.value.hp,
      mannequinMp: mannequinForm.value.mp,
      mannequinSpeed: mannequinForm.value.speed,
      mannequinType: mannequinForm.value.type,
      mannequinIsAttack: mannequinForm.value.isAttack
    }).toString()
    
    router.push(`/battle?${params}`)
  } catch (error) {
    console.error('开始训练失败:', error)
    alert('开始训练失败，请重试')
  }
}

// 普通攻击
const normalAttack = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    
    const response = await trainApi.normalAttack()
    
    if (response.code === 200) {
      trainLogs.value = response.data.trainLog
      playerElfHp.value = response.data.playerElfHp
      mannequinHp.value = response.data.mannequinHp
      
      // 检查是否训练结束
      if (response.data.trainResult) {
        showResult.value = true
        trainResult.value = response.data.trainResult
        aiScore.value = response.data.aiScore
        aiReport.value = response.data.aiReport
      }
    } else {
      alert('攻击失败: ' + response.msg)
    }
  } catch (error) {
    console.error('攻击失败:', error)
    alert('攻击失败，请重试')
  }
}

// 使用技能
const useSkill = async (skillId) => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    const user = JSON.parse(userStr)
    
    const response = await trainApi.useSkill(skillId)
    
    if (response.code === 200) {
      trainLogs.value = response.data.trainLog
      playerElfHp.value = response.data.playerElfHp
      mannequinHp.value = response.data.mannequinHp
      
      // 检查是否训练结束
      if (response.data.trainResult) {
        showResult.value = true
        trainResult.value = response.data.trainResult
        aiScore.value = response.data.aiScore
        aiReport.value = response.data.aiReport
      }
    } else {
      alert('使用技能失败: ' + response.msg)
    }
  } catch (error) {
    console.error('使用技能失败:', error)
    alert('使用技能失败，请重试')
  }
}

// 返回训练
const backToTrain = () => {
  showResult.value = false
  inTrain.value = false
  trainLogs.value = []
  playerElf.value = {}
  mannequin.value = {}
  playerElfHp.value = 0
  mannequinHp.value = 0
}

// 返回首页
const backToHome = () => {
  router.push('/')
}

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('user')
  router.push('/auth')
}

// 获取训练人偶类型名称
const getMannequinTypeName = (type) => {
  switch (type) {
    case 1:
      return '火系训练人偶'
    case 2:
      return '水系训练人偶'
    case 3:
      return '草系训练人偶'
    default:
      return '训练人偶'
  }
}

// 根据元素类型获取CSS颜色类名
const getElementColorClass = (type) => {
  switch (type) {
    case 1:
      return 'element-fire'
    case 2:
      return 'element-water'
    case 3:
      return 'element-grass'
    default:
      return ''
  }
}

onMounted(() => {
  // 初始化
})
</script>

<style scoped>
/* 全局样式 */
.train-container {
  min-height: 100vh;
  background-image: url('https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=colorful%20fantasy%20game%20background%20with%20magical%20elements%20and%20floating%20islands&image_size=landscape_16_9');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Arial', sans-serif;
}

.main-content {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

h2 {
  text-align: center;
  margin-bottom: 1rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  font-size: 28px;
}

.create-mannequin {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 2px solid #ff8c00;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h3 {
  color: #333;
  margin: 0;
  font-size: 20px;
  font-weight: bold;
  color: #ff8c00;
}

.back-btn {
  background: #999;
  padding: 10px 20px;
  border-radius: 25px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #888;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #555;
  font-size: 16px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s ease;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #ff8c00;
  box-shadow: 0 0 0 3px rgba(255, 140, 0, 0.1);
}

.btn {
  display: inline-block;
  padding: 12px 24px;
  background: #4caf50;
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
}

.btn:hover {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(76, 175, 80, 0.4);
}

.create-btn {
  width: 100%;
  margin-top: 20px;
  padding: 15px;
  font-size: 18px;
  background: linear-gradient(135deg, #ff8c00, #ff6b00);
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.3);
}

.create-btn:hover {
  background: linear-gradient(135deg, #ff6b00, #e65c00);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(255, 140, 0, 0.4);
}

.attack-btn {
  background: #ff5722;
  box-shadow: 0 4px 8px rgba(255, 87, 34, 0.3);
}

.attack-btn:hover {
  background: #e64a19;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(255, 87, 34, 0.4);
}

.train-battle {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 2px solid #4caf50;
}

.battle-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 25px;
  padding: 20px;
  background: rgba(76, 175, 80, 0.1);
  border-radius: 10px;
  border: 2px solid #4caf50;
}

.player-info,
.mannequin-info {
  text-align: center;
  flex: 1;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  border: 2px solid transparent;
}

/* 人偶元素类型颜色 */
.mannequin-info.element-fire {
  border-color: var(--color-fire);
  background: var(--color-fire-bg);
}

.mannequin-info.element-water {
  border-color: var(--color-water);
  background: var(--color-water-bg);
}

.mannequin-info.element-grass {
  border-color: var(--color-grass);
  background: var(--color-grass-bg);
}

.player-info h3,
.mannequin-info h3 {
  color: var(--color-neutral-700);
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: bold;
}

.player-info p,
.mannequin-info p {
  margin: 8px 0;
  color: var(--color-neutral-500);
  font-size: 16px;
}

/* 元素徽章 */
.element-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.element-badge.element-fire {
  background: var(--color-fire);
  color: white;
}

.element-badge.element-water {
  background: var(--color-water);
  color: white;
}

.element-badge.element-grass {
  background: var(--color-grass);
  color: white;
}

.battle-log {
  margin-bottom: 25px;
}

.battle-log h3 {
  color: #333;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: bold;
}

.log-content {
  max-height: 250px;
  overflow-y: auto;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 10px;
  border: 2px solid #ddd;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.log-item {
  margin-bottom: 8px;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
  border-left: 4px solid #ff8c00;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.log-item:hover {
  transform: translateX(5px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.action-buttons {
  margin-top: 25px;
}

.skills {
  margin-top: 15px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.skill-btn {
  padding: 12px 16px;
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(33, 150, 243, 0.3);
}

.skill-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1976d2, #1565c0);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(33, 150, 243, 0.4);
}

.skill-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.train-result {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  border: 2px solid #ff8c00;
}

.train-result h3 {
  color: #333;
  margin-bottom: 25px;
  font-size: 24px;
  font-weight: bold;
  color: #ff8c00;
}

.result-text {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 25px;
  color: #4caf50;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.ai-score {
  margin-bottom: 25px;
  padding: 15px;
  background: rgba(255, 140, 0, 0.1);
  border-radius: 10px;
  border: 2px solid #ff8c00;
}

.ai-score h4 {
  color: #333;
  margin-bottom: 10px;
  font-size: 18px;
  font-weight: bold;
}

.ai-report {
  margin-bottom: 35px;
  text-align: left;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 10px;
  border: 2px solid #ddd;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.ai-report h4 {
  color: #333;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: bold;
}

.ai-report p {
  line-height: 1.6;
  color: #666;
}

.result-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .train-container {
    padding: 10px;
  }
  
  .main-content {
    padding: 20px;
  }
  
  .page-header h1 {
    font-size: 2.2rem;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .battle-info {
    flex-direction: column;
    gap: 15px;
  }
  
  .skills {
    grid-template-columns: 1fr;
  }
  
  .result-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .result-buttons .btn {
    width: 100%;
    max-width: 200px;
  }
}

/* Game-style training prep overrides */
.train-container {
  min-height: 100vh;
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
}

.main-content {
  max-width: 1200px;
  margin: 22px auto 0;
  padding: 0 40px;
  background: none;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.create-mannequin,
.train-battle,
.train-result {
  border: 1px solid rgba(255, 195, 112, 0.14);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(15, 19, 31, 0.96), rgba(9, 12, 19, 0.96));
  box-shadow:
    0 22px 42px rgba(5, 9, 15, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.create-mannequin,
.train-battle,
.train-result {
  padding: 26px;
}

.page-header {
  text-align: center;
  margin-bottom: 1.5rem;
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

.mannequin-form {
  display: grid;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 20px;
}

.form-group {
  margin-bottom: 0;
}

.form-group label {
  margin-bottom: 10px;
  color: rgba(247, 237, 220, 0.8);
}

.form-group input,
.form-group select {
  width: 100%;
  min-height: 56px;
  padding: 0 18px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff3db;
  font-size: 1rem;
  line-height: 1;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.form-group input:focus,
.form-group select:focus {
  border-color: rgba(255, 182, 81, 0.62);
  box-shadow: 0 0 0 3px rgba(255, 167, 81, 0.12);
}

.form-group select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 52px;
  cursor: pointer;
  background-image:
    linear-gradient(45deg, transparent 50%, rgba(255, 232, 198, 0.82) 50%),
    linear-gradient(135deg, rgba(255, 232, 198, 0.82) 50%, transparent 50%);
  background-position:
    calc(100% - 24px) calc(50% - 4px),
    calc(100% - 18px) calc(50% - 4px);
  background-size: 7px 7px, 7px 7px;
  background-repeat: no-repeat;
}

.form-group select option {
  background: #141a27;
  color: #fff3db;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: center;
}

.create-btn {
  width: auto;
  min-width: 320px;
  min-height: 54px;
  margin-top: 0;
  border-radius: 18px;
  box-shadow: 0 16px 28px rgba(255, 121, 37, 0.2);
}

.battle-info {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 194, 107, 0.14);
}

.player-info,
.mannequin-info {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 194, 107, 0.12);
  color: #fff3db;
}

.player-info h3,
.mannequin-info h3,
.battle-log h3,
.train-result h3 {
  color: #fff0d5;
}

.player-info p,
.mannequin-info p,
.ai-report p,
.result-text {
  color: rgba(247, 237, 220, 0.78);
}

.log-content {
  border-color: rgba(255, 194, 107, 0.1);
  background: rgba(7, 11, 18, 0.76);
}

.log-item {
  border-left-color: #ffab5c;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(247, 237, 220, 0.78);
}

.result-buttons {
  margin-top: 26px;
}

@media (max-width: 960px) {
  .train-container {
    padding: 0 14px 24px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-footer {
    justify-content: stretch;
  }

  .create-btn {
    width: 100%;
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .main-content {
    margin-top: 14px;
  }

  .create-mannequin,
  .train-battle,
  .train-result {
    padding: 20px;
    border-radius: 22px;
  }
}
</style>
