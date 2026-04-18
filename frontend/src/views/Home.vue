<template>
  <div class="home-container">
    <GameTopNav />

    <div class="main-content">
      <section class="home-hero">
        <div class="home-hero__glow home-hero__glow--warm"></div>
        <div class="home-hero__glow home-hero__glow--cool"></div>

        <div class="home-hero__intro">
          <div class="user-avatar-shell">
            <div class="user-avatar" @click="showAvatarUpload = true">
              <img :src="user?.avatar || avatarImage" alt="用户头像">
              <div class="avatar-edit-icon">📷 更换头像</div>
            </div>
          </div>

          <div class="hero-copy">
            <p class="section-eyebrow">HOME LOBBY</p>
            <h1>欢迎，{{ user?.nickname }}</h1>

            <div class="identity-list">
              <div class="identity-item">
                <span class="identity-label">手机号</span>
                <strong class="identity-value">{{ user?.phone || '未填写' }}</strong>
              </div>
              <div class="identity-item">
                <span class="identity-label">邮箱</span>
                <strong class="identity-value">{{ user?.email || '未填写' }}</strong>
              </div>
            </div>

            <div class="hero-actions">
              <button class="hero-btn hero-btn--primary" @click="showEditModal = true">修改个人信息</button>
              <button class="hero-btn hero-btn--secondary" @click="showChangePasswordModal = true">修改密码</button>
            </div>
          </div>
        </div>

        <div class="home-hero__status stage-panel">
          <div class="panel-heading panel-heading--compact">
            <div>
              <p class="section-eyebrow">MISSION STATUS</p>
              <h2>大厅概览</h2>
            </div>
            <span class="status-pill">{{ elves.length > 0 ? '编队在线' : '等待组队' }}</span>
          </div>

          <div class="user-stats">
            <div class="stat-item stat-days">
              <span class="stat-label">游戏天数</span>
              <span class="stat-value">{{ gameDays }}天</span>
            </div>
            <div class="stat-item stat-elves">
              <span class="stat-label">精灵数量</span>
              <span class="stat-value">{{ elves.length }}只</span>
            </div>
            <div class="stat-item stat-level">
              <span class="stat-label">当前关卡</span>
              <span class="stat-value">{{ currentLevelName }}</span>
            </div>
          </div>

          <div class="hero-summary">
            <div class="hero-summary__item">
              <span class="hero-summary__label">当前状态</span>
              <strong class="hero-summary__value">{{ elves.length > 0 ? '可立即出战' : '请选择你的首只精灵' }}</strong>
            </div>
            <div class="hero-summary__item">
              <span class="hero-summary__label">关卡推进</span>
              <strong class="hero-summary__value">{{ currentLevelName || '等待解锁' }}</strong>
            </div>
          </div>
        </div>
      </section>
      
      <div class="modal" v-if="showAvatarUpload">
        <div class="modal-content">
          <h3>上传头像</h3>
          <input type="file" @change="handleAvatarUpload" accept="image/*">
          <div class="modal-buttons">
            <button @click="showAvatarUpload = false">取消</button>
          </div>
        </div>
      </div>
      
      <div class="modal" v-if="showEditModal">
        <div class="modal-content">
          <h3>修改个人信息</h3>
          <form @submit.prevent="updateUserInfo">
            <div class="form-group">
              <label for="nickname">昵称</label>
              <input type="text" id="nickname" v-model="editForm.nickname" required>
            </div>
            <div class="form-group">
              <label for="phone">手机号</label>
              <input type="text" id="phone" v-model="editForm.phone" required>
            </div>
            <div class="form-group">
              <label for="email">邮箱</label>
              <input type="email" id="email" v-model="editForm.email" required>
            </div>
            <div class="modal-buttons">
              <button type="button" @click="showEditModal = false">取消</button>
              <button type="submit" class="primary-btn">保存</button>
            </div>
          </form>
        </div>
      </div>
      
      <div class="modal" v-if="showChangePasswordModal">
        <div class="modal-content">
          <h3>修改密码</h3>
          <form @submit.prevent="changePassword">
            <div class="form-group">
              <label for="oldPassword">旧密码</label>
              <input type="password" id="oldPassword" v-model="passwordForm.oldPassword" required>
            </div>
            <div class="form-group">
              <label for="newPassword">新密码</label>
              <input type="password" id="newPassword" v-model="passwordForm.newPassword" required>
            </div>
            <div class="form-group">
              <label for="confirmNewPassword">确认新密码</label>
              <input type="password" id="confirmNewPassword" v-model="passwordForm.confirmNewPassword" required>
            </div>
            <div class="modal-buttons">
              <button type="button" @click="showChangePasswordModal = false">取消</button>
              <button type="submit" class="primary-btn">保存</button>
            </div>
          </form>
        </div>
      </div>
      
      <section v-if="elves.length > 0" class="stage-panel elves-showcase">
        <div class="panel-heading">
          <div>
            <p class="section-eyebrow">ACTIVE ROSTER</p>
            <h3>我的精灵</h3>
          </div>
        </div>

        <div class="elves-grid">
          <div v-for="elf in elves" :key="elf.id" class="elf-card">
            <div class="elf-card__media">
              <div class="elf-card__halo"></div>
              <img :src="getElfImage(elf.elfId)" :alt="elf.elfId" class="elf-image">
            </div>
            <div class="elf-info">
              <span class="elf-card__eyebrow">精灵档案</span>
              <h4>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h4>
              <div class="elf-meta">
                <span class="elf-meta__item">等级 {{ elf.level }}</span>
                <span class="elf-meta__item">{{ elf.fightOrder > 0 ? `出战 ${elf.fightOrder} 号位` : '待命中' }}</span>
              </div>
              <p v-if="elf.fightOrder > 0" class="active-tag">出战：{{ elf.fightOrder }}号位</p>
              <p v-else class="standby-tag">当前未上阵</p>
            </div>
          </div>
        </div>
      </section>
      
      <section v-if="elves.length === 0" class="stage-panel starter-selection">
        <div class="panel-heading">
          <div>
            <p class="section-eyebrow">FIRST PARTNER</p>
            <h3>选择你的初始精灵</h3>
          </div>
        </div>

        <div v-if="loadingStarterElves" class="loading">加载中...</div>
        <div v-else class="starter-elves">
          <div v-for="elf in starterElves" :key="elf.id" class="starter-elf">
            <div class="starter-elf__media">
              <div class="starter-elf__halo"></div>
              <img :src="getElfImage(elf.id)" :alt="elf.elfName" class="elf-image">
            </div>
            <div class="starter-elf__body">
              <span class="starter-elf__eyebrow">初始伙伴</span>
              <h4>{{ elf.elfName }}</h4>
              <p>类型: {{ elf.elementType === 1 ? '火系' : elf.elementType === 2 ? '水系' : '草系' }}</p>
              <p>等级: 1</p>
              <button @click="selectStarter(elf.id)">选择</button>
            </div>
          </div>
        </div>
      </section>
      
      <section class="stage-panel action-section">
        <div class="panel-heading">
          <div>
            <p class="section-eyebrow">QUICK ACCESS</p>
            <h3>快捷入口</h3>
          </div>
        </div>

        <div class="action-buttons">
          <button class="action-btn primary" @click="navigateTo('/elves')">
            <span class="btn-icon">✨</span>
            <span class="action-btn__content">
              <strong>精灵中心</strong>
              <small>查看养成与编队</small>
            </span>
          </button>
          <button class="action-btn secondary" @click="navigateTo('/pve')">
            <span class="btn-icon">⚔️</span>
            <span class="action-btn__content">
              <strong>开始冒险</strong>
              <small>进入主线战斗</small>
            </span>
          </button>
          <button class="action-btn tertiary" @click="navigateTo('/shop')">
            <span class="btn-icon">🛒</span>
            <span class="action-btn__content">
              <strong>道具商店</strong>
              <small>补给与资源采购</small>
            </span>
          </button>
          <button class="action-btn quaternary" @click="navigateTo('/train')">
            <span class="btn-icon">🏋️</span>
            <span class="action-btn__content">
              <strong>单人训练</strong>
              <small>验证技能与节奏</small>
            </span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userElfApi } from '../api/userElf'
