import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge, { statusBadgeVariant, statusLabel } from '../../components/ui/Badge'
import type { ApiResponse, Product, Order } from '../../types'

export default function SupplierDashboard() {
  const [products, setProducts] = useState<Product[]>([])
  const [pendingOrders, setPendingOrders] = useState<Order[]>([])

  useEffect(() => {
    api.get<ApiResponse<Product[]>>('/supplier/products').then(res => {
      if (res.data.success) setProducts(res.data.data)
    }).catch(() => {})
    api.get<ApiResponse<Order[]>>('/supplier/orders').then(res => {
      if (res.data.success) {
        setPendingOrders(res.data.data.filter(o => o.status === 'PENDING').slice(0, 5))
      }
    }).catch(() => {})
  }, [])

  const lowStockProducts = products.filter(p => p.totalStock <= p.minStock)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Dashboard de Proveedor</h1>
        <p className="text-sm text-ink-tertiary mt-1">Resumen de productos y pedidos</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{products.length}</p>
            <p className="text-sm text-ink-tertiary mt-1">Productos</p>
          </div>
        </Card>
        <Card statusColor={lowStockProducts.length > 0 ? 'amber' : 'green'}>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{lowStockProducts.length}</p>
            <p className="text-sm text-ink-tertiary mt-1">Stock bajo</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{pendingOrders.length}</p>
            <p className="text-sm text-ink-tertiary mt-1">Pedidos pendientes</p>
          </div>
        </Card>
      </div>

      <div>
        <h2 className="text-sm font-medium text-ink-secondary mb-3">Pedidos pendientes</h2>
        <div className="space-y-2">
          {pendingOrders.map(order => (
            <Card key={order.id}>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-ink-primary">
                    Pedido #{order.id} - {order.storeName}
                  </p>
                  <p className="text-xs text-ink-muted">
                    {order.itemCount} productos - {new Date(order.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium tabular-nums">
                    ${order.total?.toFixed(2)}
                  </span>
                  <Badge variant="warning">Pendiente</Badge>
                  <Link to="/supplier/orders">
                    <span className="text-xs text-accent hover:text-accent-hover font-medium">
                      Responder
                    </span>
                  </Link>
                </div>
              </div>
            </Card>
          ))}
          {pendingOrders.length === 0 && (
            <p className="text-sm text-ink-muted py-4">No hay pedidos pendientes</p>
          )}
        </div>
      </div>
    </div>
  )
}
