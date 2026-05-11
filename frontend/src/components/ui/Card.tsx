import React from 'react'

interface CardProps {
  children: React.ReactNode
  className?: string
  statusColor?: 'green' | 'amber' | 'red' | 'none'
  padding?: 'sm' | 'md' | 'lg'
}

const statusColors = {
  green: 'border-l-accent',
  amber: 'border-l-semantic-warning',
  red: 'border-l-semantic-error',
  none: 'border-l-transparent',
}

const paddings = {
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
}

export default function Card({ children, className = '', statusColor = 'none', padding = 'md' }: CardProps) {
  return (
    <div
      className={`
        bg-surface-secondary rounded-md border border-border-standard
        border-l-4 ${statusColors[statusColor]} shadow-card
        ${paddings[padding]} ${className}
      `}
    >
      {children}
    </div>
  )
}
