<template>
  <div class="home-container">
    <GameTopNav active-path="/elves" />

    <!-- 主内容区 -->
    <div class="main-content">
      <p class="section-eyebrow">ELF PROFILE</p>
      <h1>精灵详情</h1>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else-if="elfDetail" class="elf-info">
        <div class="elf-header">
          <div class="elf-media">
            <div class="elf-halo"></div>
            <img :src="getElfImage(elfDetail.elf.elfId)" :alt="elfDetail.elf.elfId" class="elf-image">
          </div>
          <div class="elf-basic-info">
            <h2>精灵 {{ elfDetail.elf.elfId }}</h2>
            <div class="elf-meta">
              <span class="element-badge" :class="getElementColorClass(elfDetail.elf.elfId)">{{ getElementType(elfDetail.elf.elfId) }}</span>
              <p v-if="elfDetail.elf.fightOrder > 0" class="active-tag">出战：{{ elfDetail.elf.fightOrder }}号位</p>
              <p v-else class="standby-tag">未出战</p>
            </div>
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
            <span class="stat-value stat-max">已满级</span>
          </div>

          <div class="stat-row">
            <div class="stat-item">
              <span class="stat-label">HP</span>
              <span class="stat-value stat-hp">{{ elfDetail.elf.maxHp }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">MP</span>
              <span class="stat-value stat-mp">{{ elfDetail.elf.maxMp }}</span>
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
      <div v-if="elfDetail" class="stage-panel section">
        <div class="panel-heading">
          <h3>装备管理</h3>
        </div>
        <div class="equipment-grid">
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

      <div v-if="elfDetail" class="stage-panel section">
        <div class="panel-heading">
          <h3>已解锁技能</h3>
        </div>
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

      <div v-if="elfDetail" class="stage-panel section">
        <div class="panel-heading">
          <h3>可解锁技能</h3>
        </div>
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
.home-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #05070d 0%, #09111f 32%, #101a29 100%);
  padding: 0 20px 32px;
}

.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 40px;
  padding-top: 12px;
}

.section-eyebrow {
  margin: 0 0 4px;
  color: rgba(255, 220, 162, 0.78);
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

h1 {
  margin: 0 0 14px;
  color: #fff4df;
  font-size: clamp(1.4rem, 2.8vw, 2.2rem);
  font-weight: 800;
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.loading,
.error {
  text-align: center;
  padding: 36px 20px;
  border-radius: 24px;
  border: 1px solid rgba(255, 191, 112, 0.14);
  background: rgba(11, 15, 24, 0.86);
  box-shadow: 0 12px 20px rgba(4, 8, 15, 0.24);
  font-size: 0.95rem;
}

.error {
  color: #ff6b6b;
  border-color: rgba(255, 107, 107, 0.2);
}

.loading {
  color: rgba(247, 237, 220, 0.72);
}

/* 精灵信息面板 */
.elf-info {
  position: relative;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 191, 112, 0.14);
  background: rgba(11, 15, 24, 0.86);
  box-shadow: 0 12px 24px rgba(4, 8, 15, 0.24);
  overflow: hidden;
}

.elf-info::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 218, 157, 0.3), transparent);
  pointer-events: none;
}

.elf-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.elf-media {
  position: relative;
  display: grid;
  place-items: center;
  width: 110px;
  height: 110px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 50% 35%, rgba(255, 173, 91, 0.22), transparent 58%),
    linear-gradient(180deg, rgba(23, 28, 44, 0.98), rgba(11, 15, 24, 0.96));
  flex-shrink: 0;
}

.elf-halo {
  position: absolute;
  inset: auto 12% 12%;
  height: 24px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 196, 112, 0.28), transparent);
  filter: blur(8px);
}

.elf-image {
  position: relative;
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 16px;
  border: 2px solid rgba(255, 169, 79, 0.6);
  box-shadow:
    0 10px 18px rgba(0, 0, 0, 0.24),
    0 0 0 5px rgba(255, 169, 79, 0.06);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.elf-image:hover {
  transform: translateY(-3px);
  box-shadow:
    0 16px 26px rgba(0, 0, 0, 0.28),
    0 0 0 5px rgba(255, 169, 79, 0.1);
}

.elf-basic-info {
  flex: 1;
  min-width: 0;
}

.elf-basic-info h2 {
  margin: 0 0 6px;
  color: #fff4df;
  font-size: 1.25rem;
  font-weight: 800;
}

.elf-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.element-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
}

.element-badge.element-fire {
  background: oklch(0.55 0.22 30 / 0.18);
  color: oklch(0.65 0.22 30);
}

.element-badge.element-water {
  background: oklch(0.55 0.18 240 / 0.18);
  color: oklch(0.65 0.18 240);
}

