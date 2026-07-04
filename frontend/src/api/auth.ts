import api from './client'

export interface LoginResponse {
  token: string
  username: string
}

export const login = (username: string, password: string) =>
  api.post<LoginResponse>('/auth/login', { username, password }).then(r => r.data)
