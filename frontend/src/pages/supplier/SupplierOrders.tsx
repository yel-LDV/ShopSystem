import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge, { statusBadgeVariant, statusLabel } from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Modal from '../../components/ui/Modal'
import Spinner from '../../components/ui/Spinner'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Order } from '../../types'

export default function SupplierOrders() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [respondModal, setRespondModal] = useState<{ orderId: number; accept: boolean } | null>(null)
  const [reason, setReason] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)

  const fetchOrders = async () => {
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<Order[]>>('/supplier/orders')
      if (res.data.success) setOrders(res.data.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchOrders() }, [])

  const handleRespond = async () => {
    if (!respondModal) return
    setActionLoading(true)
    try {
      await api.post(`/supplier/orders/${respondModal.orderId}/respond`, {
        accept: respondModal.accept,
        reason: respondModal.accept ? undefined : reason,
      })
      setRespondModal(null)
      setReason('')
      setToast({
        message: respondModal.accept ? 'Pedido aceptado' : 'Pedido rechazado',
        type: 'success',
      })
      fetchOrders()
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al responder', type: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return <Spinner className="py-12" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Pedidos Recibidos</h1>
        <p className="text-sm text-ink-tertiary mt-1">{orders.length} pedidos</p>
      </div>

      <div className="space-y-3">
        {orders.length === 0 ? (
          <Card><p className="text-sm text-ink-muted text-center py-4">No hay pedidos</p></Card>
        ) : (
          orders.map(order => (
            <Card key={order.id}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-ink-primary">Pedido #{order.id}</p>
                    <Badge variant={statusBadgeVariant(order.status)}>
                      {statusLabel(order.status)}
                    </Badge>
                  </div>
                  <p className="text-sm text-ink-secondary mt-0.5">
                    Tienda: {order.storeName}
                  </p>
                  <p className="text-xs text-ink-muted">
                    {order.itemCount} productos - {new Date(order.createdAt).toLocaleString()}
                    {order.rejectionReason && ` - Motivo: ${order.rejectionReason}`}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium tabular-nums">
                    ${order.total?.toFixed(2)}
                  </span>
                  {order.status === 'PENDING' && (
                    <>
                      <Button size="sm" onClick={() => setRespondModal({ orderId: order.id, accept: true })}>
                        Aceptar
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => setRespondModal({ orderId: order.id, accept: false })}>
                        Rechazar
                      </Button>
                    </>
                  )}
                  {order.status === 'DISPUTED' && (
                    <Link to={`/ticket/${order.ticketId || '0'}`}>
                      <Button size="sm" variant="secondary">Ver ticket</Button>
                    </Link>
                  )}
                </div>
              </div>
            </Card>
          ))
        )}
      </div>

      <Modal
        isOpen={!!respondModal}
        onClose={() => { setRespondModal(null); setReason('') }}
        title={respondModal?.accept ? 'Aceptar pedido' : 'Rechazar pedido'}
      >
        <div className="space-y-4">
          <p className="text-sm text-ink-secondary">
            {respondModal?.accept
              ? 'Al aceptar, el inventario se reservara para este pedido.'
              : 'Por favor, indica el motivo del rechazo.'}
          </p>
          {!respondModal?.accept && (
            <Input
              label="Motivo"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Ej: Stock insuficiente, producto descontinuado..."
              required
            />
          )}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => { setRespondModal(null); setReason('') }}>
              Cancelar
            </Button>
            <Button
              variant={respondModal?.accept ? 'primary' : 'danger'}
              loading={actionLoading}
              onClick={handleRespond}
            >
              {respondModal?.accept ? 'Aceptar' : 'Rechazar'}
            </Button>
          </div>
        </div>
      </Modal>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  )
}
