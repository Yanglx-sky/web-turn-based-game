<template>
  <div class="shop-container">
    <GameTopNav />

    <h1>装备商店</h1>
    
    <!-- 金币显示 -->
    <div class="gold-display">
      <span class="gold-icon">💰</span>
      <span class="gold-amount">{{ gold }}</span>
    </div>

    <!-- 装备分类 -->
    <div class="equip-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 装备列表 -->
    <div class="equip-list">
      <div
        v-for="equip in equips"
        :key="equip.id"
        class="equip-card"
        :class="getPriceTierClass(equip.price)"
      >
        <div class="equip-image">
          <img
            :src="getEquipImage(equip)"
            :alt="equip.name"
          />
        </div>
        <div class="equip-info">
          <h3>{{ equip.name }}</h3>
          <div class="equip-stats">
            <template v-if="equip.itemType === 2">
              <!-- 药品显示描述 -->
              <span class="stat hp">{{ equip.name }}</span>
              <span class="stat mp">{{ equip.description || '使用后恢复生命值和魔法值' }}</span>
            </template>
            <template v-else>
              <!-- 装备显示属性加成 -->
              <span v-if="equip.atk > 0" class="stat atk">攻击: +{{ equip.atk }}</span>
              <span v-if="equip.def > 0" class="stat def">防御: +{{ equip.def }}</span>
              <span v-if="equip.hp > 0" class="stat hp">生命: +{{ equip.hp }}</span>
              <span v-if="equip.mp > 0" class="stat mp">蓝量: +{{ equip.mp }}</span>
              <span v-if="equip.speed > 0" class="stat speed">速度: +{{ equip.speed }}</span>
            </template>
          </div>
          <div class="equip-price">
            <span class="price">{{ equip.price }} 金币</span>
            <button
              class="buy-btn"
              :class="getPriceTierClass(equip.price)"
              @click="buyEquip(equip.id)"
              :disabled="false"
            >
              购买
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi } from '../api/shop'
import { userApi } from '../api/user'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()
const gold = ref(0)
const activeTab = ref(0)
const equips = ref([])

const tabs = [
  { label: '全部', value: 0 },
  { label: '武器', value: 3 },
  { label: '防具', value: 4 },
  { label: '药品', value: 2 }
]

// 导航到其他页面
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('user')
  router.push('/')
}

// 根据价格获取稀有度CSS类名
const getPriceTierClass = (price) => {
  if (price >= 10000) return 'tier-legendary'
  if (price >= 5000) return 'tier-epic'
  if (price >= 2000) return 'tier-rare'
  return 'tier-common'
}

// 获取装备图片
const getEquipImage = (item) => {
  // 根据装备名称返回对应的图片路径
  if (item.itemType === 1) {
    // 装备
    switch (item.name) {
      case '不祥征兆':
        return new URL('../assets/photo/equip/不祥征兆.jpg', import.meta.url).href
      case '圣杯':
        return new URL('../assets/photo/equip/圣杯.jpg', import.meta.url).href
      case '影刃':
        return new URL('../assets/photo/equip/影刃.jpg', import.meta.url).href
      case '破军':
        return new URL('../assets/photo/equip/破军.png', import.meta.url).href
      case '霸者重装':
        return new URL('../assets/photo/equip/霸者重装.webp', import.meta.url).href
      default:
        if (item.type === 1) {
          // 武器
          return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=sword%20weapon%20fantasy%20style&image_size=square'
        } else {
          // 防具
          return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=shield%20armor%20fantasy%20style&image_size=square'
        }
    }
  } else if (item.itemType === 2) {
    // 药品
    if (item.name === '血瓶') {
      return new URL('../assets/photo/equip/血瓶.jpg', import.meta.url).href
    } else if (item.name === '蓝瓶') {
      return new URL('../assets/photo/equip/蓝瓶.jpg', import.meta.url).href
    } else {
      return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=health%20potion%20blue%20potion%20fantasy%20style&image_size=square'
    }
  } else {
    return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=shield%20armor%20fantasy%20style&image_size=square'
  }
}

// 购买装备
const buyEquip = async (shopItemId) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    alert('请先登录')
    return
  }
  
  const user = JSON.parse(userStr)
  
  try {
    // 查找要购买的商品
    const item = equips.value.find(item => item.id === shopItemId)
    if (!item) {
      alert('商品不存在')
      return
    }
    
    // 检查金币是否足够
    if (gold.value < item.price) {
      alert('金币不足，无法购买')
      return
    }
    
    // 调用购买API
    const response = await shopApi.buyItem(shopItemId)
    if (response.code === 200) {
      alert('购买成功！')
      // 刷新金币数量
      loadUserGold()
    } else {
      alert('购买失败: ' + response.msg)
    }
  } catch (error) {
    console.error('购买装备失败:', error)
    alert('网络错误，购买失败')
  }
}

