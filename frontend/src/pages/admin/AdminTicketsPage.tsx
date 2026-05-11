import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Spinner from '../../components/ui/Spinner'
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

const RESOLUTION_LABELS: Record<string, string> = {
  CANCEL: 'Cancelado',
  ACCEPT: 'Aceptado',
  DISCOUNT: 'Descuento',
}

export default function AdminTicketsPage() {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('active')

  const fetchTickets = (statusFilter: string) => {
    setLoading(true)
    const url = statusFilter === 'all'
      ? '/admin/tickets/history'
      : `/admin/tickets?status=${statusFilter === 'active' ? 'OPEN,VOTING,NEGOTIATING' : statusFilter}`

    api.get<ApiResponse<Ticket[]>>(url).then(res => {
      if (res.data.success) setTickets(res.data.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }

  useEffect(() => { fetchTickets('active') }, [])

  const handleFilter = (f: string) => {
    setFilter(f)
    fetchTickets(f)
  }

  if (loading) return <Spinner className="py-12" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Historial de Tickets</h1>
        <p className="text-sm text-ink-tertiary mt-1">{tickets.length} tickets</p>
      </div>

      <div className="flex gap-2">
        {(['active', 'OPEN', 'VOTING', 'NEGOTIATING', 'RESOLVED', 'all'] as const).map(f => (
          <button
            key={f}
            onClick={() => handleFilter(f)}
            className={`px-3 py-1 text-xs font-medium rounded-full transition-colors ${
              filter === f ? 'bg-accent text-white' : 'bg-surface-tertiary text-ink-secondary hover:text-ink-primary'
            }`}
          >
            {f === 'active' ? 'Activos' : f === 'all' ? 'Todos' : STATUS_LABEL[f] || f}
          </button>
        ))}
      </div>

      <div className="space-y-2">
        {tickets.map(ticket => (
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
                  {ticket.finalResolution && (
                    <p className="text-xs text-ink-muted mt-0.5">
                      Resultado: {RESOLUTION_LABELS[ticket.finalResolution] || ticket.finalResolution}
                      {ticket.discountPercentage != null && ticket.discountPercentage > 0
                        && ` (${ticket.discountPercentage}% desc)`}
                      {ticket.proposedPrice != null && ` - $${ticket.proposedPrice}`}
                    </p>
                  )}
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
        {tickets.length === 0 && (
          <p className="text-sm text-ink-muted py-4">No hay tickets</p>
        )}
      </div>
    </div>
  )
}
