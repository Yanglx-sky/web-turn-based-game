import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 添加请求拦截器，设置缓存控制头和Authorization头
api.interceptors.request.use(
  config => {
    // 禁用缓存
    config.headers['Cache-Control'] = 'no-cache'
    config.headers['Pragma'] = 'no-cache'
    config.headers['Expires'] = '0'
    // 添加Authorization头
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    // 在URL中添加随机参数，避免缓存
    if (config.method === 'get') {
      config.url += (config.url.includes('?') ? '&' : '?') + 't=' + new Date().getTime()
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API错误:', error)
    return Promise.reject(error)
  }
)

export default api