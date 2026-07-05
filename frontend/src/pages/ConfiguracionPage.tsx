import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTheme } from '../hooks/useTheme'
import { useAuth } from '../context/AuthContext'
import { cambiarPassword, cambiarUsername } from '../api/auth'
import { resetearDB } from '../api/ciclos'
import { listarCategorias, crearCategoria, editarCategoria, eliminarCategoria } from '../api/catalogos'
import { useToast } from '../components/Toast'
import type { Categoria } from '../types'

function SunIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
      <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
  )
}

async function extractError(e: unknown, on401?: string): Promise<string> {
  if (e && typeof e === 'object' && 'response' in e) {
    const ax = e as { response?: { data?: { error?: string }; status?: number } }
    if (ax.response?.data?.error) return ax.response.data.error
    if (ax.response?.status === 401) return on401 ?? 'La contraseña actual no es correcta'
  }
  if (e instanceof Error) return e.message
  return 'Error desconocido'
}

const FRASE_CONFIRMACION = 'BORRAR TODO'
const emptyCategoria = { nombre: '', icono: '' }

export default function ConfiguracionPage() {
  const { theme, toggle } = useTheme()
  const { username, logout, setSession } = useAuth()
  const { success, error } = useToast()
  const navigate = useNavigate()

  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [savingPassword, setSavingPassword] = useState(false)

  const [usernameForm, setUsernameForm] = useState({ newUsername: '', currentPassword: '' })
  const [savingUsername, setSavingUsername] = useState(false)

  const [confirmText, setConfirmText] = useState('')
  const [resetting, setResetting] = useState(false)

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loadingCategorias, setLoadingCategorias] = useState(true)
  const [nuevaCategoria, setNuevaCategoria] = useState(emptyCategoria)
  const [savingCategoria, setSavingCategoria] = useState(false)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(emptyCategoria)

  const cargarCategorias = () => listarCategorias().then(setCategorias)

  useEffect(() => {
    cargarCategorias().finally(() => setLoadingCategorias(false))
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const handleCambiarPassword = async () => {
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      error('La nueva contraseña y su confirmación no coinciden')
      return
    }
    setSavingPassword(true)
    try {
      await cambiarPassword(passwordForm.currentPassword, passwordForm.newPassword)
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      success('Contraseña actualizada')
    } catch (e) {
      error(await extractError(e))
    } finally {
      setSavingPassword(false)
    }
  }

  const handleCambiarUsername = async () => {
    setSavingUsername(true)
    try {
      const res = await cambiarUsername(usernameForm.newUsername, usernameForm.currentPassword)
      setSession(res.token, res.username)
      setUsernameForm({ newUsername: '', currentPassword: '' })
      success(`Nombre de usuario actualizado a "${res.username}"`)
    } catch (e) {
      error(await extractError(e))
    } finally {
      setSavingUsername(false)
    }
  }

  const handleBorrarDatos = async () => {
    setResetting(true)
    try {
      await resetearDB()
      setConfirmText('')
      success('Todos los datos fueron borrados')
    } catch {
      error('No se pudo borrar los datos (¿estás en un entorno de producción?)')
    } finally {
      setResetting(false)
    }
  }

  const handleCrearCategoria = async () => {
    setSavingCategoria(true)
    try {
      await crearCategoria({ nombre: nuevaCategoria.nombre, icono: nuevaCategoria.icono || undefined })
      setNuevaCategoria(emptyCategoria)
      await cargarCategorias()
      success('Categoría creada')
    } catch (e) {
      error(await extractError(e))
    } finally {
      setSavingCategoria(false)
    }
  }

  const abrirEditarCategoria = (c: Categoria) => {
    setEditandoId(c.id)
    setEditForm({ nombre: c.nombre, icono: c.icono ?? '' })
  }

  const handleGuardarCategoria = async (id: number) => {
    setSavingCategoria(true)
    try {
      await editarCategoria(id, { nombre: editForm.nombre, icono: editForm.icono || undefined })
      setEditandoId(null)
      await cargarCategorias()
      success('Categoría actualizada')
    } catch (e) {
      error(await extractError(e))
    } finally {
      setSavingCategoria(false)
    }
  }

  const handleEliminarCategoria = async (c: Categoria) => {
    if (!confirm(`¿Eliminar la categoría "${c.nombre}"?`)) return
    try {
      await eliminarCategoria(c.id)
      await cargarCategorias()
      success('Categoría eliminada')
    } catch (e) {
      error(await extractError(e))
    }
  }

  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Configuración</h1>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 480 }}>

        {/* Apariencia */}
        <div className="card">
          <span className="settings-panel-title" style={{ display: 'block', marginBottom: 12 }}>Apariencia</span>
          <label className="theme-switch">
            <MoonIcon />
            <div className={`theme-switch-track ${theme === 'light' ? 'on' : ''}`} onClick={toggle}>
              <div className="theme-switch-thumb" />
            </div>
            <SunIcon />
            <span style={{ marginLeft: 2 }}>{theme === 'dark' ? 'Oscuro' : 'Claro'}</span>
          </label>
        </div>

        {/* Cuenta */}
        <div className="card">
          <span className="settings-panel-title" style={{ display: 'block', marginBottom: 4 }}>Cuenta</span>
          <p className="text-muted" style={{ fontSize: 12, marginBottom: 14 }}>
            Conectado como <strong style={{ color: 'var(--text)' }}>{username}</strong>
          </p>

          <div className="form-group">
            <label className="form-label">Nombre de usuario nuevo</label>
            <input className="form-input" value={usernameForm.newUsername} placeholder={username ?? ''}
              onChange={e => setUsernameForm(f => ({ ...f, newUsername: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Contraseña actual</label>
            <input className="form-input" type="password" value={usernameForm.currentPassword}
              onChange={e => setUsernameForm(f => ({ ...f, currentPassword: e.target.value }))} />
          </div>
          <button className="btn btn-primary btn-sm" onClick={handleCambiarUsername}
            disabled={savingUsername || !usernameForm.newUsername || !usernameForm.currentPassword}>
            {savingUsername ? 'Guardando...' : 'Cambiar nombre de usuario'}
          </button>

          <hr className="settings-divider" style={{ margin: '16px 0' }} />

          <div className="form-group">
            <label className="form-label">Contraseña actual</label>
            <input className="form-input" type="password" value={passwordForm.currentPassword}
              onChange={e => setPasswordForm(f => ({ ...f, currentPassword: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Contraseña nueva</label>
            <input className="form-input" type="password" value={passwordForm.newPassword}
              onChange={e => setPasswordForm(f => ({ ...f, newPassword: e.target.value }))} />
          </div>
          <div className="form-group">
            <label className="form-label">Confirmar contraseña nueva</label>
            <input className="form-input" type="password" value={passwordForm.confirmPassword}
              onChange={e => setPasswordForm(f => ({ ...f, confirmPassword: e.target.value }))} />
          </div>
          <button className="btn btn-primary btn-sm" onClick={handleCambiarPassword}
            disabled={savingPassword || !passwordForm.currentPassword || passwordForm.newPassword.length < 6}>
            {savingPassword ? 'Guardando...' : 'Cambiar contraseña'}
          </button>

          <hr className="settings-divider" style={{ margin: '16px 0' }} />

          <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
            Cerrar sesión
          </button>
        </div>

        {/* Categorías */}
        <div className="card">
          <span className="settings-panel-title" style={{ display: 'block', marginBottom: 12 }}>
            Categorías de gastos
          </span>

          {loadingCategorias ? (
            <p className="text-muted" style={{ fontSize: 13 }}>Cargando...</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 }}>
              {categorias.map(c => (
                <div key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {editandoId === c.id ? (
                    <>
                      <input className="form-input" style={{ width: 50 }} value={editForm.icono}
                        placeholder="🏷️" onChange={e => setEditForm(f => ({ ...f, icono: e.target.value }))} />
                      <input className="form-input" style={{ flex: 1 }} value={editForm.nombre} autoFocus
                        onChange={e => setEditForm(f => ({ ...f, nombre: e.target.value }))} />
                      <button className="btn btn-ghost btn-xs" onClick={() => handleGuardarCategoria(c.id)}
                        disabled={savingCategoria || !editForm.nombre}>✓</button>
                      <button className="btn btn-ghost btn-xs" onClick={() => setEditandoId(null)}>✕</button>
                    </>
                  ) : (
                    <>
                      <span style={{ width: 50, textAlign: 'center' }}>{c.icono || '🏷️'}</span>
                      <span style={{ flex: 1, fontSize: 13 }}>{c.nombre}</span>
                      <button className="btn btn-ghost btn-xs" onClick={() => abrirEditarCategoria(c)}>✎</button>
                      <button className="btn btn-ghost btn-xs" onClick={() => handleEliminarCategoria(c)}>✕</button>
                    </>
                  )}
                </div>
              ))}
              {categorias.length === 0 && <p className="empty-state">Sin categorías todavía.</p>}
            </div>
          )}

          <div style={{ display: 'flex', gap: 8 }}>
            <input className="form-input" style={{ width: 50 }} value={nuevaCategoria.icono} placeholder="🏷️"
              onChange={e => setNuevaCategoria(f => ({ ...f, icono: e.target.value }))} />
            <input className="form-input" style={{ flex: 1 }} value={nuevaCategoria.nombre} placeholder="Nueva categoría"
              onChange={e => setNuevaCategoria(f => ({ ...f, nombre: e.target.value }))} />
            <button className="btn btn-ghost btn-sm" onClick={handleCrearCategoria}
              disabled={savingCategoria || !nuevaCategoria.nombre}>
              + Agregar
            </button>
          </div>
        </div>

        {/* Zona de peligro */}
        <div className="card" style={{ borderColor: 'var(--red)' }}>
          <span className="settings-panel-title" style={{ display: 'block', marginBottom: 4, color: 'var(--red)' }}>
            Zona de peligro
          </span>
          <p className="text-muted" style={{ fontSize: 12, lineHeight: 1.6, marginBottom: 14 }}>
            Esto borra <strong style={{ color: 'var(--text)' }}>todos</strong> los ciclos, gastos, tarjetas, cuotas y
            calculadoras. No se puede deshacer. Para confirmar, escribí <strong style={{ color: 'var(--text)' }}>{FRASE_CONFIRMACION}</strong> abajo.
          </p>
          <div className="form-group">
            <input className="form-input" value={confirmText} placeholder={FRASE_CONFIRMACION}
              onChange={e => setConfirmText(e.target.value)} />
          </div>
          <button className="btn btn-danger btn-sm" onClick={handleBorrarDatos}
            disabled={resetting || confirmText !== FRASE_CONFIRMACION}>
            {resetting ? 'Borrando...' : 'Borrar todos mis datos'}
          </button>
        </div>
      </div>
    </>
  )
}
