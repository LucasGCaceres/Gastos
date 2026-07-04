import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  obtenerResumen, actualizarIngresos, cerrarCiclo,
  registrarGastoVariable, editarGastoVariable, eliminarGastoVariable,
  registrarCompra, editarCompra, getCompra, eliminarCompra, actualizarEstadoCuota
} from '../api/ciclos'
import { listarCategorias } from '../api/catalogos'
import { listarTarjetas } from '../api/tarjetas'
import { crearGastoFijo, editarGastoFijo, desactivarGastoFijo } from '../api/gastosFijos'
import { fetchDolarBlue } from '../api/dolar'
import {
  crearEvento, agregarItemEvento, editarItemEvento, eliminarItemEvento, eliminarEvento,
} from '../api/eventos'
import type { CicloResumen, Categoria, Tarjeta, GastoVariable, GastoFijoItem, CuotaImputada, EventoGastoItem } from '../types'

const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre']

function fmt(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n)
}
function fmtUsd(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(n)
}

type Modal =
  | 'nuevo-gasto' | 'editar-gasto'
  | 'nuevo-fijo' | 'editar-fijo'
  | 'compra' | 'editar-compra' | 'ingresos'
  | 'nuevo-evento' | 'editar-item-evento'
  | null

const emptyGasto = { categoriaId: 0, concepto: '', monto: '', fecha: new Date().toISOString().slice(0, 10) }
const emptyFijo = { nombre: '', montoActual: '' }
const emptyCompra = {
  tarjetaId: 0, concepto: '', montoOriginal: '', cantidadCuotas: 1, cuotasYaAbonadas: 0,
  fechaCompra: new Date().toISOString().slice(0, 10), monedaOriginal: 'ARS', cotizacionAplicada: ''
}
const emptyEvento = { nombre: '', fecha: new Date().toISOString().slice(0, 10) }
const emptyItemEvento = { concepto: '', monto: '' }

