<template>
  <div class="ai-container">
    <GameTopNav />
    
    <div class="main-content">
      <div class="page-header">
        <p class="section-eyebrow">AI ASSISTANT</p>
        <h1>AI精灵训练助手</h1>
        <p class="ai-subtitle">为你提供精灵训练建议和战斗策略</p>
      </div>
      
      <div class="ai-workspace">
        <!-- 左侧会话列表 -->
        <div class="session-list-section">
          <div class="session-header">
            <h3>会话列表</h3>
            <button class="new-session-btn" @click="createNewSession" title="新建会话">
              ＋
            </button>
          </div>
          <div class="session-list">
            <div v-if="sessions.length === 0" class="empty-sessions">
              暂无会话
            </div>
            <div 
              v-for="session in sessions" 
              :key="session.id"
              class="session-item"
              :class="{ active: sessionId === session.id.toString() }"
              @click="switchSession(session.id)"
            >
              <div class="session-info">
                <div class="session-title">{{ session.title || '新对话' }}</div>
                <div class="session-time">{{ formatTime(session.createTime) }}</div>
              </div>
              <button 
                class="delete-session-btn" 
                @click.stop="deleteSession(session.id, session.title)"
                title="删除会话"
              >
                ✕
              </button>
            </div>
          </div>
        </div>
        
        <!-- 中间聊天区域 -->
        <div class="chat-section">
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
        </div>
        
        <!-- 右侧信息面板 -->
        <div class="sidebar-section">
          <div class="ai-info">
            <div class="info-item">每日额度<span>{{ aiCallLimit }}次</span></div>
            <div class="info-item">今日已用<span>{{ aiCallCount }}次</span></div>
            <div class="info-item">剩余次数<span class="highlight">{{ aiCallLimit - aiCallCount }}次</span></div>
          </div>
          
          <div class="ai-actions">
            <button class="action-btn" @click="clearChat">清空对话</button>
            <button class="action-btn" @click="getTrainingSummary">训练总结</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import GameTopNav from '../components/GameTopNav.vue'

const router = useRouter()

// 状态管理
const activeTab = ref('ai')
const sessionId = ref('')
const sessions = ref([])  // 会话列表
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
      const result = await response.json()
      // 后端返回格式: {code: 200, msg: "操作成功", data: 18}
      aiCallCount.value = result.data || 0
      console.log('AI调用次数:', aiCallCount.value)
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
      body: `title=新对话&scene=common`
    })
    
    console.log('响应状态:', response.status)
    console.log('响应OK:', response.ok)
    
    const result = await response.json()
    console.log('完整响应数据:', JSON.stringify(result, null, 2))

    // 兼容多种响应格式
    const errorCode = result.code || result.status || result.errorCode
    const errorMsg = result.msg || result.message || result.error || result.errorMessage

    if (errorCode !== 200 && errorCode !== 0) {
      console.error('后端返回错误:', errorMsg, '完整数据:', result)
      throw new Error(errorMsg || '创建会话失败')
    }

    const sessionIdLong = result.data
    console.log('sessionId:', sessionIdLong)

    if (typeof sessionIdLong !== 'number' && typeof sessionIdLong !== 'string') {
      throw new Error('会话ID格式错误')
    }
    
    sessionId.value = sessionIdLong.toString()
    console.log('会话初始化成功，sessionId:', sessionId.value)
    
    // 加载会话列表
    await loadSessions()
    
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

    // 显示友好的错误提示
    const errorMsg = error.message || '初始化AI会话失败'
    console.log('错误信息:', errorMsg)

    if (errorMsg.includes('额度') || errorMsg.includes('上限') || errorMsg.includes('限制') || errorMsg.includes('次数')) {
      // 记录错误时间，避免频繁重试
      sessionStorage.setItem('aiSessionErrorTime', Date.now().toString())

      alert('⚠️ AI使用提示\n\n' + errorMsg + '\n\n💡 建议：\n• 请明天再试（每日额度会重置）\n• 或联系客服提升额度')
    } else if (errorMsg.includes('登录') || errorMsg.includes('权限') || errorMsg.includes('token')) {
      alert('🔐 登录已过期\n\n请重新登录后再试')
      router.push('/auth')
    } else {
      alert('❌ 初始化失败\n\n' + errorMsg + '\n\n请刷新页面重试')
    }
  }
}

