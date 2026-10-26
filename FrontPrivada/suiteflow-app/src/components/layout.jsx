import { NavLink, Outlet } from 'react-router-dom'
import { FiBell, FiSettings, FiUser } from 'react-icons/fi'
import './Layout.css'

function Layout() {
  const linkClass = ({ isActive }) => isActive ? 'nav-link active' : 'nav-link'

  return (
    <div className="app-shell">
      <header className="navbar">
        <div className="navbar-brand">SuiteFlow</div>

        <nav className="navbar-links">
          <NavLink to="/" end className={linkClass}>Panel de Control</NavLink>
          <NavLink to="/reservas" className={linkClass}>Reservas</NavLink>
          <NavLink to="/registro" className={linkClass}>Check-in</NavLink>
          <NavLink to="/disponibilidad" className={linkClass}>Disponibilidad</NavLink>
        </nav>

        <div className="navbar-actions">
          <button className="icon-btn"><FiBell /></button>
          <button className="icon-btn"><FiSettings /></button>
          <div className="avatar"><FiUser /></div>
        </div>
      </header>

      <main className="app-content">
        <Outlet />
      </main>

      <footer className="app-footer">
        <span className="footer-brand">SuiteFlow</span>
        <div className="footer-links">
          <a href="#">Soporte</a>
          <a href="#">Política de Privacidad</a>
          <a href="#">Términos de Servicio</a>
        </div>
        <span className="footer-copy">© 2024 SuiteFlow Consola Operativa. Todos los derechos reservados.</span>
      </footer>
    </div>
  )
}

export default Layout