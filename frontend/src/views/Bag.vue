<template>
  <div class="bag-container">
    <GameTopNav />

    <!-- 主内容区 -->
    <div class="main-content">
      <div class="bag-top-bar">
        <div class="page-header">
          <p class="section-eyebrow">INVENTORY STATION</p>
          <h1>我的背包</h1>
        </div>
        
        <!-- 背包概览信息 -->
        <div class="bag-overview">
          <div class="overview-item">
            <span class="overview-label">当前分类</span>
            <span class="overview-value">{{ activeTabMeta.label }}</span>
          </div>
          <div class="overview-divider"></div>
          <div class="overview-item">
            <span class="overview-label">物品总数</span>
            <span class="overview-value">{{ items.length }}</span>
          </div>
        </div>
      </div>
      
      <div class="bag-content-wrapper">
        <!-- 侧边栏/顶部信息：分类 -->
        <div class="bag-controls">
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

        <!-- 物品列表 -->
        <div class="equip-list-container">
          <div v-if="!items || items.length === 0" class="empty-state">
            暂无该分类物品
          </div>
          <div v-else class="equip-list">
            <div
              v-for="item in items"
              :key="item.id || item.itemId"
              class="equip-card"
              :class="getPriceTierClass(item.price)"
            >
              <div class="equip-image">
                <img
                  :src="getItemImage(item)"
                  :alt="item.name"
                />
                <div v-if="item.isWorn" class="bound-tag">已绑定</div>
              </div>
              <div class="equip-info">
                <div class="equip-headline">
                  <h3>{{ item.name }}</h3>
                  <span class="tier-pill" :class="getPriceTierClass(item.price)">{{ getPriceTierLabel(item.price) }}</span>
                </div>
                <div class="equip-stats">
                  <template v-if="item.itemType === 2">
                    <span class="stat hp desc">{{ item.description || '使用后恢复生命值和魔法值' }}</span>
                    <span class="stat count">数量: {{ item.count || 0 }}</span>
                  </template>
                  <template v-else>
                    <span v-if="item.atk > 0" class="stat atk">攻击 +{{ item.atk }}</span>
                    <span v-if="item.def > 0" class="stat def">防御 +{{ item.def }}</span>
                    <span v-if="item.hp > 0" class="stat hp">生命 +{{ item.hp }}</span>
                    <span v-if="item.mp > 0" class="stat mp">蓝量 +{{ item.mp }}</span>
                    <span v-if="item.speed > 0" class="stat speed">速度 +{{ item.speed }}</span>
                    <span v-if="item.count > 1" class="stat count">数量: ×{{ item.count }}</span>
                  </template>
                </div>
                <div class="equip-action">
                  <div class="equip-price"></div>
                  <button
                    class="action-btn"
                    :class="getPriceTierClass(item.price)"
                    @click="viewItemDetail(item)"
                    v-if="item.itemType !== 2"
                  >
                    查看
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
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi } from '../api/shop'
import { potionApi } from '../api/potion'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()
const activeTab = ref(3)
const items = ref([])

const tabs = [
  { label: '武器', value: 3 },
  { label: '防具', value: 4 },
  { label: '药品', value: 2 }
]

const activeTabMeta = computed(() => tabs.find(tab => tab.value === activeTab.value) || tabs[0])

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
  if (!price) return 'tier-common'
  if (price >= 10000) return 'tier-legendary'
  if (price >= 5000) return 'tier-epic'
  if (price >= 2000) return 'tier-rare'
  return 'tier-common'
}

const getPriceTierLabel = (price) => {
  const tier = getPriceTierClass(price)
  if (tier === 'tier-legendary') return '传说'
  if (tier === 'tier-epic') return '史诗'
  if (tier === 'tier-rare') return '稀有'
  return '普通'
}

// 获取物品图片
const getItemImage = (item) => {
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
    return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=item%20fantasy%20style&image_size=square'
  }
}

// 使用物品
const useItem = async (item) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    alert('请先登录')
    return
  }

  const user = JSON.parse(userStr)

  // 这里需要选择要使用药品的精灵
  // 简化处理，使用第一个精灵
  try {
    // 这里应该获取用户的精灵列表，让用户选择
    // 暂时使用一个默认精灵ID，实际项目中需要修改
    const elfId = 1
    const response = await potionApi.usePotion(user.id, elfId, item.itemId)
    if (response.code === 200) {
      alert('使用成功！')
      // 重新加载背包
      loadItems()
    } else {
      alert('使用失败: ' + response.msg)
    }
  } catch (error) {
    console.error('使用物品失败:', error)
    alert('网络错误，使用失败')
  }
}

// 查看物品详情
const viewItemDetail = (item) => {
  // 这里可以实现查看物品详情的逻辑
  alert(`物品详情: ${item.name}`)
}

// 加载物品列表
const loadItems = async () => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    alert('请先登录')
    return
  }

  const user = JSON.parse(userStr)

  try {
    console.log('开始加载物品列表，类型:', activeTab.value)
    let response
    if (activeTab.value === 2) {
      // 加载药品
      response = await potionApi.getUserPotions(user.id)
      if (response.code === 200) {
        items.value = response.data.map(potion => {
          return {
            id: potion.id,
            itemType: 2,
            itemId: potion.potionConfigId,
            name: potion.name || '药品',
            description: potion.description || '使用后恢复生命值和魔法值',
            count: potion.count
          }
        })
      }
    } else {
      // 加载装备
      response = await shopApi.getUserEquipsByType(user.id, activeTab.value)
      if (response.code === 200) {
        items.value = response.data
      }
    }

    console.log('加载物品成功，数量:', items.value.length)
    console.log('物品详情:', items.value)
  } catch (error) {
    console.error('加载物品失败:', error)
  }
}

