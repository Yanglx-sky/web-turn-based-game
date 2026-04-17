<template>
  <div class="shop-container">
    <GameTopNav />

    <!-- 主内容区 -->
    <div class="main-content">
      <div class="shop-top-bar">
        <div class="page-header">
          <p class="section-eyebrow">EQUIPMENT SHOP</p>
          <h1>装备商店</h1>
        </div>
        
        <!-- 金币显示 -->
        <div class="gold-display">
          <div class="gold-icon"></div>
          <div class="gold-text">
            <span class="gold-label">我的金币</span>
            <span class="gold-amount">{{ gold }}</span>
          </div>
        </div>
      </div>
      
      <div class="shop-content-wrapper">
        <!-- 侧边栏/顶部信息：分类 -->
        <div class="shop-controls">
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
        </div>

        <!-- 装备列表 -->
        <div class="equip-list-container">
          <div v-if="!equips || equips.length === 0" class="empty-state">
            暂无商品可购买
          </div>
          <div v-else class="equip-list">
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
                    <span class="stat hp desc">{{ equip.name }}</span>
                    <span class="stat mp desc">{{ equip.description || '使用后恢复生命值和魔法值' }}</span>
                  </template>
                  <template v-else>
                    <!-- 装备显示属性加成 -->
                    <span v-if="equip.atk > 0" class="stat atk">攻击 +{{ equip.atk }}</span>
                    <span v-if="equip.def > 0" class="stat def">防御 +{{ equip.def }}</span>
                    <span v-if="equip.hp > 0" class="stat hp">生命 +{{ equip.hp }}</span>
                    <span v-if="equip.mp > 0" class="stat mp">蓝量 +{{ equip.mp }}</span>
                    <span v-if="equip.speed > 0" class="stat speed">速度 +{{ equip.speed }}</span>
                  </template>
                </div>
                <div class="equip-action">
                  <div class="equip-price">
                    <span class="price-icon"></span>
                    <span class="price">{{ equip.price }}</span>
                  </div>
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
/* 全局样式 */
.shop-container {
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

.shop-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.page-header {
  text-align: left;
  margin-bottom: 0;
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

/* 商店布局 */
.shop-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.shop-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* 金币显示 */
.gold-display {
  display: flex;
  align-items: center;
  gap: 15px;
  background: linear-gradient(135deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.98));
  border: 1px solid rgba(255, 215, 0, 0.4);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 12px 25px;
  min-width: 250px;
  justify-content: center;
}

.gold-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: radial-gradient(circle, #ffe259, #ffa751);
  box-shadow: 0 0 15px rgba(255, 215, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.gold-icon::after {
  content: "💰";
  font-size: 20px;
}

.gold-text {
  display: flex;
  flex-direction: column;
}

.gold-label {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.gold-amount {
  font-size: 1.5rem;
  font-weight: 800;
  color: #ffd700;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.4);
}

/* 装备分类 */
.equip-tabs {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 24px;
  background: rgba(16, 9, 3, 0.8);
  border: 1px solid rgba(255, 169, 79, 0.3);
  color: rgba(255, 255, 255, 0.7);
  border-radius: 30px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(4px);
}

.tab-btn:hover {
  background: rgba(255, 140, 0, 0.1);
  border-color: rgba(255, 140, 0, 0.5);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 140, 0, 0.2);
}

.tab-btn.active {
  background: linear-gradient(90deg, #ff9c3a, #ff7a1a);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 122, 26, 0.4);
}

/* 装备列表 */
.equip-list-container {
  width: 100%;
}

.empty-state {
  text-align: center;
  padding: 4rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 1.2rem;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 20px;
  border: 1px dashed rgba(255, 169, 79, 0.2);
}

.equip-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  padding-bottom: 40px;
}

.equip-card {
  position: relative;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.96), rgba(16, 9, 3, 0.96));
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.4);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  display: flex;
  gap: 1.2rem;
  align-items: flex-start;
}

