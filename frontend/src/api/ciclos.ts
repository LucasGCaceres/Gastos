import api from './client'
import type { CicloListItem, CicloResumen } from '../types'

export const listarCiclos = () =>
  api.get<CicloListItem[]>('/ciclos').then(r => r.data)

export const obtenerResumen = (id: number) =>
  api.get<CicloResumen>(`/ciclos/${id}/resumen`).then(r => r.data)

export const crearCiclo = (anio: number, mes: number) =>
  api.post<CicloResumen>('/ciclos', { anio, mes }).then(r => r.data)

export const actualizarIngresos = (id: number, totalIngresos: number) =>
  api.patch<CicloResumen>(`/ciclos/${id}/ingresos`, { totalIngresos }).then(r => r.data)

export const cerrarCiclo = (id: number) =>
  api.post<CicloResumen>(`/ciclos/${id}/cerrar`).then(r => r.data)

export const reabrirCiclo = (id: number) =>
  api.post<CicloResumen>(`/ciclos/${id}/reabrir`).then(r => r.data)

export const registrarGastoVariable = (
  cicloId: number,
  data: { categoriaId: number; concepto: string; monto: number; fecha: string }
) => api.post(`/ciclos/${cicloId}/gastos-variables`, data).then(r => r.data)

export const editarGastoVariable = (
  cicloId: number,
  gastoId: number,
  data: { categoriaId: number; concepto: string; monto: number; fecha: string }
) => api.patch(`/ciclos/${cicloId}/gastos-variables/${gastoId}`, data).then(r => r.data)

export const eliminarGastoVariable = (cicloId: number, gastoId: number) =>
  api.delete(`/ciclos/${cicloId}/gastos-variables/${gastoId}`)

export const registrarCompra = (data: {
  tarjetaId: number
  concepto: string
  montoOriginal: number
  cantidadCuotas: number
  cuotasYaAbonadas?: number
  fechaCompra: string
  monedaOriginal: string
  cotizacionAplicada?: number
}) => api.post('/compras-tarjeta', data).then(r => r.data)

export interface CompraDetalle {
  id: number
  tarjetaId: number
  tarjeta: string
  concepto: string
  fechaCompra: string
  monedaOriginal: string
  montoOriginal: number
  cotizacionAplicada: number
  montoEnPesos: number
  cantidadCuotas: number
  cuotasYaAbonadas: number
}

export const getCompra = (id: number) =>
  api.get<CompraDetalle>(`/compras-tarjeta/${id}`).then(r => r.data)

export const editarCompra = (id: number, data: {
  tarjetaId: number
  concepto: string
  montoOriginal: number
  cantidadCuotas: number
  cuotasYaAbonadas?: number
  fechaCompra: string
  monedaOriginal: string
  cotizacionAplicada?: number
}) => api.put<CompraDetalle>(`/compras-tarjeta/${id}`, data).then(r => r.data)

export const eliminarCompra = (compraId: number) =>
  api.delete(`/compras-tarjeta/${compraId}`)

export const actualizarEstadoCuota = (cuotaId: number, estado: 'PENDIENTE' | 'PAGADO') =>
  api.patch(`/compras-tarjeta/cuotas/${cuotaId}/estado`, { estado }).then(r => r.data)

export const resetearDB = () =>
  api.post('/dev/reset')