export default function CicloDetallePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const cicloId = Number(id)

  const [ciclo, setCiclo] = useState<CicloResumen | null>(null)
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<Modal>(null)
  const [saving, setSaving] = useState(false)
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [tarjetas, setTarjetas] = useState<Tarjeta[]>([])

  const [gastoForm, setGastoForm] = useState(emptyGasto)
  const [editandoGastoId, setEditandoGastoId] = useState<number | null>(null)

  const [fijoForm, setFijoForm] = useState(emptyFijo)
  const [editandoFijoId, setEditandoFijoId] = useState<number | null>(null)

  const [compraForm, setCompraForm] = useState(emptyCompra)
  const [editandoCompraId, setEditandoCompraId] = useState<number | null>(null)
  const [cargandoCompra, setCargandoCompra] = useState(false)
  const [ingresosForm, setIngresosForm] = useState('')
  const [cotizacionDolar, setCotizacionDolar] = useState<number | null>(null)

  const [eventoForm, setEventoForm] = useState(emptyEvento)
  const [nuevoItemForms, setNuevoItemForms] = useState<Record<number, { concepto: string; monto: string }>>({})
  const [itemEventoForm, setItemEventoForm] = useState(emptyItemEvento)
  const [editandoItem, setEditandoItem] = useState<{ eventoId: number; itemId: number } | null>(null)

  const reload = () => obtenerResumen(cicloId).then(setCiclo)

  useEffect(() => {
    Promise.all([obtenerResumen(cicloId), listarCategorias(), listarTarjetas()])
      .then(([c, cats, tars]) => {
        setCiclo(c)
        setCategorias(cats)
        setTarjetas(tars)
        setGastoForm(f => ({ ...f, categoriaId: cats[0]?.id ?? 0 }))
        setCompraForm(f => ({ ...f, tarjetaId: tars[0]?.id ?? 0 }))
      })
      .finally(() => setLoading(false))
    fetchDolarBlue().then(d => setCotizacionDolar(d.venta)).catch(() => setCotizacionDolar(null))
  }, [cicloId])

  // ── Cuotas: control de pago ──────────────────────────────────────────────────

  const handleToggleCuotaPagada = async (cuota: CuotaImputada) => {
    const nuevoEstado = cuota.estado === 'PAGADO' ? 'PENDIENTE' : 'PAGADO'
    setCiclo(c => c ? { ...c, cuotas: c.cuotas.map(q => q.id === cuota.id ? { ...q, estado: nuevoEstado } : q) } : c)
    try {
      await actualizarEstadoCuota(cuota.id, nuevoEstado)
    } catch {
      await reload()
    }
  }

  // ── Gastos variables ─────────────────────────────────────────────────────────

  const abrirNuevoGasto = () => {
    setGastoForm({ ...emptyGasto, categoriaId: categorias[0]?.id ?? 0 })
    setEditandoGastoId(null)
    setModal('nuevo-gasto')
  }

  const abrirEditarGasto = (g: GastoVariable) => {
    setGastoForm({
      categoriaId: categorias.find(c => c.nombre === g.categoria)?.id ?? categorias[0]?.id ?? 0,
      concepto: g.concepto,
      monto: String(g.monto),
      fecha: g.fecha,
    })
    setEditandoGastoId(g.id)
    setModal('editar-gasto')
  }

  const handleGuardarGasto = async () => {
    setSaving(true)
    try {
      const data = { ...gastoForm, monto: +gastoForm.monto }
      if (modal === 'editar-gasto' && editandoGastoId) {
        await editarGastoVariable(cicloId, editandoGastoId, data)
      } else {
        await registrarGastoVariable(cicloId, data)
      }
      setModal(null)
      await reload()
    } finally { setSaving(false) }
  }

  const handleEliminarGasto = async (gastoId: number) => {
    if (!confirm('¿Eliminar este gasto?')) return
    await eliminarGastoVariable(cicloId, gastoId)
    await reload()
  }

  // ── Gastos fijos ─────────────────────────────────────────────────────────────

  const abrirNuevoFijo = () => {
    setFijoForm(emptyFijo)
    setEditandoFijoId(null)
    setModal('nuevo-fijo')
  }

  const abrirEditarFijo = (g: GastoFijoItem) => {
    setFijoForm({ nombre: g.nombre, montoActual: String(g.monto) })
    setEditandoFijoId(g.id)
    setModal('editar-fijo')
  }

  const handleGuardarFijo = async () => {
    setSaving(true)
    try {
      const data = { nombre: fijoForm.nombre, montoActual: +fijoForm.montoActual }
      if (modal === 'editar-fijo' && editandoFijoId) {
        await editarGastoFijo(editandoFijoId, data)
      } else {
        await crearGastoFijo(data)
      }
      setModal(null)
      await reload()
    } finally { setSaving(false) }
  }

  const handleEliminarFijo = async (id: number, nombre: string) => {
    if (!confirm(`¿Desactivar "${nombre}"? Dejará de aparecer en los ciclos futuros.`)) return
    await desactivarGastoFijo(id)
    await reload()
  }

  // ── Compra tarjeta ───────────────────────────────────────────────────────────

  const abrirNuevaCompra = () => {
    setCompraForm({ ...emptyCompra, tarjetaId: tarjetas[0]?.id ?? 0 })
    setEditandoCompraId(null)
    setModal('compra')
  }

  const abrirEditarCompra = async (compraId: number) => {
    setEditandoCompraId(compraId)
    setModal('editar-compra')
    setCargandoCompra(true)
    try {
      const c = await getCompra(compraId)
      setCompraForm({
        tarjetaId: c.tarjetaId,
        concepto: c.concepto,
        montoOriginal: String(c.montoOriginal),
        cantidadCuotas: c.cantidadCuotas,
        cuotasYaAbonadas: c.cuotasYaAbonadas,
        fechaCompra: c.fechaCompra,
        monedaOriginal: c.monedaOriginal,
        cotizacionAplicada: c.monedaOriginal === 'USD' ? String(c.cotizacionAplicada) : '',
      })
    } finally { setCargandoCompra(false) }
  }

  const handleGuardarCompra = async () => {
    setSaving(true)
    try {
      const payload: Record<string, unknown> = {
        tarjetaId: compraForm.tarjetaId,
        concepto: compraForm.concepto,
        montoOriginal: +compraForm.montoOriginal,
        cantidadCuotas: compraForm.cantidadCuotas,
        cuotasYaAbonadas: compraForm.cuotasYaAbonadas,
        fechaCompra: compraForm.fechaCompra,
        monedaOriginal: compraForm.monedaOriginal,
      }
      if (compraForm.monedaOriginal === 'USD' && compraForm.cotizacionAplicada) {
        payload.cotizacionAplicada = +compraForm.cotizacionAplicada
      }
      if (modal === 'editar-compra' && editandoCompraId) {
        await editarCompra(editandoCompraId, payload as Parameters<typeof editarCompra>[1])
      } else {
        await registrarCompra(payload as Parameters<typeof registrarCompra>[0])
      }
      setModal(null)
      setCompraForm({ ...emptyCompra, tarjetaId: tarjetas[0]?.id ?? 0 })
      await reload()
    } finally { setSaving(false) }
  }

  const handleEliminarCompra = async (compraId: number, concepto: string) => {
    if (!confirm(`¿Eliminar "${concepto}"? Se borrarán todas las cuotas proyectadas y se recalcularán los ciclos afectados.`)) return
    await eliminarCompra(compraId)
    await reload()
  }

  // ── Eventos / salidas ────────────────────────────────────────────────────────

  const handleCrearEvento = async () => {
    setSaving(true)
    try {
      await crearEvento(cicloId, eventoForm)
      setModal(null)
      setEventoForm(emptyEvento)
      await reload()
    } finally { setSaving(false) }
  }

  const handleEliminarEvento = async (eventoId: number, nombre: string) => {
    if (!confirm(`¿Eliminar el evento "${nombre}" y todos sus gastos?`)) return
    await eliminarEvento(eventoId)
    await reload()
  }

  const nuevoItemForm = (eventoId: number) => nuevoItemForms[eventoId] ?? { concepto: '', monto: '' }

  const handleAgregarItem = async (eventoId: number) => {
    const form = nuevoItemForm(eventoId)
    if (!form.concepto || !form.monto) return
    setSaving(true)
    try {
      await agregarItemEvento(eventoId, { concepto: form.concepto, monto: +form.monto })
      setNuevoItemForms(prev => ({ ...prev, [eventoId]: { concepto: '', monto: '' } }))
      await reload()
    } finally { setSaving(false) }
  }

  const abrirEditarItem = (eventoId: number, item: EventoGastoItem) => {
    setEditandoItem({ eventoId, itemId: item.id })
    setItemEventoForm({ concepto: item.concepto, monto: String(item.monto) })
    setModal('editar-item-evento')
  }

  const handleGuardarItemEvento = async () => {
    if (!editandoItem) return
    setSaving(true)
    try {
      await editarItemEvento(editandoItem.eventoId, editandoItem.itemId, {
        concepto: itemEventoForm.concepto, monto: +itemEventoForm.monto,
      })
      setModal(null)
      setEditandoItem(null)
      await reload()
    } finally { setSaving(false) }
  }

  const handleEliminarItem = async (eventoId: number, itemId: number) => {
    await eliminarItemEvento(eventoId, itemId)
    await reload()
  }

  // ── Ingresos ─────────────────────────────────────────────────────────────────

  const handleActualizarIngresos = async () => {
    setSaving(true)
    try {
      await actualizarIngresos(cicloId, +ingresosForm)
      setModal(null)
      await reload()
    } finally { setSaving(false) }
  }

  const handleCerrar = async () => {
    if (!confirm('¿Cerrar este ciclo? No podrá modificarse después.')) return
    await cerrarCiclo(cicloId)
    await reload()
  }

  if (loading) return <p className="empty-state">Cargando...</p>
  if (!ciclo) return <p className="empty-state">Ciclo no encontrado.</p>

  const abierto = ciclo.estado === 'ABIERTO'

  return (
    <>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="btn btn-ghost btn-sm" onClick={() => navigate('/')}>← Volver</button>
          <h1 className="page-title">{MESES[ciclo.mes - 1]} {ciclo.anio}</h1>
          <span className={`badge ${abierto ? 'badge-open' : 'badge-closed'}`}>
            {abierto ? 'Abierto' : 'Cerrado'}
          </span>
        </div>
        {abierto && (
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-ghost" onClick={() => { setIngresosForm(String(ciclo.totalIngresos)); setModal('ingresos') }}>
              Editar ingresos
            </button>
            <button className="btn btn-danger" onClick={handleCerrar}>Cerrar ciclo</button>
          </div>
        )}
      </div>

      {/* Resumen */}
      <div className="summary-grid">
        <div className="summary-card">
          <div className="summary-label">Ingresos</div>
          <div className="summary-value">{fmt(ciclo.totalIngresos)}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Gastos fijos</div>
          <div className="summary-value text-red">{fmt(ciclo.totalFijos)}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Variables</div>
          <div className="summary-value text-red">{fmt(ciclo.totalVariables)}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Tarjetas</div>
          <div className="summary-value text-yellow">{fmt(ciclo.totalCuotas)}</div>
        </div>
        <div className="summary-card">
          <div className="summary-label">Saldo final</div>
          <div className={`summary-value ${ciclo.saldoFinal >= 0 ? 'positive' : 'negative'}`}>
            {fmt(ciclo.saldoFinal)}
          </div>
        </div>
      </div>

      {/* Gastos fijos */}
      <div className="section">
        <div className="section-header">
          <span className="section-title">Gastos fijos</span>
          {abierto && <button className="btn btn-ghost btn-sm" onClick={abrirNuevoFijo}>+ Agregar</button>}
        </div>
        <div className="card table-wrap">
          {ciclo.gastosFijos.length === 0 ? (
            <p className="empty-state">Sin gastos fijos activos.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th className="text-right">Monto</th>
                  {abierto && <th></th>}
                </tr>
              </thead>
              <tbody>
                {ciclo.gastosFijos.map(g => (
                  <tr key={g.id}>
                    <td>{g.nombre}</td>
                    <td className="text-right">{fmt(g.monto)}</td>
                    {abierto && (
                      <td className="text-right" style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => abrirEditarFijo(g)}>✎</button>
                        <button className="btn btn-ghost btn-sm" onClick={() => handleEliminarFijo(g.id, g.nombre)}>✕</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Gastos variables */}
      <div className="section">
        <div className="section-header">
          <span className="section-title">Gastos variables</span>
          {abierto && <button className="btn btn-ghost btn-sm" onClick={abrirNuevoGasto}>+ Agregar</button>}
        </div>
        <div className="card table-wrap">
          {ciclo.gastosVariables.length === 0 ? (
            <p className="empty-state">Sin gastos variables.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Categoría</th>
                  <th>Concepto</th>
                  <th className="text-right">Monto</th>
                  {abierto && <th></th>}
                </tr>
              </thead>
              <tbody>
                {ciclo.gastosVariables.map(g => (
                  <tr key={g.id}>
                    <td className="text-muted">{g.fecha}</td>
                    <td>{g.categoria}</td>
                    <td>{g.concepto}</td>
                    <td className="text-right">{fmt(g.monto)}</td>
                    {abierto && (
                      <td className="text-right" style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => abrirEditarGasto(g)}>✎</button>
                        <button className="btn btn-ghost btn-sm" onClick={() => handleEliminarGasto(g.id)}>✕</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Cuotas de tarjeta */}
      <div className="section">
        <div className="section-header">
          <span className="section-title">Cuotas de tarjeta</span>
          {abierto && <button className="btn btn-ghost btn-sm" onClick={abrirNuevaCompra}>+ Agregar compra</button>}
        </div>
        <div className="card table-wrap">
          {ciclo.cuotas.length === 0 ? (
            <p className="empty-state">Sin cuotas este mes.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Concepto</th>
                  <th>Tarjeta</th>
                  <th>Cuota</th>
                  <th className="text-right">Monto</th>
                  <th style={{ textAlign: 'center' }}>Pagado</th>
                  {abierto && <th></th>}
                </tr>
              </thead>
              <tbody>
                {ciclo.cuotas.map(c => (
                  <tr key={c.id} style={{ opacity: c.estado === 'PAGADO' ? 0.6 : 1 }}>
                    <td>{c.concepto}</td>
                    <td className="text-muted">{c.tarjeta}</td>
                    <td className="text-muted">{c.numeroCuota}/{c.totalCuotas}</td>
                    <td className="text-right" style={{ textDecoration: c.estado === 'PAGADO' ? 'line-through' : undefined }}>
                      {fmt(c.montoEnPesos)}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <input type="checkbox" checked={c.estado === 'PAGADO'}
                        onChange={() => handleToggleCuotaPagada(c)} title="Marcar como pagada" />
                    </td>
                    {abierto && (
                      <td className="text-right" style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                        <button className="btn btn-ghost btn-xs" onClick={() => abrirEditarCompra(c.compraId)}>✎</button>
                        <button className="btn btn-ghost btn-xs" style={{ color: 'var(--red)', borderColor: 'transparent' }}
                          onClick={() => handleEliminarCompra(c.compraId, c.concepto)}>✕</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Gastos esperados (proyectados, no consolidados) */}
      {ciclo.gastosEsperados.length > 0 && (
        <div className="section">
          <div className="section-header">
            <span className="section-title">Gastos esperados (proyectado)</span>
            <span className="text-muted" style={{ fontSize: 11 }}>
              {cotizacionDolar ? `Dólar blue venta: ${fmt(cotizacionDolar)}` : 'Sin cotización'}
            </span>
          </div>
          <div className="card table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Calculadora</th>
                  <th className="text-right">Monto USD</th>
                  <th className="text-right">≈ ARS (estimado)</th>
                </tr>
              </thead>
              <tbody>
                {ciclo.gastosEsperados.map(g => (
                  <tr key={g.calculadoraId}>
                    <td>{g.nombre}</td>
                    <td className="text-right">{fmtUsd(g.objetivoUsd)}</td>
                    <td className="text-right" style={{ color: 'var(--yellow)', fontWeight: 600 }}>
                      {cotizacionDolar ? fmt(g.objetivoUsd * cotizacionDolar) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="text-muted" style={{ fontSize: 11, marginTop: 8 }}>
              No está incluido en el saldo final: es un objetivo proyectado que se recalcula con la cotización vigente
              hasta que lo efectivices desde la calculadora de Ahorro USD.
            </p>
          </div>
        </div>
      )}

      {/* Eventos / Salidas — grupos de gastos puntuales, cuentan como gasto variable */}
      <div className="section">
        <div className="section-header">
          <span className="section-title">Eventos / Salidas</span>
          {abierto && <button className="btn btn-ghost btn-sm" onClick={() => { setEventoForm(emptyEvento); setModal('nuevo-evento') }}>
            + Nuevo evento
          </button>}
        </div>
        {ciclo.eventos.length === 0 ? (
          <div className="card"><p className="empty-state">Sin eventos este mes. Creá uno para agrupar los gastos de una salida.</p></div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {ciclo.eventos.map(ev => (
              <div key={ev.id} className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                  <div>
                    <strong>{ev.nombre}</strong>
                    <span className="text-muted" style={{ fontSize: 12, marginLeft: 8 }}>{ev.fecha}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ fontWeight: 700, color: 'var(--yellow)' }}>{fmt(ev.total)}</span>
                    {abierto && (
                      <button className="btn btn-ghost btn-xs" style={{ color: 'var(--red)', borderColor: 'transparent' }}
                        onClick={() => handleEliminarEvento(ev.id, ev.nombre)}>✕</button>
                    )}
                  </div>
                </div>
                {ev.items.length > 0 && (
                  <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: abierto ? 10 : 0 }}>
                    <tbody>
                      {ev.items.map(item => (
                        <tr key={item.id}>
                          <td style={{ padding: '4px 0', fontSize: 13 }}>{item.concepto}</td>
                          <td style={{ padding: '4px 0', fontSize: 13, textAlign: 'right' }}>{fmt(item.monto)}</td>
                          {abierto && (
                            <td style={{ padding: '4px 0', textAlign: 'right', width: 60 }}>
                              <button className="btn btn-ghost btn-xs" onClick={() => abrirEditarItem(ev.id, item)}>✎</button>
                              <button className="btn btn-ghost btn-xs" onClick={() => handleEliminarItem(ev.id, item.id)}>✕</button>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                {abierto && (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input className="form-input" placeholder="Concepto (ej: Uber)" style={{ flex: 1 }}
                      value={nuevoItemForm(ev.id).concepto}
                      onChange={e => setNuevoItemForms(prev => ({ ...prev, [ev.id]: { ...nuevoItemForm(ev.id), concepto: e.target.value } }))} />
                    <input className="form-input" type="number" min="0" placeholder="Monto" style={{ width: 120 }}
                      value={nuevoItemForm(ev.id).monto}
                      onChange={e => setNuevoItemForms(prev => ({ ...prev, [ev.id]: { ...nuevoItemForm(ev.id), monto: e.target.value } }))} />
                    <button className="btn btn-ghost btn-sm" onClick={() => handleAgregarItem(ev.id)}
                      disabled={saving || !nuevoItemForm(ev.id).concepto || !nuevoItemForm(ev.id).monto}>
                      + Agregar
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Modales ────────────────────────────────────────────────────────────── */}

      {/* Gasto variable: crear / editar */}
      {(modal === 'nuevo-gasto' || modal === 'editar-gasto') && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">{modal === 'editar-gasto' ? 'Editar gasto variable' : 'Nuevo gasto variable'}</h2>
            <div className="form-group">
              <label className="form-label">Categoría</label>
              <select className="form-input" value={gastoForm.categoriaId}
                onChange={e => setGastoForm(f => ({ ...f, categoriaId: +e.target.value }))}>
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Concepto</label>
              <input className="form-input" value={gastoForm.concepto} autoFocus
                onChange={e => setGastoForm(f => ({ ...f, concepto: e.target.value }))} />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Monto ($)</label>
                <input className="form-input" type="number" value={gastoForm.monto}
                  onChange={e => setGastoForm(f => ({ ...f, monto: e.target.value }))} />
              </div>
              <div className="form-group">
                <label className="form-label">Fecha</label>
                <input className="form-input" type="date" value={gastoForm.fecha}
                  onChange={e => setGastoForm(f => ({ ...f, fecha: e.target.value }))} />
              </div>
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleGuardarGasto}
                disabled={saving || !gastoForm.concepto || !gastoForm.monto}>
                {saving ? 'Guardando...' : modal === 'editar-gasto' ? 'Guardar cambios' : 'Registrar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Gasto fijo: crear / editar */}
      {(modal === 'nuevo-fijo' || modal === 'editar-fijo') && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">{modal === 'editar-fijo' ? 'Editar gasto fijo' : 'Nuevo gasto fijo'}</h2>
            <div className="form-group">
              <label className="form-label">Nombre</label>
              <input className="form-input" value={fijoForm.nombre} autoFocus
                placeholder="Ej: Spotify, Internet, Alquiler"
                onChange={e => setFijoForm(f => ({ ...f, nombre: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Monto mensual ($)</label>
              <input className="form-input" type="number" value={fijoForm.montoActual}
                onChange={e => setFijoForm(f => ({ ...f, montoActual: e.target.value }))} />
            </div>
            {modal === 'editar-fijo' && (
              <p className="text-muted" style={{ fontSize: 12 }}>
                El cambio aplica a ciclos futuros. Los ciclos cerrados conservan el monto original.
              </p>
            )}
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleGuardarFijo}
                disabled={saving || !fijoForm.nombre || !fijoForm.montoActual}>
                {saving ? 'Guardando...' : modal === 'editar-fijo' ? 'Guardar cambios' : 'Crear'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Compra tarjeta: crear / editar */}
      {(modal === 'compra' || modal === 'editar-compra') && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">{modal === 'editar-compra' ? 'Editar compra con tarjeta' : 'Registrar compra con tarjeta'}</h2>
            {tarjetas.length === 0 ? (
              <p className="text-muted">No hay tarjetas. Agregá una en la sección Tarjetas del menú.</p>
            ) : cargandoCompra ? (
              <p className="text-muted" style={{ fontSize: 13 }}>Cargando...</p>
            ) : (
              <>
                <div className="form-group">
                  <label className="form-label">Tarjeta</label>
                  <select className="form-input" value={compraForm.tarjetaId}
                    onChange={e => setCompraForm(f => ({ ...f, tarjetaId: +e.target.value }))}>
                    {tarjetas.map(t => <option key={t.id} value={t.id}>{t.nombre}</option>)}
                  </select>
                  {modal === 'editar-compra' && (
                    <p className="text-muted" style={{ fontSize: 12, marginTop: 4 }}>
                      Cambiar la tarjeta reasigna esta compra y todas sus cuotas proyectadas a la nueva entidad.
                    </p>
                  )}
                </div>
                <div className="form-group">
                  <label className="form-label">Concepto</label>
                  <input className="form-input" value={compraForm.concepto} autoFocus
                    onChange={e => setCompraForm(f => ({ ...f, concepto: e.target.value }))} />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Monto</label>
                    <input className="form-input" type="number" value={compraForm.montoOriginal}
                      onChange={e => setCompraForm(f => ({ ...f, montoOriginal: e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Moneda</label>
                    <select className="form-input" value={compraForm.monedaOriginal}
                      onChange={e => setCompraForm(f => ({ ...f, monedaOriginal: e.target.value, cotizacionAplicada: '' }))}>
                      <option value="ARS">ARS</option>
                      <option value="USD">USD</option>
                    </select>
                  </div>
                </div>
                {compraForm.monedaOriginal === 'USD' && (
                  <div className="form-group">
                    <label className="form-label">Cotización ARS/USD</label>
                    <input className="form-input" type="number" placeholder="Ej: 1250"
                      value={compraForm.cotizacionAplicada}
                      onChange={e => setCompraForm(f => ({ ...f, cotizacionAplicada: e.target.value }))} />
                  </div>
                )}
                <div className="form-row-3">
                  <div className="form-group">
                    <label className="form-label">Cuotas totales</label>
                    <input className="form-input" type="number" min={1} value={compraForm.cantidadCuotas}
                      onChange={e => setCompraForm(f => ({ ...f, cantidadCuotas: +e.target.value, cuotasYaAbonadas: 0 }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Ya abonadas</label>
                    <input className="form-input" type="number" min={0} max={compraForm.cantidadCuotas - 1}
                      value={compraForm.cuotasYaAbonadas}
                      onChange={e => setCompraForm(f => ({ ...f, cuotasYaAbonadas: +e.target.value }))} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fecha de compra</label>
                    <input className="form-input" type="date" value={compraForm.fechaCompra}
                      onChange={e => setCompraForm(f => ({ ...f, fechaCompra: e.target.value }))} />
                  </div>
                </div>
                {compraForm.cuotasYaAbonadas > 0 && compraForm.cuotasYaAbonadas < compraForm.cantidadCuotas && (
                  <p className="text-muted" style={{ fontSize: 12 }}>
                    Se proyectarán cuotas {compraForm.cuotasYaAbonadas + 1} a {compraForm.cantidadCuotas}, desde el mes ingresado.
                  </p>
                )}
                {compraForm.cuotasYaAbonadas > 0 && compraForm.cuotasYaAbonadas === compraForm.cantidadCuotas && (
                  <p style={{ fontSize: 12, color: 'var(--accent)' }}>
                    Todas las cuotas ya fueron abonadas. Se registrará la cuota final ({compraForm.cantidadCuotas}/{compraForm.cantidadCuotas}) en el ciclo del mes ingresado.
                  </p>
                )}
              </>
            )}
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              {tarjetas.length > 0 && !cargandoCompra && (
                <button className="btn btn-primary" onClick={handleGuardarCompra}
                  disabled={saving || !compraForm.concepto || !compraForm.montoOriginal ||
                    (compraForm.monedaOriginal === 'USD' && !compraForm.cotizacionAplicada)}>
                  {saving ? 'Guardando...' : modal === 'editar-compra' ? 'Guardar cambios' : 'Registrar'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Nuevo evento */}
      {modal === 'nuevo-evento' && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Nuevo evento / salida</h2>
            <div className="form-group">
              <label className="form-label">Nombre</label>
              <input className="form-input" value={eventoForm.nombre} autoFocus placeholder="Ej: Salida fiesta"
                onChange={e => setEventoForm(f => ({ ...f, nombre: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Fecha</label>
              <input className="form-input" type="date" value={eventoForm.fecha}
                onChange={e => setEventoForm(f => ({ ...f, fecha: e.target.value }))} />
            </div>
            <p className="text-muted" style={{ fontSize: 12 }}>
              Después de crearlo vas a poder agregar los gastos de la ocasión (Uber, bebidas, comida, etc.).
            </p>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleCrearEvento} disabled={saving || !eventoForm.nombre}>
                {saving ? 'Creando...' : 'Crear'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Editar ítem de evento */}
      {modal === 'editar-item-evento' && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Editar gasto del evento</h2>
            <div className="form-group">
              <label className="form-label">Concepto</label>
              <input className="form-input" value={itemEventoForm.concepto} autoFocus
                onChange={e => setItemEventoForm(f => ({ ...f, concepto: e.target.value }))} />
            </div>
            <div className="form-group">
              <label className="form-label">Monto ($)</label>
              <input className="form-input" type="number" min="0" value={itemEventoForm.monto}
                onChange={e => setItemEventoForm(f => ({ ...f, monto: e.target.value }))} />
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleGuardarItemEvento}
                disabled={saving || !itemEventoForm.concepto || !itemEventoForm.monto}>
                {saving ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Ingresos */}
      {modal === 'ingresos' && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Actualizar ingresos</h2>
            <div className="form-group">
              <label className="form-label">Total ingresos del mes ($)</label>
              <input className="form-input" type="number" value={ingresosForm} autoFocus
                onChange={e => setIngresosForm(e.target.value)} />
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleActualizarIngresos} disabled={saving}>
                {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
