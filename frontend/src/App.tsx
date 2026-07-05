import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import LoginPage from './pages/LoginPage'
import CiclosPage from './pages/CiclosPage'
import CicloDetallePage from './pages/CicloDetallePage'
import TarjetasPage from './pages/TarjetasPage'
import CalculadorasPage from './pages/CalculadorasPage'
import ConfiguracionPage from './pages/ConfiguracionPage'
import { useTheme } from './hooks/useTheme'

export default function App() {
  // Aplica el tema guardado apenas arranca la app, sin depender de qué página se monte primero.
  useTheme()

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route path="/" element={<CiclosPage />} />
            <Route path="/ciclos/:id" element={<CicloDetallePage />} />
            <Route path="/tarjetas" element={<TarjetasPage />} />
            <Route path="/calculadoras" element={<CalculadorasPage />} />
            <Route path="/configuracion" element={<ConfiguracionPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
