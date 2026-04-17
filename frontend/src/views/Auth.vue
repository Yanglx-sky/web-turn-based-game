<template>
  <div class="auth-container">
    <div class="auth-box" v-if="!showStarterSelection">
      <h1 class="welcome-title">洛克王国：世界</h1>
      <h2>{{ isLogin ? '登录' : '注册' }}</h2>
      <!-- 提示信息 -->
      <div v-if="message" :class="['message', messageType]">
        {{ message }}
      </div>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="phone">手机号</label>
          <input type="text" id="phone" v-model="form.phone" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="form.password" required>
        </div>
        <div class="form-group" v-if="!isLogin">
          <label for="confirmPassword">确认密码</label>
          <input type="password" id="confirmPassword" v-model="form.confirmPassword" required>
        </div>
        <div class="form-group" v-if="!isLogin">
          <label for="nickname">昵称</label>
          <input type="text" id="nickname" v-model="form.nickname" required>
        </div>
        <div class="form-group" v-if="!isLogin">
          <label for="email">邮箱</label>
          <input type="email" id="email" v-model="form.email" required>
        </div>
        <button type="submit" class="submit-btn">{{ isLogin ? '登录' : '注册' }}</button>
      </form>
      <p class="toggle-text">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <a href="#" @click.prevent="isLogin = !isLogin">{{ isLogin ? '立即注册' : '立即登录' }}</a>
      </p>
    </div>
    
    <!-- 御三家选择界面 -->
    <div class="auth-box" v-else>
      <h1 class="welcome-title">洛克王国：世界</h1>
      <h2>选择你的初始精灵</h2>
      <p class="starter-intro">请从以下三只精灵中选择一只作为你的初始伙伴</p>
      <div class="starter-list" v-if="!loadingStarterElves && starterElves.length > 0">
        <div
          v-for="elf in starterElves"
          :key="elf.id"
          class="starter-item"
          :class="getElementClass(elf.type)"
          @click="selectStarter(elf.id)"
        >
          <div class="starter-image" :class="getElementClass(elf.type)">
            <div class="elf-icon">{{ elf.name.charAt(0) }}</div>
          </div>
          <h3>{{ elf.name }}</h3>
          <p class="elf-type" :class="getElementClass(elf.type)">{{ elf.type }}</p>
        </div>
      </div>
      <div v-else-if="loadingStarterElves" class="loading-state">
        <p>加载中...</p>
      </div>
      <div v-else class="no-elfs">
        <p>无</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../api/user'
import { userElfApi } from '../api/userElf'
import { elfApi } from '../api/elf'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLogin = ref(true)
const message = ref('')
const messageType = ref('') // success 或 error
const form = ref({
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: ''
})

const setMessage = (text, type) => {
  message.value = text
  messageType.value = type
  // 3秒后清空提示
  setTimeout(() => {
    message.value = ''
    messageType.value = ''
  }, 3000)
}

const showStarterSelection = ref(false)
const starterElves = ref([])
const loadingStarterElves = ref(false)

// 获取御三家精灵
const loadStarterElves = async () => {
  try {
    loadingStarterElves.value = true
    const response = await elfApi.getStarterElves()
    console.log('获取御三家精灵响应:', response)
    if (response && Array.isArray(response)) {
      starterElves.value = response
    }
  } catch (error) {
    console.error('获取御三家精灵失败:', error)
  } finally {
    loadingStarterElves.value = false
  }
}

// 根据精灵类型返回CSS类名
const getElementClass = (type) => {
  const typeMap = {
    '火': 'element-fire',
    '水': 'element-water',
    '草': 'element-grass'
  }
  return typeMap[type] || ''
}

// 组件挂载时获取御三家精灵
onMounted(() => {
  if (showStarterSelection.value) {
    loadStarterElves()
  }
})

