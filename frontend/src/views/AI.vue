<template>
  <div class="ai-container">
    <!-- 顶部导航栏 -->
    <nav class="nav-bar">
      <div class="nav-logo">洛克王国</div>
      <div class="nav-menu">
        <button class="nav-btn" @click="navigateTo('/')">首页</button>
        <button class="nav-btn" @click="navigateTo('/elves')">我的精灵</button>
        <button class="nav-btn" @click="navigateTo('/pve')">冒险</button>
        <button class="nav-btn" @click="navigateTo('/shop')">商店</button>
        <button class="nav-btn" @click="navigateTo('/bag')">背包</button>
        <button class="nav-btn" @click="navigateTo('/train')">训练</button>
        <button class="nav-btn" @click="navigateTo('/rank')">排行榜</button>
        <button class="nav-btn" @click="navigateTo('/achievement')">成就</button>
        <button class="nav-btn" @click="navigateTo('/ai')">AI助手</button>
        <button class="nav-btn" @click="navigateTo('/chat')">聊天</button>
        <button class="nav-btn" @click="logout">退出</button>
      </div>
    </nav>
    
    <div class="main-content">
      <h2>AI精灵训练助手</h2>
      
      <div class="ai-header">
        <p class="ai-subtitle">为你提供精灵训练建议和战斗策略</p>
      </div>
      
      <div class="chat-container">
        <div class="chat-messages">
          <div 
            v-for="(message, index) in chatMessages" 
            :key="index" 
            class="chat-message" 
            :class="message.role === 'user' ? 'user-message' : 'ai-message'"
          >
            <div class="message-content">{{ message.content }}</div>
            <div class="message-time">{{ message.time }}</div>
          </div>
          <div v-if="isStreaming" class="chat-message ai-message">
            <div class="message-content">{{ streamingContent }}</div>
            <div class="message-time">正在输入...</div>
          </div>
        </div>
        
        <div class="chat-input">
          <input 
            type="text" 
            v-model="userInput" 
            placeholder="输入你的问题..." 
            @keyup.enter="sendMessage"
            :disabled="isStreaming"
          >
          <button 
            class="btn send-btn" 
            @click="sendMessage"
            :disabled="isStreaming || !userInput.trim()"
          >
            发送
          </button>
        </div>
      </div>
      
      <div class="ai-actions">
        <button class="btn action-btn" @click="clearChat">清空对话</button>
        <button class="btn action-btn" @click="getTrainingSummary">训练总结</button>
      </div>
      
      <div class="ai-info">
        <p>每日AI调用限制：{{ aiCallLimit }}次</p>
        <p>今日已使用：{{ aiCallCount }}次</p>
        <p>剩余次数：{{ aiCallLimit - aiCallCount }}次</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态管理
const activeTab = ref('ai')
const sessionId = ref('')
const chatMessages = ref([])
const userInput = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const aiCallCount = ref(0)
const aiCallLimit = 20

// 获取AI调用次数
const getAICallCount = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      return
    }
    const userData = JSON.parse(userStr)
    const user = userData.user || userData
    const token = userData.token || localStorage.getItem('token')
    
    if (!token || !user || !user.id) {
      return
    }
    
    const response = await fetch(`/api/ai/get/count`, {
          headers: {
            'Authorization': token
          }
        })
    if (response.ok) {
      const count = await response.json()
      aiCallCount.value = count
      console.log('AI调用次数:', count)
    }
  } catch (error) {
    console.error('获取AI调用次数失败:', error)
  }
}

