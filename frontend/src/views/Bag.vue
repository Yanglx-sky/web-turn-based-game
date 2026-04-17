<template>
  <div class="bag-container">
    <!-- 顶部导航栏 -->
    <nav class="nav-bar">
      <div class="nav-logo">洛克王国</div>
      <div class="nav-menu">
        <button @click="navigateTo('/')" class="nav-btn">首页</button>
        <button @click="navigateTo('/elves')" class="nav-btn">我的精灵</button>
        <button @click="navigateTo('/pve')" class="nav-btn">冒险</button>
        <button @click="navigateTo('/shop')" class="nav-btn">商店</button>
        <button @click="navigateTo('/bag')" class="nav-btn active">背包</button>
        <button @click="navigateTo('/train')" class="nav-btn">训练</button>
        <button @click="navigateTo('/rank')" class="nav-btn">排行榜</button>
        <button @click="navigateTo('/ai')" class="nav-btn">AI助手</button>
        <button @click="navigateTo('/chat')" class="nav-btn">聊天</button>
        <button @click="navigateTo('/achievement')" class="nav-btn">成就</button>
        <button @click="logout" class="nav-btn">退出</button>
      </div>
    </nav>

    <h1>我的背包</h1>
    
    <!-- 物品分类 -->
    <div class="item-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.value"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 物品列表 -->
    <div class="item-list">
      <div
        v-for="item in items"
        :key="item.id || item.itemId"
        class="item-card"
        :class="getPriceTierClass(item.price)"
      >
        <div class="item-image">
            <img
              :src="getItemImage(item)"
              :alt="item.name"
            />
            <div v-if="item.isWorn" class="bound-tag">已绑定</div>
          </div>
        <div class="item-info">
          <h3>{{ item.name }}</h3>
          <div class="item-stats">
            <template v-if="item.itemType === 2">
              <!-- 药品显示描述和数量 -->
              <span class="stat hp">{{ item.description || '使用后恢复生命值和魔法值' }}</span>
              <span class="stat count">数量: {{ item.count || 0 }}</span>
            </template>
            <template v-else>
              <!-- 装备显示属性加成和数量 -->
              <span v-if="item.atk > 0" class="stat atk">攻击: +{{ item.atk }}</span>
              <span v-if="item.def > 0" class="stat def">防御: +{{ item.def }}</span>
              <span v-if="item.hp > 0" class="stat hp">生命: +{{ item.hp }}</span>
              <span v-if="item.mp > 0" class="stat mp">蓝量: +{{ item.mp }}</span>
              <span v-if="item.speed > 0" class="stat speed">速度: +{{ item.speed }}</span>
              <span v-if="item.count > 1" class="stat count">数量: ×{{ item.count }}</span>
            </template>
          </div>
          <div class="item-actions">
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
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi } from '../api/shop'
import { potionApi } from '../api/potion'

const router = useRouter()
const activeTab = ref(3)
const items = ref([])

const tabs = [
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
  if (!price) return 'tier-common'
  if (price >= 10000) return 'tier-legendary'
  if (price >= 5000) return 'tier-epic'
  if (price >= 2000) return 'tier-rare'
  return 'tier-common'
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
.bag-container {
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

.item-tabs {
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

.item-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.item-card {
  display: flex;
  background: var(--color-neutral-100);
  border-radius: 10px;
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.item-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

/* 稀有度颜色 - 价格分层 */
.item-card.tier-common {
  border-color: var(--color-common);
}

.item-card.tier-rare {
  border-color: var(--color-rare);
  background: var(--color-rare-bg);
}

.item-card.tier-epic {
  border-color: var(--color-epic);
  background: var(--color-epic-bg);
}

.item-card.tier-legendary {
  border-color: var(--color-legendary);
  background: var(--color-legendary-bg);
  box-shadow: var(--shadow-md), 0 0 20px oklch(0.72 0.16 85 / 0.3);
}

.bound-tag {
  position: absolute;
  top: -5px;
  right: -5px;
  background: var(--color-neutral-600);
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
}

.item-image {
  flex: 0 0 80px;
  margin-right: 20px;
  position: relative;
}

.item-image img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.worn-tag {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff5722;
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.item-info {
  flex: 1;
}

.item-info h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.item-stats {
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
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.stat.count {
  background: rgba(54, 162, 235, 0.2);
  color: #007bff;
  margin-top: 5px;
  display: inline-block;
}

.item-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 8px 16px;
  background: var(--color-brand);
  color: white;
  border: none;
  border-radius: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: var(--color-brand-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px oklch(0.55 0.20 50 / 0.4);
}

/* 稀有度按钮颜色 */
.action-btn.tier-common {
  background: var(--color-common);
}

.action-btn.tier-rare {
  background: var(--color-rare);
}

.action-btn.tier-epic {
  background: var(--color-epic);
}

.action-btn.tier-legendary {
  background: var(--color-legendary);
  box-shadow: 0 0 15px oklch(0.72 0.16 85 / 0.5);
}
</style>