.element-badge.element-grass {
  background: oklch(0.55 0.18 150 / 0.18);
  color: oklch(0.65 0.18 150);
}

.active-tag,
.standby-tag {
  margin: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
}

.active-tag {
  background: rgba(99, 202, 122, 0.14);
  border: 1px solid rgba(99, 202, 122, 0.24);
  color: #97ef9c;
}

.standby-tag {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 194, 107, 0.12);
  color: rgba(247, 237, 220, 0.74);
}

/* 属性统计 */
.elf-stats {
  display: grid;
  gap: 4px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.stat-item {
  display: grid;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(255, 196, 112, 0.12);
  background: rgba(255, 255, 255, 0.03);
}

.stat-label {
  color: rgba(255, 222, 184, 0.68);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.stat-value {
  color: #fff2d4;
  font-size: 0.9rem;
  font-weight: 700;
}

.stat-hp { color: #8fe69a; }
.stat-mp { color: #8fc5ff; }
.stat-max {
  color: rgba(255, 220, 162, 0.6);
  font-size: 0.8rem;
}

.level-status {
  margin: 0;
  padding: 6px 10px;
}

/* 分区面板 */
.stage-panel {
  position: relative;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 191, 112, 0.14);
  background: rgba(11, 15, 24, 0.86);
  box-shadow: 0 16px 28px rgba(4, 8, 15, 0.24);
  overflow: hidden;
}

.stage-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 218, 157, 0.3), transparent);
  pointer-events: none;
}

.section {
  margin-top: 12px;
}

.panel-heading h3 {
  margin: 0;
  color: #ffd07b;
  font-size: 1.05rem;
  font-weight: 800;
}

/* 装备管理 */
.equipment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.equipment-slot {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 196, 112, 0.12);
  background: rgba(255, 255, 255, 0.03);
}

.equipment-slot h4 {
  margin: 0 0 10px;
  color: rgba(255, 222, 184, 0.72);
  font-size: 0.9rem;
  font-weight: 700;
  text-align: center;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.equipment-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}

.equipment-card.empty {
  flex-direction: column;
  justify-content: center;
  text-align: center;
  padding: 18px;
  color: rgba(247, 237, 220, 0.6);
}

.equipment-card p {
  margin: 3px 0;
  color: rgba(247, 237, 220, 0.8);
  font-size: 0.88rem;
}

.equipment-image {
  flex: 0 0 50px;
}

.equipment-image img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 10px;
}

.equipment-details {
  flex: 1;
  min-width: 0;
}

.equip-btn, .unequip-btn {
  margin-top: 8px;
  padding: 6px 14px;
  border: none;
  border-radius: 12px;
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease;
}

.equip-btn {
  background: linear-gradient(135deg, rgba(99, 202, 122, 0.3), rgba(28, 86, 47, 0.9));
  color: #effee9;
}

.equip-btn:hover {
  transform: translateY(-1px);
}

.unequip-btn {
  background: linear-gradient(135deg, rgba(255, 118, 82, 0.3), rgba(180, 40, 20, 0.9));
  color: #ffe0d6;
}

.unequip-btn:hover {
  transform: translateY(-1px);
}

/* 技能列表 */
.skill-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.skill-item {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 194, 107, 0.12);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.92), rgba(10, 13, 21, 0.96));
  transition: transform 160ms ease, border-color 160ms ease;
}

.skill-item:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 194, 107, 0.24);
}

.skill-item.element-fire {
  border-color: oklch(0.55 0.22 30 / 0.3);
}

.skill-item.element-water {
  border-color: oklch(0.55 0.18 240 / 0.3);
}

.skill-item.element-grass {
  border-color: oklch(0.55 0.18 150 / 0.3);
}

.skill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.skill-header h4 {
  margin: 0;
  color: #fff4df;
  font-size: 1rem;
  font-weight: 700;
}

.skill-type {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  background: oklch(0.55 0.18 150 / 0.2);
  color: oklch(0.7 0.18 150);
}

.skill-type.element-fire {
  background: oklch(0.55 0.22 30 / 0.2);
  color: oklch(0.7 0.22 30);
}

.skill-type.element-water {
  background: oklch(0.55 0.18 240 / 0.2);
  color: oklch(0.7 0.18 240);
}

.skill-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.skill-stat {
  padding: 4px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.8rem;
}

.skill-stat-label {
  color: rgba(255, 222, 184, 0.68);
  font-weight: 600;
  margin-right: 4px;
}

.skill-stat-value {
  color: #fff2d4;
  font-weight: 700;
}

.skill-description {
  color: rgba(247, 237, 220, 0.6);
  line-height: 1.5;
  margin: 0 0 10px;
  font-size: 0.86rem;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
}