// 初始化会话
const initSession = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      router.push('/auth')
      return
    }
    const userData = JSON.parse(userStr)
    const user = userData.user || userData
    const token = userData.token || localStorage.getItem('token')
    
    if (!token || !user || !user.id) {
      router.push('/auth')
      return
    }
    
    const response = await fetch('/api/ai/session/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': token
      },
      body: `userId=${user.id}&title=新对话&scene=common`
    })
    
    if (!response.ok) {
      throw new Error('创建会话失败')
    }
    
    const sessionIdLong = await response.json()
    
    // 确保sessionIdLong是数字类型
    if (typeof sessionIdLong !== 'number') {
      throw new Error('会话ID格式错误')
    }
    
    sessionId.value = sessionIdLong.toString()
    console.log('会话初始化成功，sessionId:', sessionId.value)
    
    // 添加欢迎消息
    chatMessages.value = [{
      role: 'ai',
      content: '你好！我是你的精灵训练AI助手，有什么可以帮助你的吗？',
      time: new Date().toLocaleTimeString()
    }]
    
    // 获取AI调用次数
    await getAICallCount()
  } catch (error) {
    console.error('初始化会话失败:', error)
    sessionId.value = ''
    alert('初始化AI会话失败，请刷新页面重试')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isStreaming.value) return
  
  // 验证sessionId是否有效
  if (!sessionId.value || typeof sessionId.value !== 'string' || sessionId.value === '') {
    console.error('会话ID无效，重新初始化会话')
    await initSession()
    if (!sessionId.value) {
      alert('会话初始化失败，请刷新页面重试')
      return
    }
  }
  
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    router.push('/auth')
    return
  }
  const userData = JSON.parse(userStr)
  const user = userData.user || userData
  
  if (!user || !user.id) {
    router.push('/auth')
    return
  }
  
  // 添加用户消息
  const messageTime = new Date().toLocaleTimeString()
  chatMessages.value.push({
    role: 'user',
    content: userInput.value,
    time: messageTime
  })
  
  const inputContent = userInput.value
  userInput.value = ''
  
  // 流式获取AI回复
  isStreaming.value = true
  streamingContent.value = ''
  
  try {
    const token = userData.token || localStorage.getItem('token')
    
    if (!token) {
      router.push('/auth')
      return
    }
    
    console.log('发送消息，sessionId:', sessionId.value)
    
    // 使用fetch API获取AI回复
    const response = await fetch(`/api/ai/stream/analysis?sessionId=${sessionId.value}&content=${encodeURIComponent(inputContent)}`, {
      headers: {
        'Authorization': token
      }
    })
    
    if (!response.ok) {
      throw new Error('获取AI回复失败')
    }
    
    // 读取响应流
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      
      const chunk = decoder.decode(value, { stream: true })
      if (chunk === '[DONE]') {
        break
      }
      
      streamingContent.value += chunk
    }
    
    isStreaming.value = false
    chatMessages.value.push({
      role: 'ai',
      content: streamingContent.value,
      time: new Date().toLocaleTimeString()
    })
    streamingContent.value = ''
  } catch (error) {
    console.error('发送消息失败:', error)
    isStreaming.value = false
    chatMessages.value.push({
      role: 'ai',
      content: '发送消息失败，请稍后重试',
      time: new Date().toLocaleTimeString()
    })
  } finally {
    // 无论成功与否，都更新AI调用次数
    await getAICallCount()
  }
}

// 清空对话
const clearChat = () => {
  chatMessages.value = [{
    role: 'ai',
    content: '你好！我是你的精灵训练AI助手，有什么可以帮助你的吗？',
    time: new Date().toLocaleTimeString()
  }]
}

// 获取训练总结
const getTrainingSummary = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      router.push('/auth')
      return
    }
    const userData = JSON.parse(userStr)
    const user = userData.user || userData
    const token = userData.token || localStorage.getItem('token')
    
    if (!token || !user || !user.id) {
      router.push('/auth')
      return
    }
    
    const response = await fetch(`/api/ai/training/summary`, {
      headers: {
        'Authorization': token
      }
    })
    const summary = await response.text()
    
    chatMessages.value.push({
      role: 'ai',
      content: summary,
      time: new Date().toLocaleTimeString()
    })
  } catch (error) {
    console.error('获取训练总结失败:', error)
    chatMessages.value.push({
      role: 'ai',
      content: '获取训练总结失败，请稍后重试',
      time: new Date().toLocaleTimeString()
    })
  }
}



// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 退出登录
const logout = () => {
  // 关闭会话
  if (sessionId.value) {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      const user = userData.user || userData
      const token = userData.token || localStorage.getItem('token')
      if (token) {
                fetch(`/api/ai/session/close?sessionId=${sessionId.value}`, {
                  headers: {
                    'Authorization': token
                  }
                })
              }
    }
  }
  localStorage.removeItem('user')
  router.push('/auth')
}

onMounted(() => {
  initSession()
})

onUnmounted(() => {
  // 关闭会话
  if (sessionId.value) {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      const token = userData.token || localStorage.getItem('token')
      if (token) {
        fetch(`/api/ai/session/close?sessionId=${sessionId.value}`, {
          headers: {
            'Authorization': token
          }
        })
      }
    }
  }
})
</script>

<style scoped>
/* 全局样式 */
.ai-container {
  min-height: 100vh;
  background-image: url('https://a0ai.marscode.cn/api/ide/v1/text_to_image?prompt=colorful%20fantasy%20game%20background%20with%20magical%20elements%20and%20floating%20islands&image_size=landscape_16_9');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Arial', sans-serif;
}

/* 导航栏 */
.nav-bar {
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #ff6b00;
  text-shadow: 0 0 10px rgba(255, 107, 0, 0.5);
}

.nav-menu {
  display: flex;
  gap: 1rem;
}

.nav-btn {
  background: transparent;
  color: white;
  border: 1px solid #ff6b00;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
}

.nav-btn:hover {
  background: #ff6b00;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 0, 0.4);
}

.main-content {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

h2 {
  text-align: center;
  margin-bottom: 1rem;
  color: #ff8c00;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  font-size: 28px;
}

.ai-header {
  text-align: center;
  margin-bottom: 30px;
}

.ai-subtitle {
  color: #666;
  font-size: 16px;
  margin: 0;
}

/* 对话容器 */
.chat-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 20px;
  border: 2px solid #ff8c00;
}

.chat-messages {
  max-height: 500px;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.chat-message {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 18px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  align-self: flex-end;
  background: #e3f2fd;
  border-bottom-right-radius: 4px;
}

.ai-message {
  align-self: flex-start;
  background: #f5f5f5;
  border-bottom-left-radius: 4px;
}

.message-content {
  font-size: 16px;
  line-height: 1.5;
  margin-bottom: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

/* 输入区域 */
.chat-input {
  display: flex;
  padding: 15px;
  border-top: 1px solid #eee;
  background: #f9f9f9;
}

.chat-input input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #ddd;
  border-radius: 25px;
  font-size: 16px;
  margin-right: 10px;
  transition: border-color 0.3s ease;
}

.chat-input input:focus {
  outline: none;
  border-color: #ff8c00;
  box-shadow: 0 0 0 3px rgba(255, 140, 0, 0.1);
}

.send-btn {
  background: linear-gradient(135deg, #ff8c00, #ff6b00);
  color: white;
  border: none;
  border-radius: 25px;
  padding: 0 24px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(255, 140, 0, 0.3);
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff6b00, #e65c00);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(255, 140, 0, 0.4);
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

/* 操作按钮 */
.ai-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-bottom: 20px;
}

.action-btn {
  background: #4caf50;
  color: white;
  border: none;
  border-radius: 25px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.3);
}

.action-btn:hover {
  background: #43a047;
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(76, 175, 80, 0.4);
}

/* AI信息 */
.ai-info {
  text-align: center;
  padding: 15px;
  background: rgba(255, 140, 0, 0.1);
  border-radius: 10px;
  border: 2px solid #ff8c00;
}

.ai-info p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

@media (max-width: 768px) {
  .ai-container {
    padding: 10px;
  }
  
  .main-content {
    padding: 20px;
  }
  
  .nav-menu {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .nav-btn {
    font-size: 0.8rem;
    padding: 0.4rem 0.8rem;
  }
  
  .chat-message {
    max-width: 90%;
  }
  
  .ai-actions {
    flex-wrap: wrap;
  }
  
  .action-btn {
    flex: 1;
    min-width: 120px;
  }
}
</style>