// 加载会话列表
const loadSessions = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    
    const userData = JSON.parse(userStr)
    const token = userData.token || localStorage.getItem('token')
    
    if (!token) return
    
    const response = await fetch('/api/ai/session/list', {
      headers: {
        'Authorization': token
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.code === 200) {
        sessions.value = result.data || []
        console.log('加载会话列表成功:', sessions.value.length, '个会话')
      }
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 创建新会话
const createNewSession = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      router.push('/auth')
      return
    }
    
    const userData = JSON.parse(userStr)
    const token = userData.token || localStorage.getItem('token')
    
    if (!token) {
      router.push('/auth')
      return
    }
    
    const response = await fetch('/api/ai/session/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': token
      },
      body: `title=新对话&scene=common`
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.code === 200) {
        const newSessionId = result.data.toString()
        console.log('创建新会话成功:', newSessionId)
        
        // 切换到新会话
        sessionId.value = newSessionId
        chatMessages.value = [{
          role: 'ai',
          content: '你好！我是你的精灵训练AI助手，有什么可以帮助你的吗？',
          time: new Date().toLocaleTimeString()
        }]
        
        // 重新加载会话列表
        await loadSessions()
      }
    }
  } catch (error) {
    console.error('创建新会话失败:', error)
    alert('创建会话失败，请重试')
  }
}

// 切换会话
const switchSession = async (targetSessionId) => {
  sessionId.value = targetSessionId.toString()
  chatMessages.value = []  // 清空当前消息
  userInput.value = ''
  
  // TODO: 可以添加加载历史消息的功能
  console.log('切换到会话:', sessionId.value)
}