// 监听标签页变化
watch(activeTab, () => {
  loadItems()
})

onMounted(() => {
  loadItems()
})
</script>

<style scoped>
/* 全局样式 */
.bag-container {
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

.bag-top-bar {
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

/* 概览信息 */
.bag-overview {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.98));
  border: 1px solid rgba(255, 169, 79, 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 12px 30px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.overview-divider {
  width: 1px;
  height: 30px;
  background: rgba(255, 255, 255, 0.1);
}

.overview-label {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}

.overview-value {
  font-size: 1.2rem;
  font-weight: 800;
  color: #ff9c3a;
  text-shadow: 0 0 10px rgba(255, 156, 58, 0.3);
}

/* 布局 */
.bag-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.bag-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

/* 装备分类 */
.equip-tabs {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 30px;
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

/* 物品列表 */
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
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
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
.equip-card.tier-common { border-top-color: #a0aec0; }
.equip-card.tier-common:hover { border-color: rgba(160, 174, 192, 0.4); box-shadow: 0 24px 40px rgba(160, 174, 192, 0.15); }

.equip-card.tier-rare { border-top-color: #4299e1; }
.equip-card.tier-rare:hover { border-color: rgba(66, 153, 225, 0.4); box-shadow: 0 24px 40px rgba(66, 153, 225, 0.15); }

.equip-card.tier-epic { border-top-color: #9f7aea; }
.equip-card.tier-epic:hover { border-color: rgba(159, 122, 234, 0.4); box-shadow: 0 24px 40px rgba(159, 122, 234, 0.15); }

.equip-card.tier-legendary {
  border-top-color: #ffd700;
  background: linear-gradient(180deg, rgba(35, 25, 5, 0.96), rgba(20, 15, 2, 0.96));
}
.equip-card.tier-legendary:hover { border-color: rgba(255, 215, 0, 0.4); box-shadow: 0 24px 40px rgba(255, 215, 0, 0.25); }

.equip-image {
  position: relative;
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

.bound-tag {
  position: absolute;
  top: 0;
  right: 0;
  padding: 2px 6px;
  border-bottom-left-radius: 8px;
  background: rgba(255, 156, 58, 0.9);
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
  backdrop-filter: blur(2px);
}

.equip-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.equip-headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.equip-info h3 {
  margin: 0;
  color: #fff4df;
  font-size: 1.25rem;
  font-weight: 800;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.equip-card.tier-rare h3 { color: #63b3ed; }
.equip-card.tier-epic h3 { color: #b794f4; }
.equip-card.tier-legendary h3 { color: #ffd700; text-shadow: 0 0 8px rgba(255, 215, 0, 0.3); }

.tier-pill {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.8);
}
.equip-card.tier-rare .tier-pill { border-color: rgba(66, 153, 225, 0.4); color: #63b3ed; background: rgba(66, 153, 225, 0.1); }
.equip-card.tier-epic .tier-pill { border-color: rgba(159, 122, 234, 0.4); color: #b794f4; background: rgba(159, 122, 234, 0.1); }
.equip-card.tier-legendary .tier-pill { border-color: rgba(255, 215, 0, 0.4); color: #ffd700; background: rgba(255, 215, 0, 0.1); }

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
.stat.count { color: #fff; border-color: rgba(255, 255, 255, 0.2); background: rgba(255, 255, 255, 0.1); }
.stat.desc { color: #cbd5e0; font-weight: normal; font-size: 0.85rem; width: 100%; }

.equip-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed rgba(255, 255, 255, 0.1);
}

/* 使用 action-btn 设计，类似于按钮 */
.action-btn {
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
  margin-left: auto;
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 15px rgba(255, 122, 26, 0.5);
}

.action-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* 按钮根据稀有度变色，与 Shop 一致 */
.action-btn.tier-common { background: linear-gradient(90deg, #718096, #4a5568); box-shadow: 0 4px 10px rgba(113, 128, 150, 0.3); }
.action-btn.tier-rare { background: linear-gradient(90deg, #4299e1, #2b6cb0); box-shadow: 0 4px 10px rgba(66, 153, 225, 0.3); }
.action-btn.tier-epic { background: linear-gradient(90deg, #9f7aea, #6b46c1); box-shadow: 0 4px 10px rgba(159, 122, 234, 0.3); }
.action-btn.tier-legendary { background: linear-gradient(90deg, #fada5e, #d4af37); color: #422006; box-shadow: 0 4px 10px rgba(255, 215, 0, 0.4); }

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .main-content {
    padding: 0 15px;
  }

  .bag-top-bar {
    flex-direction: column;
    align-items: center;
    gap: 15px;
  }
  
  .page-header {
    text-align: center;
  }
  
  .bag-controls {
    align-items: center;
  }
  
  .page-header h1 {
    font-size: 2.2rem;
  }

  .bag-overview {
    width: 100%;
    justify-content: center;
  }

  .equip-list {
    grid-template-columns: 1fr;
  }
}
</style>
