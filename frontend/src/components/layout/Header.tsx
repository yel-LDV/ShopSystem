import { useAuth } from '../../contexts/AuthContext'
import { useTheme } from '../../contexts/ThemeContext'
import NotificationsBell from './NotificationsBell'

export default function Header() {
  const { user, logout } = useAuth()
  const { theme, toggle } = useTheme()

  return (
    <header className="h-14 bg-surface-primary border-b border-border-standard flex items-center justify-between px-6 flex-shrink-0">
      <div />
      <div className="flex items-center gap-3">
        <NotificationsBell />

        <button
          onClick={toggle}
          className="p-2 rounded-sm text-ink-secondary hover:text-ink-primary hover:bg-surface-tertiary transition-colors"
          title={theme === 'light' ? 'Modo oscuro' : 'Modo claro'}
        >
          {theme === 'light' ? (
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          )}
        </button>

        <div className="flex items-center gap-2 pl-3 border-l border-border-soft">
          <span className="text-sm font-medium text-ink-primary">{user?.fullName}</span>
          <button
            onClick={logout}
            className="text-xs text-ink-muted hover:text-ink-primary transition-colors px-2 py-1 rounded-sm hover:bg-surface-tertiary"
          >
            Salir
          </button>
        </div>
      </div>
    </header>
  )
}