import { elfApi } from '../api/elf'
import { levelApi } from '../api/level'
import { userApi } from '../api/user'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()
const user = ref(null)
const elves = ref([])
const levels = ref([])
const starterElves = ref([])
const loadingStarterElves = ref(false)

// 用户头像图片路径
const avatarImage = '/src/assets/photo/微信图片_2025-10-21_163432_159.jpg'

// 弹窗状态
const showAvatarUpload = ref(false)
const showEditModal = ref(false)
const showChangePasswordModal = ref(false)

// 编辑表单
const editForm = ref({
  nickname: '',
  phone: '',
  email: ''
})

// 密码修改表单
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: ''
})

// 根据精灵ID获取图片路径
const getElfImage = (elfId) => {
  switch (elfId) {
    case 1:
      return '/src/assets/photo/sasuke/佐助.jpg'
    case 2:
      return '/src/assets/photo/zhaomeiming/照美冥.webp'
    case 3:
      return '/src/assets/photo/qianshouzhujian/千手柱间.jpg'
    default:
      return ''
  }
}

// 计算游戏天数（从注册日期开始计算）
const gameDays = computed(() => {
  if (!user.value || !user.value.createTime) return 0
  const createDate = new Date(user.value.createTime)
  const now = new Date()
  const diffTime = Math.abs(now - createDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays
})

// 计算当前关卡名称
const currentLevelName = computed(() => {
  if (!user.value) return ''
  const currentLevelId = user.value.currentLevel
  const level = levels.value.find(l => l.id === currentLevelId)
  return level ? level.levelName : ''
})

// 获取用户精灵列表
const loadElves = async () => {
  if (!user.value) return
  try {
    console.log('开始获取精灵列表')
    const response = await userElfApi.list()
    console.log('精灵列表响应:', response)
    if (response && response.code === 200 && response.data && Array.isArray(response.data)) {
      elves.value = response.data
    } else {
      // 如果响应格式不正确，默认设置精灵列表为空数组
      elves.value = []
      console.log('响应格式不正确，设置精灵列表为空数组')
    }
    console.log('精灵列表:', elves.value)
    console.log('精灵数量:', elves.value.length)
  } catch (error) {
    console.error('获取精灵列表失败:', error)
    // 如果发生错误，默认设置精灵列表为空数组
    elves.value = []
    console.log('获取精灵列表失败，设置精灵列表为空数组')
  }
}

// 获取御三家精灵
const loadStarterElves = async () => {
  console.log('开始加载御三家精灵')
  try {
    loadingStarterElves.value = true
    console.log('调用elfApi.getStarterElves()')
    
    // 使用elfApi，会自动携带token
    const response = await elfApi.getStarterElves()
    console.log('御三家精灵响应:', response)
    
    if (response && response.code === 200 && response.data && Array.isArray(response.data)) {
      starterElves.value = response.data
      console.log('御三家精灵列表:', starterElves.value)
    } else {
      console.log('响应格式不正确，设置御三家精灵列表为空数组')
      starterElves.value = []
    }
  } catch (error) {
    console.error('获取御三家精灵失败:', error)
    console.log('获取御三家精灵失败，设置御三家精灵列表为空数组')
    starterElves.value = []
  } finally {
    console.log('设置loadingStarterElves为false')
    loadingStarterElves.value = false
  }
}

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  localStorage.removeItem('user')
  localStorage.removeItem('token')
  router.push('/auth')
}

