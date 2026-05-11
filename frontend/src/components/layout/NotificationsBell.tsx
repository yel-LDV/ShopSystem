import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useNotifications } from '../../hooks/useNotifications'
import { subscribeToNotifications, disconnectWebSocket } from '../../services/socket'
import type { Client } from '@stomp/stompjs'

export default function NotificationsBell() {
  const { user } = useAuth()
  const { notifications, unreadCount, fetchNotifications, markAsRead } = useNotifications()
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    if (user) {
      fetchNotifications()
      clientRef.current = subscribeToNotifications(() => {
        fetchNotifications()
      })
    }
    return () => {
      if (clientRef.current) disconnectWebSocket(clientRef.current)
    }
  }, [user])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleNotificationClick = (notif: { id: number; referenceId: number | null; type: string }) => {
    markAsRead(notif.id)
    setIsOpen(false)
    if (notif.referenceId) {
      if (notif.type?.includes('TICKET')) {
        navigate(`/ticket/${notif.referenceId}`)
      } else if (notif.type?.includes('ORDER')) {
        const role = user?.role
        if (role === 'ROLE_STORE') navigate('/store/orders')
        else if (role === 'ROLE_SUPPLIER') navigate('/supplier/orders')
      }
    }
  }

  return (
    <div ref={dropdownRef} className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-sm text-ink-secondary hover:text-ink-primary hover:bg-surface-tertiary transition-colors"
      >
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 bg-semantic-error text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-surface-primary border border-border-standard rounded-md shadow-dropdown z-50">
          <div className="px-4 py-3 border-b border-border-soft">
            <h3 className="text-sm font-semibold text-ink-primary">Notificaciones</h3>
          </div>
          <div className="max-h-72 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="px-4 py-6 text-center text-sm text-ink-muted">
                Sin notificaciones
              </div>
            ) : (
              notifications.map((notif) => (
                <button
                  key={notif.id}
                  onClick={() => handleNotificationClick(notif)}
                  className={`
                    w-full text-left px-4 py-3 border-b border-border-soft last:border-0
                    transition-colors hover:bg-surface-tertiary/50
                    ${!notif.read ? 'bg-accent-subtle/30' : ''}
                  `}
                >
                  <p className="text-sm text-ink-primary leading-snug">{notif.message}</p>
                  <p className="text-xs text-ink-muted mt-1">
                    {new Date(notif.createdAt).toLocaleString()}
                  </p>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
