import api from './client'
import type { EventoGasto } from '../types'

export const crearEvento = (cicloId: number, data: { nombre: string; fecha?: string }) =>
  api.post<EventoGasto>(`/ciclos/${cicloId}/eventos`, data).then(r => r.data)

export const listarEventos = (cicloId: number) =>
  api.get<EventoGasto[]>(`/ciclos/${cicloId}/eventos`).then(r => r.data)

export const agregarItemEvento = (eventoId: number, data: { concepto: string; monto: number }) =>
  api.post<EventoGasto>(`/eventos/${eventoId}/items`, data).then(r => r.data)

export const editarItemEvento = (eventoId: number, itemId: number, data: { concepto: string; monto: number }) =>
  api.patch<EventoGasto>(`/eventos/${eventoId}/items/${itemId}`, data).then(r => r.data)

export const eliminarItemEvento = (eventoId: number, itemId: number) =>
  api.delete<EventoGasto>(`/eventos/${eventoId}/items/${itemId}`).then(r => r.data)

export const eliminarEvento = (eventoId: number) =>
  api.delete(`/eventos/${eventoId}`)
