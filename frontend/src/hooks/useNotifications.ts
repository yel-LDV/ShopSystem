import { useState, useEffect, useCallback } from 'react'
import api from '../services/api'
import type { Notification, ApiResponse } from '../types'

export function useNotifications() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)

  const fetchNotifications = useCallback(async () => {
    try {
      const res = await api.get<ApiResponse<Notification[]>>('/notifications')
      if (res.data.success) {
        setNotifications(res.data.data)
      }
      const countRes = await api.get<ApiResponse<number>>('/notifications/unread-count')
      if (countRes.data.success) {
        setUnreadCount(countRes.data.data)
      }
    } catch {
      // Silently fail
    }
  }, [])

  const markAsRead = async (id: number) => {
    try {
      await api.post(`/notifications/${id}/read`)
      setNotifications(prev =>
        prev.map(n => n.id === id ? { ...n, read: true } : n)
      )
      setUnreadCount(prev => Math.max(0, prev - 1))
    } catch {
      // Silently fail
    }
  }

  return { notifications, unreadCount, fetchNotifications, markAsRead }
}
