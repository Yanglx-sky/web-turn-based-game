<template>
  <div class="home-container">
    <GameTopNav active-path="/elves" />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <h1>精灵详情</h1>
      
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else-if="elfDetail" class="elf-info">
        <div class="elf-header">
          <img :src="getElfImage(elfDetail.elf.elfId)" :alt="elfDetail.elf.elfId" class="elf-image">
          <div class="elf-basic-info">
            <h2>精灵 {{ elfDetail.elf.elfId }}</h2>
            <p v-if="elfDetail.elf.fightOrder > 0" class="active-tag">出战：{{ elfDetail.elf.fightOrder }}号位</p>
            <p>系别: {{ getElementType(elfDetail.elf.elfId) }}</p>
          </div>
        </div>
        
        <div class="elf-stats">
          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-label">等级</span>
              <span class="stat-value">{{ elfDetail.elf.level }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">经验</span>
              <span class="stat-value">{{ elfDetail.elf.exp }}/{{ elfDetail.elf.expNeed }}</span>
            </div>
          </div>
          <div v-if="elfDetail.elf.level >= 10" class="stat-item level-status">
            <span class="stat-label">状态</span>
            <span class="stat-value">已满级</span>
          </div>
          
          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-label">HP</span>
              <span class="stat-value">{{ elfDetail.elf.maxHp }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">MP</span>
              <span class="stat-value">{{ elfDetail.elf.maxMp }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-label">攻击</span>
              <span class="stat-value">{{ elfDetail.elf.attack }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">防御</span>
              <span class="stat-value">{{ elfDetail.elf.defense }}</span>
            </div>
          </div>
          
          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-label">普攻伤害</span>
              <span class="stat-value">{{ elfDetail.elf.normalDamage }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">速度</span>
              <span class="stat-value">{{ elfDetail.elf.speed }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 装备管理 -->
      <div v-if="elfDetail" class="section">
        <h3>装备管理</h3>
        <div class="equipment-grid">
          <!-- 武器 -->
          <div class="equipment-slot">
            <h4>武器</h4>
            <div class="equipment-card" v-if="equippedWeapon">
              <div class="equipment-image">
                <img :src="getEquipImage(1, equippedWeapon.name)" :alt="equippedWeapon.name" />
              </div>
              <div class="equipment-details">
                <p>{{ equippedWeapon.name }}</p>
                <p v-if="equippedWeapon.atk > 0">攻击: +{{ equippedWeapon.atk }}</p>
              <p v-if="equippedWeapon.speed > 0">速度: +{{ equippedWeapon.speed }}</p>
                <button class="unequip-btn" @click="unequipWeapon">卸下</button>
              </div>
            </div>
            <div class="equipment-card empty" v-else>
              <p>未装备武器</p>
              <button class="equip-btn" @click="openWeaponModal">装备武器</button>
            </div>
          </div>
          
          <!-- 防具 -->
          <div class="equipment-slot">
            <h4>防具</h4>
            <div class="equipment-card" v-if="equippedArmor">
              <div class="equipment-image">
                <img :src="getEquipImage(2, equippedArmor.name)" :alt="equippedArmor.name" />
              </div>
              <div class="equipment-details">
                <p>{{ equippedArmor.name }}</p>
                <p v-if="equippedArmor.def > 0">防御: +{{ equippedArmor.def }}</p>
              <p v-if="equippedArmor.speed > 0">速度: +{{ equippedArmor.speed }}</p>
                <button class="unequip-btn" @click="unequipArmor">卸下</button>
              </div>
            </div>
            <div class="equipment-card empty" v-else>
              <p>未装备防具</p>
              <button class="equip-btn" @click="openArmorModal">装备防具</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="elfDetail" class="section">
        <h3>已解锁技能</h3>
        <div class="skill-list">
          <div v-for="skill in elfDetail.unlockedSkills" :key="skill.id" class="skill-item" :class="getElementColorClass(skill.elementType)">
            <div class="skill-header">
              <h4>{{ skill.skillName }}</h4>
              <span class="skill-type" :class="getElementColorClass(skill.elementType)">{{ getElementType(skill.elementType) }}</span>
            </div>
            <div class="skill-stats">
              <div class="skill-stat">
                <span class="skill-stat-label">伤害:</span>
                <span class="skill-stat-value">{{ skill.skillDamage }}</span>
              </div>
              <div class="skill-stat">
                <span class="skill-stat-label">消耗MP:</span>
                <span class="skill-stat-value">{{ skill.costMp }}</span>
              </div>
            </div>
            <p class="skill-description">{{ skill.des }}</p>
          </div>
          <p v-if="!elfDetail.unlockedSkills || elfDetail.unlockedSkills.length === 0" class="no-skills">
            暂无已解锁技能
          </p>
        </div>
      </div>

      <div v-if="elfDetail" class="section">
        <h3>可解锁技能</h3>
        <div class="skill-list">
          <div v-for="skill in elfDetail.unlockableSkills" :key="skill.id" class="skill-item" :class="getElementColorClass(skill.elementType)">
            <div class="skill-header">
              <h4>{{ skill.skillName }}</h4>
              <span class="skill-type" :class="getElementColorClass(skill.elementType)">{{ getElementType(skill.elementType) }}</span>
            </div>
            <div class="skill-stats">
              <div class="skill-stat">
                <span class="skill-stat-label">伤害:</span>
                <span class="skill-stat-value">{{ skill.skillDamage }}</span>
              </div>
              <div class="skill-stat">
                <span class="skill-stat-label">消耗MP:</span>
                <span class="skill-stat-value">{{ skill.costMp }}</span>
              </div>
              <div class="skill-stat">
                <span class="skill-stat-label">解锁等级:</span>
                <span class="skill-stat-value">{{ skill.unlockLevel }}</span>
              </div>
            </div>
            <p class="skill-description">{{ skill.des }}</p>
            <button @click="unlockSkill(skill.id)" class="unlock-btn" :class="getElementColorClass(skill.elementType)">解锁技能</button>
          </div>
          <p v-if="!elfDetail.unlockableSkills || elfDetail.unlockableSkills.length === 0" class="no-skills">
            暂无可解锁技能
          </p>
        </div>
      </div>

      <button class="back-btn" @click="goBack">返回我的精灵</button>
    </div>
    
    <!-- 装备选择弹窗 -->
    <div class="modal" v-if="showEquipModal">
      <div class="modal-content">
        <h3>{{ modalTitle }}</h3>
        <div class="equip-list">
          <div v-for="equip in availableEquips" :key="equip.id" 
            class="equip-item" 
            :class="{ 'bound': equip.isWorn && equip.elfId && equip.elfId !== elfDetail.elf.id, 'worn': equip.isWorn && equip.elfId && equip.elfId === elfDetail.elf.id }"
            @click="!equip.isWorn || (equip.isWorn && equip.elfId === elfDetail.elf.id) ? selectEquip(equip.itemId) : null"
          >
            <div class="equip-item-image">
              <img :src="getEquipImage(equip.type, equip.name)" :alt="equip.name" />
              <div v-if="equip.isWorn && equip.elfId && equip.elfId !== elfDetail.elf.id" class="bound-tag">已绑定</div>
              <div v-else-if="equip.isWorn && equip.elfId && equip.elfId === elfDetail.elf.id" class="bound-tag current">已装备</div>
            </div>
            <div class="equip-item-info">
              <h4>{{ equip.name }}</h4>
              <div class="equip-item-stats">
                <span v-if="equip.atk > 0" class="stat atk">攻击: +{{ equip.atk }}</span>
                <span v-if="equip.def > 0" class="stat def">防御: +{{ equip.def }}</span>
                <span v-if="equip.hp > 0" class="stat hp">生命: +{{ equip.hp }}</span>
                <span v-if="equip.mp > 0" class="stat mp">蓝量: +{{ equip.mp }}</span>
                <span v-if="equip.speed > 0" class="stat speed">速度: +{{ equip.speed }}</span>
                <span v-if="equip.count > 1" class="stat count">数量: ×{{ equip.count }}</span>
                <span v-if="equip.elfId && equip.elfId !== elfDetail.elf.id" class="stat bound">已绑定到其他精灵</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-buttons">
          <button @click="showEquipModal = false" class="cancel-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { elfApi } from '../api/elf'
import { skillApi } from '../api/skill'
import { equipApi } from '../api/equip'
import { useRouter, useRoute } from 'vue-router'
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



const router = useRouter()
const route = useRoute()
const elfDetail = ref(null)
const loading = ref(true)
const error = ref('')

// 装备相关变量
const equippedWeapon = ref(null)
const equippedArmor = ref(null)
const showEquipModal = ref(false)
const modalTitle = ref('')
const availableEquips = ref([])
const currentEquipType = ref(0)

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/auth')
}

onMounted(async () => {
  const elfId = route.params.id
  await loadElfDetail(elfId)
})

let loadElfDetail = async (elfId) => {
  try {
    loading.value = true
    error.value = ''
    console.log('加载精灵详情，精灵ID:', elfId)
    // 确保精灵ID是数字类型
    const numericElfId = parseInt(elfId)
    console.log('转换后的精灵ID:', numericElfId)
    const response = await elfApi.getElfDetail(numericElfId)
    console.log('精灵详情响应:', response)
    if (response.code === 200) {
      elfDetail.value = response.data
      console.log('精灵详情数据:', elfDetail.value)
    } else {
      console.error('获取精灵详情失败，响应码:', response.code)
      error.value = '获取精灵详情失败: ' + (response.message || '未知错误')
    }
  } catch (err) {
    console.error('获取精灵详情失败:', err)
    error.value = '网络错误，获取精灵详情失败: ' + (err.message || '未知错误')
  } finally {
    loading.value = false
  }
}

const unlockSkill = async (skillId) => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      const response = await skillApi.unlockSkill(userData.id, elfDetail.value.elf.elfId, skillId)
      if (response.code === 200) {
        await loadElfDetail(elfDetail.value.elf.id)
        alert('技能解锁成功！')
      } else {
        alert('技能解锁失败：' + response.message)
      }
    }
  } catch (err) {
    console.error('解锁技能失败:', err)
    alert('技能解锁失败：网络错误')
  }
}

const getElementType = (elfId) => {
  const types = {
    1: '火系',
    2: '水系',
    3: '草系'
  }
  return types[elfId] || '未知'
}

// 根据元素类型获取CSS颜色类名
const getElementColorClass = (elementType) => {
  switch (elementType) {
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

const goBack = () => {
  router.push('/elves')
}

// 获取装备图片
const getEquipImage = (type, name) => {
  // 根据装备名称返回对应的图片路径
  if (name) {
    switch (name) {
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
        if (type === 1) {
          // 武器
          return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=sword%20weapon%20fantasy%20style&image_size=square'
        } else {
          // 防具
          return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=shield%20armor%20fantasy%20style&image_size=square'
        }
    }
  } else {
    if (type === 1) {
      // 武器
      return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=sword%20weapon%20fantasy%20style&image_size=square'
    } else {
      // 防具
      return 'https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=shield%20armor%20fantasy%20style&image_size=square'
    }
  }
}

// 打开武器选择弹窗
const openWeaponModal = async () => {
  currentEquipType.value = 1
  modalTitle.value = '选择武器'
  await loadEquipsByType(3)
  showEquipModal.value = true
}

// 打开防具选择弹窗
const openArmorModal = async () => {
  currentEquipType.value = 2
  modalTitle.value = '选择防具'
  await loadEquipsByType(4)
  showEquipModal.value = true
}

// 加载指定类型的装备
  const loadEquipsByType = async (type) => {
    try {
      const userStr = localStorage.getItem('user')
      if (!userStr) return
      
      const user = JSON.parse(userStr)
      const response = await equipApi.getUserEquipsByType(type)
      if (response.code === 200) {
        // 显示所有装备，包括已绑定的
        availableEquips.value = response.data
      }
    } catch (error) {
      console.error('加载装备失败:', error)
    }
  }

// 选择装备
const selectEquip = async (equipId) => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    alert('请先登录')
    return
  }
  
  const user = JSON.parse(userStr)
  
  try {
    let response
    if (currentEquipType.value === 1) {
      response = await equipApi.equipWeapon(elfDetail.value.elf.id, equipId)
    } else {
      response = await equipApi.equipArmor(elfDetail.value.elf.id, equipId)
    }
    
    if (response.code === 200) {
      alert('装备成功！')
      showEquipModal.value = false
      await loadElfDetail(elfDetail.value.elf.id)
      await loadEquippedItems()
    } else {
      alert('装备失败: ' + response.msg)
    }
  } catch (error) {
    console.error('装备失败:', error)
    alert('网络错误，装备失败')
  }
}

// 卸下武器
  const unequipWeapon = async () => {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      alert('请先登录')
      return
    }
    
    try {
      const response = await equipApi.unequipWeapon(elfDetail.value.elf.id)
      if (response.code === 200) {
        alert('武器卸下成功！')
        // 立即更新前端状态
        equippedWeapon.value = null
        // 然后重新加载精灵详情和装备信息
        await loadElfDetail(elfDetail.value.elf.id)
        await loadEquippedItems()
        // 重新加载可用装备列表，更新绑定状态
        if (showEquipModal.value) {
          await loadEquipsByType(currentEquipType.value === 1 ? 3 : 4)
        }
      } else {
        alert('卸下失败: ' + response.msg)
      }
    } catch (error) {
      console.error('卸下武器失败:', error)
      alert('网络错误，卸下失败')
    }
  }
  
  // 卸下防具
  const unequipArmor = async () => {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      alert('请先登录')
      return
    }
    
    try {
      const response = await equipApi.unequipArmor(elfDetail.value.elf.id)
      if (response.code === 200) {
        alert('防具卸下成功！')
        // 立即更新前端状态
        equippedArmor.value = null
        // 然后重新加载精灵详情和装备信息
        await loadElfDetail(elfDetail.value.elf.id)
        await loadEquippedItems()
        // 重新加载可用装备列表，更新绑定状态
        if (showEquipModal.value) {
          await loadEquipsByType(currentEquipType.value === 1 ? 3 : 4)
        }
      } else {
        alert('卸下失败: ' + response.msg)
      }
    } catch (error) {
      console.error('卸下防具失败:', error)
      alert('网络错误，卸下失败')
    }
  }

