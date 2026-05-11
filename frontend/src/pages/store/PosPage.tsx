import { useEffect, useState, useRef } from 'react'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Badge from '../../components/ui/Badge'
import Spinner from '../../components/ui/Spinner'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Product, Sale } from '../../types'

interface CartItem {
  productId: number
  productName: string
  productCode: string | null
  unitPrice: number
  unitAbbreviation: string
  stock: number
  quantity: number
}

export default function PosPage() {
  const [search, setSearch] = useState('')
  const [results, setResults] = useState<Product[]>([])
  const [showDropdown, setShowDropdown] = useState(false)
  const [cart, setCart] = useState<CartItem[]>([])
  const [sales, setSales] = useState<Sale[]>([])
  const [loading, setLoading] = useState(false)
  const [selling, setSelling] = useState(false)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const [historyLoading, setHistoryLoading] = useState(true)
  const searchRef = useRef<HTMLDivElement>(null)

  const fetchSales = () => {
    api.get<ApiResponse<Sale[]>>('/store/sales').then(res => {
      if (res.data.success) setSales(res.data.data)
    }).catch(() => {}).finally(() => setHistoryLoading(false))
  }

  useEffect(() => { fetchSales() }, [])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleSearch = (value: string) => {
    setSearch(value)
    if (value.length < 1) {
      setResults([])
      setShowDropdown(false)
      return
    }
    api.get<ApiResponse<Product[]>>(`/store/products?query=${encodeURIComponent(value)}`).then(res => {
      if (res.data.success) {
        setResults(res.data.data)
        setShowDropdown(true)
      }
    }).catch(() => {})
  }

  const addToCart = (product: Product) => {
    const existing = cart.find(c => c.productId === product.id)
    if (existing) {
      if (existing.quantity >= product.totalStock) return
      setCart(cart.map(c =>
        c.productId === product.id ? { ...c, quantity: c.quantity + 1 } : c
      ))
    } else {
      setCart([...cart, {
        productId: product.id,
        productName: product.name,
        productCode: product.code,
        unitPrice: product.basePrice,
        unitAbbreviation: product.unitAbbreviation || 'pza',
        stock: product.totalStock,
        quantity: 1,
      }])
    }
    setSearch('')
    setResults([])
    setShowDropdown(false)
  }

  const updateQuantity = (productId: number, qty: number) => {
    if (qty < 0) return
    const item = cart.find(c => c.productId === productId)
    if (!item) return
    if (qty > item.stock) return
    if (qty === 0) {
      setCart(cart.filter(c => c.productId !== productId))
      return
    }
    setCart(cart.map(c => c.productId === productId ? { ...c, quantity: qty } : c))
  }

  const total = cart.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)

  const handleSell = async () => {
    if (cart.length === 0) return
    setSelling(true)
    try {
      const res = await api.post<ApiResponse<Sale>>('/store/sales', {
        items: cart.map(c => ({ productId: c.productId, quantity: c.quantity })),
      })
      if (res.data.success) {
        setCart([])
        setToast({ message: `Venta #${res.data.data.id} registrada - $${total.toFixed(2)}`, type: 'success' })
        fetchSales()
      }
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al registrar venta', type: 'error' })
    } finally {
      setSelling(false)
      setTimeout(() => setToast(null), 4000)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Punto de Venta</h1>
        <p className="text-sm text-ink-tertiary mt-1">Vender productos del inventario</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-4">
          {/* Search */}
          <div ref={searchRef} className="relative">
            <Input
              placeholder="Buscar producto por nombre o codigo..."
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
              onFocus={() => results.length > 0 && setShowDropdown(true)}
            />
            {showDropdown && results.length > 0 && (
              <div className="absolute z-20 w-full mt-1 bg-surface-primary border border-border-standard rounded-md shadow-dropdown max-h-60 overflow-y-auto">
                {results.map(product => (
                  <button
                    key={product.id}
                    className="w-full text-left px-4 py-2.5 hover:bg-surface-tertiary transition-colors border-b border-border-soft last:border-b-0"
                    onClick={() => addToCart(product)}
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <span className="text-sm font-medium text-ink-primary">{product.name}</span>
                        {product.code && (
                          <span className="text-xs text-ink-muted ml-2">{product.code}</span>
                        )}
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-xs text-ink-muted">{product.supplierName}</span>
                        <span className="text-sm font-medium text-ink-primary tabular-nums">
                          ${product.basePrice?.toFixed(2)}
                        </span>
                        <Badge variant={product.totalStock > 0 ? 'success' : 'error'}>
                          {product.totalStock} {product.unitAbbreviation || 'pza'}
                        </Badge>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Cart */}
          <Card>
            <h3 className="font-medium text-ink-primary mb-3">
              Carrito ({cart.length} productos)
            </h3>
            {cart.length === 0 ? (
              <p className="text-sm text-ink-muted py-4 text-center">
                Busca y selecciona productos para agregar al carrito
              </p>
            ) : (
              <div className="space-y-2">
                {cart.map(item => (
                  <div key={item.productId} className="flex items-center justify-between py-2 border-b border-border-soft last:border-b-0">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-ink-primary truncate">
                        {item.productName}
                        {item.productCode && <span className="text-xs text-ink-muted ml-1">({item.productCode})</span>}
                      </p>
                      <p className="text-xs text-ink-muted">
                        ${item.unitPrice.toFixed(2)} / {item.unitAbbreviation}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 ml-2">
                      <button
                        className="w-8 h-8 rounded-sm border border-border-standard text-ink-secondary hover:bg-surface-tertiary flex items-center justify-center text-sm"
                        onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                      >-</button>
                      <input
                        type="number"
                        className="w-14 text-center text-sm border border-border-standard rounded-sm py-1 bg-surface-primary text-ink-primary"
                        value={item.quantity}
                        onChange={(e) => updateQuantity(item.productId, parseInt(e.target.value) || 0)}
                        min={1}
                        max={item.stock}
                      />
                      <button
                        className="w-8 h-8 rounded-sm border border-border-standard text-ink-secondary hover:bg-surface-tertiary flex items-center justify-center text-sm"
                        onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                      >+</button>
                      <span className="text-sm font-medium text-ink-primary tabular-nums w-20 text-right">
                        ${(item.unitPrice * item.quantity).toFixed(2)}
                      </span>
                    </div>
                  </div>
                ))}

                <div className="flex justify-between items-center pt-3 border-t border-border-standard">
                  <span className="text-sm font-medium text-ink-primary">Total</span>
                  <span className="text-lg font-semibold text-ink-primary tabular-nums">
                    ${total.toFixed(2)}
                  </span>
                </div>

                <Button
                  className="w-full"
                  onClick={handleSell}
                  loading={selling}
                  disabled={cart.length === 0}
                >
                  Vender
                </Button>
              </div>
            )}
          </Card>
        </div>

        {/* Sales History */}
        <div>
          <h2 className="text-sm font-medium text-ink-secondary mb-3">
            Historial de ventas ({sales.length})
          </h2>
          {historyLoading ? (
            <Spinner />
          ) : sales.length === 0 ? (
            <Card><p className="text-sm text-ink-muted text-center py-4">No hay ventas registradas</p></Card>
          ) : (
            <div className="space-y-2 max-h-[70vh] overflow-y-auto">
              {sales.map(sale => (
                <Card key={sale.id}>
                  <div className="flex justify-between items-center mb-2">
                    <div>
                      <p className="text-sm font-medium text-ink-primary">Venta #{sale.id}</p>
                      <p className="text-xs text-ink-muted">{new Date(sale.saleDate).toLocaleString()}</p>
                    </div>
                    <span className="text-sm font-semibold text-ink-primary tabular-nums">
                      ${sale.total?.toFixed(2)}
                    </span>
                  </div>
                  <div className="space-y-1">
                    {sale.items?.map(item => (
                      <div key={item.productId + '-' + item.quantity} className="flex justify-between text-xs">
                        <span className="text-ink-secondary">
                          {item.productName}
                          {item.productCode && <span className="text-ink-muted ml-1">({item.productCode})</span>}
                          <span className="text-ink-muted ml-1">x{item.quantity}</span>
                        </span>
                        <span className="text-ink-muted tabular-nums">${item.subtotal?.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  )
}