// 删除会话
const deleteSession = async (targetSessionId, title) => {
  if (!confirm(`确定要删除会话“${title || '新对话'}”吗？`)) {
    return
  }
  
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) return
    
    const userData = JSON.parse(userStr)
    const token = userData.token || localStorage.getItem('token')
    
    if (!token) return
    
    const response = await fetch('/api/ai/session/close', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': token
      },
      body: `sessionId=${targetSessionId}`
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.code === 200 && result.data) {
        console.log('删除会话成功:', targetSessionId)
        
        // 如果删除的是当前会话，清空聊天
        if (sessionId.value === targetSessionId.toString()) {
          sessionId.value = ''
          chatMessages.value = []
          // 如果有其他会话，切换到第一个
          if (sessions.value.length > 1) {
            const otherSession = sessions.value.find(s => s.id !== targetSessionId)
            if (otherSession) {
              switchSession(otherSession.id)
            }
          }
        }
        
        // 重新加载会话列表
        await loadSessions()
        
        alert('删除成功')
      }
    }
  } catch (error) {
    console.error('删除会话失败:', error)
    alert('删除失败，请重试')
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  // 今天
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 昨天
  else if (diff < 48 * 60 * 60 * 1000) {
    return '昨天'
  }
  // 更早
  else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isStreaming.value) return
  
  // 验证sessionId是否有效
  if (!sessionId.value || typeof sessionId.value !== 'string' || sessionId.value === '') {
    console.log('会话ID无效，尝试重新初始化会话')

    // 检查是否已经因为额度问题初始化失败过
    const lastErrorTime = sessionStorage.getItem('aiSessionErrorTime')
    const now = Date.now()

    if (lastErrorTime && (now - parseInt(lastErrorTime)) < 60000) {
      // 1分钟内已经失败过，不再重试
      console.log('1分钟内已因额度问题失败，不再重试')
      chatMessages.value.push({
        role: 'ai',
        content: '⚠️ 今日AI调用次数已达上限\n\n请明天再试，或联系客服提升额度',
        time: new Date().toLocaleTimeString()
      })
      userInput.value = ''
      return
    }

    await initSession()
    if (!sessionId.value) {
      // initSession 已经显示了错误提示，这里只需要清空输入
      userInput.value = ''
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
    
    // 使用真正的SSE流式接口获取AI回复
    const response = await fetch(`/api/ai/stream/chat?sessionId=${sessionId.value}&content=${encodeURIComponent(inputContent)}`, {
      headers: {
        'Authorization': token
      }
    })
    
    if (!response.ok) {
      // 尝试解析错误信息
      let errorMsg = '获取AI回复失败'
      try {
        const contentType = response.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const errorResult = await response.json()
          console.log('流式接口错误响应:', errorResult)
          errorMsg = errorResult.msg || errorResult.message || errorResult.error || errorMsg
        } else {
          // 如果不是JSON，可能是纯文本错误
          const errorText = await response.text()
          console.log('流式接口错误文本:', errorText)
          errorMsg = errorText || errorMsg
        }
      } catch (e) {
        console.error('解析错误响应失败:', e)
      }
      throw new Error(errorMsg)
    }
    
    // 读取响应流
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let chunkCount = 0
    
    console.log('[SSE前端] 开始接收流式数据')
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        console.log('[SSE前端] 流式接收完成，总共接收', chunkCount, '个数据块')
        break
      }
      
      chunkCount++
      const chunk = decoder.decode(value, { stream: true })
      console.log(`[SSE前端] 第${chunkCount}块数据 (${chunk.length}字节):`, chunk.substring(0, 100))
      
      buffer += chunk
      
      // 按行处理SSE数据
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留不完整的行
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        
        // 跳过空行
        if (!trimmedLine) continue
        
        // 检查是否结束
        if (trimmedLine === '[DONE]') {
          console.log('[SSE前端] 收到[DONE]结束信号')
          break
        }
        
        // 解析data: 行
        if (trimmedLine.startsWith('data: ')) {
          const content = trimmedLine.substring(6) // 去掉 "data: " 前缀
          console.log('[SSE前端] 解析到内容:', content)
          streamingContent.value += content
        }
      }
    }
    
    console.log('[SSE前端] 最终流式内容长度:', streamingContent.value.length)
    console.log('[SSE前端] 最终流式内容:', streamingContent.value.substring(0, 100))
    
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

    // 显示友好的错误提示
    const errorMsg = error.message || '发送消息失败'
    console.log('发送消息错误:', errorMsg)

    let displayMessage = '❌ ' + errorMsg
    if (errorMsg.includes('额度') || errorMsg.includes('上限') || errorMsg.includes('限制') || errorMsg.includes('次数')) {
      displayMessage = '⚠️ AI使用额度已达上限\n\n' + errorMsg + '\n\n请明天再试，或联系客服提升额度'
      // 记录错误时间，避免频繁重试
      sessionStorage.setItem('aiSessionErrorTime', Date.now().toString())
    } else if (errorMsg.includes('登录') || errorMsg.includes('权限')) {
      displayMessage = '🔐 登录已过期，请重新登录'
      setTimeout(() => router.push('/auth'), 2000)
    }

    chatMessages.value.push({
      role: 'ai',
      content: displayMessage,
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
    
    if (response.ok) {
      const result = await response.json()
      // 后端返回格式: {code: 200, msg: "操作成功", data: "总结内容"}
      const summary = result.data || '获取训练总结失败'
      
      chatMessages.value.push({
        role: 'ai',
        content: summary,
        time: new Date().toLocaleTimeString()
      })
      
      // 更新AI调用次数
      await getAICallCount()
    } else {
      throw new Error('获取训练总结失败')
    }
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
  padding: 0 20px 28px;
  background:
    radial-gradient(circle at top, rgba(255, 165, 81, 0.16), transparent 24%),
    linear-gradient(180deg, #06080f 0%, #101827 52%, #111d2e 100%);
  color: #f8f1e4;
  overflow-x: hidden;
}

.main-content {
  max-width: 1100px;
  margin: 22px auto 0;
  padding: 0 40px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-workspace {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-top: 10px;
}

/* 会话列表面板 */
.session-list-section {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-height: 65vh;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.85), rgba(16, 9, 3, 0.85));
  border-radius: 16px;
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.5);
}

.session-header h3 {
  margin: 0;
  color: #fff4df;
  font-size: 1.1rem;
  font-weight: 700;
}

