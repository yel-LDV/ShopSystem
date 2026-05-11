import { useEffect, useState } from 'react'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import Table from '../../components/ui/Table'
import Modal from '../../components/ui/Modal'
import Spinner from '../../components/ui/Spinner'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, Product } from '../../types'

export default function ProductList() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [batchModal, setBatchModal] = useState<{ productId: number } | null>(null)
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [batchSubmitting, setBatchSubmitting] = useState(false)

  const [form, setForm] = useState({ name: '', basePrice: '', minStock: '', maxStock: '' })
  const [batchForm, setBatchForm] = useState({
    quantity: '',
    year: new Date().getFullYear() + 1,
    month: '12',
    day: '31',
    purchasePrice: '',
  })

  const fetchProducts = async () => {
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<Product[]>>('/supplier/products')
      if (res.data.success) setProducts(res.data.data)
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al cargar productos', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchProducts() }, [])

  const handleCreate = async () => {
    if (submitting) return
    setSubmitting(true)
    try {
      await api.post('/supplier/products', {
        name: form.name,
        basePrice: parseFloat(form.basePrice),
        minStock: parseInt(form.minStock),
        maxStock: parseInt(form.maxStock),
      })
      setShowCreate(false)
      setForm({ name: '', basePrice: '', minStock: '', maxStock: '' })
      setToast({ message: 'Producto creado', type: 'success' })
      fetchProducts()
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al crear producto', type: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  const handleAddBatch = async () => {
    if (!batchModal || batchSubmitting) return
    setBatchSubmitting(true)
    try {
      await api.post(`/supplier/products/${batchModal.productId}/batches`, {
        quantity: parseInt(batchForm.quantity),
        expirationYear: batchForm.year,
        expirationMonth: parseInt(batchForm.month),
        expirationDay: parseInt(batchForm.day),
        purchasePrice: batchForm.purchasePrice ? parseFloat(batchForm.purchasePrice) : null,
      })
      setBatchModal(null)
      setBatchForm({ quantity: '', year: new Date().getFullYear() + 1, month: '12', day: '31', purchasePrice: '' })
      setToast({ message: 'Lote agregado', type: 'success' })
      fetchProducts()
    } catch (err: any) {
      setToast({ message: err.response?.data?.message || 'Error al agregar lote', type: 'error' })
    } finally {
      setBatchSubmitting(false)
    }
  }

  if (loading) return <Spinner className="py-12" />

  const columns = [
    { key: 'name', label: 'Nombre' },
    {
      key: 'basePrice',
      label: 'Precio',
      render: (item: Product) => `$${item.basePrice?.toFixed(2)}`,
    },
    { key: 'totalStock', label: 'Stock' },
    {
      key: 'minMax',
      label: 'Min / Max',
      render: (item: Product) => `${item.minStock} / ${item.maxStock}`,
    },
    {
      key: 'actions',
      label: 'Acciones',
      render: (item: Product) => (
        <div className="flex gap-1">
          <Button size="sm" variant="secondary" onClick={() => setBatchModal({ productId: item.id })}>
            Agregar lote
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ink-primary">Productos</h1>
          <p className="text-sm text-ink-tertiary mt-1">{products.length} productos</p>
        </div>
        <Button onClick={() => setShowCreate(true)}>Nuevo producto</Button>
      </div>

      <Card>
        <Table columns={columns} data={products} emptyMessage="No hay productos" />
      </Card>

      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Nuevo Producto">
        <div className="space-y-4">
          <Input label="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <Input label="Precio base" type="number" step="0.01" value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: e.target.value })} required />
          <div className="flex gap-2">
            <Input label="Stock minimo" type="number" value={form.minStock} onChange={(e) => setForm({ ...form, minStock: e.target.value })} />
            <Input label="Stock maximo" type="number" value={form.maxStock} onChange={(e) => setForm({ ...form, maxStock: e.target.value })} />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setShowCreate(false)}>Cancelar</Button>
            <Button onClick={handleCreate} loading={submitting} disabled={submitting}>Crear</Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={!!batchModal} onClose={() => setBatchModal(null)} title="Agregar Lote">
        <div className="space-y-4">
          <Input label="Cantidad" type="number" value={batchForm.quantity} onChange={(e) => setBatchForm({ ...batchForm, quantity: e.target.value })} required />
          <div className="flex gap-2">
            <Input label="Año" type="number" value={batchForm.year} onChange={(e) => setBatchForm({ ...batchForm, year: parseInt(e.target.value) })} />
            <Input label="Mes" type="number" value={batchForm.month} onChange={(e) => setBatchForm({ ...batchForm, month: e.target.value })} />
            <Input label="Dia" type="number" value={batchForm.day} onChange={(e) => setBatchForm({ ...batchForm, day: e.target.value })} />
          </div>
          <Input label="Precio de compra" type="number" step="0.01" value={batchForm.purchasePrice} onChange={(e) => setBatchForm({ ...batchForm, purchasePrice: e.target.value })} />
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setBatchModal(null)}>Cancelar</Button>
            <Button onClick={handleAddBatch} loading={batchSubmitting} disabled={batchSubmitting}>Agregar</Button>
          </div>
        </div>
      </Modal>

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  )
}