.unlock-btn {
  width: 100%;
  padding: 8px 14px;
  border: none;
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease;
  background: oklch(0.55 0.18 150 / 0.3);
  color: oklch(0.75 0.18 150);
}

.unlock-btn:hover {
  transform: translateY(-1px);
}

.unlock-btn.element-fire {
  background: oklch(0.55 0.22 30 / 0.3);
  color: oklch(0.75 0.22 30);
}

.unlock-btn.element-water {
  background: oklch(0.55 0.18 240 / 0.3);
  color: oklch(0.75 0.18 240);
}

.no-skills {
  text-align: center;
  padding: 20px;
  color: rgba(247, 237, 220, 0.5);
  font-size: 0.92rem;
  grid-column: 1 / -1;
}

/* 返回按钮 */
.back-btn {
  display: block;
  margin: 24px auto 0;
  padding: 10px 30px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(26, 31, 48, 0.94), rgba(11, 14, 22, 0.96));
  border: 1px solid rgba(255, 191, 110, 0.18);
  color: rgba(255, 244, 220, 0.92);
  font-size: 0.94rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease;
  box-shadow: 0 8px 16px rgba(4, 8, 15, 0.2);
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(4, 8, 15, 0.28);
}

/* 装备弹窗 */
.modal {
  position: fixed;
  inset: 0;
  padding: 20px;
  background: rgba(4, 7, 13, 0.62);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
}

.modal-content {
  width: min(100%, 560px);
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(255, 191, 112, 0.16);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.98), rgba(9, 12, 19, 0.98));
  color: #fff4df;
  box-shadow: 0 20px 40px rgba(4, 8, 15, 0.38);
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 14px;
  color: #ffd07b;
  font-size: 1.3rem;
  font-weight: 800;
  text-align: center;
}

.equip-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.equip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease;
}

.equip-item:hover {
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 194, 107, 0.2);
}

.equip-item.bound {
  opacity: 0.5;
  cursor: not-allowed;
}

.equip-item.bound:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: transparent;
}

.equip-item.worn {
  opacity: 0.7;
  cursor: not-allowed;
}

.equip-item.worn:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: transparent;
}

.equip-item-image {
  flex: 0 0 48px;
  position: relative;
}

.equip-item-image img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 10px;
}

.equip-item-info {
  flex: 1;
  min-width: 0;
}

.equip-item-info h4 {
  margin: 0 0 4px;
  color: #fff4df;
  font-size: 0.95rem;
  font-weight: 700;
}

.equip-item-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.equip-item-stats .stat {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 0.74rem;
  font-weight: 600;
}

.equip-item-stats .stat.atk {
  background: rgba(255, 99, 132, 0.15);
  color: #ff6b7a;
}

.equip-item-stats .stat.def {
  background: rgba(54, 162, 235, 0.15);
  color: #6bb9f0;
}

.equip-item-stats .stat.hp {
  background: rgba(75, 192, 192, 0.15);
  color: #5ed8c8;
}

.equip-item-stats .stat.mp {
  background: rgba(153, 102, 255, 0.15);
  color: #b088f5;
}

.equip-item-stats .stat.speed {
  background: rgba(255, 193, 7, 0.15);
  color: #ffd05a;
}

.equip-item-stats .stat.bound {
  background: rgba(200, 200, 200, 0.1);
  color: rgba(200, 200, 200, 0.7);
}

.bound-tag {
  position: absolute;
  top: -4px;
  right: -4px;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 8px;
  color: white;
}

.bound-tag.current {
  background: #4caf50;
}

.bound-tag:not(.current) {
  background: #666;
}

.modal-buttons {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.cancel-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(26, 31, 48, 0.94), rgba(11, 14, 22, 0.96));
  border: 1px solid rgba(255, 191, 110, 0.18);
  color: rgba(255, 244, 220, 0.92);
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease;
}

.cancel-btn:hover {
  transform: translateY(-1px);
}

/* 响应式 */
@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .home-container {
    padding: 0 14px 24px;
  }

  .elf-header {
    flex-direction: column;
    text-align: center;
  }

  .elf-media {
    width: 110px;
    height: 110px;
  }

  .elf-image {
    width: 86px;
    height: 86px;
  }

  .stat-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .skill-list {
    grid-template-columns: 1fr;
  }

  .equipment-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    padding: 18px;
  }
}

@media (max-width: 560px) {
  h1 {
    font-size: 1.6rem;
  }

  .elf-info,
  .stage-panel {
    border-radius: 24px;
  }

  .elf-media {
    width: 100px;
    height: 100px;
  }

  .elf-image {
    width: 76px;
    height: 76px;
    border-radius: 16px;
  }
}
</style>
