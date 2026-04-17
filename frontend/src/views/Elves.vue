<template>
  <div class="home-container">
    <GameTopNav />

    <!-- 主内容区 -->
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">ACTIVE ROSTER</p>
        <h1>我的精灵</h1>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="elves-list">
        <div v-for="elf in elves" :key="elf.id" class="elf-card" :class="getElementColorClass(elf.elfId)">
          <div class="elf-card__media">
            <div class="elf-card__halo"></div>
            <img :src="getElfImage(elf.elfId)" :alt="elf.elfId" class="elf-image">
          </div>
          <div class="elf-info">
            <h3>
              {{ elf.elfName || `精灵 ${elf.elfId}` }}
              <span class="element-badge" :class="getElementColorClass(elf.elfId)">{{ getElementType(elf.elfId) }}</span>
            </h3>
            <div class="elf-meta">
              <span class="elf-meta__item">等级 {{ elf.level }}</span>
              <span v-if="elf.fightOrder > 0" class="active-tag">出战：{{ elf.fightOrder }}号位</span>
              <span v-else class="standby-tag">未出战</span>
            </div>
            <div class="elf-stats">
              <div class="stat-row">
                <div class="stat-item">
                  <span class="stat-label">HP</span>
                  <span class="stat-value stat-hp">{{ elf.maxHp }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">MP</span>
                  <span class="stat-value stat-mp">{{ elf.maxMp }}</span>
                </div>
              </div>
              <div class="stat-row">
                <div class="stat-item">
                  <span class="stat-label">攻击</span>
                  <span class="stat-value">{{ elf.attack }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">防御</span>
                  <span class="stat-value">{{ elf.defense }}</span>
                </div>
              </div>
              <div class="stat-row">
                <div class="stat-item">
                  <span class="stat-label">速度</span>
                  <span class="stat-value">{{ elf.speed }}</span>
                </div>
                <div class="stat-item">
                  <span v-if="elf.level < 10" class="stat-label">经验</span>
                  <span v-else class="stat-label">状态</span>
                  <span v-if="elf.level < 10" class="stat-value">{{ elf.exp }}/{{ elf.expNeed }}</span>
                  <span v-else class="stat-value stat-max">已满级</span>
                </div>
              </div>
            </div>
            <div class="elf-actions-wrapper">
              <div class="battle-order-selector">
                <select v-model="selectedOrder[elf.id]" class="order-select">
                  <option value="0">不出战</option>
                  <option value="1">1号位</option>
                  <option value="2">2号位</option>
                  <option value="3">3号位</option>
                </select>
                <button @click="setActiveElf(elf.id, selectedOrder[elf.id])" class="order-btn">设置</button>
              </div>
              <button @click="viewElfDetail(elf.id)" class="detail-btn" :class="getElementColorClass(elf.elfId)">查看详情</button>
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
import { userElfApi } from '../api/userElf'
import GameTopNav from '../components/GameTopNav.vue'

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
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
}

/* 主内容区 */
.main-content {
  max-width: 1400px;
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

.loading,
.error {
  text-align: center;
  padding: 48px 24px;
  border-radius: 28px;
  border: 1px dashed rgba(255, 169, 79, 0.2);
  background: rgba(0, 0, 0, 0.2);
  box-shadow: 0 16px 28px rgba(4, 8, 15, 0.24);
  font-size: 1.05rem;
}

.error {
  color: #fc8181;
  border-color: rgba(252, 129, 129, 0.3);
}

.loading {
  color: rgba(255, 255, 255, 0.5);
}

/* 精灵列表 */
.elves-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  padding-bottom: 40px;
}

.elf-card {
  display: grid;
  gap: 0;
  border-radius: 20px;
  border: 1px solid rgba(255, 169, 79, 0.2);
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.96), rgba(16, 9, 3, 0.96));
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
  position: relative;
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

.elf-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 218, 157, 0.3), transparent);
  pointer-events: none;
  z-index: 1;
}

