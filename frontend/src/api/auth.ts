import api from './client'

export interface LoginResponse {
  token: string
  username: string
}

export const login = (username: string, password: string) =>
  api.post<LoginResponse>('/auth/login', { username, password }).then(r => r.data)

export const cambiarPassword = (currentPassword: string, newPassword: string) =>
  api.patch('/auth/password', { currentPassword, newPassword })

export const cambiarUsername = (newUsername: string, currentPassword: string) =>
  api.patch<LoginResponse>('/auth/username', { newUsername, currentPassword }).then(r => r.data)
