import api from './client'
import type { Tarjeta } from '../types'

export const listarTarjetas = () =>
  api.get<Tarjeta[]>('/tarjetas').then(r => r.data)

export const listarTodasTarjetas = () =>
  api.get<Tarjeta[]>('/tarjetas/todas').then(r => r.data)

export const actualizarEstadoTarjeta = (id: number, activa: boolean) =>
  api.patch<Tarjeta>(`/tarjetas/${id}/estado`, { activa }).then(r => r.data)

export interface TarjetaDeuda {
  tarjetaId: number
  nombre: string
  banco: string | null
  totalAdeudado: number
}

export const getDesgloseDeudas = (anio: number, mes: number) =>
  api.get<TarjetaDeuda[]>('/tarjetas/deudas', { params: { anio, mes } }).then(r => r.data)

export const crearTarjeta = (data: {
  nombre: string
  banco: string
  diaCierreEstimado: number
  diaVencimientoEstimado: number
}) => api.post<Tarjeta>('/tarjetas', data).then(r => r.data)

export const editarTarjeta = (id: number, data: {
  nombre: string; banco: string; diaCierreEstimado: number; diaVencimientoEstimado: number
}) => api.put<Tarjeta>(`/tarjetas/${id}`, data).then(r => r.data)

export const eliminarTarjeta = (id: number) =>
  api.delete(`/tarjetas/${id}`)

export interface CierreMes {
  id: number
  tarjetaId: number
  anio: number
  mes: number
  fechaCierreReal: string
  fechaVencimientoReal: string
}

export const getCierreMes = (tarjetaId: number, anio: number, mes: number) =>
  api.get<CierreMes>(`/tarjetas/${tarjetaId}/cierres`, { params: { anio, mes } })
    .then(r => r.data).catch(() => null)

export const setCierreMes = (tarjetaId: number, data: {
  anio: number; mes: number; fechaCierreReal: string; fechaVencimientoReal: string
}) => api.put<CierreMes>(`/tarjetas/${tarjetaId}/cierres`, data).then(r => r.data)
