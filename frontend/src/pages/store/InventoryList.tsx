import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Table from '../../components/ui/Table'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Spinner from '../../components/ui/Spinner'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, InventoryItem, Product } from '../../types'

export default function InventoryList() {
  const [items, setItems] = useState<InventoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editMin, setEditMin] = useState('')
  const [editMax, setEditMax] = useState('')
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState('')

  const fetchInventory = () => {
    api.get<ApiResponse<InventoryItem[]>>('/store/inventory').then(res => {
      if (res.data.success) setItems(res.data.data)
    }).catch((err: any) => {
      setError(err.response?.data?.message || 'Error al cargar inventario')
    }).finally(() => setLoading(false))
  }

  useEffect(() => { fetchInventory() }, [])

  const startEdit = (item: InventoryItem) => {
    setEditingId(item.id)
    setEditMin(String(item.minStock || 0))
    setEditMax(String(item.maxStock || 0))
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditMin('')
    setEditMax('')
  }

  const saveThresholds = async (id: number) => {
    setSaving(true)
    try {
      await api.put(`/store/inventory/${id}/thresholds`, {
        minStock: parseInt(editMin) || 0,
        maxStock: parseInt(editMax) || 0,
      })
      setToast('Umbrales guardados')
      setEditingId(null)
      fetchInventory()
    } catch (err: any) {
      setToast(err.response?.data?.message || 'Error al guardar')
    } finally {
      setSaving(false)
      setTimeout(() => setToast(''), 3000)
    }
  }

  if (loading) return <Spinner className="py-12" />

  const columns = [
    {
      key: 'product',
      label: 'Producto',
      render: (item: InventoryItem) => item.supplierProduct?.name || '-',
    },
    { key: 'quantity', label: 'Stock' },
    {
      key: 'thresholds',
      label: 'Min / Max',
      render: (item: InventoryItem) => {
        if (editingId === item.id) {
          return (
            <div className="flex items-center gap-1">
              <Input
                type="number"
                className="w-16 text-xs"
                value={editMin}
                onChange={(e) => setEditMin(e.target.value)}
              />
              <span className="text-xs text-ink-muted">/</span>
              <Input
                type="number"
                className="w-16 text-xs"
                value={editMax}
                onChange={(e) => setEditMax(e.target.value)}
              />
            </div>
          )
        }
        return `${item.minStock || 0} / ${item.maxStock || 0}`
      },
    },
    {
      key: 'supplier',
      label: 'Proveedor',
      render: (item: InventoryItem) => item.supplierProduct?.supplierName || '-',
    },
    {
      key: 'status',
      label: 'Estado',
      render: (item: InventoryItem) => {
        const qty = item.quantity
        const min = item.minStock || 0
        if (min > 0 && qty <= min) {
          return <span className="text-semantic-error text-xs font-medium">Stock bajo</span>
        }
        if (min > 0 && qty <= min * 2) {
          return <span className="text-semantic-warning text-xs font-medium">Stock medio</span>
        }
        return <span className="text-accent text-xs font-medium">Stock normal</span>
      },
    },
    {
      key: 'lastUpdated',
      label: 'Actualizado',
      render: (item: InventoryItem) => new Date(item.lastUpdated).toLocaleDateString(),
    },
    {
      key: 'actions',
      label: '',
      render: (item: InventoryItem) => {
        if (editingId === item.id) {
          return (
            <div className="flex gap-1">
              <Button size="sm" onClick={() => saveThresholds(item.id)} loading={saving}>
                Guardar
              </Button>
              <Button size="sm" variant="secondary" onClick={cancelEdit}>
                Cancelar
              </Button>
            </div>
          )
        }
        return (
          <Button size="sm" variant="secondary" onClick={() => startEdit(item)}>
            Ajustar
          </Button>
        )
      },
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ink-primary">Inventario</h1>
          <p className="text-sm text-ink-tertiary mt-1">{items.length} productos</p>
        </div>
        <Link
          to="/store/new-order"
          className="text-sm font-medium text-accent hover:text-accent-hover transition-colors"
        >
          Nuevo pedido
        </Link>
      </div>

      <Card>
        {error && <p className="text-sm text-semantic-error mb-4">{error}</p>}
        <Table
          columns={columns}
          data={items}
          emptyMessage="No hay productos en inventario"
        />
      </Card>

      {toast && <Toast message={toast} onClose={() => setToast('')} />}
    </div>
  )
}
