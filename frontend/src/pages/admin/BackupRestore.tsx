import { useState, useRef } from 'react'
import api from '../../services/api'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import type { ApiResponse } from '../../types'

export default function BackupRestore() {
  const [restoring, setRestoring] = useState(false)
  const [message, setMessage] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleRestore = async () => {
    const file = fileInputRef.current?.files?.[0]
    if (!file) return

    setRestoring(true)
    setMessage('')
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await api.post<ApiResponse<null>>('/admin/backup/restore', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setMessage(res.data.message)
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error al restaurar backup')
    } finally {
      setRestoring(false)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-ink-primary">Backup y Restauracion</h1>
        <p className="text-sm text-ink-tertiary mt-1">Gestion de copias de seguridad</p>
      </div>

      <Card>
        <h2 className="text-sm font-medium text-ink-secondary mb-4">Restaurar backup</h2>
        <p className="text-sm text-ink-muted mb-4">
          Selecciona un archivo de backup encriptado (.enc) para restaurar el sistema.
          Los backups automaticos se generan diariamente a las 2:00 AM.
        </p>

        <div className="flex items-center gap-3">
          <input
            ref={fileInputRef}
            type="file"
            accept=".enc"
            className="text-sm text-ink-secondary file:mr-3 file:py-1.5 file:px-3 file:rounded-sm file:border-0 file:text-sm file:font-medium file:bg-surface-tertiary file:text-ink-primary hover:file:bg-surface-secondary"
          />
          <Button onClick={handleRestore} loading={restoring} size="sm">
            Restaurar
          </Button>
        </div>

        {message && (
          <p className={`mt-4 text-sm ${message.includes('Error') ? 'text-semantic-error' : 'text-accent'}`}>
            {message}
          </p>
        )}
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-ink-secondary mb-2">Informacion</h2>
        <ul className="text-sm text-ink-muted space-y-1 list-disc list-inside">
          <li>Los backups se generan automaticamente cada dia a las 2:00 AM</li>
          <li>Los archivos se encriptan con AES-256 antes de guardarse</li>
          <li>El nombre del archivo incluye la fecha: backup_YYYY-MM-DD.enc</li>
          <li>La restauracion sobrescribe los datos actuales</li>
        </ul>
      </Card>
    </div>
  )
}
