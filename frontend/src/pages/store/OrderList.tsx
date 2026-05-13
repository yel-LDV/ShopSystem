import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge, { statusBadgeVariant, statusLabel } from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Modal from '../../components/ui/Modal'
import Input from '../../components/ui/Input'
import Spinner from '../../components/ui/Spinner'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Order } from '../../types'

export default function OrderList() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [receiveModal, setReceiveModal] = useState<{ orderId: number } | null>(null)
  const [discrepancyMsg, setDiscrepancyMsg] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)

  const fetchOrders = async () => {
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<Order[]>>('/store/orders')
      if (res.data.success) setOrders(res.data.data)
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al cargar pedidos', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchOrders() }, [])

  const handleReceive = async (orderId: number, withDiscrepancy: boolean) => {
    setActionLoading(true)
    try {
      await api.post(`/store/orders/${orderId}/receive`, {
        withDiscrepancy,
        discrepancyMessage: withDiscrepancy ? discrepancyMsg : '',
      })
      setReceiveModal(null)
      setDiscrepancyMsg('')
      setToast({ message: 'Pedido recibido', type: 'success' })
      await fetchOrders()
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al recibir pedido', type: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return <Spinner className="py-12" />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Mis Pedidos</h1>
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
                    Proveedor: {order.supplierName}
                  </p>
                  <p className="text-xs text-ink-muted">
                    {order.itemCount} productos - {new Date(order.createdAt).toLocaleString()}
                    {order.rejectionReason && ` - Motivo: ${order.rejectionReason}`}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-ink-primary tabular-nums">
                    ${order.total?.toFixed(2)}
                  </span>
                  {order.status === 'ACCEPTED_BY_SUPPLIER' && (
                    <>
                      <Button size="sm" onClick={() => handleReceive(order.id, false)} loading={actionLoading}>
                        Confirmar recepcion
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => setReceiveModal({ orderId: order.id })}>
                        Reportar problema
                      </Button>
                    </>
                  )}
                  {order.status === 'DISPUTED' && (
                    <Button size="sm" variant="secondary" onClick={() => navigate(`/ticket/${order.ticketId || '0'}`)}>
                      Ver ticket
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))
        )}
      </div>

      <Modal
        isOpen={!!receiveModal}
        onClose={() => { setReceiveModal(null); setDiscrepancyMsg('') }}
        title="Reportar discrepancia"
      >
        <div className="space-y-4">
          <p className="text-sm text-ink-secondary">
            Describe el problema con este pedido. Se abrira un ticket para resolverlo.
          </p>
          <Input
            label="Descripcion del problema"
            value={discrepancyMsg}
            onChange={(e) => setDiscrepancyMsg(e.target.value)}
            placeholder="Ej: Faltaron productos, productos danados..."
          />
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setReceiveModal(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={actionLoading}
              onClick={() => receiveModal && handleReceive(receiveModal.orderId, true)}
            >
              Abrir ticket
            </Button>
          </div>
        </div>
      </Modal>
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  )
}
