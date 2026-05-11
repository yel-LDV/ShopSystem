import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (originalRequest.url?.includes('/auth/login') || originalRequest.url?.includes('/auth/refresh')) {
        return Promise.reject(error)
      }
      originalRequest._retry = true
      const refreshToken = sessionStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const response = await axios.post(
            '/api/auth/refresh',
            {},
            { headers: { Authorization: `Bearer ${refreshToken}` } }
          )
          const { token, refreshToken: newRefresh } = response.data.data
          sessionStorage.setItem('token', token)
          sessionStorage.setItem('refreshToken', newRefresh)
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        } catch {
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('refreshToken')
          sessionStorage.removeItem('user')
          window.location.href = '/login'
        }
      } else {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
