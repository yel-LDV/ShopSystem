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

export default function StoreTicketsPage() {
  const [tickets, setTickets] = useState<Ticket[]>([])
  const [loading, setLoading] = useState(true)

  const fetchTickets = () => {
    setLoading(true)
    api.get<ApiResponse<Ticket[]>>('/store/tickets')
      .then(res => { if (res.data.success) setTickets(res.data.data) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchTickets() }, [])

  if (loading) return <Spinner className="py-12" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Mis Tickets</h1>
        <p className="text-sm text-ink-tertiary mt-1">{tickets.length} tickets</p>
      </div>

      <div className="space-y-2">
        {tickets.length === 0 ? (
          <Card><p className="text-sm text-ink-muted text-center py-4">No hay tickets</p></Card>
        ) : (
          tickets.map(ticket => (
            <Link key={ticket.id} to={`/ticket/${ticket.id}`}>
              <Card className="hover:bg-surface-tertiary/50 transition-colors cursor-pointer">
                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-sm font-medium text-ink-primary">
                      Ticket #{ticket.id}
                    </p>
                    <p className="text-xs text-ink-muted">
                      Proveedor: {ticket.supplierName}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {ticket.finalResolution && (
                      <Badge variant="success">
                        {RESOLUTION_LABELS[ticket.finalResolution] || ticket.finalResolution}
                      </Badge>
                    )}
                    <Badge variant={STATUS_VARIANT[ticket.status] || 'default'}>
                      {STATUS_LABEL[ticket.status] || ticket.status}
                    </Badge>
                  </div>
                </div>
              </Card>
            </Link>
          ))
        )}
      </div>
    </div>
  )
}
