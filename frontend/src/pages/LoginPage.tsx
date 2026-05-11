import { useState, FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'

export default function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Credenciales invalidas')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-primary px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-semibold text-ink-primary tracking-tight">
            Sistema de Inventario
          </h1>
          <p className="text-sm text-ink-tertiary mt-1">
            Gestion de pedidos y proveedores
          </p>
        </div>

        <form onSubmit={handleSubmit} className="bg-surface-secondary rounded-md border border-border-standard shadow-card p-6 space-y-4">
          <Input
            label="Correo electronico"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
          <Input
            label="Contrasena"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error && (
            <p className="text-sm text-semantic-error">{error}</p>
          )}

          <Button type="submit" loading={loading} className="w-full">
            Iniciar sesion
          </Button>
        </form>

        <div className="mt-6 space-y-2 text-center">
          <Link to="/register/store" className="block text-sm text-accent hover:text-accent-hover transition-colors">
            Registrar mi tienda
          </Link>
          <Link to="/register/supplier" className="block text-sm text-accent hover:text-accent-hover transition-colors">
            Registrar como proveedor
          </Link>
        </div>
      </div>
    </div>
  )
}