// 选择初始精灵
const selectStarter = async (elfId) => {
  if (!user.value) return
  try {
    const response = await userElfApi.create(elfId)
    console.log('精灵创建响应:', response)
    if (response && response.data) {
      // 重新加载精灵列表
      await loadElves()
      alert('精灵选择成功！')
    } else {
      alert('精灵选择失败')
    }
  } catch (error) {
    console.error('精灵选择失败:', error)
    alert('精灵选择失败，请稍后重试')
  }
}

// 获取关卡列表
const loadLevels = async () => {
  try {
    const response = await levelApi.getLevelList()
    if (response && response.code === 200 && response.data) {
      levels.value = response.data
    }
  } catch (error) {
    console.error('获取关卡列表失败:', error)
  }
}

// 处理头像上传
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/users/me/avatar', {
      method: 'POST',
      headers: {
        'Authorization': token
      },
      body: formData
    })
    
    const data = await response.json()
    if (data.code === 200 && data.data) {
      // 更新用户头像
      const updateResponse = await userApi.updateUserInfo({
        userId: user.value.id,
        nickname: user.value.nickname,
        phone: user.value.phone,
        email: user.value.email,
        avatar: data.data
      })
      
      if (updateResponse.code === 200 && updateResponse.data) {
        user.value = updateResponse.data
        localStorage.setItem('user', JSON.stringify(user.value))
        alert('头像上传成功！')
      } else {
        alert('头像保存失败：' + (updateResponse.msg || '未知错误'))
      }
    } else {
      alert('头像上传失败：' + (data.msg || '未知错误'))
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    alert('头像上传失败，请稍后重试')
  } finally {
    showAvatarUpload.value = false
  }
}