// 加载用户金币
const loadUserGold = async () => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return
  
  const user = JSON.parse(userStr)
  
  try {
    console.log('开始加载用户金币')
    const response = await userApi.getUserAsset()
    console.log('获取金币响应:', response)
    if (response.code === 200) {
      gold.value = response.data
      console.log('加载金币成功:', gold.value)
    } else {
      console.error('获取金币失败:', response.msg)
    }
  } catch (error) {
    console.error('加载金币失败:', error)
  }
}

// 加载装备列表
const loadEquips = async () => {
  try {
    console.log('开始加载装备列表，类型:', activeTab.value)
    let response
    if (activeTab.value === 0) {
      response = await shopApi.getAllItems()
    } else if (activeTab.value === 3) {
      // 加载武器，type=3
      response = await shopApi.getItemsByType(3)
    } else if (activeTab.value === 4) {
      // 加载防具，type=4
      response = await shopApi.getItemsByType(4)
    } else if (activeTab.value === 2) {
      // 加载药品，type=2
      response = await shopApi.getItemsByType(2)
    } else {
      response = await shopApi.getItemsByType(activeTab.value)
    }
    
    console.log('获取装备响应:', response)
    if (response.code === 200) {
      equips.value = response.data
      console.log('加载装备成功，数量:', equips.value.length)
      console.log('装备详情:', equips.value)
    } else {
      console.error('获取装备失败:', response.msg)
    }
  } catch (error) {
    console.error('加载装备失败:', error)
  }
}

// 监听标签页变化
const handleTabChange = () => {
  loadEquips()
}

// 监听activeTab变化，自动加载对应类型的装备
watch(activeTab, () => {
  loadEquips()
})

onMounted(() => {
  loadUserGold()
  loadEquips()
})
</script>

<style scoped>
.shop-container {
  min-height: 100vh;
  background: white;
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 2rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.gold-display {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 300px;
  margin-left: auto;
  margin-right: auto;
}

.gold-icon {
  font-size: 24px;
  margin-right: 10px;
}

.gold-amount {
  font-size: 24px;
  font-weight: bold;
  color: #ffd700;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.equip-tabs {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.tab-btn {
  padding: 10px 20px;
  border: 2px solid #ff8c00;
  background: white;
  color: #ff8c00;
  border-radius: 25px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  background: #ff8c00;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.4);
}

.tab-btn.active {
  background: #ff8c00;
  color: white;
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.4);
}

.equip-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.equip-card {
  display: flex;
  background: var(--color-neutral-100);
  border-radius: 10px;
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.equip-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

/* 稀有度颜色 - 价格分层 */
.equip-card.tier-common {
  border-color: var(--color-common);
}

.equip-card.tier-rare {
  border-color: var(--color-rare);
  background: var(--color-rare-bg);
}

.equip-card.tier-epic {
  border-color: var(--color-epic);
  background: var(--color-epic-bg);
}

.equip-card.tier-legendary {
  border-color: var(--color-legendary);
  background: var(--color-legendary-bg);
  box-shadow: var(--shadow-md), 0 0 20px oklch(0.72 0.16 85 / 0.3);
}

.equip-image {
  flex: 0 0 80px;
  margin-right: 20px;
}

.equip-image img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.equip-info {
  flex: 1;
}

.equip-info h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.equip-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.stat {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.stat.atk {
  background: rgba(255, 99, 132, 0.2);
  color: #dc3545;
}

.stat.def {
  background: rgba(54, 162, 235, 0.2);
  color: #007bff;
}

.stat.hp {
  background: rgba(75, 192, 192, 0.2);
  color: #28a745;
}

.stat.mp {
  background: rgba(153, 102, 255, 0.2);
  color: #6f42c1;
}

.stat.speed {
  background: oklch(0.80 0.12 85 / 0.2);
  color: var(--color-gold);
}

.equip-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: var(--color-gold);
}

.buy-btn {
  padding: 8px 16px;
  background: var(--color-brand);
  color: white;
  border: none;
  border-radius: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.buy-btn:hover:not(:disabled) {
  background: var(--color-brand-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px oklch(0.55 0.20 50 / 0.4);
}

.buy-btn:disabled {
  background: var(--color-neutral-400);
  cursor: not-allowed;
}

/* 稀有度按钮颜色 */
.buy-btn.tier-common {
  background: var(--color-common);
}

.buy-btn.tier-rare {
  background: var(--color-rare);
}

.buy-btn.tier-epic {
  background: var(--color-epic);
}

.buy-btn.tier-legendary {
  background: var(--color-legendary);
  box-shadow: 0 0 15px oklch(0.72 0.16 85 / 0.5);
}

.buy-btn.tier-legendary:hover:not(:disabled) {
  box-shadow: 0 0 25px oklch(0.72 0.16 85 / 0.7);
}
</style>