.new-session-btn {
  background: rgba(255, 156, 58, 0.2);
  color: #ffc107;
  border: 1px solid rgba(255, 156, 58, 0.4);
  border-radius: 8px;
  width: 32px;
  height: 32px;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.new-session-btn:hover {
  background: rgba(255, 156, 58, 0.3);
  border-color: rgba(255, 156, 58, 0.8);
  transform: scale(1.1);
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: calc(65vh - 80px);
  padding: 8px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-thumb {
  background: rgba(255, 156, 58, 0.3);
  border-radius: 3px;
}

.empty-sessions {
  text-align: center;
  color: rgba(247, 239, 224, 0.5);
  padding: 40px 20px;
  font-size: 0.9rem;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(24, 15, 6, 0.6);
  border: 1px solid rgba(255, 169, 79, 0.15);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.session-item:hover {
  background: rgba(255, 156, 58, 0.1);
  border-color: rgba(255, 156, 58, 0.4);
  transform: translateX(4px);
}

.session-item.active {
  background: rgba(255, 156, 58, 0.2);
  border-color: rgba(255, 156, 58, 0.6);
  box-shadow: 0 0 12px rgba(255, 156, 58, 0.2);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  color: #fff4df;
  font-size: 0.95rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-time {
  color: rgba(247, 239, 224, 0.5);
  font-size: 0.75rem;
}

.delete-session-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  line-height: 1;
  opacity: 0;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  background: rgba(255, 68, 68, 0.2);
  color: #ff4444;
  transform: scale(1.1);
}

.chat-section {
  flex: 1;
  min-width: 0;
}

.sidebar-section {
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 标题区域 */
.page-header {
  text-align: center;
  margin-bottom: 0.5rem;
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

.ai-subtitle {
  color: rgba(247, 239, 224, 0.6);
  font-size: 1rem;
  margin: 0.5rem 0 1rem;
}

/* 对话容器 */
.chat-container {
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.85), rgba(16, 9, 3, 0.85));
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(255, 152, 0, 0.5);
  border-radius: 20px;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 65vh;
  min-height: 500px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 156, 58, 0.3);
  border-radius: 4px;
}

.chat-message {
  max-width: 85%;
  padding: 14px 18px;
  border-radius: 18px;
  animation: fadeIn 0.3s ease;
  line-height: 1.6;
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
  background: linear-gradient(135deg, rgba(255, 156, 58, 0.15), rgba(255, 122, 26, 0.15));
  border: 1px solid rgba(255, 156, 58, 0.3);
  border-bottom-right-radius: 4px;
  color: #fff4df;
  box-shadow: 0 4px 12px rgba(255, 122, 26, 0.05);
}

.ai-message {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom-left-radius: 4px;
  color: rgba(247, 239, 224, 0.9);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.message-content {
  font-size: 0.95rem;
  margin-bottom: 5px;
  white-space: pre-wrap;
}

.message-time {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.4);
  text-align: right;
}

/* 输入区域 */
.chat-input {
  display: flex;
  padding: 15px 20px;
  background: rgba(0, 0, 0, 0.4);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  align-items: center;
}

.chat-input input {
  flex: 1;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 25px;
  font-size: 0.95rem;
  margin-right: 15px;
  color: #fff;
  transition: all 0.3s ease;
}

.chat-input input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.chat-input input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 156, 58, 0.5);
  box-shadow: 0 0 0 2px rgba(255, 156, 58, 0.2);
}

.send-btn {
  background: linear-gradient(135deg, #ffe1a2, #ff9c3a 55%, #ff7a1a);
  color: #2d1a0a;
  box-shadow: 0 4px 12px rgba(255, 132, 29, 0.2);
  border: none;
  border-radius: 25px;
  padding: 0 28px;
  height: 48px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 0.05em;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(255, 132, 29, 0.35);
}

.send-btn:disabled {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
  box-shadow: none;
}

/* 操作按钮 */
.ai-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-btn {
  background: rgba(16, 9, 3, 0.8);
  color: #fff4df;
  border: 1px solid rgba(255, 156, 58, 0.3);
  border-radius: 16px;
  padding: 12px 24px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
}

.action-btn:hover {
  background: rgba(255, 156, 58, 0.15);
  border-color: rgba(255, 156, 58, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 156, 58, 0.15);
}

/* AI信息 */
.ai-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px 20px;
  background: linear-gradient(180deg, rgba(24, 15, 6, 0.85), rgba(16, 9, 3, 0.85));
  border-radius: 20px;
  border: 1px solid rgba(255, 169, 79, 0.2);
  border-top: 3px solid rgba(162, 255, 196, 0.5);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  color: rgba(247, 239, 224, 0.7);
  font-size: 0.85rem;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.info-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-item span {
  color: #ffc107;
  font-size: 1.2rem;
  font-weight: bold;
  text-shadow: 0 0 5px rgba(255, 193, 7, 0.3);
}

.info-item span.highlight {
  color: #a2ffc4;
  text-shadow: 0 0 5px rgba(162, 255, 196, 0.3);
}

@media (max-width: 768px) {
  .main-content {
    padding: 0 15px;
  }

  .ai-workspace {
    flex-direction: column;
    gap: 15px;
  }
  
  .session-list-section {
    width: 100%;
    max-height: 200px;
  }
  
  .session-list {
    max-height: calc(200px - 80px);
  }
  
  .sidebar-section {
    width: 100%;
    flex-direction: column-reverse;
  }

  .ai-info {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: space-around;
    padding: 15px;
  }
  
  .info-item {
    align-items: center;
    border-bottom: none;
    padding-bottom: 0;
  }
  
  .ai-actions {
    flex-direction: row;
  }
  
  .chat-container {
    height: 60vh;
    min-height: 400px;
  }
}
</style>
