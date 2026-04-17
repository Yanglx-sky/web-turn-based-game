<template>
  <div class="home-container">
    <GameTopNav />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 用户信息卡片 -->
      <div class="user-info-card">
        <div class="user-avatar" @click="showAvatarUpload = true">
          <img :src="user?.avatar || avatarImage" alt="用户头像">
          <div class="avatar-edit-icon">📷</div>
        </div>
        <div class="user-details">
          <h2>欢迎，{{ user?.nickname }}</h2>
          <p>手机号: {{ user?.phone }}</p>
          <p>邮箱: {{ user?.email }}</p>
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
          <div class="action-buttons-group">
            <button class="edit-info-btn" @click="showEditModal = true">修改个人信息</button>
            <button class="edit-password-btn" @click="showChangePasswordModal = true">修改密码</button>
          </div>
        </div>
      </div>
      
      <!-- 头像上传弹窗 -->
      <div class="modal" v-if="showAvatarUpload">
        <div class="modal-content">
          <h3>上传头像</h3>
          <input type="file" @change="handleAvatarUpload" accept="image/*">
          <div class="modal-buttons">
            <button @click="showAvatarUpload = false">取消</button>
          </div>
        </div>
      </div>
      
      <!-- 修改个人信息弹窗 -->
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
      
      <!-- 修改密码弹窗 -->
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
      
      <!-- 精灵展示区 -->
      <div v-if="elves.length > 0" class="elves-showcase">
        <h3>我的精灵</h3>
        <div class="elves-grid">
          <div v-for="elf in elves" :key="elf.id" class="elf-card">
            <img :src="getElfImage(elf.elfId)" :alt="elf.elfId" class="elf-image">
            <div class="elf-info">
              <h4>{{ elf.elfName || `精灵 ${elf.elfId}` }}</h4>
              <p>等级: {{ elf.level }}</p>
              <p v-if="elf.fightOrder > 0" class="active-tag">出战：{{ elf.fightOrder }}号位</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 御三家选择 -->
      <div v-if="elves.length === 0" class="starter-selection">
        <h3>选择你的初始精灵</h3>
        <p>请从以下三只精灵中选择一只作为你的初始伙伴</p>
        <div v-if="loadingStarterElves" class="loading">加载中...</div>
        <div v-else class="starter-elves">
          <div v-for="elf in starterElves" :key="elf.id" class="starter-elf">
            <img :src="getElfImage(elf.id)" :alt="elf.elfName" class="elf-image">
            <h4>{{ elf.elfName }}</h4>
            <p>类型: {{ elf.elementType === 1 ? '火系' : elf.elementType === 2 ? '水系' : '草系' }}</p>
            <p>等级: 1</p>
            <button @click="selectStarter(elf.id)">选择</button>
          </div>
        </div>
      </div>
      
      <!-- 功能按钮区 -->
      <div class="action-buttons">
        <button class="action-btn primary" @click="navigateTo('/elves')">
          <i class="btn-icon">✨</i> 精灵中心
        </button>
        <button class="action-btn secondary" @click="navigateTo('/pve')">
          <i class="btn-icon">⚔️</i> 开始冒险
        </button>
        <button class="action-btn tertiary" @click="navigateTo('/shop')">
          <i class="btn-icon">🛒</i> 道具商店
        </button>
        <button class="action-btn quaternary" @click="navigateTo('/train')">
          <i class="btn-icon">🏋️</i> 单人训练
        </button>
      </div>
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
    const response = await fetch('http://localhost:8080/users/upload-avatar', {
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* 用户信息卡片 */
.user-info-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 2rem;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 107, 0, 0.3);
}

.user-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #ff6b00;
  box-shadow: 0 0 20px rgba(255, 107, 0, 0.5);
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 0 25px rgba(255, 107, 0, 0.7);
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
  background: rgba(0, 0, 0, 0.6);
  color: white;
  text-align: center;
  padding: 5px;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.user-avatar:hover .avatar-edit-icon {
  opacity: 1;
}

