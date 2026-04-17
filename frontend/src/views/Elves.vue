<template>
  <div class="home-container">
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
          <button class="nav-btn" @click="navigateTo('/achievement')">成就</button>
          <button class="nav-btn" @click="navigateTo('/ai')">AI助手</button>
          <button class="nav-btn" @click="navigateTo('/chat')">聊天</button>
          <button class="nav-btn" @click="logout">退出</button>
        </div>
    </nav>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <h1>我的精灵</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="elves-list">
        <div v-for="elf in elves" :key="elf.id" class="elf-card" :class="getElementColorClass(elf.elfId)">
          <img :src="getElfImage(elf.elfId)" :alt="elf.elfId" class="elf-image">
          <span class="element-badge" :class="getElementColorClass(elf.elfId)">{{ getElementType(elf.elfId) }}</span>
          <h3>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h3>
          <p>等级: {{ elf.level }}</p>
          <p v-if="elf.level < 10">经验: {{ elf.exp }}/{{ elf.expNeed }}</p>
          <p v-else>状态: 已满级</p>
          <p>HP: {{ elf.maxHp }}</p>
          <p>MP: {{ elf.maxMp }}</p>
          <p>攻击: {{ elf.attack }}</p>
          <p>防御: {{ elf.defense }}</p>
          <p>速度: {{ elf.speed }}</p>
          <p v-if="elf.fightOrder > 0" class="active-tag">出战：{{ elf.fightOrder }}号位</p>
          <div class="battle-order-selector">
            <select v-model="selectedOrder[elf.id]" class="order-select">
              <option value="0">不出战</option>
              <option value="1">1号位</option>
              <option value="2">2号位</option>
              <option value="3">3号位</option>
            </select>
            <button @click="setActiveElf(elf.id, selectedOrder[elf.id])">设置</button>
          </div>
          <button @click="viewElfDetail(elf.id)" class="detail-btn" :class="getElementColorClass(elf.elfId)">查看详情</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userElfApi } from '../api/userElf'

// 根据精灵ID获取图片路径
const getElfImage = (elfId) => {
  switch (elfId) {
    case 1:
      return new URL('../assets/photo/sasuke/佐助.jpg', import.meta.url).href
    case 2:
      return new URL('../assets/photo/zhaomeiming/照美冥.webp', import.meta.url).href
    case 3:
      return new URL('../assets/photo/qianshouzhujian/千手柱间.jpg', import.meta.url).href
    default:
      return ''
  }
}

// 根据精灵ID获取系别名称
const getElementType = (elfId) => {
  switch (elfId) {
    case 1:
      return '火系'
    case 2:
      return '水系'
    case 3:
      return '草系'
    default:
      return '未知'
  }
}

// 根据精灵ID获取元素颜色CSS类名
const getElementColorClass = (elfId) => {
  switch (elfId) {
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

const router = useRouter()
const elves = ref([])
const loading = ref(true)
const error = ref('')
const selectedOrder = ref({})

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('user')
  router.push('/auth')
}

const fetchElves = async () => {
  try {
    loading.value = true
    const user = JSON.parse(localStorage.getItem('user'))
    const response = await userElfApi.list(user.id)
    if (response.code === 200) {
      elves.value = response.data
      // 初始化selectedOrder
      elves.value.forEach(elf => {
        selectedOrder.value[elf.id] = (elf.fightOrder || 0).toString()
      })
    }
  } catch (err) {
    error.value = '获取精灵列表失败'
    console.error('获取精灵列表失败:', err)
  } finally {
    loading.value = false
  }
}

const setActiveElf = async (elfId, fightOrder) => {
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    // 确保fightOrder是数字类型
    const order = parseInt(fightOrder)
    
    // 出战顺序校验：1-3号位不能重复
    if (order > 0) {
      const existingElf = elves.value.find(elf => elf.fightOrder === order && elf.id !== elfId)
      if (existingElf) {
        alert(`第${order}号位已经被其他精灵占用，请选择其他位置`)
        return
      }
    }
    
    const response = await userElfApi.setActive(elfId, order)
    if (response.code === 200) {
      // 重新获取精灵列表
      await fetchElves()
      // 显示成功提示
      alert('设置成功！')
    } else {
      // 显示失败提示
      alert('设置失败：' + response.message)
    }
  } catch (err) {
    error.value = '设置出战精灵失败'
    console.error('设置出战精灵失败:', err)
    // 显示错误提示
    alert('设置失败：网络错误')
  }
}

const viewElfDetail = (elfId) => {
  router.push(`/elf/${elfId}`)
}

onMounted(() => {
  fetchElves()
})
</script>

