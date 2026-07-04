import api from './client'
import type { Categoria } from '../types'

export const listarCategorias = () =>
  api.get<Categoria[]>('/categorias').then(r => r.data)
