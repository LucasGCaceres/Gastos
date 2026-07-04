import api from './client'

export type TipoCalculadora = 'GENERICA' | 'SUBE' | 'AHORRO'

export interface CalculadoraItem {
  id: number; nombre: string; cantidad: number; precioUnitario: number; subtotal: number
}
export interface CalculadoraTramo {
  id: number; nombre: string; precio: number; orden: number; factor: number; precioEfectivo: number
}
export interface CalculadoraRuta {
  id: number; nombre: string; viajesPorMes: number; tramos: CalculadoraTramo[]; costoMensual: number
}
export interface AhorroOperacion {
  id: number; objetivoUsd: number; cotizacion: number; montoArs: number
  concepto: string; fecha: string; mesImpacto: number; anioImpacto: number
}
export interface AhorroData {
  objetivoUsd: number
  historial: AhorroOperacion[]
}
export interface Calculadora {
  id: number; nombre: string; tipo: TipoCalculadora
  gastoFijoId: number | null; gastoFijoNombre: string | null
  totalCalculado: number
  items: CalculadoraItem[]; rutas: CalculadoraRuta[]; ahorro: AhorroData | null
}

export const listarCalculadoras = () =>
  api.get<Calculadora[]>('/calculadoras').then(r => r.data)

export const crearCalculadora = (data: { nombre: string; tipo: TipoCalculadora; gastoFijoId?: number; nuevoGastoFijoNombre?: string }) =>
  api.post<Calculadora>('/calculadoras', data).then(r => r.data)

export const actualizarItems = (id: number, items: { nombre: string; cantidad: number; precioUnitario: number }[]) =>
  api.put<Calculadora>(`/calculadoras/${id}/items`, { items }).then(r => r.data)

export const actualizarRutas = (id: number, rutas: { nombre: string; viajesPorMes: number; tramos: { nombre: string; precio: number }[] }[]) =>
  api.put<Calculadora>(`/calculadoras/${id}/rutas`, { rutas }).then(r => r.data)

export const actualizarAhorro = (id: number, objetivoUsd: number) =>
  api.put<Calculadora>(`/calculadoras/${id}/ahorro`, { objetivoUsd }).then(r => r.data)

export const efectivizarAhorro = (id: number, data: { cotizacionActual: number; nombreGasto: string }) =>
  api.post<Calculadora>(`/calculadoras/${id}/efectivizar`, data).then(r => r.data)

export const aplicarCalculadora = (id: number) =>
  api.post<Calculadora>(`/calculadoras/${id}/aplicar`).then(r => r.data)

export const desactivarCalculadora = (id: number) =>
  api.delete(`/calculadoras/${id}`)
