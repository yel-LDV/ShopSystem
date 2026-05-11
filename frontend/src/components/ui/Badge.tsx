interface BadgeProps {
  children: React.ReactNode
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info'
  className?: string
}

const variants = {
  default: 'bg-surface-tertiary text-ink-secondary',
  success: 'bg-accent-subtle text-semantic-success',
  warning: 'bg-accent-subtle/60 text-semantic-warning',
  error: 'bg-accent-subtle/40 text-semantic-error',
  info: 'bg-accent-subtle/30 text-ink-secondary',
}

export default function Badge({ children, variant = 'default', className = '' }: BadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center px-2 py-0.5 rounded text-xs font-medium
        ${variants[variant]} ${className}
      `}
    >
      {children}
    </span>
  )
}

export function statusBadgeVariant(status: string): BadgeProps['variant'] {
  const s = status?.toUpperCase()
  if (s === 'PENDING') return 'warning'
  if (s === 'ACCEPTED_BY_SUPPLIER') return 'info'
  if (s === 'RECEIVED') return 'success'
  if (s === 'REJECTED_BY_SUPPLIER' || s === 'DISPUTED') return 'error'
  if (s === 'RESOLVED') return 'success'
  if (s === 'VOTING') return 'warning'
  return 'default'
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING: 'Pendiente',
    ACCEPTED_BY_SUPPLIER: 'Aceptado',
    REJECTED_BY_SUPPLIER: 'Rechazado',
    RECEIVED: 'Recibido',
    DISPUTED: 'Disputado',
    OPEN: 'Abierto',
    VOTING: 'Votacion',
    RESOLVED: 'Resuelto',
  }
  return labels[status] || status
}