<style scoped>
/* 全局样式 */
.home-container {
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

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

h1 {
  text-align: center;
  margin-bottom: 2rem;
  color: #4CAF50;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  font-size: 2rem;
}

.loading, .error {
  text-align: center;
  padding: 3rem;
  font-size: 1.2rem;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin: 2rem auto;
  max-width: 500px;
}

.error {
  color: #f44336;
  border: 1px solid rgba(244, 67, 54, 0.3);
}

.loading {
  color: #666;
  border: 1px solid rgba(102, 102, 102, 0.3);
}

/* 精灵列表 */
.elves-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 2rem;
}

.elf-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  box-shadow: var(--shadow-lg);
  position: relative;
  text-align: center;
  backdrop-filter: blur(10px);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.elf-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-xl);
}

/* 元素类型颜色边框 */
.elf-card.element-fire {
  border-color: var(--color-fire);
}

.elf-card.element-fire:hover {
  box-shadow: var(--shadow-xl), 0 0 20px oklch(0.55 0.22 30 / 0.3);
}

.elf-card.element-water {
  border-color: var(--color-water);
}

.elf-card.element-water:hover {
  box-shadow: var(--shadow-xl), 0 0 20px oklch(0.55 0.18 240 / 0.3);
}

.elf-card.element-grass {
  border-color: var(--color-grass);
}

.elf-card.element-grass:hover {
  box-shadow: var(--shadow-xl), 0 0 20px oklch(0.55 0.18 150 / 0.3);
}

.elf-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 50%;
  margin-bottom: 1.5rem;
  border: 3px solid var(--color-grass);
  box-shadow: 0 4px 15px oklch(0.55 0.18 150 / 0.4);
  transition: all 0.3s ease;
}

.elf-card.element-fire .elf-image {
  border-color: var(--color-fire);
  box-shadow: 0 4px 15px oklch(0.55 0.22 30 / 0.4);
}

.elf-card.element-water .elf-image {
  border-color: var(--color-water);
  box-shadow: 0 4px 15px oklch(0.55 0.18 240 / 0.4);
}

.elf-card:hover .elf-image {
  transform: scale(1.1);
}

.elf-card h3 {
  margin-top: 0;
  color: var(--color-neutral-700);
  font-size: 1.3rem;
  margin-bottom: 1rem;
}

.elf-card p {
  margin: 0.5rem 0;
  color: var(--color-neutral-500);
  font-size: 0.9rem;
}

/* 元素徽章 */
.element-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.element-badge.element-fire {
  background: var(--color-fire-bg);
  color: var(--color-fire-text);
}

.element-badge.element-water {
  background: var(--color-water-bg);
  color: var(--color-water-text);
}

.element-badge.element-grass {
  background: var(--color-grass-bg);
  color: var(--color-grass-text);
}

.active-tag {
  position: absolute;
  top: 15px;
  right: 15px;
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(76, 175, 80, 0.3);
}

/* 出战顺序选择器 */
.battle-order-selector {
  margin-top: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.order-select {
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 1rem;
  background-color: white;
  cursor: pointer;
  flex: 1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.order-select:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.2);
}

/* 按钮样式 */
.battle-order-selector button {
  margin-top: 0;
  padding: 0.8rem 1.5rem;
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
  white-space: nowrap;
}

.battle-order-selector button:hover {
  background: linear-gradient(135deg, #388E3C 0%, #66BB6A 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.detail-btn {
  margin-top: 1rem;
  padding: 0.8rem 1.5rem;
  background: var(--color-info);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px oklch(0.55 0.15 240 / 0.3);
  width: 100%;
}

.detail-btn:hover {
  background: var(--color-info-light);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px oklch(0.55 0.15 240 / 0.4);
}

/* 元素类型按钮颜色 */
.detail-btn.element-fire {
  background: var(--color-fire);
  box-shadow: 0 4px 12px oklch(0.55 0.22 30 / 0.3);
}

.detail-btn.element-fire:hover {
  background: var(--color-fire-light);
  box-shadow: 0 6px 16px oklch(0.55 0.22 30 / 0.4);
}

.detail-btn.element-water {
  background: var(--color-water);
  box-shadow: 0 4px 12px oklch(0.55 0.18 240 / 0.3);
}

.detail-btn.element-water:hover {
  background: var(--color-water-light);
  box-shadow: 0 6px 16px oklch(0.55 0.18 240 / 0.4);
}

.detail-btn.element-grass {
  background: var(--color-grass);
  box-shadow: 0 4px 12px oklch(0.55 0.18 150 / 0.3);
}

.detail-btn.element-grass:hover {
  background: var(--color-grass-light);
  box-shadow: 0 6px 16px oklch(0.55 0.18 150 / 0.4);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
  
  .elves-list {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
  
  .battle-order-selector {
    flex-direction: column;
    align-items: stretch;
  }
  
  .battle-order-selector button {
    width: 100%;
  }
  
  .nav-menu {
    gap: 0.5rem;
  }
  
  .nav-btn {
    padding: 0.3rem 0.6rem;
    font-size: 0.8rem;
  }
}
</style>