// 加载已装备的物品
const loadEquippedItems = async () => {
  if (!elfDetail.value) return
  
  try {
    // 这里应该根据精灵的weaponId和armorId获取装备详情
    // 暂时使用模拟数据
    if (elfDetail.value.elf.weaponId) {
      const weaponResponse = await equipApi.getEquipById(elfDetail.value.elf.weaponId)
      if (weaponResponse.code === 200) {
        equippedWeapon.value = weaponResponse.data
      }
    } else {
      equippedWeapon.value = null
    }
    
    if (elfDetail.value.elf.armorId) {
      const armorResponse = await equipApi.getEquipById(elfDetail.value.elf.armorId)
      if (armorResponse.code === 200) {
        equippedArmor.value = armorResponse.data
      }
    } else {
      equippedArmor.value = null
    }
  } catch (error) {
    console.error('加载已装备物品失败:', error)
  }
}

// 在加载精灵详情后加载装备信息
const originalLoadElfDetail = loadElfDetail
loadElfDetail = async (elfId) => {
  await originalLoadElfDetail(elfId)
  await loadEquippedItems()
}
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

/* 主内容区 */
.main-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  margin-top: 2rem;
  margin-bottom: 2rem;
  backdrop-filter: blur(10px);
}

h1 {
  text-align: center;
  margin-bottom: 2rem;
  color: #ff6b00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
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

/* 精灵信息 */
.elf-info {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.elf-header {
  display: flex;
  align-items: center;
  gap: 2rem;
  margin-bottom: 2rem;
  position: relative;
}

.elf-image {
  width: 180px;
  height: 180px;
  object-fit: cover;
  border-radius: 50%;
  border: 4px solid #4CAF50;
  box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
  transition: all 0.3s ease;
}

.elf-image:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 25px rgba(76, 175, 80, 0.6);
}

.elf-basic-info {
  flex: 1;
}

.elf-basic-info h2 {
  color: #333;
  margin-bottom: 1rem;
  font-size: 1.8rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.active-tag {
  display: inline-block;
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  padding: 0.5rem 1.2rem;
  border-radius: 20px;
  font-size: 1rem;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(76, 175, 80, 0.3);
  margin-top: 0.5rem;
}

/* 精灵属性 */
.elf-stats {
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
  padding: 1.5rem;
  border: 1px solid rgba(102, 102, 102, 0.2);
}

.stat-row {
  display: flex;
  gap: 2rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.stat-item {
  flex: 1;
  min-width: 200px;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-label {
  font-weight: 600;
  color: #666;
}

.stat-value {
  font-weight: bold;
  color: #333;
  font-size: 1.1rem;
}

/* 已满级状态框 */
.level-status {
  min-width: 200px;
  margin: 0 auto;
  margin-bottom: 1rem;
  height: auto;
  padding: 1rem;
}

/* 技能部分 */
.section {
  margin-bottom: 3rem;
}

.section h3 {
  color: var(--color-grass);
  margin-bottom: 1.5rem;
  text-align: center;
  font-size: 1.5rem;
  border-bottom: 2px solid oklch(0.55 0.18 150 / 0.3);
  padding-bottom: 0.5rem;
}

.skill-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.skill-item {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: var(--shadow-md);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.skill-item:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

/* 技能元素类型颜色 */
.skill-item.element-fire {
  border-color: var(--color-fire);
}

.skill-item.element-fire:hover {
  box-shadow: var(--shadow-lg), 0 0 20px oklch(0.55 0.22 30 / 0.3);
}

.skill-item.element-water {
  border-color: var(--color-water);
}

.skill-item.element-water:hover {
  box-shadow: var(--shadow-lg), 0 0 20px oklch(0.55 0.18 240 / 0.3);
}

.skill-item.element-grass {
  border-color: var(--color-grass);
}

.skill-item.element-grass:hover {
  box-shadow: var(--shadow-lg), 0 0 20px oklch(0.55 0.18 150 / 0.3);
}

.skill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.skill-header h4 {
  color: var(--color-neutral-700);
  margin: 0;
  font-size: 1.2rem;
}

.skill-type {
  background: var(--color-grass);
  color: white;
  padding: 0.3rem 0.8rem;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: bold;
}

/* 技能类型徽章元素颜色 */
.skill-type.element-fire {
  background: var(--color-fire);
}

.skill-type.element-water {
  background: var(--color-water);
}

.skill-type.element-grass {
  background: var(--color-grass);
}

.skill-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}

.skill-stat {
  background: rgba(245, 245, 245, 0.8);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.9rem;
}

.skill-stat-label {
  font-weight: 600;
  color: #666;
  margin-right: 0.5rem;
}

.skill-stat-value {
  font-weight: bold;
  color: #333;
}

.skill-description {
  color: #666;
  line-height: 1.5;
  margin-bottom: 1rem;
  font-size: 0.95rem;
  background: rgba(245, 245, 245, 0.6);
  padding: 1rem;
  border-radius: 8px;
}

.unlock-btn {
  width: 100%;
  padding: 0.8rem 1.5rem;
  background: var(--color-grass);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px oklch(0.55 0.18 150 / 0.3);
}

.unlock-btn:hover {
  background: var(--color-grass-light);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px oklch(0.55 0.18 150 / 0.4);
}

/* 解锁按钮元素颜色 */
.unlock-btn.element-fire {
  background: var(--color-fire);
  box-shadow: 0 4px 12px oklch(0.55 0.22 30 / 0.3);
}

.unlock-btn.element-fire:hover {
  background: var(--color-fire-light);
  box-shadow: 0 6px 16px oklch(0.55 0.22 30 / 0.4);
}

.unlock-btn.element-water {
  background: var(--color-water);
  box-shadow: 0 4px 12px oklch(0.55 0.18 240 / 0.3);
}

.unlock-btn.element-water:hover {
  background: var(--color-water-light);
  box-shadow: 0 6px 16px oklch(0.55 0.18 240 / 0.4);
}

.unlock-btn.element-grass {
  background: var(--color-grass);
  box-shadow: 0 4px 12px oklch(0.55 0.18 150 / 0.3);
}

.unlock-btn.element-grass:hover {
  background: var(--color-grass-light);
  box-shadow: 0 6px 16px oklch(0.55 0.18 150 / 0.4);
}

.no-skills {
  text-align: center;
  padding: 2rem;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 10px;
  color: #666;
  font-size: 1.1rem;
  grid-column: 1 / -1;
}

/* 返回按钮 */
.back-btn {
  display: block;
  margin: 3rem auto 0;
  padding: 1rem 2.5rem;
  background: linear-gradient(135deg, #666 0%, #888 100%);
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  text-align: center;
  max-width: 200px;
}

.back-btn:hover {
  background: linear-gradient(135deg, #555 0%, #777 100%);
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

/* 装备管理 */
.equipment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-top: 1.5rem;
}

.equipment-slot {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(76, 175, 80, 0.2);
  transition: all 0.3s ease;
}

.equipment-slot:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(76, 175, 80, 0.25);
}

.equipment-slot h4 {
  color: #333;
  margin-bottom: 1rem;
  text-align: center;
  font-size: 1.2rem;
  border-bottom: 2px solid rgba(76, 175, 80, 0.3);
  padding-bottom: 0.5rem;
}

.equipment-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 8px;
  padding: 1rem;
  transition: all 0.3s ease;
}

.equipment-card:hover {
  background: rgba(245, 245, 245, 1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.equipment-card.empty {
  justify-content: center;
  flex-direction: column;
  text-align: center;
  padding: 2rem;
  color: #666;
}

.equipment-image {
  flex: 0 0 60px;
}

.equipment-image img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.equipment-details {
  flex: 1;
}

.equipment-details p {
  margin: 0.5rem 0;
  color: #333;
}

.equip-btn, .unequip-btn {
  margin-top: 1rem;
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
}

.equip-btn {
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.equip-btn:hover {
  background: linear-gradient(135deg, #388E3C 0%, #66BB6A 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.unequip-btn {
  background: linear-gradient(135deg, #f44336 0%, #e57373 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.3);
}

.unequip-btn:hover {
  background: linear-gradient(135deg, #d32f2f 0%, #c62828 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(244, 67, 54, 0.4);
}

/* 装备选择弹窗 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 15px;
  padding: 2rem;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-content h3 {
  color: #ff6b00;
  margin-bottom: 1.5rem;
  text-align: center;
  font-size: 1.5rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  border-bottom: 2px solid rgba(255, 107, 0, 0.3);
  padding-bottom: 0.5rem;
}

.equip-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.equip-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(245, 245, 245, 0.8);
  border-radius: 8px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.equip-item:hover {
  background: rgba(245, 245, 245, 1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #ff6b00;
}

.equip-item-image {
  flex: 0 0 60px;
}

.equip-item-image img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.equip-item-info {
  flex: 1;
}

.equip-item-info h4 {
  color: #333;
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
}

.equip-item-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.equip-item-stats .stat {
  padding: 0.3rem 0.6rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}

.equip-item-stats .stat.atk {
  background: rgba(255, 99, 132, 0.2);
  color: #dc3545;
}

.equip-item-stats .stat.def {
  background: rgba(54, 162, 235, 0.2);
  color: #007bff;
}

.equip-item-stats .stat.hp {
  background: rgba(75, 192, 192, 0.2);
  color: #28a745;
}

.equip-item-stats .stat.mp {
  background: rgba(153, 102, 255, 0.2);
  color: #6f42c1;
}

.equip-item-stats .stat.speed {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.equip-item-stats .stat.bound {
  background: rgba(200, 200, 200, 0.2);
  color: #666;
}

.equip-item {
  position: relative;
}

.equip-item.bound {
  opacity: 0.6;
  cursor: not-allowed;
}

.equip-item.bound:hover {
  transform: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.equip-item.worn {
  opacity: 0.8;
  cursor: not-allowed;
}

.equip-item.worn:hover {
  transform: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.bound-tag {
  position: absolute;
  top: -5px;
  right: -5px;
  color: white;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.bound-tag.current {
  background: #4CAF50;
}

.bound-tag:not(.current) {
  background: #666;
}

.modal-buttons {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
  gap: 1rem;
}

.cancel-btn {
  padding: 0.8rem 2rem;
  background: linear-gradient(135deg, #666 0%, #888 100%);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.cancel-btn:hover {
  background: linear-gradient(135deg, #555 0%, #777 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
  
  .elf-header {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-row {
    flex-direction: column;
    gap: 1rem;
  }
  
  .skill-list {
    grid-template-columns: 1fr;
  }
  
  .equipment-grid {
    grid-template-columns: 1fr;
  }
  
  .nav-menu {
    gap: 0.5rem;
  }
  
  .nav-btn {
    padding: 0.3rem 0.6rem;
    font-size: 0.8rem;
  }
  
  .modal-content {
    width: 95%;
    padding: 1.5rem;
  }
}
</style>
