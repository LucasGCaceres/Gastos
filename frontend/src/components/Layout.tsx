import { useRef, useState, useEffect } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useTheme } from '../hooks/useTheme'
import { resetearDB } from '../api/ciclos'
import { useToast } from './Toast'
import { useAuth } from '../context/AuthContext'

function GearIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="3"/>
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
    </svg>
  )
}

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

export default function Layout() {
  const { theme, toggle } = useTheme()
  const { success, error } = useToast()
  const { username, logout } = useAuth()
  const navigate = useNavigate()
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [resetting, setResetting] = useState(false)
  const [showResetConfirm, setShowResetConfirm] = useState(false)
  const panelRef = useRef<HTMLDivElement>(null)

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  // Cierra el panel si se clickea afuera
  useEffect(() => {
    if (!settingsOpen) return
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setSettingsOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [settingsOpen])

  const handleReset = async () => {
    setResetting(true)
    try {
      await resetearDB()
      setShowResetConfirm(false)
      setSettingsOpen(false)
      success('Base de datos reseteada')
    } catch {
      error('Error al resetear la base de datos')
    } finally {
      setResetting(false)
    }
  }

  return (
    <div className="app-layout">
      <nav className="sidebar" style={{ position: 'relative' }}>
        <div className="sidebar-title">💰 Gastos</div>

        <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>Ciclos</NavLink>
        <NavLink to="/tarjetas" className={({ isActive }) => isActive ? 'active' : ''}>Tarjetas</NavLink>
        <NavLink to="/calculadoras" className={({ isActive }) => isActive ? 'active' : ''}>Calculadoras</NavLink>

        {/* Gear button anclado al fondo */}
        <div style={{ flex: 1 }} />
        <button
          className={`sidebar-settings-btn ${settingsOpen ? 'active' : ''}`}
          onClick={() => setSettingsOpen(o => !o)}
        >
          <GearIcon />
          Configuración
        </button>

        {/* Panel de configuración */}
        {settingsOpen && (
          <div className="settings-panel" ref={panelRef}>
            <span className="settings-panel-title">Apariencia</span>

            {/* Toggle tema */}
            <label className="theme-switch">
              <MoonIcon />
              <div
                className={`theme-switch-track ${theme === 'light' ? 'on' : ''}`}
                onClick={toggle}
              >
                <div className="theme-switch-thumb" />
              </div>
              <SunIcon />
              <span style={{ marginLeft: 2 }}>{theme === 'dark' ? 'Oscuro' : 'Claro'}</span>
            </label>

            <hr className="settings-divider" />

            <span className="settings-panel-title">Sesión</span>
            <p className="text-muted" style={{ fontSize: 11, marginBottom: 8 }}>
              Conectado como <strong style={{ color: 'var(--text)' }}>{username}</strong>
            </p>
            <button className="btn btn-ghost btn-sm" style={{ width: '100%' }} onClick={handleLogout}>
              Cerrar sesión
            </button>

            <hr className="settings-divider" />

            <span className="settings-panel-title">Base de datos</span>

            {!showResetConfirm ? (
              <button
                className="btn btn-ghost btn-sm"
                style={{ color: 'var(--red)', borderColor: 'var(--red)', width: '100%' }}
                onClick={() => setShowResetConfirm(true)}
              >
                Reset DB
              </button>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <p style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.5 }}>
                  ⚠️ Borra <strong style={{ color: 'var(--text)' }}>todos los datos</strong>. No se puede deshacer.
                </p>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button className="btn btn-ghost btn-xs" style={{ flex: 1 }}
                    onClick={() => setShowResetConfirm(false)}>
                    Cancelar
                  </button>
                  <button className="btn btn-danger btn-xs" style={{ flex: 1 }}
                    onClick={handleReset} disabled={resetting}>
                    {resetting ? '...' : 'Borrar'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </nav>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
