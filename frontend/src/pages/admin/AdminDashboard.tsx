import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import type { ApiResponse, Ticket } from '../../types'

const STATUS_VARIANT: Record<string, 'warning' | 'success' | 'default'> = {
  OPEN: 'default',
  VOTING: 'warning',
  NEGOTIATING: 'warning',
  RESOLVED: 'success',
}

const STATUS_LABEL: Record<string, string> = {
  OPEN: 'Abierto',
  VOTING: 'Votacion',
  NEGOTIATING: 'Negociando',
  RESOLVED: 'Resuelto',
}

export default function AdminDashboard() {
  const [activeTickets, setActiveTickets] = useState<Ticket[]>([])
  const [stats, setStats] = useState({
    pendingRegistrations: 0,
    openTickets: 0,
    votingTickets: 0,
    negotiatingTickets: 0,
  })

  useEffect(() => {
    api.get<ApiResponse<any[]>>('/admin/registrations').then(res => {
      setStats(prev => ({ ...prev, pendingRegistrations: res.data.data?.length || 0 }))
    }).catch(() => {})

    api.get<ApiResponse<Ticket[]>>('/admin/tickets').then(res => {
      if (res.data.success) {
        const data = res.data.data
        setActiveTickets(data.slice(0, 5))
        setStats(prev => ({
          ...prev,
          openTickets: data.filter(t => t.status === 'OPEN').length,
          votingTickets: data.filter(t => t.status === 'VOTING').length,
          negotiatingTickets: data.filter(t => t.status === 'NEGOTIATING').length,
        }))
      }
    }).catch(() => {})
  }, [])

  const totalActive = stats.openTickets + stats.votingTickets + stats.negotiatingTickets

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ink-primary">Panel de Administracion</h1>
          <p className="text-sm text-ink-tertiary mt-1">Resumen del sistema</p>
        </div>
        <Link to="/admin/tickets" className="text-sm font-medium text-accent hover:text-accent-hover">
          Ver todos los tickets
        </Link>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{stats.pendingRegistrations}</p>
            <p className="text-sm text-ink-tertiary mt-1">Solicitudes</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{stats.openTickets}</p>
            <p className="text-sm text-ink-tertiary mt-1">Abiertos</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-yellow-600 tabular-nums">{stats.votingTickets}</p>
            <p className="text-sm text-ink-tertiary mt-1">En votacion</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-purple-600 tabular-nums">{stats.negotiatingTickets}</p>
            <p className="text-sm text-ink-tertiary mt-1">Negociando</p>
          </div>
        </Card>
      </div>

      <div>
        <h2 className="text-sm font-medium text-ink-secondary mb-3">
          Tickets activos ({totalActive})
        </h2>
        <div className="space-y-2">
          {activeTickets.map(ticket => (
            <Link key={ticket.id} to={`/ticket/${ticket.id}`}>
              <Card className="hover:bg-surface-tertiary/50 transition-colors cursor-pointer">
                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-sm font-medium text-ink-primary">
                      Ticket #{ticket.id} - Pedido #{ticket.orderId}
                    </p>
                    <p className="text-xs text-ink-muted">
                      {ticket.storeName} vs {ticket.supplierName}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={STATUS_VARIANT[ticket.status] || 'default'}>
                      {STATUS_LABEL[ticket.status] || ticket.status}
                    </Badge>
                    <span className="text-xs text-ink-muted">
                      {new Date(ticket.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </Card>
            </Link>
          ))}
          {activeTickets.length === 0 && (
            <p className="text-sm text-ink-muted py-4">No hay tickets activos</p>
          )}
        </div>
      </div>
    </div>
  )
}