// 更新用户信息
const updateUserInfo = async () => {
  try {
    const response = await userApi.updateUserInfo({
      userId: user.value.id,
      nickname: editForm.value.nickname,
      phone: editForm.value.phone,
      email: editForm.value.email,
      avatar: user.value.avatar
    })
    
    if (response.code === 200 && response.data) {
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(user.value))
      showEditModal.value = false
      alert('个人信息更新成功！')
    } else if (response && response.code !== 200) {
      alert(response.msg || '个人信息更新失败，请稍后重试')
    } else {
      alert('个人信息更新失败，请稍后重试')
    }
  } catch (error) {
    console.error('更新个人信息失败:', error)
    alert('更新个人信息失败，请稍后重试')
  }
}

// 修改密码
const changePassword = async () => {
  // 验证新密码和确认新密码是否一致
  if (passwordForm.value.newPassword !== passwordForm.value.confirmNewPassword) {
    alert('新密码和确认新密码不一致')
    return
  }
  
  // 验证新密码长度
  if (passwordForm.value.newPassword.length < 6 || passwordForm.value.newPassword.length > 20) {
    alert('新密码长度应在6-20个字符之间')
    return
  }
  
  try {
    const response = await userApi.updatePassword({
      userId: user.value.id,
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    
    if (response.code === 200 && response.data) {
      showChangePasswordModal.value = false
      // 重置密码表单
      passwordForm.value = {
        oldPassword: '',
        newPassword: '',
        confirmNewPassword: ''
      }
      alert('密码修改成功！')
    } else if (response && response.code !== 200) {
      alert(response.msg || '密码修改失败，请稍后重试')
    } else {
      alert('密码修改失败，请稍后重试')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    alert('修改密码失败，请稍后重试')
  }
}

onMounted(async () => {
  console.log('开始执行onMounted')
  const userStr = localStorage.getItem('user')
  if (userStr) {
    console.log('获取到用户信息')
    const userData = JSON.parse(userStr)
    user.value = userData
    console.log('用户信息:', user.value)
    
    // 初始化编辑表单
    editForm.value = {
      nickname: user.value.nickname,
      phone: user.value.phone,
      email: user.value.email
    }
    
    // 加载关卡列表
    console.log('开始加载关卡列表')
    await loadLevels()
    console.log('关卡列表:', levels.value)
    // 加载精灵列表
    console.log('开始加载精灵列表')
    await loadElves()
    console.log('精灵数量:', elves.value.length)
    // 如果精灵数量为0，加载御三家精灵
    if (elves.value.length === 0) {
      console.log('精灵数量为0，开始加载御三家精灵')
      await loadStarterElves()
    } else {
      console.log('精灵数量不为0，不加载御三家精灵')
    }
  } else {
    console.log('未获取到用户信息，跳转到登录页面')
    router.push('/auth')
  }
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  padding: 0 20px 32px;
  overflow-x: hidden;
  background:
    radial-gradient(circle at top, rgba(255, 153, 51, 0.16), transparent 28%),
    radial-gradient(circle at 18% 24%, rgba(99, 202, 122, 0.12), transparent 24%),
    linear-gradient(180deg, #05070d 0%, #09111f 32%, #101a29 100%);
  color: #f8f2e9;
}

.main-content {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 20px;
  max-width: 1400px;
  margin: 10px auto 0;
}

.home-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 20px;
  padding: 30px;
  border-radius: 32px;
  border: 1px solid rgba(255, 194, 107, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 196, 127, 0.05), transparent 24%),
    linear-gradient(180deg, rgba(8, 12, 22, 0.96) 0%, rgba(12, 19, 34, 0.94) 100%);
  box-shadow:
    0 24px 50px rgba(4, 8, 15, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.home-hero__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(6px);
  pointer-events: none;
}

.home-hero__glow--warm {
  top: -90px;
  right: -10px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 144, 66, 0.28), rgba(255, 144, 66, 0));
}

.home-hero__glow--cool {
  bottom: -120px;
  left: -40px;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(79, 145, 255, 0.18), rgba(79, 145, 255, 0));
}

.home-hero__intro,
.home-hero__status {
  position: relative;
  z-index: 1;
}

.home-hero__intro {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 26px;
  align-items: center;
}

.user-avatar-shell {
  position: relative;
  display: grid;
  place-items: center;
  width: 186px;
  height: 186px;
  border-radius: 32px;
  border: 1px solid rgba(255, 200, 128, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.user-avatar {
  width: 144px;
  height: 144px;
  border-radius: 28px;
  overflow: hidden;
  border: 2px solid rgba(255, 169, 79, 0.86);
  box-shadow:
    0 18px 30px rgba(255, 129, 35, 0.22),
    0 0 0 10px rgba(255, 169, 79, 0.08);
  position: relative;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.user-avatar:hover {
  transform: translateY(-4px);
  box-shadow:
    0 24px 38px rgba(255, 129, 35, 0.28),
    0 0 0 10px rgba(255, 169, 79, 0.12);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-icon {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 12px;
  background: linear-gradient(180deg, rgba(8, 12, 22, 0), rgba(8, 12, 22, 0.88));
  color: rgba(255, 244, 220, 0.92);
  text-align: center;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0;
  transition: opacity 180ms ease;
}

.user-avatar:hover .avatar-edit-icon {
  opacity: 1;
}

.hero-copy {
  display: grid;
  gap: 18px;
}

.section-eyebrow {
  margin: 0;
  color: rgba(255, 220, 162, 0.78);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.hero-copy h1,
.panel-heading h2,
.panel-heading h3 {
  margin: 0;
  color: #fff4df;
  font-weight: 800;
}

.hero-copy h1 {
  font-size: clamp(2rem, 3.4vw, 3.4rem);
  line-height: 1.04;
  letter-spacing: -0.04em;
}

.hero-description,
.panel-description {
  margin: 0;
  color: rgba(247, 239, 224, 0.72);
  font-size: 1rem;
  line-height: 1.7;
}

.identity-list {
  display: grid;
  gap: 10px;
}

.identity-item {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 194, 107, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.identity-label {
  color: rgba(255, 222, 184, 0.68);
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.identity-value {
  color: #fff2d4;
  font-size: 1rem;
  font-weight: 700;
  word-break: break-all;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-btn,
.action-btn,
.starter-elf button,
.modal-buttons button {
  border: none;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease, border-color 180ms ease;
}

.hero-btn {
  min-height: 48px;
  padding: 0 20px;
  border-radius: 16px;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.hero-btn:hover,
.action-btn:hover,
.starter-elf button:hover,
.modal-buttons button:hover {
  transform: translateY(-2px);
}

.hero-btn--primary {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  box-shadow: 0 16px 28px rgba(255, 132, 29, 0.24);
}

.hero-btn--secondary {
  border: 1px solid rgba(255, 191, 110, 0.18);
  background: linear-gradient(180deg, rgba(26, 31, 48, 0.94), rgba(11, 14, 22, 0.96));
  color: rgba(255, 244, 220, 0.92);
  box-shadow: 0 10px 20px rgba(4, 8, 15, 0.22);
}

.stage-panel {
  position: relative;
  padding: 22px;
  border-radius: 28px;
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

.panel-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
  margin-bottom: 14px;
}

.panel-heading--compact {
  align-items: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid rgba(255, 196, 112, 0.16);
  background: rgba(8, 12, 22, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  color: rgba(255, 238, 214, 0.84);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.user-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.stat-item {
  display: grid;
  gap: 6px;
  min-height: 112px;
  padding: 18px 18px 16px;
  border-radius: 24px;
  border: 1px solid rgba(255, 196, 112, 0.16);
  background: linear-gradient(180deg, rgba(16, 22, 36, 0.95), rgba(10, 13, 21, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.stat-label {
  color: rgba(255, 222, 184, 0.72);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stat-value {
  color: #ffd07b;
  font-size: clamp(1.4rem, 2vw, 2rem);
  font-weight: 800;
  line-height: 1.1;
  word-break: break-word;
}

.stat-item.stat-days {
  border-color: rgba(255, 207, 123, 0.2);
}

.stat-item.stat-days .stat-value {
  color: #ffd07b;
}

.stat-item.stat-elves {
  border-color: rgba(99, 202, 122, 0.22);
}

.stat-item.stat-elves .stat-value {
  color: #8fe69a;
}

.stat-item.stat-level {
  border-color: rgba(79, 145, 255, 0.22);
}

.stat-item.stat-level .stat-value {
  color: #8fc5ff;
}

.hero-summary {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.hero-summary__item {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(255, 194, 107, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.hero-summary__label {
  color: rgba(255, 222, 184, 0.68);
  font-size: 0.84rem;
  font-weight: 700;
}

.hero-summary__value {
  color: #fff2d4;
  font-size: 0.96rem;
  font-weight: 700;
  text-align: right;
}

.elves-showcase {
  background:
    radial-gradient(circle at top right, rgba(96, 212, 128, 0.08), transparent 26%),
    rgba(11, 15, 24, 0.88);
}

.elves-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.elf-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.92), rgba(10, 13, 21, 0.96));
  box-shadow: 0 12px 24px rgba(5, 7, 13, 0.22);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.elf-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 194, 107, 0.28);
  box-shadow: 0 18px 28px rgba(5, 7, 13, 0.3);
}

.elf-card__media {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 148px;
  border-radius: 18px;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 35%, rgba(255, 173, 91, 0.22), rgba(255, 173, 91, 0) 58%),
    linear-gradient(180deg, rgba(23, 28, 44, 0.98), rgba(11, 15, 24, 0.96));
}

.elf-card__halo {
  position: absolute;
  inset: auto 16% 14%;
  height: 38px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 196, 112, 0.28), rgba(255, 196, 112, 0));
  filter: blur(12px);
}

.elf-card .elf-image {
  position: relative;
  width: 112px;
  height: 112px;
  border-radius: 22px;
  object-fit: cover;
  border: 1px solid rgba(255, 213, 151, 0.18);
  box-shadow: 0 18px 30px rgba(0, 0, 0, 0.26);
}

.elf-info {
  display: grid;
  gap: 8px;
}

.elf-card__eyebrow,
.starter-elf__eyebrow {
  color: rgba(255, 220, 162, 0.66);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.elf-card h4,
.starter-elf h4 {
  margin: 0;
  color: #fff4df;
  font-size: 1.1rem;
}

.elf-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.elf-meta__item {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(247, 237, 220, 0.8);
  font-size: 0.76rem;
  font-weight: 600;
}

.active-tag,
.standby-tag {
  width: fit-content;
  margin: 0;
  padding: 7px 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
  display: inline-block;
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

.starter-selection {
  background:
    radial-gradient(circle at top left, rgba(255, 153, 51, 0.09), transparent 28%),
    rgba(11, 15, 24, 0.88);
}

.loading {
  text-align: center;
  padding: 48px 24px;
  color: rgba(247, 237, 220, 0.72);
  font-size: 1.05rem;
}

.starter-elves {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.starter-elf {
  display: grid;
  gap: 18px;
  padding: 20px;
  border-radius: 26px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  background: linear-gradient(180deg, rgba(18, 22, 34, 0.94), rgba(10, 13, 21, 0.98));
  box-shadow: 0 14px 24px rgba(5, 7, 13, 0.24);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
  position: relative;
  overflow: hidden;
}

.starter-elf:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 32px rgba(5, 7, 13, 0.3);
  border-color: rgba(255, 194, 107, 0.28);
}

.starter-elf__media {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 220px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 50% 35%, rgba(255, 173, 91, 0.22), rgba(255, 173, 91, 0) 56%),
    linear-gradient(180deg, rgba(23, 28, 44, 0.98), rgba(11, 15, 24, 0.96));
}

.starter-elf__halo {
  position: absolute;
  inset: auto 12% 12%;
  height: 56px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(255, 196, 112, 0.3), rgba(255, 196, 112, 0));
  filter: blur(14px);
}

.starter-elf__body {
  display: grid;
  gap: 10px;
}

.starter-elf .elf-image {
  position: relative;
  width: 168px;
  height: 168px;
  border-radius: 32px;
  object-fit: cover;
  border: 1px solid rgba(255, 213, 151, 0.18);
  box-shadow: 0 18px 30px rgba(0, 0, 0, 0.28);
}

.starter-elf p {
  margin: 0;
  color: rgba(247, 237, 220, 0.74);
  font-size: 0.96rem;
}

.starter-elf button {
  margin-top: 8px;
  min-height: 48px;
  padding: 0 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  font-size: 0.94rem;
  font-weight: 800;
  box-shadow: 0 16px 28px rgba(255, 132, 29, 0.22);
}

.action-section {
  background:
    radial-gradient(circle at top center, rgba(79, 145, 255, 0.08), transparent 28%),
    rgba(11, 15, 24, 0.88);
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.action-btn {
  min-width: 0;
  min-height: 110px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  box-shadow: 0 14px 24px rgba(5, 7, 13, 0.22);
}

.action-btn .btn-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 1.4rem;
}

.action-btn__content {
  display: grid;
  gap: 6px;
}

.action-btn__content strong {
  color: inherit;
  font-size: 1rem;
}

.action-btn__content small {
  color: inherit;
  opacity: 0.76;
  font-size: 0.8rem;
}

.action-btn.primary {
  background: linear-gradient(135deg, rgba(102, 195, 117, 0.3), rgba(28, 86, 47, 0.9));
  color: #effee9;
}

.action-btn.secondary {
  background: linear-gradient(135deg, rgba(56, 169, 255, 0.28), rgba(20, 63, 138, 0.9));
  color: #eef7ff;
}

.action-btn.tertiary {
  background: linear-gradient(135deg, rgba(255, 170, 79, 0.3), rgba(143, 78, 16, 0.92));
  color: #fff3e4;
}

.action-btn.quaternary {
  background: linear-gradient(135deg, rgba(255, 225, 162, 0.26), rgba(182, 93, 18, 0.92));
  color: #fff1dd;
}

.modal {
  position: fixed;
  inset: 0;
  padding: 24px;
  background: rgba(4, 7, 13, 0.62);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1200;
}

.modal-content {
  width: min(100%, 520px);
  padding: 28px;
  border-radius: 28px;
  border: 1px solid rgba(255, 191, 112, 0.16);
  background:
    linear-gradient(180deg, rgba(18, 22, 34, 0.98), rgba(9, 12, 19, 0.98));
  color: #fff4df;
  box-shadow: 0 24px 50px rgba(4, 8, 15, 0.38);
}

.modal-content form {
  display: grid;
  gap: 16px;
}

.modal-content h3 {
  margin: 0 0 8px;
  color: #ffd07b;
  text-align: left;
  font-size: 1.5rem;
}

.modal-content .form-group {
  margin-bottom: 0;
}

.modal-content label {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 222, 184, 0.72);
  font-size: 0.86rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.modal-content input {
  width: 90%;
  padding: 14px 16px;
  border: 1px solid rgba(255, 194, 107, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff4df;
  font-size: 1rem;
  box-sizing: border-box;
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.modal-content input:focus {
  outline: none;
  border-color: rgba(255, 191, 110, 0.44);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 4px rgba(255, 153, 51, 0.12);
}

.modal-content input[type="file"] {
  width: 100%;
  padding: 16px;
}

.modal-content input::file-selector-button {
  margin-right: 12px;
  padding: 8px 12px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  font-weight: 700;
  cursor: pointer;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.modal-buttons button {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 244, 220, 0.92);
  font-size: 0.94rem;
  font-weight: 700;
}

.modal-buttons .primary-btn {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  box-shadow: 0 16px 28px rgba(255, 132, 29, 0.22);
}

@media (max-width: 1180px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .home-container {
    padding: 0 14px 24px;
  }

  .main-content {
    gap: 16px;
  }

  .home-hero {
    padding: 22px;
  }

  .home-hero__intro {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }

  .hero-copy {
    width: 100%;
  }

  .identity-item,
  .hero-summary__item {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }

  .user-stats {
    grid-template-columns: 1fr;
  }

  .panel-heading,
  .panel-heading--compact {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-buttons {
    grid-template-columns: 1fr;
  }

  .action-btn {
    width: 100%;
    min-height: 94px;
  }

  .starter-elves,
  .elves-grid {
    grid-template-columns: 1fr;
  }

  .modal-content {
    padding: 22px;
  }

  .modal-buttons {
    flex-direction: column;
  }

  .modal-buttons button {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .home-hero,
  .stage-panel {
    border-radius: 24px;
  }

  .user-avatar-shell {
    width: 160px;
    height: 160px;
  }

  .user-avatar {
    width: 124px;
    height: 124px;
    border-radius: 24px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-btn {
    width: 100%;
  }

  .starter-elf .elf-image {
    width: 144px;
    height: 144px;
  }

  .elf-card .elf-image {
    width: 120px;
    height: 120px;
  }
}
</style>
