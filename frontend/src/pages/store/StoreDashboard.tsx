import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Badge, { statusBadgeVariant, statusLabel } from '../../components/ui/Badge'
import type { ApiResponse, InventoryItem, Order, Sale } from '../../types'

export default function StoreDashboard() {
  const [lowStock, setLowStock] = useState<InventoryItem[]>([])
  const [recentOrders, setRecentOrders] = useState<Order[]>([])
  const [inventoryCount, setInventoryCount] = useState(0)
  const [todaySales, setTodaySales] = useState(0)

  useEffect(() => {
    api.get<ApiResponse<InventoryItem[]>>('/store/inventory/low-stock').then(res => {
      if (res.data.success) setLowStock(res.data.data)
    }).catch(() => {})
    api.get<ApiResponse<Order[]>>('/store/orders').then(res => {
      if (res.data.success) {
        setRecentOrders(res.data.data.slice(0, 5))
      }
    }).catch(() => {})
    api.get<ApiResponse<InventoryItem[]>>('/store/inventory').then(res => {
      if (res.data.success) setInventoryCount(res.data.data.length)
    }).catch(() => {})
    api.get<ApiResponse<Sale[]>>('/store/sales').then(res => {
      if (res.data.success) {
        const today = new Date().toDateString()
        setTodaySales(res.data.data.filter(s => new Date(s.saleDate).toDateString() === today).length)
      }
    }).catch(() => {})
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Dashboard de Tienda</h1>
        <p className="text-sm text-ink-tertiary mt-1">Resumen de inventario y pedidos</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{inventoryCount}</p>
            <p className="text-sm text-ink-tertiary mt-1">Productos en inventario</p>
          </div>
        </Card>
        <Card statusColor={lowStock.length > 0 ? 'amber' : 'green'}>
          <div className="text-center">
            <p className="text-3xl font-semibold text-ink-primary tabular-nums">{lowStock.length}</p>
            <p className="text-sm text-ink-tertiary mt-1">Productos con stock bajo</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-semibold text-accent tabular-nums">{todaySales}</p>
            <p className="text-sm text-ink-tertiary mt-1">Ventas de hoy</p>
          </div>
        </Card>
      </div>

      {lowStock.length > 0 && (
        <div>
          <h2 className="text-sm font-medium text-ink-secondary mb-3">Stock bajo - Reordenar</h2>
          <div className="space-y-2">
            {lowStock.map(item => {
              const product = item.supplierProduct
              const stockPercent = product.maxStock > 0
                ? (item.quantity / product.maxStock) * 100
                : 0
              return (
                <Card key={item.id}>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-ink-primary">{product.name}</p>
                      <p className="text-xs text-ink-muted">
                        Stock: {item.quantity} / Min: {product.minStock} / Max: {product.maxStock}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="w-24 h-2 bg-surface-tertiary rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all ${
                            stockPercent <= 25 ? 'bg-semantic-error' :
                            stockPercent <= 50 ? 'bg-semantic-warning' : 'bg-accent'
                          }`}
                          style={{ width: `${Math.min(stockPercent, 100)}%` }}
                        />
                      </div>
                      <Link to="/store/new-order">
                        <span className="text-xs text-accent hover:text-accent-hover font-medium">
                          Reordenar
                        </span>
                      </Link>
                    </div>
                  </div>
                </Card>
              )
            })}
          </div>
        </div>
      )}

      <div>
        <h2 className="text-sm font-medium text-ink-secondary mb-3">Pedidos recientes</h2>
        <div className="space-y-2">
          {recentOrders.map(order => (
            <Card key={order.id}>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-ink-primary">
                    Pedido #{order.id} - {order.supplierName}
                  </p>
                  <p className="text-xs text-ink-muted">
                    {order.itemCount} productos - {new Date(order.createdAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-ink-primary tabular-nums">
                    ${order.total?.toFixed(2)}
                  </span>
                  <Badge variant={statusBadgeVariant(order.status)}>
                    {statusLabel(order.status)}
                  </Badge>
                </div>
              </div>
            </Card>
          ))}
          {recentOrders.length === 0 && (
            <p className="text-sm text-ink-muted py-4">No hay pedidos</p>
          )}
        </div>
      </div>
    </div>
  )
}
