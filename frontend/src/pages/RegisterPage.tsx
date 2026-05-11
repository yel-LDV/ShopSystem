import { useState, FormEvent } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api from '../services/api'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import type { ApiResponse } from '../types'

export default function RegisterPage() {
  const { type } = useParams<{ type: string }>()
  const navigate = useNavigate()
  const isStore = type === 'store'
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [form, setForm] = useState({
    email: '',
    password: '',
    fullName: '',
    storeName: '',
    storeAddress: '',
    companyName: '',
    contactPhone: '',
    emergencyEmail: '',
    address: '',
  })

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post<ApiResponse<null>>('/register', {
        email: form.email,
        password: form.password,
        fullName: form.fullName,
        role: isStore ? 'ROLE_STORE' : 'ROLE_SUPPLIER',
        storeName: isStore ? form.storeName : undefined,
        storeAddress: isStore ? form.storeAddress : undefined,
        companyName: !isStore ? form.companyName : undefined,
        contactPhone: !isStore ? form.contactPhone : undefined,
        emergencyEmail: !isStore ? form.emergencyEmail : undefined,
        address: !isStore ? form.address : undefined,
      })
      setSuccess(true)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error al registrar')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-accent-subtle text-accent rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-ink-primary">Solicitud enviada</h2>
          <p className="text-sm text-ink-secondary mt-2 max-w-sm">
            Tu solicitud de registro ha sido enviada. No podras iniciar sesion hasta que un administrador apruebe tu cuenta.
          </p>
          <Link to="/login" className="inline-block mt-6 text-sm text-accent hover:text-accent-hover">
            Volver al inicio de sesion
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-primary px-4 py-8">
      <div className="w-full max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-xl font-semibold text-ink-primary">
            {isStore ? 'Registrar Tienda' : 'Registrar Proveedor'}
          </h1>
        </div>

        <form onSubmit={handleSubmit} className="bg-surface-secondary rounded-md border border-border-standard shadow-card p-6 space-y-4">
          <Input
            label="Nombre completo"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            required
          />
          <Input
            label="Correo electronico"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
          <Input
            label="Contrasena"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
            minLength={6}
          />

          {isStore ? (
            <>
              <Input
                label="Nombre de la tienda"
                value={form.storeName}
                onChange={(e) => setForm({ ...form, storeName: e.target.value })}
                required
              />
              <Input
                label="Direccion de la tienda"
                value={form.storeAddress}
                onChange={(e) => setForm({ ...form, storeAddress: e.target.value })}
              />
            </>
          ) : (
            <>
              <Input
                label="Nombre de la empresa"
                value={form.companyName}
                onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                required
              />
              <Input
                label="Telefono de contacto"
                value={form.contactPhone}
                onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
              />
              <Input
                label="Correo de emergencia"
                type="email"
                value={form.emergencyEmail}
                onChange={(e) => setForm({ ...form, emergencyEmail: e.target.value })}
              />
              <Input
                label="Direccion"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />
            </>
          )}

          {error && <p className="text-sm text-semantic-error">{error}</p>}

          <Button type="submit" loading={loading} className="w-full">
            Enviar solicitud
          </Button>
        </form>

        <div className="mt-4 text-center">
          <Link to="/login" className="text-sm text-ink-muted hover:text-ink-primary transition-colors">
            Ya tengo cuenta
          </Link>
        </div>
      </div>
    </div>
  )
}
