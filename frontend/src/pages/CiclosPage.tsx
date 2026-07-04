import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listarCiclos, crearCiclo, reabrirCiclo } from '../api/ciclos'
import { useToast } from '../components/Toast'
import type { CicloListItem } from '../types'

const MESES = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic']
const MESES_LARGO = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
  'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre']

function fmt(n: number) {
  return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(n)
}

function calcularProximo(ciclos: CicloListItem[]) {
  if (ciclos.length === 0) { const h = new Date(); return { anio: h.getFullYear(), mes: h.getMonth() + 1 } }
  const u = ciclos[0]
  const s = new Date(u.anio, u.mes)
  return { anio: s.getFullYear(), mes: s.getMonth() + 1 }
}

export default function CiclosPage() {
  const navigate = useNavigate()
  const { success, error } = useToast()
  const [ciclos, setCiclos] = useState<CicloListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [reabriendo, setReabriendo] = useState<number | null>(null)

  const reload = () => listarCiclos().then(setCiclos)
  useEffect(() => { reload().finally(() => setLoading(false)) }, [])

  const handleCrearProximo = async () => {
    const { anio, mes } = calcularProximo(ciclos)
    setCreating(true)
    try {
      const nuevo = await crearCiclo(anio, mes)
      navigate(`/ciclos/${nuevo.id}`)
    } catch { error('No se pudo crear el ciclo') }
    finally { setCreating(false) }
  }

  const handleReabrir = async (e: React.MouseEvent, ciclo: CicloListItem) => {
    e.stopPropagation()
    if (!confirm(`¿Reabrir ${MESES[ciclo.mes - 1]} ${ciclo.anio}? Se eliminarán los snapshots de gastos fijos de ese mes.`)) return
    setReabriendo(ciclo.id)
    try {
      await reabrirCiclo(ciclo.id)
      await reload()
      success(`Ciclo ${MESES[ciclo.mes - 1]} ${ciclo.anio} reabierto`)
    } catch { error('No se pudo reabrir el ciclo') }
    finally { setReabriendo(null) }
  }

  const proximo = calcularProximo(ciclos)
  const proximoExiste = ciclos.some(c => c.anio === proximo.anio && c.mes === proximo.mes)

  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Ciclos mensuales</h1>
        {!proximoExiste && (
          <button className="btn btn-primary" onClick={handleCrearProximo} disabled={creating || loading}>
            {creating ? 'Creando...' : `+ ${MESES_LARGO[proximo.mes - 1]} ${proximo.anio}`}
          </button>
        )}
      </div>

      <div className="card table-wrap">
        {loading ? (
          <p className="empty-state">Cargando...</p>
        ) : ciclos.length === 0 ? (
          <p className="empty-state">No hay ciclos todavía.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Período</th>
                <th>Estado</th>
                <th className="text-right">Ingresos</th>
                <th className="text-right">Gastos</th>
                <th className="text-right">Saldo</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {ciclos.map(c => {
                const gastos = (c.totalIngresos || 0) - (c.saldoFinal || 0)
                return (
                  <tr key={c.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/ciclos/${c.id}`)}>
                    <td><span className="font-bold">{MESES[c.mes - 1]}</span>{' '}
                      <span className="text-muted">{c.anio}</span></td>
                    <td>
                      <span className={`badge ${c.estado === 'ABIERTO' ? 'badge-open' : 'badge-closed'}`}>
                        {c.estado === 'ABIERTO' ? 'Abierto' : 'Cerrado'}
                      </span>
                    </td>
                    <td className="text-right">{c.totalIngresos > 0 ? fmt(c.totalIngresos) : <span className="text-dim">—</span>}</td>
                    <td className="text-right">
                      {gastos > 0 ? <span className="text-red">{fmt(gastos)}</span> : <span className="text-dim">—</span>}
                    </td>
                    <td className="text-right">
                      <span className={c.saldoFinal >= 0 ? 'text-green font-bold' : 'text-red font-bold'}>
                        {fmt(c.saldoFinal)}
                      </span>
                      {c.estado === 'ABIERTO' && gastos > 0 && (
                        <span className="text-dim text-xs" style={{ marginLeft: 6 }}>proyectado</span>
                      )}
                    </td>
                    <td className="text-right" onClick={e => e.stopPropagation()}>
                      <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                        {c.estado === 'CERRADO' && (
                          <button className="btn btn-ghost btn-xs"
                            disabled={reabriendo === c.id}
                            onClick={e => handleReabrir(e, c)}>
                            {reabriendo === c.id ? '...' : 'Reabrir'}
                          </button>
                        )}
                        <span className="text-dim" style={{ padding: '2px 4px' }}>→</span>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

    </>
  )
}
