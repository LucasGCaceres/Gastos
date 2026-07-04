import { useEffect, useState } from 'react'
import {
  listarCalculadoras, crearCalculadora, actualizarItems, actualizarRutas,
  actualizarAhorro, efectivizarAhorro, aplicarCalculadora, desactivarCalculadora,
  type Calculadora, type TipoCalculadora,
} from '../api/calculadoras'
import { listarGastosFijos, type GastoFijoApi } from '../api/gastosFijos'
import { fetchDolarBlue, fetchDolarMep, type CotizacionDolar } from '../api/dolar'
import { useToast } from '../components/Toast'

function fmt(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n)
}
function fmtUsd(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(n)
}
function factorLabel(orden: number) {
  return orden === 0 ? '100%' : orden === 1 ? '50%' : '25%'
}
function factorColor(orden: number) {
  return orden === 0 ? 'var(--green)' : orden === 1 ? 'var(--yellow)' : 'var(--accent)'
}

async function extractError(e: unknown): Promise<string> {
  if (e && typeof e === 'object' && 'response' in e) {
    const ax = e as { response?: { data?: { error?: string }; status?: number } }
    if (ax.response?.data?.error) return ax.response.data.error
    if (ax.response?.status === 409) return 'Operación no permitida (conflicto de estado)'
    if (ax.response?.status === 400) return 'Datos inválidos. Revisá los campos.'
    if (ax.response?.status === 0) return 'No se pudo conectar con el servidor'
  }
  if (e instanceof Error) return e.message
  return 'Error desconocido'
}

type ItemDraft = { nombre: string; cantidad: string; precioUnitario: string }
type TramoDraft = { nombre: string; precio: string }
type RutaDraft = { nombre: string; viajesPorMes: string; tramos: TramoDraft[] }

function calcGenericaTotal(items: ItemDraft[]) {
  return items.reduce((acc, i) => acc + (+i.cantidad || 0) * (+i.precioUnitario || 0), 0)
}
function calcSubeTotal(rutas: RutaDraft[]) {
  return rutas.reduce((acc, ruta) => {
    const costo = ruta.tramos.reduce((s, t, idx) => s + (+t.precio || 0) * (idx === 0 ? 1 : idx === 1 ? 0.5 : 0.25), 0)
    return acc + costo * (+ruta.viajesPorMes || 0)
  }, 0)
}

// ─── Modal: nueva calculadora ────────────────────────────────────────────────

interface NuevaModalProps {
  gastosFijos: GastoFijoApi[]
  onClose: () => void
  onCreate: (nombre: string, tipo: TipoCalculadora, gastoFijoId?: number, nuevoGastoFijoNombre?: string) => Promise<void>
}

