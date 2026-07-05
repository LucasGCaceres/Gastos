import api from './client'
import type { Categoria } from '../types'

export const listarCategorias = () =>
  api.get<Categoria[]>('/categorias').then(r => r.data)

export const crearCategoria = (data: { nombre: string; icono?: string }) =>
  api.post<Categoria>('/categorias', data).then(r => r.data)

export const editarCategoria = (id: number, data: { nombre: string; icono?: string }) =>
  api.patch<Categoria>(`/categorias/${id}`, data).then(r => r.data)

export const eliminarCategoria = (id: number) =>
  api.delete(`/categorias/${id}`)
