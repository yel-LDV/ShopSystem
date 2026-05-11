import { useEffect, useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import api from '../services/api'
import { useAuth } from '../contexts/AuthContext'
import { connectWebSocket, disconnectWebSocket } from '../services/socket'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Badge from '../components/ui/Badge'
import Spinner from '../components/ui/Spinner'
import type { ApiResponse, Ticket, Message } from '../types'
import type { Client } from '@stomp/stompjs'

const RESOLUTION_LABELS: Record<string, string> = {
  CANCEL: 'Cancelado',
  ACCEPT: 'Aceptado',
  DISCOUNT: 'Descuento',
}

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Abierto',
  VOTING: 'Votacion',
  NEGOTIATING: 'Negociando',
  RESOLVED: 'Resuelto',
}

function formatTimeLeft(seconds: number) {
  if (seconds <= 0) return 'Expirado'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

export default function TicketChatPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const [ticket, setTicket] = useState<Ticket | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [newMessage, setNewMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [voting, setVoting] = useState(false)
  const [timeLeft, setTimeLeft] = useState(0)
  const [proposedPrice, setProposedPrice] = useState('')
  const [negotiating, setNegotiating] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const clientRef = useRef<Client | null>(null)

  const ticketId = parseInt(id || '0')
  const role = user?.role || 'ROLE_STORE'
  const roleEndpoint = role === 'ROLE_ADMIN' ? 'admin' : role === 'ROLE_STORE' ? 'store' : 'supplier'
  const isAdmin = role === 'ROLE_ADMIN'
  const isResolved = ticket?.status === 'RESOLVED'
  const isNegotiating = ticket?.negotiationStatus === 'PROPOSED'
  const myVote = role === 'ROLE_STORE' ? ticket?.storeOwnerVote : ticket?.supplierVote
  const otherVote = role === 'ROLE_STORE' ? ticket?.supplierVote : ticket?.storeOwnerVote

  useEffect(() => {
    if (!ticketId) return

    const fetchTicket = () => {
      api.get<ApiResponse<Ticket>>(`/${roleEndpoint}/tickets/${ticketId}`).then(res => {
        if (res.data.success) {
          setTicket(res.data.data)
          setMessages(res.data.data.messages || [])
          if (res.data.data.votingEndDate) {
            setTimeLeft(Math.max(0, Math.floor(
              (new Date(res.data.data.votingEndDate).getTime() - Date.now()) / 1000
            )))
          }
        }
      }).catch(() => {
        if (isAdmin) {
          api.get<ApiResponse<Ticket[]>>('/admin/tickets').then(r => {
            if (r.data.success) {
              const found = r.data.data.find((t: Ticket) => t.id === ticketId)
              if (found) {
                setTicket(found)
                setMessages(found.messages || [])
              }
            }
          }).catch(() => {})
        }
      }).finally(() => setLoading(false))
    }

    fetchTicket()
    const interval = setInterval(fetchTicket, 10000)

    clientRef.current = connectWebSocket(ticketId, (msg) => {
      setMessages(prev => [...prev, msg])
    })

    return () => {
      if (clientRef.current) disconnectWebSocket(clientRef.current)
      clearInterval(interval)
    }
  }, [ticketId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (timeLeft <= 0) return
    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) { clearInterval(timer); return 0 }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [timeLeft])

  const handleSend = async () => {
    if (!newMessage.trim() || !user) return
    setSending(true)
    try {
      await api.post('/messages', {
        ticketId,
        senderId: user.userId,
        senderRole: user.role,
        content: newMessage.trim(),
      })
      setNewMessage('')
    } catch { /* ignore */ } finally {
      setSending(false)
    }
  }

  const handleVote = async (resolution: 'ACCEPT' | 'CANCEL') => {
    if (!ticket) return
    setVoting(true)
    try {
      const res = await api.post<ApiResponse<Ticket>>(
        `/${roleEndpoint}/tickets/${ticketId}/vote`,
        { resolution }
      )
      if (res.data.success && res.data.data) setTicket(res.data.data)
    } catch { /* ignore */ } finally { setVoting(false) }
  }

  const handleCancel = async () => {
    if (!ticket || !confirm('Cancelar este ticket?')) return
    try {
      const res = await api.post<ApiResponse<Ticket>>(`/${roleEndpoint}/tickets/${ticketId}/cancel`)
      if (res.data.success && res.data.data) setTicket(res.data.data)
    } catch { /* ignore */ }
  }

  const handleProposePrice = async () => {
    if (!ticket || !proposedPrice) return
    setNegotiating(true)
    try {
      const res = await api.post<ApiResponse<Ticket>>(
        `/${roleEndpoint}/tickets/${ticketId}/propose-price`,
        { price: parseFloat(proposedPrice) }
      )
      if (res.data.success && res.data.data) setTicket(res.data.data)
      setProposedPrice('')
    } catch { /* ignore */ } finally { setNegotiating(false) }
  }

  const handleNegotiationResponse = async (accept: boolean) => {
    if (!ticket) return
    setNegotiating(true)
    try {
      const res = await api.post<ApiResponse<Ticket>>(
        `/${roleEndpoint}/tickets/${ticketId}/negotiation-response`,
        { accept }
      )
      if (res.data.success && res.data.data) setTicket(res.data.data)
    } catch { /* ignore */ } finally { setNegotiating(false) }
  }

  if (loading) return <Spinner className="py-12" />
  if (!ticket) return <div className="text-center py-12 text-ink-muted">Ticket no encontrado</div>

  const canVote = !isResolved && role !== 'ROLE_ADMIN' && !myVote
  const canCancel = !isResolved
  const canProposePrice = !isResolved && !isNegotiating && ticket.negotiationStatus !== 'REJECTED'
  const canRespondNegotiation = isNegotiating && role !== 'ROLE_ADMIN'
    && ticket.priceProposedBy !== role

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ink-primary">
            Ticket #{ticket.id} - Pedido #{ticket.orderId}
          </h1>
          <p className="text-sm text-ink-tertiary mt-1">
            {ticket.storeName} vs {ticket.supplierName}
          </p>
        </div>
        <div className="flex items-center gap-3">
          {!isResolved && (timeLeft > 0) && (
            <span className={`text-sm font-medium tabular-nums px-3 py-1 rounded-full ${timeLeft < 60 ? 'bg-red-50 text-red-600' : 'bg-surface-tertiary text-ink-secondary'}`}>
              {formatTimeLeft(timeLeft)}
            </span>
          )}
          <Badge variant={
            ticket.status === 'RESOLVED' ? 'success' :
            ticket.status === 'VOTING' ? 'warning' : 'default'
          }>
            {STATUS_LABELS[ticket.status] || ticket.status}
          </Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card className="flex flex-col h-[60vh]">
            <div className="flex-1 overflow-y-auto space-y-3 mb-4">
              {messages.map(msg => (
                <div
                  key={msg.id}
                  className={`flex ${msg.senderRole === role ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[70%] rounded-md px-4 py-2 text-sm ${
                      msg.senderRole === role
                        ? 'bg-accent text-white'
                        : 'bg-surface-tertiary text-ink-primary'
                    }`}
                  >
                    <p className="text-xs opacity-70 mb-0.5">
                      {msg.senderRole === 'ROLE_STORE' ? 'Tienda' :
                       msg.senderRole === 'ROLE_SUPPLIER' ? 'Proveedor' :
                       msg.senderRole === 'ROLE_ADMIN' ? 'Admin' : 'Sistema'}
                      {' - '}{new Date(msg.createdAt).toLocaleTimeString()}
                    </p>
                    <p>{msg.content}</p>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {!isResolved && (
              <div className="flex gap-2">
                <Input
                  placeholder="Escribe un mensaje..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                />
                <Button onClick={handleSend} loading={sending}>Enviar</Button>
              </div>
            )}
          </Card>
        </div>

        <div className="space-y-4">
          {/* Resolution status */}
          {isResolved && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-3">Resolucion</h3>
              <div className="text-center">
                <Badge variant="success" className="mb-2">
                  {RESOLUTION_LABELS[ticket.finalResolution || ''] || ticket.finalResolution}
                </Badge>
                {ticket.discountPercentage != null && ticket.discountPercentage > 0 && (
                  <p className="text-sm text-ink-secondary">Descuento: {ticket.discountPercentage}%</p>
                )}
                {ticket.proposedPrice != null && (
                  <p className="text-sm text-ink-secondary mt-1">
                    Precio negociado: ${ticket.proposedPrice}
                  </p>
                )}
              </div>
            </Card>
          )}

          {/* Voting panel */}
          {!isResolved && role !== 'ROLE_ADMIN' && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-4">Votacion</h3>
              <div className="space-y-2">
                <Button
                  variant="danger"
                  className="w-full"
                  onClick={() => handleVote('CANCEL')}
                  disabled={!canVote || voting}
                  loading={voting}
                >
                  Rechazar pedido
                </Button>
                <Button
                  className="w-full"
                  onClick={() => handleVote('ACCEPT')}
                  disabled={!canVote || voting}
                  loading={voting}
                >
                  Aceptar pedido
                </Button>
              </div>
              {myVote && (
                <p className="text-xs text-ink-muted mt-3 text-center">
                  Ya votaste: {RESOLUTION_LABELS[myVote] || myVote}
                </p>
              )}
            </Card>
          )}

          {/* Cancel button */}
          {canCancel && (
            <Card>
              <Button
                variant="secondary"
                className="w-full"
                onClick={handleCancel}
              >
                Cancelar ticket
              </Button>
            </Card>
          )}

          {/* Price negotiation */}
          {!isResolved && role !== 'ROLE_ADMIN' && !isNegotiating
            && canProposePrice && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-3">Negociar precio</h3>
              <div className="flex gap-2">
                <Input
                  type="number"
                  placeholder="$ Nuevo precio"
                  value={proposedPrice}
                  onChange={(e) => setProposedPrice(e.target.value)}
                />
                <Button onClick={handleProposePrice} loading={negotiating} disabled={!proposedPrice}>
                  Proponer
                </Button>
              </div>
            </Card>
          )}

          {/* Respond to negotiation */}
          {canRespondNegotiation && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-3">Respuesta a negociacion</h3>
              <p className="text-sm text-ink-secondary mb-3">
                Precio propuesto: <strong>${ticket.proposedPrice}</strong> por{' '}
                {ticket.priceProposedBy === 'ROLE_STORE' ? 'la tienda' : 'el proveedor'}
              </p>
              <div className="space-y-2">
                <Button
                  className="w-full"
                  onClick={() => handleNegotiationResponse(true)}
                  loading={negotiating}
                >
                  Aceptar
                </Button>
                <Button
                  variant="danger"
                  className="w-full"
                  onClick={() => handleNegotiationResponse(false)}
                  loading={negotiating}
                >
                  Rechazar
                </Button>
              </div>
            </Card>
          )}

          {/* Negotiation status */}
          {!isResolved && isNegotiating && !canRespondNegotiation && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-3">Negociacion en curso</h3>
              <p className="text-sm text-ink-secondary">
                Precio propuesto: <strong>${ticket.proposedPrice}</strong>
              </p>
              <p className="text-xs text-ink-muted mt-1">
                Esperando respuesta de la otra parte...
              </p>
            </Card>
          )}

          {/* Admin panel */}
          {isAdmin && !isResolved && (
            <Card>
              <h3 className="font-medium text-ink-primary mb-4">Decision administrativa</h3>
              <div className="space-y-2">
                <Button
                  variant="danger"
                  className="w-full"
                  onClick={() => handleVote('CANCEL')}
                  loading={voting}
                >
                  Cancelar pedido
                </Button>
                <Button
                  className="w-full"
                  onClick={() => handleVote('ACCEPT')}
                  loading={voting}
                >
                  Aceptar pedido
                </Button>
              </div>
            </Card>
          )}

          {/* Votes status */}
          <Card>
            <h4 className="text-xs font-medium text-ink-muted mb-2">Estado de votos</h4>
            <div className="space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-ink-muted">Tienda</span>
                <span className="font-medium text-ink-primary">
                  {ticket.storeOwnerVote ? RESOLUTION_LABELS[ticket.storeOwnerVote] : 'Sin voto'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Proveedor</span>
                <span className="font-medium text-ink-primary">
                  {ticket.supplierVote ? RESOLUTION_LABELS[ticket.supplierVote] : 'Sin voto'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-ink-muted">Admin</span>
                <span className="font-medium text-ink-primary">
                  {ticket.adminVote ? RESOLUTION_LABELS[ticket.adminVote] : 'Sin voto'}
                </span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