function NuevaCalculadoraModal({ gastosFijos, onClose, onCreate }: NuevaModalProps) {
  const [nombre, setNombre] = useState('')
  const [tipo, setTipo] = useState<TipoCalculadora>('GENERICA')
  const [gastoFijoId, setGastoFijoId] = useState('')
  const [modoGastoFijo, setModoGastoFijo] = useState<'ninguno' | 'existente' | 'nuevo'>('ninguno')
  const [nuevoGastoFijoNombre, setNuevoGastoFijoNombre] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSubmit = async () => {
    setSaving(true)
    try {
      await onCreate(
        nombre, tipo,
        modoGastoFijo === 'existente' && gastoFijoId ? +gastoFijoId : undefined,
        modoGastoFijo === 'nuevo' && nuevoGastoFijoNombre ? nuevoGastoFijoNombre : undefined,
      )
    } finally { setSaving(false) }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h2 className="modal-title">Nueva calculadora</h2>
        <div className="form-group">
          <label className="form-label">Nombre</label>
          <input className="form-input" value={nombre} autoFocus placeholder="Ej: Transporte, Yerba, Ahorro USD"
            onChange={e => setNombre(e.target.value)} />
        </div>
        <div className="form-group">
          <label className="form-label">Tipo</label>
          <select className="form-input" value={tipo} onChange={e => setTipo(e.target.value as TipoCalculadora)}>
            <option value="GENERICA">Genérica (cantidad × precio)</option>
            <option value="SUBE">SUBE (descuentos por combinación)</option>
            <option value="AHORRO">Ahorro en moneda extranjera (USD)</option>
          </select>
        </div>
        <div className="form-group">
          <label className="form-label">Vincular a gasto fijo</label>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
            {(['ninguno', 'existente', 'nuevo'] as const).map(m => (
              <button key={m} className={`btn btn-sm ${modoGastoFijo === m ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setModoGastoFijo(m)}>
                {m === 'ninguno' ? 'Sin vincular' : m === 'existente' ? 'Elegir existente' : '+ Crear nuevo'}
              </button>
            ))}
          </div>
          {modoGastoFijo === 'existente' && (
            <select className="form-input" value={gastoFijoId} onChange={e => setGastoFijoId(e.target.value)}>
              <option value="">— Seleccionar —</option>
              {gastosFijos.map(g => <option key={g.id} value={g.id}>{g.nombre} ({fmt(g.montoActual)})</option>)}
            </select>
          )}
          {modoGastoFijo === 'nuevo' && (
            <input className="form-input" value={nuevoGastoFijoNombre} placeholder="Nombre del nuevo gasto fijo"
              onChange={e => setNuevoGastoFijoNombre(e.target.value)} />
          )}
          {modoGastoFijo !== 'ninguno' && (
            <span className="text-muted" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              Al aplicar, el total calculado reemplaza el monto de ese gasto fijo.
            </span>
          )}
        </div>
        <div className="form-actions">
          <button className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn btn-primary" onClick={handleSubmit}
            disabled={saving || !nombre || (modoGastoFijo === 'nuevo' && !nuevoGastoFijoNombre)}>
            {saving ? 'Creando...' : 'Crear'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ─── Editor AHORRO ────────────────────────────────────────────────────────────

interface AhorroEditorProps {
  calc: Calculadora
  onUpdate: (updated: Calculadora) => void
}

function AhorroEditor({ calc, onUpdate }: AhorroEditorProps) {
  const { success, error } = useToast()
  const ahorro = calc.ahorro
  const [objetivoUsd, setObjetivoUsd] = useState(String(ahorro?.objetivoUsd ?? 0))
  const [tipoCotizacion, setTipoCotizacion] = useState<'blue' | 'mep'>('blue')
  const [cotizacion, setCotizacion] = useState<CotizacionDolar | null>(null)
  const [loadingCotiz, setLoadingCotiz] = useState(false)
  const [saving, setSaving] = useState(false)
  const [efectivizando, setEfectivizando] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [nombreGasto, setNombreGasto] = useState(`Compra USD - ${new Date().toLocaleDateString('es-AR')}`)

  const fetchCotizacion = async () => {
    setLoadingCotiz(true)
    try {
      const data = tipoCotizacion === 'blue' ? await fetchDolarBlue() : await fetchDolarMep()
      setCotizacion(data)
    } catch (e) {
      error(await extractError(e))
    } finally { setLoadingCotiz(false) }
  }

  useEffect(() => { fetchCotizacion() }, [tipoCotizacion])

  // Sync local state when calc prop changes (after efectivizar auto-reset)
  useEffect(() => {
    setObjetivoUsd(String(calc.ahorro?.objetivoUsd ?? 0))
  }, [calc.ahorro?.objetivoUsd])

  const estimadoArs = cotizacion ? (+objetivoUsd || 0) * cotizacion.venta : null
  const historial = ahorro?.historial ?? []

  const handleGuardar = async () => {
    setSaving(true)
    try {
      const updated = await actualizarAhorro(calc.id, +objetivoUsd)
      onUpdate(updated)
      success(+objetivoUsd > 0
        ? 'Guardado como gasto esperado del ciclo abierto — se recalcula con la cotización vigente hasta que lo efectivices'
        : 'Objetivo actualizado')
    } catch (e) { error(await extractError(e)) }
    finally { setSaving(false) }
  }

  const handleEfectivizar = async () => {
    if (!cotizacion) return
    setEfectivizando(true)
    try {
      const updated = await efectivizarAhorro(calc.id, {
        cotizacionActual: cotizacion.venta,
        nombreGasto,
      })
      onUpdate(updated)
      setShowConfirm(false)
      setNombreGasto(`Compra USD - ${new Date().toLocaleDateString('es-AR')}`)
      success(`¡Efectivizado! Se registró ${fmt(estimadoArs!)} como gasto variable.`)
    } catch (e) { error(await extractError(e)) }
    finally { setEfectivizando(false) }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Cotización en vivo */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <span className="section-title">Cotización actual</span>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <select className="form-input" value={tipoCotizacion} style={{ width: 'auto' }}
              onChange={e => setTipoCotizacion(e.target.value as 'blue' | 'mep')}>
              <option value="blue">Dólar Blue</option>
              <option value="mep">Dólar MEP (Bolsa)</option>
            </select>
            <button className="btn btn-ghost btn-sm" onClick={fetchCotizacion} disabled={loadingCotiz}>
              {loadingCotiz ? '...' : '↻'}
            </button>
          </div>
        </div>
        {cotizacion ? (
          <div style={{ display: 'flex', gap: 32, alignItems: 'center' }}>
            <div>
              <div className="text-muted" style={{ fontSize: 11 }}>Compra</div>
              <div style={{ fontSize: 20, fontWeight: 800 }}>{fmt(cotizacion.compra)}</div>
            </div>
            <div>
              <div className="text-muted" style={{ fontSize: 11 }}>Venta</div>
              <div style={{ fontSize: 20, fontWeight: 800, color: 'var(--yellow)' }}>{fmt(cotizacion.venta)}</div>
            </div>
            <span className="text-muted" style={{ fontSize: 11, marginLeft: 'auto' }}>
              dolarapi.com · {new Date(cotizacion.fechaActualizacion).toLocaleString('es-AR')}
            </span>
          </div>
        ) : (
          <span className="text-muted" style={{ fontSize: 13 }}>
            {loadingCotiz ? 'Cargando...' : 'Sin cotización. Verificá la conexión.'}
          </span>
        )}
      </div>

      {/* Objetivo + efectivizar */}
      <div className="card" style={{ border: +objetivoUsd > 0 && cotizacion ? '1px solid var(--accent)' : undefined }}>
        <span className="section-title" style={{ display: 'block', marginBottom: 14 }}>Objetivo del período</span>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Monto en USD</label>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 700, fontSize: 13 }}>USD</span>
              <input className="form-input" type="number" min="0" step="10" value={objetivoUsd} style={{ width: 120 }}
                onChange={e => setObjetivoUsd(e.target.value)} />
            </div>
          </div>

          {estimadoArs !== null && +objetivoUsd > 0 && cotizacion && (
            <div style={{ flex: 1, minWidth: 160 }}>
              <div className="text-muted" style={{ fontSize: 11, marginBottom: 4 }}>≈ en ARS (venta)</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--yellow)' }}>{fmt(estimadoArs)}</div>
            </div>
          )}

          <div style={{ display: 'flex', gap: 8, marginLeft: 'auto' }}>
            <button className="btn btn-ghost btn-sm" onClick={handleGuardar} disabled={saving}
              title="Guarda el monto como gasto esperado/proyectado en el ciclo abierto, sin efectivizarlo">
              {saving ? '...' : 'Guardar'}
            </button>
            {cotizacion && +objetivoUsd > 0 && (
              <button className="btn btn-primary btn-sm" onClick={() => setShowConfirm(true)}>
                Efectivizar →
              </button>
            )}
          </div>
        </div>
        {+objetivoUsd > 0 && (
          <p className="text-muted" style={{ fontSize: 11, marginTop: 10 }}>
            "Guardar" lo deja como <strong style={{ color: 'var(--text)' }}>gasto esperado</strong> en el ciclo abierto
            (visible en la pantalla del ciclo, recalculado con la cotización vigente). No impacta el saldo final hasta
            que uses "Efectivizar →", que lo consolida como gasto variable cerrado con la cotización de ese momento.
          </p>
        )}
      </div>

      {/* Historial */}
      <div>
        <div className="section-header" style={{ marginBottom: 10 }}>
          <span className="section-title">Historial de compras</span>
          <span className="text-muted" style={{ fontSize: 11 }}>{historial.length} operación{historial.length !== 1 ? 'es' : ''}</span>
        </div>
        {historial.length === 0 ? (
          <div className="card">
            <p className="empty-state" style={{ padding: '24px 0' }}>
              Todavía no hay compras efectivizadas. Definí un objetivo y usá "Efectivizar →".
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {[...historial].reverse().map((op, idx) => (
              <div key={op.id} className="card" style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '14px 18px',
                borderLeft: idx === 0 ? '3px solid var(--accent)' : '3px solid var(--border)',
              }}>
                <div style={{ display: 'flex', gap: 20, alignItems: 'center' }}>
                  <div style={{ textAlign: 'center', minWidth: 48 }}>
                    <div style={{ fontSize: 11, color: 'var(--text-dim)' }}>#{historial.length - idx}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{op.fecha}</div>
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{op.concepto}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                      {fmtUsd(op.objetivoUsd)} · cotización {fmt(op.cotizacion)}
                    </div>
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 800, fontSize: 16, color: 'var(--green)' }}>{fmt(op.montoArs)}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 2 }}>
                    {op.mesImpacto}/{op.anioImpacto}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal confirmación efectivizar */}
      {showConfirm && cotizacion && (
        <div className="modal-overlay" onClick={() => setShowConfirm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Confirmar efectivización</h2>
            <p style={{ fontSize: 14, color: 'var(--text-muted)' }}>
              Vas a registrar la compra de{' '}
              <strong style={{ color: 'var(--text)' }}>{fmtUsd(+objetivoUsd)}</strong>{' '}
              al tipo <strong style={{ color: 'var(--yellow)' }}>{fmt(cotizacion.venta)}</strong>{' '}
              ({tipoCotizacion === 'blue' ? 'Dólar Blue' : 'Dólar MEP'}).
              <br /><br />
              Total a imputar:{' '}
              <strong style={{ color: 'var(--green)', fontSize: 18 }}>{fmt((+objetivoUsd) * cotizacion.venta)}</strong>
            </p>
            <div className="form-group">
              <label className="form-label">Nombre del gasto variable</label>
              <input className="form-input" value={nombreGasto} onChange={e => setNombreGasto(e.target.value)} />
            </div>
            <div className="form-actions">
              <button className="btn btn-ghost" onClick={() => setShowConfirm(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={handleEfectivizar} disabled={efectivizando}>
                {efectivizando ? 'Procesando...' : 'Confirmar y registrar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Main ────────────────────────────────────────────────────────────────────

export default function CalculadorasPage() {
  const { success, error } = useToast()
  const [calculadoras, setCalculadoras] = useState<Calculadora[]>([])
  const [gastosFijos, setGastosFijos] = useState<GastoFijoApi[]>([])
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<Calculadora | null>(null)
  const [saving, setSaving] = useState(false)
  const [applying, setApplying] = useState(false)
  const [showNueva, setShowNueva] = useState(false)

  const [items, setItems] = useState<ItemDraft[]>([])
  const [rutas, setRutas] = useState<RutaDraft[]>([])

  const reload = async () => {
    const [calcs, fijos] = await Promise.all([listarCalculadoras(), listarGastosFijos()])
    setCalculadoras(calcs)
    setGastosFijos(fijos)
    return calcs
  }

  useEffect(() => { reload().finally(() => setLoading(false)) }, [])

  const seleccionar = (calc: Calculadora) => {
    setSelected(calc)
    if (calc.tipo === 'GENERICA') {
      setItems(calc.items.map(i => ({ nombre: i.nombre, cantidad: String(i.cantidad), precioUnitario: String(i.precioUnitario) })))
    } else if (calc.tipo === 'SUBE') {
      setRutas(calc.rutas.map(r => ({
        nombre: r.nombre, viajesPorMes: String(r.viajesPorMes),
        tramos: r.tramos.map(t => ({ nombre: t.nombre, precio: String(t.precio) })),
      })))
    }
  }

  const handleGuardar = async () => {
    if (!selected) return
    setSaving(true)
    try {
      let updated: Calculadora
      if (selected.tipo === 'GENERICA') {
        updated = await actualizarItems(selected.id, items.map(i => ({
          nombre: i.nombre, cantidad: +i.cantidad, precioUnitario: +i.precioUnitario,
        })))
      } else if (selected.tipo === 'SUBE') {
        updated = await actualizarRutas(selected.id, rutas.map(r => ({
          nombre: r.nombre, viajesPorMes: +r.viajesPorMes,
          tramos: r.tramos.map(t => ({ nombre: t.nombre, precio: +t.precio })),
        })))
      } else { return }
      setSelected(updated)
      await reload()
      success('Calculadora guardada')
    } catch (e) { error(await extractError(e)) }
    finally { setSaving(false) }
  }

  const handleAplicar = async () => {
    if (!selected) return
    setApplying(true)
    try {
      await handleGuardar()
      const updated = await aplicarCalculadora(selected.id)
      setSelected(updated)
      await reload()
      success(`Total aplicado a "${selected.gastoFijoNombre}"`)
    } catch (e) { error(await extractError(e)) }
    finally { setApplying(false) }
  }

  const handleCrear = async (nombre: string, tipo: TipoCalculadora, gastoFijoId?: number, nuevoGastoFijoNombre?: string) => {
    try {
      const calc = await crearCalculadora({ nombre, tipo, gastoFijoId, nuevoGastoFijoNombre })
      setShowNueva(false)
      const calcs = await reload()
      const fresh = calcs.find(c => c.id === calc.id) ?? calc
      seleccionar(fresh)
      success(`Calculadora "${nombre}" creada`)
    } catch (e) { error(await extractError(e)) }
  }

  const handleEliminar = async (id: number) => {
    if (!confirm('¿Eliminar esta calculadora?')) return
    try {
      await desactivarCalculadora(id)
      if (selected?.id === id) setSelected(null)
      await reload()
      success('Calculadora eliminada')
    } catch (e) { error(await extractError(e)) }
  }

  const liveTotal = selected
    ? selected.tipo === 'GENERICA' ? calcGenericaTotal(items)
    : selected.tipo === 'SUBE' ? calcSubeTotal(rutas)
    : 0
    : 0

  return (
    <div style={{ display: 'flex', gap: 24, height: '100%' }}>

      {/* Lista lateral */}
      <div style={{ width: 220, flexShrink: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <span style={{ fontWeight: 600, fontSize: 14 }}>Calculadoras</span>
          <button className="btn btn-primary btn-sm" onClick={() => setShowNueva(true)}>+</button>
        </div>
        {loading
          ? <p className="text-muted" style={{ fontSize: 13 }}>Cargando...</p>
          : calculadoras.length === 0
          ? <p className="text-muted" style={{ fontSize: 13 }}>Sin calculadoras.</p>
          : calculadoras.map(c => (
            <div key={c.id} onClick={() => seleccionar(c)} style={{
              padding: '10px 12px', borderRadius: 6, cursor: 'pointer', marginBottom: 4,
              background: selected?.id === c.id ? 'var(--bg-hover)' : 'transparent',
              border: `1px solid ${selected?.id === c.id ? 'var(--accent)' : 'transparent'}`,
            }}>
              <div style={{ fontWeight: 500, fontSize: 13 }}>{c.nombre}</div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                {c.tipo === 'GENERICA' ? 'Genérica' : c.tipo === 'SUBE' ? 'SUBE' : '💵 Ahorro USD'}
                {' · '}
                {c.tipo === 'AHORRO' && c.ahorro
                  ? `${(c.ahorro.historial?.length ?? 0)} ops · ${fmtUsd(c.ahorro.objetivoUsd)}`
                  : fmt(c.totalCalculado)}
              </div>
            </div>
          ))}
      </div>

      {/* Editor */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {!selected ? (
          <div className="card empty-state">
            Seleccioná una calculadora o creá una nueva con el botón +.
          </div>
        ) : (
          <>
            <div className="page-header">
              <div>
                <h2 style={{ fontSize: 18, fontWeight: 700 }}>{selected.nombre}</h2>
                <span className="text-muted" style={{ fontSize: 12 }}>
                  {selected.tipo === 'GENERICA' ? 'Calculadora genérica'
                    : selected.tipo === 'SUBE' ? 'Calculadora SUBE'
                    : 'Calculadora de Ahorro USD'}
                  {selected.gastoFijoNombre && ` · vinculada a "${selected.gastoFijoNombre}"`}
                </span>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-ghost btn-sm" onClick={() => handleEliminar(selected.id)}>Eliminar</button>
                {selected.tipo !== 'AHORRO' && (
                  <>
                    <button className="btn btn-ghost" onClick={handleGuardar} disabled={saving}>
                      {saving ? 'Guardando...' : 'Guardar'}
                    </button>
                    {selected.gastoFijoId && (
                      <button className="btn btn-primary" onClick={handleAplicar} disabled={applying || saving || liveTotal === 0}
                        title={liveTotal === 0 ? 'Agregá ítems antes de aplicar' : undefined}>
                        {applying ? 'Aplicando...' : `Aplicar → ${selected.gastoFijoNombre}`}
                      </button>
                    )}
                  </>
                )}
              </div>
            </div>

            {selected.tipo !== 'AHORRO' && (
              <div className="summary-card" style={{ marginBottom: 20, display: 'inline-block', minWidth: 200 }}>
                <div className="summary-label">Total calculado</div>
                <div className="summary-value text-green">{fmt(liveTotal)}</div>
              </div>
            )}

            {selected.tipo === 'GENERICA' && (
              <div className="card">
                <div className="section-header">
                  <span className="section-title">Ítems</span>
                  <button className="btn btn-ghost btn-sm" onClick={() =>
                    setItems(prev => [...prev, { nombre: '', cantidad: '1', precioUnitario: '' }])}>
                    + Agregar ítem
                  </button>
                </div>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Nombre</th>
                        <th style={{ width: 90 }}>Cantidad</th>
                        <th style={{ width: 140 }}>Precio unitario</th>
                        <th className="text-right" style={{ width: 110 }}>Subtotal</th>
                        <th style={{ width: 40 }}></th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.length === 0 && (
                        <tr><td colSpan={5} className="empty-state">Agregá ítems para calcular.</td></tr>
                      )}
                      {items.map((item, idx) => (
                        <tr key={idx}>
                          <td><input className="form-input" value={item.nombre} placeholder="Ej: Yerba Pajarito"
                            onChange={e => setItems(prev => prev.map((it, i) => i === idx ? { ...it, nombre: e.target.value } : it))} /></td>
                          <td><input className="form-input" type="number" min="0" value={item.cantidad}
                            onChange={e => setItems(prev => prev.map((it, i) => i === idx ? { ...it, cantidad: e.target.value } : it))} /></td>
                          <td><input className="form-input" type="number" min="0" value={item.precioUnitario} placeholder="$"
                            onChange={e => setItems(prev => prev.map((it, i) => i === idx ? { ...it, precioUnitario: e.target.value } : it))} /></td>
                          <td className="text-right">
                            {(+item.cantidad || 0) * (+item.precioUnitario || 0) > 0
                              ? fmt((+item.cantidad) * (+item.precioUnitario))
                              : <span className="text-muted">—</span>}
                          </td>
                          <td><button className="btn btn-ghost btn-sm"
                            onClick={() => setItems(prev => prev.filter((_, i) => i !== idx))}>✕</button></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {selected.tipo === 'SUBE' && (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <span className="section-title">Rutas / Recorridos</span>
                  <button className="btn btn-ghost btn-sm" onClick={() =>
                    setRutas(prev => [...prev, { nombre: '', viajesPorMes: '20', tramos: [] }])}>
                    + Agregar ruta
                  </button>
                </div>
                <div className="card" style={{ marginBottom: 12, fontSize: 12, color: 'var(--text-muted)' }}>
                  <strong style={{ color: 'var(--text)' }}>Descuentos Red SUBE (combinación 2 hs): </strong>
                  <span style={{ color: 'var(--green)', fontWeight: 600 }}>1° 100%</span>{' · '}
                  <span style={{ color: 'var(--yellow)', fontWeight: 600 }}>2° 50%</span>{' · '}
                  <span style={{ color: 'var(--accent)', fontWeight: 600 }}>3°+ 25%</span>
                </div>
                {rutas.map((ruta, ri) => (
                  <div key={ri} className="card" style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 12 }}>
                      <input className="form-input" value={ruta.nombre} placeholder="Ej: Facultad ida" style={{ flex: 1 }}
                        onChange={e => setRutas(prev => prev.map((r, i) => i === ri ? { ...r, nombre: e.target.value } : r))} />
                      <label className="form-label" style={{ marginBottom: 0, flexShrink: 0 }}>Viajes/mes</label>
                      <input className="form-input" type="number" min="1" value={ruta.viajesPorMes} style={{ width: 80 }}
                        onChange={e => setRutas(prev => prev.map((r, i) => i === ri ? { ...r, viajesPorMes: e.target.value } : r))} />
                      <button className="btn btn-ghost btn-sm"
                        onClick={() => setRutas(prev => prev.filter((_, i) => i !== ri))}>✕</button>
                    </div>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr>
                          {['#', 'Medio', 'Tarifa', 'Desc.', 'Efectivo', ''].map((h, i) => (
                            <th key={i} style={{ padding: '6px 8px', fontSize: 11, color: 'var(--text-muted)', textAlign: i >= 2 && i <= 4 ? 'right' : 'left' }}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {ruta.tramos.map((tramo, ti) => {
                          const factor = ti === 0 ? 1 : ti === 1 ? 0.5 : 0.25
                          const efectivo = (+tramo.precio || 0) * factor
                          return (
                            <tr key={ti}>
                              <td style={{ padding: '6px 8px', color: 'var(--text-muted)', fontSize: 12 }}>{ti + 1}°</td>
                              <td style={{ padding: '6px 8px' }}>
                                <input className="form-input" value={tramo.nombre} placeholder="Colectivo / Subte"
                                  onChange={e => setRutas(prev => prev.map((r, i) => i === ri
                                    ? { ...r, tramos: r.tramos.map((t, j) => j === ti ? { ...t, nombre: e.target.value } : t) } : r))} />
                              </td>
                              <td style={{ padding: '6px 8px' }}>
                                <input className="form-input" type="number" min="0" value={tramo.precio} placeholder="$"
                                  style={{ textAlign: 'right' }}
                                  onChange={e => setRutas(prev => prev.map((r, i) => i === ri
                                    ? { ...r, tramos: r.tramos.map((t, j) => j === ti ? { ...t, precio: e.target.value } : t) } : r))} />
                              </td>
                              <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 700, color: factorColor(ti), fontSize: 12 }}>
                                {factorLabel(ti)}
                              </td>
                              <td style={{ padding: '6px 8px', textAlign: 'right', fontSize: 13 }}>
                                {efectivo > 0 ? fmt(efectivo) : <span className="text-muted">—</span>}
                              </td>
                              <td style={{ padding: '6px 8px' }}>
                                <button className="btn btn-ghost btn-sm"
                                  onClick={() => setRutas(prev => prev.map((r, i) => i === ri
                                    ? { ...r, tramos: r.tramos.filter((_, j) => j !== ti) } : r))}>✕</button>
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                    <div style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <button className="btn btn-ghost btn-sm" onClick={() =>
                        setRutas(prev => prev.map((r, i) => i === ri
                          ? { ...r, tramos: [...r.tramos, { nombre: '', precio: '' }] } : r))}>
                        + Tramo
                      </button>
                      {ruta.tramos.length > 0 && (
                        <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                          Por viaje: {fmt(ruta.tramos.reduce((s, t, ti2) => s + (+t.precio || 0) * (ti2 === 0 ? 1 : ti2 === 1 ? 0.5 : 0.25), 0))}
                          {' · '}<strong>{ruta.viajesPorMes} viajes = {fmt(
                            ruta.tramos.reduce((s, t, ti2) => s + (+t.precio || 0) * (ti2 === 0 ? 1 : ti2 === 1 ? 0.5 : 0.25), 0) * (+ruta.viajesPorMes || 0)
                          )}/mes</strong>
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {selected.tipo === 'AHORRO' && (
              <AhorroEditor
                calc={selected}
                onUpdate={updated => { setSelected(updated); reload() }}
              />
            )}
          </>
        )}
      </div>

      {showNueva && (
        <NuevaCalculadoraModal
          gastosFijos={gastosFijos}
          onClose={() => setShowNueva(false)}
          onCreate={handleCrear}
        />
      )}
    </div>
  )
}