.user-details h2 {
  color: #333;
  margin-bottom: 0.5rem;
  font-size: 1.8rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.user-details p {
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 1rem;
}

.action-buttons-group {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.edit-info-btn, .edit-password-btn {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.edit-info-btn {
  background: linear-gradient(135deg, #ff6b00 0%, #ff9e4f 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.3);
}

.edit-info-btn:hover {
  background: linear-gradient(135deg, #e55a00 0%, #e08a3d 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 0, 0.4);
}

.edit-password-btn {
  background: linear-gradient(135deg, #2196F3 0%, #64B5F6 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.edit-password-btn:hover {
  background: linear-gradient(135deg, #1976D2 0%, #42A5F5 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
}

.user-stats {
  display: flex;
  gap: 2rem;
  margin-top: 1rem;
}

.stat-item {
  background: rgba(255, 107, 0, 0.1);
  padding: 0.8rem 1.2rem;
  border-radius: 10px;
  text-align: center;
  min-width: 120px;
  border: 1px solid rgba(255, 107, 0, 0.2);
}

.stat-label {
  display: block;
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 0.3rem;
}

.stat-value {
  display: block;
  font-size: 1.2rem;
  font-weight: bold;
  color: var(--color-brand);
}

/* 统计项颜色区分 */
.stat-item.stat-days {
  border-color: oklch(0.70 0.18 85 / 0.3);
}

.stat-item.stat-days .stat-value {
  color: var(--color-gold);
}

.stat-item.stat-elves {
  border-color: oklch(0.55 0.18 150 / 0.3);
}

.stat-item.stat-elves .stat-value {
  color: var(--color-grass);
}

.stat-item.stat-level {
  border-color: oklch(0.55 0.18 240 / 0.3);
}

.stat-item.stat-level .stat-value {
  color: var(--color-water);
}

/* 精灵展示区 */
.elves-showcase {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(76, 175, 80, 0.3);
}

.elves-showcase h3 {
  color: #4CAF50;
  margin-bottom: 1.5rem;
  text-align: center;
  font-size: 1.5rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  border-bottom: 2px solid rgba(76, 175, 80, 0.3);
  padding-bottom: 0.5rem;
}

.elves-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1.5rem;
}

.elf-card {
  background: white;
  border-radius: 10px;
  padding: 1.5rem;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.elf-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(76, 175, 80, 0.3);
}

.elf-card .elf-image {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 1rem;
  border: 2px solid #4CAF50;
  box-shadow: 0 4px 10px rgba(76, 175, 80, 0.3);
}

.elf-card h4 {
  color: #333;
  margin-bottom: 0.5rem;
  font-size: 1.1rem;
}

.elf-card p {
  color: #666;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.elf-card .active-tag {
  background: #4CAF50;
  color: white;
  padding: 0.3rem 0.8rem;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: bold;
  display: inline-block;
  margin-top: 0.5rem;
}

/* 御三家选择 */
.starter-selection {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 2.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 107, 0, 0.3);
  text-align: center;
}

.starter-selection h3 {
  color: #ff6b00;
  margin-bottom: 1rem;
  font-size: 1.8rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
  border-bottom: 2px solid rgba(255, 107, 0, 0.3);
  padding-bottom: 0.5rem;
}

.starter-selection p {
  color: #666;
  margin-bottom: 2rem;
  font-size: 1.1rem;
}

.loading {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.2rem;
}

.starter-elves {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 2rem;
  margin-top: 2rem;
}

.starter-elf {
  background: white;
  padding: 2rem;
  border-radius: 15px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  text-align: center;
  min-width: 220px;
  transition: all 0.3s ease;
  border: 2px solid rgba(255, 107, 0, 0.2);
  position: relative;
  overflow: hidden;
}

.starter-elf::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255, 107, 0, 0.1), transparent);
  transform: rotate(45deg);
  animation: shimmer 3s infinite;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.starter-elf:hover::before {
  opacity: 1;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%) rotate(45deg);
  }
  100% {
    transform: translateX(100%) rotate(45deg);
  }
}

.starter-elf:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 25px rgba(255, 107, 0, 0.3);
  border-color: #ff6b00;
}

