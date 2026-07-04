import { useEffect, useState } from 'react'
import {
  listarTodasTarjetas, crearTarjeta, editarTarjeta, actualizarEstadoTarjeta,
  getCierreMes, setCierreMes, getDesgloseDeudas,
  type CierreMes,
} from '../api/tarjetas'
import type { Tarjeta } from '../types'

const MESES_LARGO = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

const emptyForm = { nombre: '', banco: '', diaCierreEstimado: 15, diaVencimientoEstimado: 5 }

function fmt(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n)
}

function mesActual() {
  const hoy = new Date()
  return { anio: hoy.getFullYear(), mes: hoy.getMonth() + 1 }
}

export default function TarjetasPage() {
  const [tarjetas, setTarjetas] = useState<Tarjeta[]>([])
  const [mostrarInactivas, setMostrarInactivas] = useState(false)
  const [loading, setLoading] = useState(true)
  const [showNueva, setShowNueva] = useState(false)
  const [formNueva, setFormNueva] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  // Fechas reales del mes actual, keyed por tarjetaId
  const [cierresActuales, setCierresActuales] = useState<Record<number, CierreMes>>({})
  const [deudas, setDeudas] = useState<Record<number, number>>({})

  // Modal de edición de datos base de la tarjeta
  const [editModal, setEditModal] = useState<Tarjeta | null>(null)
  const [formEdit, setFormEdit] = useState(emptyForm)

  // Modal de cierre por mes
  const [cierreModal, setCierreModal] = useState<{ tarjeta: Tarjeta } | null>(null)
  const [cierreForm, setCierreForm] = useState({
    anio: mesActual().anio,
    mes: mesActual().mes,
    fechaCierreReal: '',
    fechaVencimientoReal: '',
  })
  const [cargandoCierre, setCargandoCierre] = useState(false)

  const { anio: anioActual, mes: mesActualVal } = mesActual()

  const cargarCierresActuales = async (lista: Tarjeta[]) => {
    const entries = await Promise.all(
      lista.map(t => getCierreMes(t.id, anioActual, mesActualVal).then(c => [t.id, c] as const))
    )
    const map: Record<number, CierreMes> = {}
    for (const [id, cierre] of entries) {
      if (cierre) map[id] = cierre
    }
    setCierresActuales(map)
  }

  const cargarDeudas = async () => {
    const lista = await getDesgloseDeudas(anioActual, mesActualVal)
    const map: Record<number, number> = {}
    for (const d of lista) map[d.tarjetaId] = d.totalAdeudado
    setDeudas(map)
  }

  const reload = async () => {
    const lista = await listarTodasTarjetas()
    setTarjetas(lista)
    await Promise.all([cargarCierresActuales(lista), cargarDeudas()])
  }

  useEffect(() => {
    reload().finally(() => setLoading(false))
  }, [])

  const handleCrear = async () => {
    setSaving(true)
    try {
      await crearTarjeta(formNueva)
      setFormNueva(emptyForm)
      setShowNueva(false)
      await reload()
    } finally { setSaving(false) }
  }

  const abrirEditar = (t: Tarjeta) => {
    setFormEdit({ nombre: t.nombre, banco: t.banco ?? '', diaCierreEstimado: t.diaCierreEstimado, diaVencimientoEstimado: t.diaVencimientoEstimado })
    setEditModal(t)
  }

  const handleGuardarEditar = async () => {
    if (!editModal) return
    setSaving(true)
    try {
      await editarTarjeta(editModal.id, formEdit)
      setEditModal(null)
      await reload()
    } finally { setSaving(false) }
  }

  const handleToggleActiva = async (t: Tarjeta) => {
    if (t.activa && !confirm(`¿Desactivar la tarjeta "${t.nombre}"? Se ocultará de la carga activa pero se conserva su historial.`)) return
    await actualizarEstadoTarjeta(t.id, !t.activa)
    await reload()
  }

  const tarjetasVisibles = mostrarInactivas ? tarjetas : tarjetas.filter(t => t.activa)

  const abrirCierre = async (tarjeta: Tarjeta) => {
    const { anio, mes } = mesActual()
    setCierreModal({ tarjeta })
    setCierreForm({ anio, mes, fechaCierreReal: '', fechaVencimientoReal: '' })
    setCargandoCierre(true)
    const existente = await getCierreMes(tarjeta.id, anio, mes)
    if (existente) {
      setCierreForm({ anio, mes, fechaCierreReal: existente.fechaCierreReal, fechaVencimientoReal: existente.fechaVencimientoReal })
    }
    setCargandoCierre(false)
  }

  const handleCambiarMesCierre = async (anio: number, mes: number, tarjetaId: number) => {
    setCierreForm(f => ({ ...f, anio, mes, fechaCierreReal: '', fechaVencimientoReal: '' }))
    setCargandoCierre(true)
    const existente = await getCierreMes(tarjetaId, anio, mes)
    if (existente) {
      setCierreForm(f => ({ ...f, fechaCierreReal: existente.fechaCierreReal, fechaVencimientoReal: existente.fechaVencimientoReal }))
    }
    setCargandoCierre(false)
  }

  const handleGuardarCierre = async () => {
    if (!cierreModal) return
    setSaving(true)
    try {
      const saved = await setCierreMes(cierreModal.tarjeta.id, cierreForm)
      // Actualizar el mapa de cierres actuales si es el mes actual
      if (cierreForm.anio === anioActual && cierreForm.mes === mesActualVal) {
        setCierresActuales(prev => ({ ...prev, [cierreModal.tarjeta.id]: saved }))
      }
      setCierreModal(null)
    } finally { setSaving(false) }
  }

  // Generar opciones de mes: mes actual + 5 meses siguientes
  const opcionesMes = Array.from({ length: 6 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() + i)
    return { anio: d.getFullYear(), mes: d.getMonth() + 1 }
  })

  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Tarjetas de crédito</h1>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <label style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 13, color: 'var(--text-muted)', cursor: 'pointer' }}>
            <input type="checkbox" checked={mostrarInactivas} onChange={e => setMostrarInactivas(e.target.checked)} />
            Mostrar inactivas
          </label>
          <button className="btn btn-primary" onClick={() => setShowNueva(true)}>+ Nueva tarjeta</button>
        </div>
      </div>

      <div className="card table-wrap">
        {loading ? (
          <p className="empty-state">Cargando...</p>
        ) : tarjetasVisibles.length === 0 ? (
          <p className="empty-state">No hay tarjetas. Agregá la primera.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Banco</th>
                <th>Cierre ({MESES_LARGO[mesActualVal - 1]})</th>
                <th>Vencimiento ({MESES_LARGO[mesActualVal - 1]})</th>
                <th className="text-right">Debe este ciclo</th>
                <th>Activa</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {tarjetasVisibles.map(t => {
                const cierreReal = cierresActuales[t.id]
                return (
                  <tr key={t.id} style={{ opacity: t.activa ? 1 : 0.55 }}>
                    <td><strong>{t.nombre}</strong></td>
                    <td className="text-muted">{t.banco || '—'}</td>
                    <td>
                      {cierreReal
                        ? <span style={{ color: 'var(--green)', fontWeight: 600 }}>{cierreReal.fechaCierreReal}</span>
                        : <span className="text-muted">est. día {t.diaCierreEstimado}</span>}
                    </td>
                    <td>
                      {cierreReal
                        ? <span style={{ color: 'var(--green)', fontWeight: 600 }}>{cierreReal.fechaVencimientoReal}</span>
                        : <span className="text-muted">est. día {t.diaVencimientoEstimado}</span>}
                    </td>
                    <td className="text-right" style={{ fontWeight: 600 }}>{fmt(deudas[t.id] ?? 0)}</td>
                    <td>
                      <label style={{ display: 'inline-flex', alignItems: 'center', cursor: 'pointer' }}>
                        <input type="checkbox" checked={t.activa} onChange={() => handleToggleActiva(t)} />
                      </label>
                    </td>
                    <td className="text-right" style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                      <button className="btn btn-ghost btn-sm" onClick={() => abrirEditar(t)}>Editar</button>
                      <button className="btn btn-ghost btn-sm" onClick={() => abrirCierre(t)}>
                        {cierreReal ? 'Editar fechas' : 'Fechas reales'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      <div className="card mt-8" style={{ marginTop: 16 }}>
        <p style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
          <strong style={{ color: 'var(--text)' }}>¿Para qué sirven las "Fechas reales"?</strong><br />
          El día de cierre estimado es el que usás habitualmente, pero el banco lo corre cuando cae fin de semana o feriado.
          Configurando las fechas reales de cada mes, el sistema imputa correctamente las cuotas al ciclo que corresponde.
        </p>
      </div>

      {/* Modal: nueva tarjeta */}
      {showNueva && (
        <div className="modal-overlay" onClick={() => setShowNueva(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Nueva tarjeta</h2>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Nombre *</label>
                <input className="form-input" placeholder="Visa Galicia"
                  value={formNueva.nombre} onChange={e => setFormNueva(f => ({ ...f, nombre: e.target.value }))} autoFocus />
              </div>
              <div className="form-group">
                <label className="form-label">Banco</label>
                <input className="form-input" placeholder="Galicia"
                  value={formNueva.banco} onChange={e => setFormNueva(f => ({ ...f, banco: e.target.value }))} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Día de cierre estimado *</label>
                <input className="form-input" type="number" min={1} max={31}
                  value={formNueva.diaCierreEstimado}
                  onChange={e => setFormNueva(f => ({ ...f, diaCierreEstimado: +e.target.value }))} />
              </div>
              <div className="form-group">
                <label className="form-label">Día de vencimiento estimado *</label>
                <input className="form-input" type="number" min={1} max={31}
                  value={formNueva.diaVencimientoEstimado}
                  onChange={e => setFormNueva(f => ({ ...f, diaVencimientoEstimado: +e.target.value }))} />
              </div>
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setShowNueva(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleCrear} disabled={saving || !formNueva.nombre}>
                {saving ? 'Guardando...' : 'Crear'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: editar tarjeta */}
      {editModal && (
        <div className="modal-overlay" onClick={() => setEditModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Editar tarjeta — {editModal.nombre}</h2>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Nombre *</label>
                <input className="form-input"
                  value={formEdit.nombre} onChange={e => setFormEdit(f => ({ ...f, nombre: e.target.value }))} autoFocus />
              </div>
              <div className="form-group">
                <label className="form-label">Banco</label>
                <input className="form-input"
                  value={formEdit.banco} onChange={e => setFormEdit(f => ({ ...f, banco: e.target.value }))} />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Día de cierre estimado *</label>
                <input className="form-input" type="number" min={1} max={31}
                  value={formEdit.diaCierreEstimado}
                  onChange={e => setFormEdit(f => ({ ...f, diaCierreEstimado: +e.target.value }))} />
              </div>
              <div className="form-group">
                <label className="form-label">Día de vencimiento estimado *</label>
                <input className="form-input" type="number" min={1} max={31}
                  value={formEdit.diaVencimientoEstimado}
                  onChange={e => setFormEdit(f => ({ ...f, diaVencimientoEstimado: +e.target.value }))} />
              </div>
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setEditModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleGuardarEditar} disabled={saving || !formEdit.nombre}>
                {saving ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: fechas reales por mes */}
      {cierreModal && (
        <div className="modal-overlay" onClick={() => setCierreModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Fechas reales — {cierreModal.tarjeta.nombre}</h2>
            <div className="form-group">
              <label className="form-label">Mes</label>
              <select className="form-input"
                value={`${cierreForm.anio}-${cierreForm.mes}`}
                onChange={e => {
                  const [a, m] = e.target.value.split('-').map(Number)
                  handleCambiarMesCierre(a, m, cierreModal.tarjeta.id)
                }}>
                {opcionesMes.map(o => (
                  <option key={`${o.anio}-${o.mes}`} value={`${o.anio}-${o.mes}`}>
                    {MESES_LARGO[o.mes - 1]} {o.anio}
                  </option>
                ))}
              </select>
            </div>
            {cargandoCierre ? (
              <p className="text-muted" style={{ fontSize: 13 }}>Cargando...</p>
            ) : (
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Fecha de cierre real</label>
                  <input className="form-input" type="date" value={cierreForm.fechaCierreReal}
                    onChange={e => setCierreForm(f => ({ ...f, fechaCierreReal: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Fecha de vencimiento real</label>
                  <input className="form-input" type="date" value={cierreForm.fechaVencimientoReal}
                    onChange={e => setCierreForm(f => ({ ...f, fechaVencimientoReal: e.target.value }))} />
                </div>
              </div>
            )}
            <p className="text-muted" style={{ fontSize: 12 }}>
              Estimado: cierre día {cierreModal.tarjeta.diaCierreEstimado}, vencimiento día {cierreModal.tarjeta.diaVencimientoEstimado}.
              Si no configurás fechas reales, el sistema usa esos días como fallback.
            </p>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setCierreModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleGuardarCierre}
                disabled={saving || !cierreForm.fechaCierreReal || !cierreForm.fechaVencimientoReal}>
                {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