.equip-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 24px 40px rgba(255, 140, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

/* 稀有度效果 */
.equip-card.tier-common {
  border-top-color: #a0aec0;
}
.equip-card.tier-common:hover {
  border-color: rgba(160, 174, 192, 0.4);
  box-shadow: 0 24px 40px rgba(160, 174, 192, 0.15);
}

.equip-card.tier-rare {
  border-top-color: #4299e1;
}
.equip-card.tier-rare:hover {
  border-color: rgba(66, 153, 225, 0.4);
  box-shadow: 0 24px 40px rgba(66, 153, 225, 0.15);
}

.equip-card.tier-epic {
  border-top-color: #9f7aea;
}
.equip-card.tier-epic:hover {
  border-color: rgba(159, 122, 234, 0.4);
  box-shadow: 0 24px 40px rgba(159, 122, 234, 0.15);
}

.equip-card.tier-legendary {
  border-top-color: #ffd700;
  background: linear-gradient(180deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.96));
}
.equip-card.tier-legendary:hover {
  border-color: rgba(255, 215, 0, 0.4);
  box-shadow: 0 24px 40px rgba(255, 215, 0, 0.25);
}

.equip-image {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  background: rgba(0, 0, 0, 0.3);
}

.equip-card.tier-common .equip-image { border-color: rgba(160, 174, 192, 0.5); }
.equip-card.tier-rare .equip-image { border-color: rgba(66, 153, 225, 0.5); box-shadow: 0 0 10px rgba(66, 153, 225, 0.3); }
.equip-card.tier-epic .equip-image { border-color: rgba(159, 122, 234, 0.5); box-shadow: 0 0 10px rgba(159, 122, 234, 0.3); }
.equip-card.tier-legendary .equip-image { border-color: #ffd700; box-shadow: 0 0 15px rgba(255, 215, 0, 0.4); }

.equip-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.equip-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.equip-info h3 {
  margin: 0 0 10px 0;
  color: #fff4df;
  font-size: 1.25rem;
  font-weight: 800;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.equip-card.tier-rare h3 { color: #63b3ed; }
.equip-card.tier-epic h3 { color: #b794f4; }
.equip-card.tier-legendary h3 { color: #ffd700; text-shadow: 0 0 8px rgba(255, 215, 0, 0.3); }

.equip-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.stat {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat.atk { color: #fc8181; border-color: rgba(252, 129, 129, 0.3); background: rgba(252, 129, 129, 0.1); }
.stat.def { color: #63b3ed; border-color: rgba(99, 179, 237, 0.3); background: rgba(99, 179, 237, 0.1); }
.stat.hp { color: #68d391; border-color: rgba(104, 211, 145, 0.3); background: rgba(104, 211, 145, 0.1); }
.stat.mp { color: #b794f4; border-color: rgba(183, 148, 244, 0.3); background: rgba(183, 148, 244, 0.1); }
.stat.speed { color: #f6e05e; border-color: rgba(246, 224, 94, 0.3); background: rgba(246, 224, 94, 0.1); }
.stat.desc { color: #cbd5e0; font-weight: normal; font-size: 0.85rem; width: 100%; }

.equip-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed rgba(255, 255, 255, 0.1);
}

.equip-price {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-icon {
  width: 16px;
  height: 16px;
  background: radial-gradient(circle, #ffe259, #ffa751);
  border-radius: 50%;
  box-shadow: 0 0 5px rgba(255, 215, 0, 0.4);
}

.price {
  font-size: 1.1rem;
  font-weight: 800;
  color: #ffd700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.buy-btn {
  padding: 6px 16px;
  background: linear-gradient(90deg, #ff9c3a, #ff7a1a);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(255, 122, 26, 0.3);
}

.buy-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 15px rgba(255, 122, 26, 0.5);
}

.buy-btn:active:not(:disabled) {
  transform: translateY(0);
}

.buy-btn:disabled {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.5);
  box-shadow: none;
  cursor: not-allowed;
}

/* 稀有度按钮颜色重写 */
.buy-btn.tier-common { background: linear-gradient(90deg, #718096, #4a5568); box-shadow: 0 4px 10px rgba(113, 128, 150, 0.3); }
.buy-btn.tier-rare { background: linear-gradient(90deg, #4299e1, #2b6cb0); box-shadow: 0 4px 10px rgba(66, 153, 225, 0.3); }
.buy-btn.tier-epic { background: linear-gradient(90deg, #9f7aea, #6b46c1); box-shadow: 0 4px 10px rgba(159, 122, 234, 0.3); }
.buy-btn.tier-legendary { background: linear-gradient(90deg, #fada5e, #d4af37); color: #422006; box-shadow: 0 4px 10px rgba(255, 215, 0, 0.4); }

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .shop-top-bar {
    flex-direction: column;
    align-items: center;
    gap: 15px;
  }
  
  .page-header {
    text-align: center;
  }
  
  .shop-controls {
    flex-direction: column;
    align-items: center;
  }
  
  .page-header h1 {
    font-size: 2.2rem;
  }
}
</style>
