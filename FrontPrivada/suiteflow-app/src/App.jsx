import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Reservas from './pages/Reservas'
import Registro from './pages/Registro'
import Disponibilidad from './pages/Disponibilidad'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/reservas" element={<Reservas />} />
        <Route path="/registro" element={<Registro />} />
        <Route path="/disponibilidad" element={<Disponibilidad />} />
      </Route>
    </Routes>
  )
}

export default App