const handleSubmit = async () => {
  console.log('开始提交表单:', isLogin.value, form.value)
  
  // 密码长度校验
  if (form.value.password.length < 6 || form.value.password.length > 20) {
    setMessage('密码长度应在6-20个字符之间', 'error')
    return
  }
  
  // 注册时检查确认密码
  if (!isLogin.value && form.value.password !== form.value.confirmPassword) {
    setMessage('两次输入的密码不一致', 'error')
    return
  }
  
  try {
    if (isLogin.value) {
      // 登录
      console.log('执行登录操作')
      const response = await userApi.login({
        phone: form.value.phone,
        password: form.value.password
      })
      console.log('登录响应:', response)
      if (response && response.code === 200 && response.data) {
        // 确保存储的是用户对象
        const userData = response.data
        localStorage.setItem('user', JSON.stringify(userData.user))
        localStorage.setItem('token', userData.token)
        setMessage('登录成功！', 'success')
        
        // 直接跳转到首页
        console.log('登录成功，跳转到首页')
        setTimeout(() => {
          router.push('/')
        }, 1000)
      } else if (response && response.code !== 200) {
        setMessage(response.msg || '登录失败，请检查手机号和密码', 'error')
      } else {
        setMessage('登录失败，请检查手机号和密码', 'error')
      }
    } else {
      // 注册
      console.log('执行注册操作')
      const response = await userApi.register({
        password: form.value.password,
        confirmPassword: form.value.confirmPassword,
        nickname: form.value.nickname,
        phone: form.value.phone,
        email: form.value.email
      })
      console.log('注册响应:', response)
      if (response && response.code === 200) {
        setMessage('注册成功！请登录', 'success')
        // 延迟切换到登录页面，让用户看到提示
        setTimeout(() => {
          isLogin.value = true
        }, 1000)
      } else if (response && response.code !== 200) {
        setMessage(response.msg || '注册失败', 'error')
      } else {
        setMessage('注册失败，请稍后重试', 'error')
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    setMessage('网络错误，请检查网络连接', 'error')
  }
}

const selectStarter = async (elfId) => {
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    // 调用后端API为用户创建精灵
    const response = await userElfApi.create(elfId)
    console.log('精灵创建响应:', response)
    if (response) {
      setMessage('精灵选择成功！', 'success')
      // 延迟跳转到首页
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } else {
      setMessage('精灵选择失败', 'error')
    }
  } catch (error) {
    console.error('精灵选择失败:', error)
    setMessage('精灵选择失败，请稍后重试', 'error')
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  padding: 20px;
  overflow: hidden;
  background-image: url('../assets/photo/t0463e272ac71ce140c.jpg');
  background-size: cover;
  background-position: center;
}

.auth-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  z-index: 0;
}

.auth-box {
  background: transparent;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  padding: 40px;
  width: 100%;
  max-width: 400px;
  text-align: center;
  position: relative;
  z-index: 2;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.welcome-title {
  color: #ff6b00;
  margin-bottom: 10px;
  font-size: 28px;
  font-weight: 700;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

h2 {
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 30px;
  font-size: 22px;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.form-group {
  margin-bottom: 20px;
  text-align: left;
}

label {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  font-size: 14px;
}

input {
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}

input:focus {
  outline: none;
  border-color: #ff6b00;
  box-shadow: 0 0 0 3px rgba(255, 107, 0, 0.2);
  background: rgba(255, 255, 255, 0.3);
}

.submit-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #ff6b00 0%, #ff9e4f 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.3);
}

.submit-btn:hover {
  background: linear-gradient(135deg, #e55a00 0%, #e08a3d 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 107, 0, 0.4);
}

.toggle-text {
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.toggle-text a {
  color: #ff6b00;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s ease;
}

.toggle-text a:hover {
  text-decoration: underline;
  color: #e55a00;
}

.message {
  padding: 14px;
  border-radius: 10px;
  margin-bottom: 20px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message.success {
  background-color: rgba(212, 237, 218, 0.7);
  color: #155724;
  border: 1px solid rgba(195, 230, 203, 0.8);
}

.message.error {
  background-color: rgba(248, 215, 218, 0.7);
  color: #721c24;
  border: 1px solid rgba(245, 198, 203, 0.8);
}

/* 御三家选择样式 */
.starter-intro {
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 30px;
  font-size: 16px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.starter-list {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 30px;
}

.starter-item {
  flex: 1;
  padding: 24px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
}

.starter-item:hover {
  border-color: #ff6b00;
  box-shadow: 0 6px 20px rgba(255, 107, 0, 0.2);
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.3);
}

.starter-image {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b00 0%, #ff9e4f 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.3);
}

.elf-icon {
  font-size: 48px;
  font-weight: 700;
  color: white;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.starter-item h3 {
  margin: 15px 0 10px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 20px;
  font-weight: 600;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.elf-type {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  margin: 0;
  font-weight: 500;
}

/* 元素类型颜色 */
.starter-item.element-fire:hover {
  border-color: var(--color-fire);
  box-shadow: 0 6px 20px oklch(0.55 0.22 30 / 0.3);
}

.starter-image.element-fire {
  background: linear-gradient(135deg, var(--color-fire) 0%, var(--color-fire-light) 100%);
  box-shadow: 0 4px 12px oklch(0.55 0.22 30 / 0.4);
}

.elf-type.element-fire {
  color: oklch(0.75 0.12 30);
}

.starter-item.element-water:hover {
  border-color: var(--color-water);
  box-shadow: 0 6px 20px oklch(0.55 0.18 240 / 0.3);
}

.starter-image.element-water {
  background: linear-gradient(135deg, var(--color-water) 0%, var(--color-water-light) 100%);
  box-shadow: 0 4px 12px oklch(0.55 0.18 240 / 0.4);
}

.elf-type.element-water {
  color: oklch(0.75 0.12 240);
}

.starter-item.element-grass:hover {
  border-color: var(--color-grass);
  box-shadow: 0 6px 20px oklch(0.55 0.18 150 / 0.3);
}

.starter-image.element-grass {
  background: linear-gradient(135deg, var(--color-grass) 0%, var(--color-grass-light) 100%);
  box-shadow: 0 4px 12px oklch(0.55 0.18 150 / 0.4);
}

.elf-type.element-grass {
  color: oklch(0.75 0.12 150);
}

.loading-state,
.no-elfs {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  font-size: 18px;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .auth-box {
    padding: 30px;
    margin: 0 20px;
  }
  
  .welcome-title {
    font-size: 24px;
  }
  
  h2 {
    font-size: 20px;
  }
  
  .starter-list {
    flex-direction: column;
    gap: 15px;
  }
  
  .starter-item {
    flex-direction: row;
    align-items: center;
    text-align: left;
  }
  
  .starter-image {
    width: 80px;
    height: 80px;
    margin: 0 20px 0 0;
  }
  
  .elf-icon {
    font-size: 32px;
  }
  
  .starter-item h3 {
    margin: 0 0 5px;
  }
  
  .elf-type {
    margin: 0;
  }
  
  .loading-state,
  .no-elfs {
    min-height: 150px;
    font-size: 16px;
  }
}
</style>