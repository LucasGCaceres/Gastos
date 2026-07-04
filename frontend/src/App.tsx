import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import CiclosPage from './pages/CiclosPage'
import CicloDetallePage from './pages/CicloDetallePage'
import TarjetasPage from './pages/TarjetasPage'
import CalculadorasPage from './pages/CalculadorasPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<CiclosPage />} />
          <Route path="/ciclos/:id" element={<CicloDetallePage />} />
          <Route path="/tarjetas" element={<TarjetasPage />} />
          <Route path="/calculadoras" element={<CalculadorasPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
