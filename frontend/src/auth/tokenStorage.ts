const TOKEN_KEY = 'gastos-token'
const USERNAME_KEY = 'gastos-username'

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUsername() {
  return localStorage.getItem(USERNAME_KEY)
}

export function setStoredAuth(token: string, username: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}
