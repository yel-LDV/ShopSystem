import { useEffect, useState } from 'react'

interface ToastProps {
  message: string
  type?: 'success' | 'error' | 'info'
  onClose: () => void
  duration?: number
}

export default function Toast({ message, type = 'info', onClose, duration = 4000 }: ToastProps) {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false)
      setTimeout(onClose, 300)
    }, duration)
    return () => clearTimeout(timer)
  }, [duration, onClose])

  const colors = {
    success: 'border-accent text-accent',
    error: 'border-semantic-error text-semantic-error',
    info: 'border-accent text-ink-primary',
  }

  return (
    <div
      className={`
        fixed bottom-6 right-6 z-50 px-4 py-3 rounded-md border-l-4 bg-surface-secondary
        shadow-dropdown text-sm transition-all duration-300
        ${colors[type]}
        ${visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'}
      `}
    >
      {message}
    </div>
  )
}