.starter-elf .elf-image {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 1.5rem;
  border: 3px solid #ff6b00;
  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.4);
  transition: all 0.3s ease;
}

.starter-elf:hover .elf-image {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 107, 0, 0.6);
}

.starter-elf h4 {
  color: #333;
  margin-bottom: 1rem;
  font-size: 1.3rem;
  position: relative;
  z-index: 1;
}

.starter-elf p {
  color: #666;
  margin: 0.5rem 0;
  font-size: 1rem;
  position: relative;
  z-index: 1;
}

.starter-elf button {
  margin-top: 1.5rem;
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #ff6b00 0%, #ff9e4f 100%);
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.4);
  position: relative;
  z-index: 1;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.starter-elf button:hover {
  background: linear-gradient(135deg, #e55a00 0%, #e88e3a 100%);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(255, 107, 0, 0.6);
}

.starter-elf button:active {
  transform: translateY(0);
  box-shadow: 0 4px 15px rgba(255, 107, 0, 0.4);
}

/* 功能按钮区 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 2rem;
  flex-wrap: wrap;
  margin-top: 2rem;
}

.action-btn {
  padding: 1.2rem 2.5rem;
  border: none;
  border-radius: 30px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  text-transform: uppercase;
  letter-spacing: 1px;
  min-width: 200px;
  justify-content: center;
}

.action-btn .btn-icon {
  font-size: 1.5rem;
}

.action-btn.primary {
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
}

.action-btn.primary:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(76, 175, 80, 0.4);
}

.action-btn.secondary {
  background: linear-gradient(135deg, #2196F3 0%, #64B5F6 100%);
  color: white;
}

.action-btn.secondary:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(33, 150, 243, 0.4);
}

.action-btn.tertiary {
  background: linear-gradient(135deg, #FF9800 0%, #FFB74D 100%);
  color: white;
}

.action-btn.quaternary {
  background: linear-gradient(135deg, var(--color-brand-dark) 0%, var(--color-brand) 100%);
  color: white;
}

.action-btn.quaternary:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px oklch(0.55 0.20 50 / 0.4);
}

.action-btn.tertiary:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(255, 152, 0, 0.4);
}

/* 弹窗样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
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
  max-width: 500px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.modal-content h3 {
  color: #333;
  margin-bottom: 1.5rem;
  text-align: center;
  font-size: 1.5rem;
}

.modal-content .form-group {
  margin-bottom: 1.5rem;
}

.modal-content label {
  display: block;
  margin-bottom: 0.5rem;
  color: #666;
  font-weight: 500;
}

.modal-content input {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.modal-content input:focus {
  outline: none;
  border-color: #ff6b00;
  box-shadow: 0 0 0 3px rgba(255, 107, 0, 0.1);
}

.modal-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.modal-buttons button {
  padding: 0.8rem 1.5rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.modal-buttons button:hover {
  transform: translateY(-2px);
}

.modal-buttons .primary-btn {
  background: linear-gradient(135deg, #ff6b00 0%, #ff9e4f 100%);
  color: white;
  font-weight: 600;
}

.modal-buttons .primary-btn:hover {
  background: linear-gradient(135deg, #e55a00 0%, #e08a3d 100%);
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
  
  .user-info-card {
    flex-direction: column;
    text-align: center;
  }
  
  .user-stats {
    flex-direction: column;
    gap: 1rem;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .action-btn {
    width: 100%;
    max-width: 300px;
  }
  
  .nav-menu {
    gap: 0.5rem;
  }
  
  .nav-btn {
    padding: 0.3rem 0.6rem;
    font-size: 0.8rem;
  }
  
  .modal-content {
    padding: 1.5rem;
    width: 95%;
  }
  
  .modal-buttons {
    flex-direction: column;
  }
  
  .modal-buttons button {
    width: 100%;
  }
}
</style>
