import { useEffect, useState } from 'react'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Table from '../../components/ui/Table'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import Toast from '../../components/ui/Toast'
import type { ApiResponse, AuditLogEntry } from '../../types'

export default function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchLogs = async (username?: string) => {
    setLoading(true)
    setError('')
    try {
      const url = username ? `/admin/audit/search?username=${username}` : '/admin/audit'
      const res = await api.get<ApiResponse<{ content: AuditLogEntry[] }>>(url)
      if (res.data.success) setLogs(res.data.data.content || res.data.data as any || [])
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error al cargar auditoria')
      setLogs([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchLogs() }, [])

  const columns = [
    { key: 'username', label: 'Usuario' },
    { key: 'action', label: 'Accion' },
    { key: 'entityType', label: 'Entidad' },
    { key: 'entityId', label: 'ID' },
    {
      key: 'timestamp',
      label: 'Fecha',
      render: (item: AuditLogEntry) => new Date(item.timestamp).toLocaleString(),
    },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Registro de Auditoria</h1>
        <p className="text-sm text-ink-tertiary mt-1">Actividad del sistema</p>
      </div>

      <Card>
        <div className="flex gap-2 mb-4">
          <Input
            placeholder="Buscar por usuario..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="max-w-xs"
          />
          <Button size="sm" variant="secondary" onClick={() => fetchLogs(search)}>
            Buscar
          </Button>
        </div>

        {error && (
          <p className="text-sm text-semantic-error mb-4">{error}</p>
        )}

        <Table
          columns={columns}
          data={logs}
          emptyMessage="No hay registros de auditoria"
        />
      </Card>
    </div>
  )
}
