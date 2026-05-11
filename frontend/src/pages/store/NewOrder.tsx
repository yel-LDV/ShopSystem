import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Product } from '../../types'

export default function NewOrder() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [products, setProducts] = useState<Product[]>([])
  const [cart, setCart] = useState<Map<number, number>>(new Map())
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)

  const searchProducts = async () => {
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<Product[]>>('/store/products', {
        params: { query: query || undefined },
      })
      if (res.data.success) setProducts(res.data.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { searchProducts() }, [])

  const addToCart = (productId: number) => {
    setCart(prev => {
      const next = new Map(prev)
      next.set(productId, (next.get(productId) || 0) + 1)
      return next
    })
  }

  const removeFromCart = (productId: number) => {
    setCart(prev => {
      const next = new Map(prev)
      const count = next.get(productId) || 0
      if (count <= 1) next.delete(productId)
      else next.set(productId, count - 1)
      return next
    })
  }

  const handleCreateOrder = async () => {
    if (cart.size === 0) return
    setLoading(true)
    try {
      const items = Array.from(cart.entries()).map(([productId, quantity]) => ({
        productId,
        quantity,
      }))
      await api.post('/store/orders', items)
      setToast({ message: 'Pedido creado exitosamente', type: 'success' })
      setTimeout(() => navigate('/store/orders'), 1500)
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al crear el pedido', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const cartTotal = Array.from(cart.entries()).reduce((sum, [productId, qty]) => {
    const product = products.find(p => p.id === productId)
    return sum + (product?.basePrice || 0) * qty
  }, 0)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Nuevo Pedido</h1>
        <p className="text-sm text-ink-tertiary mt-1">Busca productos y agrega cantidades</p>
      </div>

      <div className="flex gap-2">
        <Input
          placeholder="Buscar producto..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && searchProducts()}
          className="max-w-md"
        />
        <Button variant="secondary" onClick={searchProducts}>
          Buscar
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-3">
          {loading ? (
            <p className="text-sm text-ink-muted py-4">Cargando...</p>
          ) : products.length === 0 ? (
            <Card><p className="text-sm text-ink-muted text-center py-4">No se encontraron productos</p></Card>
          ) : (
            products.map(product => (
              <Card key={product.id}>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-ink-primary">{product.name}</p>
                    <p className="text-xs text-ink-muted">
                      {product.supplierName} - Stock: {product.totalStock}
                      {product.unitAbbreviation ? ` ${product.unitAbbreviation}` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-medium text-ink-primary tabular-nums">
                      ${product.basePrice?.toFixed(2)}
                    </span>
                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => removeFromCart(product.id)}
                        disabled={!cart.has(product.id)}
                      >
                        -
                      </Button>
                      <span className="w-8 text-center text-sm tabular-nums">
                        {cart.get(product.id) || 0}
                      </span>
                      <Button size="sm" variant="secondary" onClick={() => addToCart(product.id)}>
                        +
                      </Button>
                    </div>
                  </div>
                </div>
              </Card>
            ))
          )}
        </div>

        <div>
          <Card className="sticky top-6">
            <h3 className="font-medium text-ink-primary mb-3">Resumen del pedido</h3>
            {cart.size === 0 ? (
              <p className="text-sm text-ink-muted">Agrega productos al pedido</p>
            ) : (
              <div className="space-y-3">
                {Array.from(cart.entries()).map(([productId, qty]) => {
                  const product = products.find(p => p.id === productId)
                  return (
                    <div key={productId} className="flex justify-between text-sm">
                      <span className="text-ink-secondary">{product?.name} x{qty}</span>
                      <span className="text-ink-primary tabular-nums">
                        ${((product?.basePrice || 0) * qty).toFixed(2)}
                      </span>
                    </div>
                  )
                })}
                <div className="border-t border-border-standard pt-2 flex justify-between text-sm font-medium">
                  <span className="text-ink-primary">Total</span>
                  <span className="text-ink-primary tabular-nums">${cartTotal.toFixed(2)}</span>
                </div>
                <Button
                  className="w-full"
                  onClick={handleCreateOrder}
                  loading={loading}
                >
                  Crear pedido
                </Button>
              </div>
            )}
          </Card>
        </div>
      </div>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  )
}
