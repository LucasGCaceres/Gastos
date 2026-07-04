import api from './client'

export interface GastoFijoApi {
  id: number
  nombre: string
  montoActual: number
  activo: boolean
}

export const listarGastosFijos = () =>
  api.get<GastoFijoApi[]>('/gastos-fijos').then(r => r.data)

export const crearGastoFijo = (data: { nombre: string; montoActual: number }) =>
  api.post<GastoFijoApi>('/gastos-fijos', data).then(r => r.data)

export const editarGastoFijo = (id: number, data: { nombre: string; montoActual: number }) =>
  api.patch<GastoFijoApi>(`/gastos-fijos/${id}`, data).then(r => r.data)

export const desactivarGastoFijo = (id: number) =>
  api.delete(`/gastos-fijos/${id}`)

export const actualizarEstadoGastoFijo = (id: number, activo: boolean) =>
  api.patch<GastoFijoApi>(`/gastos-fijos/${id}/estado`, { activo }).then(r => r.data)
