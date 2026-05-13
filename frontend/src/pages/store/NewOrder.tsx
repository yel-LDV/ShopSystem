import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Product, Batch } from '../../types'

interface CartItem {
  productId: number
  batchId: number
  quantity: number
  productName: string
  batchExpiration: string
  unitPrice: number
}

export default function NewOrder() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [products, setProducts] = useState<Product[]>([])
  const [cart, setCart] = useState<CartItem[]>([])
  const [loading, setLoading] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const [expandedProduct, setExpandedProduct] = useState<number | null>(null)
  const [batchesCache, setBatchesCache] = useState<Map<number, Batch[]>>(new Map())
  const [loadingBatches, setLoadingBatches] = useState(false)

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

  const toggleExpand = async (productId: number) => {
    if (expandedProduct === productId) {
      setExpandedProduct(null)
      return
    }
    setExpandedProduct(productId)
    if (!batchesCache.has(productId)) {
      setLoadingBatches(true)
      try {
        const res = await api.get<ApiResponse<Batch[]>>(`/store/products/${productId}/batches`)
        if (res.data.success) {
          setBatchesCache(prev => {
            const next = new Map(prev)
            next.set(productId, res.data.data)
            return next
          })
        }
      } catch {
        setToast({ message: 'Error al cargar lotes', type: 'error' })
      } finally {
        setLoadingBatches(false)
      }
    }
  }

  const addBatchToCart = (product: Product, batch: Batch) => {
    setCart(prev => {
      const existingIdx = prev.findIndex(
        i => i.productId === product.id && i.batchId === batch.id
      )
      if (existingIdx >= 0) {
        const next = [...prev]
        const newQty = next[existingIdx].quantity + 1
        if (newQty > batch.availableQuantity) return prev
        next[existingIdx] = { ...next[existingIdx], quantity: newQty }
        return next
      }
      return [...prev, {
        productId: product.id,
        batchId: batch.id,
        quantity: 1,
        productName: product.name,
        batchExpiration: batch.expirationDate,
        unitPrice: product.basePrice,
      }]
    })
  }

  const removeFromCart = (index: number) => {
    setCart(prev => {
      const next = [...prev]
      if (next[index].quantity <= 1) {
        return next.filter((_, i) => i !== index)
      }
      next[index] = { ...next[index], quantity: next[index].quantity - 1 }
      return next
    })
  }

  const handleCreateOrder = async () => {
    if (cart.length === 0) return
    setLoading(true)
    try {
      const items = cart.map(c => ({
        productId: c.productId,
        batchId: c.batchId,
        quantity: c.quantity,
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

  const cartTotal = cart.reduce((sum, c) => sum + c.unitPrice * c.quantity, 0)

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr)
    return d.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' })
  }

  const getCartQtyForProduct = (productId: number) =>
    cart.filter(c => c.productId === productId).reduce((s, c) => s + c.quantity, 0)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Nuevo Pedido</h1>
        <p className="text-sm text-ink-tertiary mt-1">Selecciona productos y elige el lote</p>
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
            products.map(product => {
              const isExpanded = expandedProduct === product.id
              const batches = batchesCache.get(product.id) || []
              const cartQty = getCartQtyForProduct(product.id)
              return (
                <Card key={product.id}>
                  <div
                    className="flex items-center justify-between cursor-pointer"
                    onClick={() => toggleExpand(product.id)}
                  >
                    <div>
                      <p className="text-sm font-medium text-ink-primary">{product.name}</p>
                      <p className="text-xs text-ink-muted">
                        {product.supplierName} - Stock total: {product.totalStock}
                        {product.unitAbbreviation ? ` ${product.unitAbbreviation}` : ''}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      {cartQty > 0 && (
                        <span className="text-xs bg-primary text-white rounded-full px-2 py-0.5 tabular-nums">
                          {cartQty} en carrito
                        </span>
                      )}
                      <span className="text-sm font-medium text-ink-primary tabular-nums">
                        ${product.basePrice?.toFixed(2)}
                      </span>
                      <span className="text-xs text-ink-muted">{isExpanded ? '▲' : '▼'}</span>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="mt-3 border-t border-border-standard pt-3">
                      <h4 className="text-xs font-medium text-ink-secondary mb-2">Lotes disponibles</h4>
                      {loadingBatches && !batchesCache.has(product.id) ? (
                        <p className="text-xs text-ink-muted py-2">Cargando lotes...</p>
                      ) : batches.length === 0 ? (
                        <p className="text-xs text-ink-muted py-2">Sin lotes disponibles</p>
                      ) : (
                        <div className="space-y-1">
                          {batches.map(batch => (
                            <div
                              key={batch.id}
                              className="flex items-center justify-between py-1.5 px-2 bg-surface-secondary rounded text-xs"
                            >
                              <div className="flex items-center gap-3">
                                <span className="text-ink-muted font-mono">Lote #{batch.id}</span>
                                <span className="text-ink-secondary">
                                  <span className="font-medium">{batch.availableQuantity}</span> und
                                </span>
                                <span className="text-ink-muted">
                                  Exp: {formatDate(batch.expirationDate)}
                                </span>
                              </div>
                              <div className="flex items-center gap-2">
                                {batch.purchasePrice && (
                                  <span className="text-ink-muted text-[10px]">
                                    Compra: ${batch.purchasePrice.toFixed(2)}
                                  </span>
                                )}
                                <Button
                                  size="sm"
                                  variant="secondary"
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    addBatchToCart(product, batch)
                                  }}
                                  disabled={batch.availableQuantity <= 0}
                                >
                                  +
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </Card>
              )
            })
          )}
        </div>

        <div>
          <Card className="sticky top-6">
            <h3 className="font-medium text-ink-primary mb-3">Resumen del pedido</h3>
            {cart.length === 0 ? (
              <p className="text-sm text-ink-muted">Expande un producto y agrega lotes al pedido</p>
            ) : (
              <div className="space-y-3">
                {cart.map((c, i) => (
                  <div key={`${c.productId}-${c.batchId}`} className="flex justify-between text-sm">
                    <div className="flex-1 min-w-0">
                      <span className="text-ink-secondary truncate block">
                        {c.productName} <span className="text-ink-muted text-xs">(Lote #{c.batchId})</span>
                      </span>
                      <span className="text-xs text-ink-muted">Exp: {formatDate(c.batchExpiration)}</span>
                    </div>
                    <div className="flex items-center gap-2 ml-2 shrink-0">
                      <button
                        className="text-xs text-ink-muted hover:text-error-primary"
                        onClick={() => removeFromCart(i)}
                      >
                        -
                      </button>
                      <span className="text-ink-primary tabular-nums w-6 text-center">{c.quantity}</span>
                      <span className="text-ink-primary tabular-nums">
                        ${(c.unitPrice * c.quantity).toFixed(2)}
                      </span>
                    </div>
                  </div>
                ))}
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
