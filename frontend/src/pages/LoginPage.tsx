import { useState } from 'react'
import { useLocation, useNavigate, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  if (isAuthenticated) {
    const from = (location.state as { from?: string } | null)?.from ?? '/'
    return <Navigate to={from} replace />
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch {
      setError('Usuario o contraseña incorrectos.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      height: '100vh', background: 'var(--bg)',
    }}>
      <form onSubmit={handleSubmit} className="card" style={{ width: 320, padding: 28 }}>
        <div style={{ textAlign: 'center', fontSize: 22, fontWeight: 700, marginBottom: 20 }}>
          💰 Gastos
        </div>
        <div className="form-group">
          <label className="form-label">Usuario</label>
          <input className="form-input" value={username} autoFocus
            onChange={e => setUsername(e.target.value)} />
        </div>
        <div className="form-group">
          <label className="form-label">Contraseña</label>
          <input className="form-input" type="password" value={password}
            onChange={e => setPassword(e.target.value)} />
        </div>
        {error && <p style={{ color: 'var(--red)', fontSize: 13, marginBottom: 12 }}>{error}</p>}
        <button className="btn btn-primary" type="submit" style={{ width: '100%' }}
          disabled={saving || !username || !password}>
          {saving ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </div>
  )
}