.elf-card:hover {
  transform: translateY(-5px);
  border-color: rgba(255, 194, 107, 0.4);
  box-shadow: 0 24px 40px rgba(255, 140, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

/* 元素边框 */
.elf-card.element-fire {
  border-top: 3px solid rgba(252, 129, 129, 0.8);
}
.elf-card.element-fire:hover {
  border-color: rgba(252, 129, 129, 0.5);
  box-shadow: 0 24px 40px rgba(252, 129, 129, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.elf-card.element-water {
  border-top: 3px solid rgba(99, 179, 237, 0.8);
}
.elf-card.element-water:hover {
  border-color: rgba(99, 179, 237, 0.5);
  box-shadow: 0 24px 40px rgba(99, 179, 237, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.elf-card.element-grass {
  border-top: 3px solid rgba(104, 211, 145, 0.8);
}
.elf-card.element-grass:hover {
  border-color: rgba(104, 211, 145, 0.5);
  box-shadow: 0 24px 40px rgba(104, 211, 145, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.elf-card__media {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 140px;
  background:
    radial-gradient(circle at 50% 35%, rgba(255, 173, 91, 0.1), transparent 58%),
    rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.elf-card__halo {
  position: absolute;
  inset: auto 16% 10%;
  height: 28px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 196, 112, 0.28), transparent);
  filter: blur(10px);
}

.elf-card .elf-image {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 12px;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  background: rgba(0, 0, 0, 0.3);
}

.elf-card:hover .elf-image {
  transform: translateY(-2px);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.4),
    0 0 0 4px rgba(255, 255, 255, 0.05);
}

/* 元素图片边框 */
.elf-card.element-fire .elf-image {
  border-color: rgba(252, 129, 129, 0.5);
  box-shadow: 0 4px 12px rgba(252, 129, 129, 0.3);
}
.elf-card.element-water .elf-image {
  border-color: rgba(99, 179, 237, 0.5);
  box-shadow: 0 4px 12px rgba(99, 179, 237, 0.3);
}
.elf-card.element-grass .elf-image {
  border-color: rgba(104, 211, 145, 0.5);
  box-shadow: 0 4px 12px rgba(104, 211, 145, 0.3);
}

.elf-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 1.5rem;
}

.elf-info h3 {
  margin: 0;
  color: #fff4df;
  font-size: 1.25rem;
  font-weight: 800;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 元素徽章 */
.element-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.element-badge.element-fire { background: rgba(252, 129, 129, 0.1); color: #fc8181; border-color: rgba(252, 129, 129, 0.3); }
.element-badge.element-water { background: rgba(99, 179, 237, 0.1); color: #63b3ed; border-color: rgba(99, 179, 237, 0.3); }
.element-badge.element-grass { background: rgba(104, 211, 145, 0.1); color: #68d391; border-color: rgba(104, 211, 145, 0.3); }

/* 元信息标签 */
.elf-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}

.elf-meta__item {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: #cbd5e0;
  font-size: 0.8rem;
  font-weight: 600;
}

.active-tag {
  background: rgba(104, 211, 145, 0.15);
  border: 1px solid rgba(104, 211, 145, 0.4) !important;
  color: #9ae6b4 !important;
}

.standby-tag {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: #a0aec0 !important;
}

/* 属性统计 */
.elf-stats {
  display: grid;
  gap: 8px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.stat-label {
  color: #a0aec0;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.stat-value {
  color: #f8f1e4;
  font-size: 0.95rem;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.stat-hp { color: #68d391; }
.stat-mp { color: #b794f4; }
.stat-max { color: #f6e05e; font-size: 0.85rem; }

/* 操作区 */
.elf-actions-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 5px;
  padding-top: 15px;
  border-top: 1px dashed rgba(255, 255, 255, 0.1);
}

.battle-order-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.3);
  color: #fff4df;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' stroke='rgba(255,255,255,0.5)' stroke-width='2' fill='none'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
  transition: all 0.3s ease;
}

.order-select option {
  background: #1a1f2e;
  color: #fff4df;
}

.order-select:focus {
  outline: none;
  border-color: rgba(255, 140, 0, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 140, 0, 0.15);
}

.order-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(90deg, #48bb78, #38a169);
  color: white;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(72, 187, 120, 0.3);
  white-space: nowrap;
}

.order-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 15px rgba(72, 187, 120, 0.5);
}

.order-btn:active {
  transform: translateY(0);
}

/* 详情按钮 */
.detail-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(90deg, #ff9c3a, #ff7a1a);
  color: white;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(255, 122, 26, 0.3);
  text-align: center;
}

.detail-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 15px rgba(255, 122, 26, 0.5);
}

.detail-btn:active {
  transform: translateY(0);
}

.detail-btn.element-fire {
  background: linear-gradient(90deg, #fc8181, #e53e3e);
  box-shadow: 0 4px 10px rgba(229, 62, 62, 0.3);
}

.detail-btn.element-water {
  background: linear-gradient(90deg, #63b3ed, #3182ce);
  box-shadow: 0 4px 10px rgba(49, 130, 206, 0.3);
}

.detail-btn.element-grass {
  background: linear-gradient(90deg, #68d391, #38a169);
  box-shadow: 0 4px 10px rgba(56, 161, 105, 0.3);
}

/* 响应式 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .main-content {
    padding: 0 15px;
  }
  
  .page-header {
    text-align: center;
  }

  .page-header h1 {
    font-size: 2.2rem;
  }
  
  .elves-list {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}
</style>
