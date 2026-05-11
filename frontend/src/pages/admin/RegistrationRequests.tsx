import { useEffect, useState } from 'react'
import api from '../../services/api'
import Button from '../../components/ui/Button'
import Card from '../../components/ui/Card'
import Badge, { statusBadgeVariant, statusLabel } from '../../components/ui/Badge'
import type { ApiResponse, RegistrationRequest } from '../../types'

export default function RegistrationRequests() {
  const [requests, setRequests] = useState<RegistrationRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState('')

  const fetchRequests = async () => {
    setLoading(true)
    try {
      const res = await api.get<ApiResponse<RegistrationRequest[]>>('/admin/registrations')
      if (res.data.success) setRequests(res.data.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchRequests() }, [])

  const handleAction = async (id: number, action: 'approve' | 'reject') => {
    setActionError('')
    try {
      if (action === 'approve') {
        await api.post(`/admin/registrations/${id}/approve`)
      } else {
        await api.post(`/admin/registrations/${id}/reject`)
      }
      await fetchRequests()
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Error al procesar la solicitud')
    }
  }

  if (loading) {
    return <div className="text-center py-12 text-ink-muted text-sm">Cargando...</div>
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Solicitudes de Registro</h1>
        <p className="text-sm text-ink-tertiary mt-1">Aprobar o rechazar nuevos usuarios</p>
      </div>

      <div className="space-y-3">
        {actionError && (
          <p className="text-sm text-semantic-error bg-red-50 border border-red-200 rounded-sm px-3 py-2">{actionError}</p>
        )}
        {requests.length === 0 ? (
          <Card>
            <p className="text-sm text-ink-muted text-center py-4">No hay solicitudes pendientes</p>
          </Card>
        ) : (
          requests.map(req => (
            <Card key={req.id}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-ink-primary">{req.fullName}</p>
                    <Badge variant={req.role === 'ROLE_STORE' ? 'info' : 'default'}>
                      {req.role === 'ROLE_STORE' ? 'Tienda' : 'Proveedor'}
                    </Badge>
                  </div>
                  <p className="text-sm text-ink-secondary mt-0.5">{req.email}</p>
                  <p className="text-xs text-ink-muted mt-0.5">
                    {req.storeName || req.companyName}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Button size="sm" onClick={() => handleAction(req.id, 'approve')}>
                    Aprobar
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => handleAction(req.id, 'reject')}>
                    Rechazar
                  </Button>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  )
}
