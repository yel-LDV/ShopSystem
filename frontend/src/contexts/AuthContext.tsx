import React, { createContext, useContext, useEffect, useState } from 'react'
import type { User, LoginResponse, ApiResponse } from '../types'
import api from '../services/api'

interface AuthContextType {
  user: User | null
  token: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null,
  loading: true,
  login: async () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(sessionStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`
      const savedUser = sessionStorage.getItem('user')
      if (savedUser) {
        setUser(JSON.parse(savedUser))
      }
    }
    setLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    const response = await api.post<ApiResponse<LoginResponse>>('/auth/login', { email, password })
    const data = response.data.data

    const newToken = data.token
    setToken(newToken)
    sessionStorage.setItem('token', newToken)
    sessionStorage.setItem('refreshToken', data.refreshToken)

    api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`

    const userData: User = {
      userId: data.userId,
      email: data.email,
      fullName: data.fullName,
      role: data.role,
    }
    setUser(userData)
    sessionStorage.setItem('user', JSON.stringify(userData))

    if (data.role === 'ROLE_ADMIN') window.location.href = '/admin'
    else if (data.role === 'ROLE_STORE') window.location.href = '/store'
    else if (data.role === 'ROLE_SUPPLIER') window.location.href = '/supplier'
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('user')
    delete api.defaults.headers.common['Authorization']
  }

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
