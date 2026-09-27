import './Dashboard.css'

const agenda = [
  { nombre: 'Eleanor Vance', tag: 'VIP · Frecuente', hab: 402, hora: '14:30', estado: 'Esperado', accion: 'Revisar' },
  { nombre: 'Arthur Pendelton', tag: 'Corporativo', hab: 215, hora: '15:00', estado: 'Llegó', accion: 'Registrar entrada' },
  { nombre: 'Sofia Rossi', tag: 'Ocio', hab: 308, hora: '16:45', estado: 'Esperado', accion: 'Revisar' },
]

function Dashboard() {
  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Resumen</h1>
          <p className="page-subtitle">Estado operativo para hoy.</p>
        </div>
        <button className="btn-primary">+ Huésped sin reserva</button>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <span className="stat-label">Reservas de hoy</span>
          <span className="stat-value">24</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Check-ins pendientes</span>
          <span className="stat-value stat-danger">12</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Habitaciones disponibles</span>
          <span className="stat-value">05</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Ocupación</span>
          <span className="stat-value">92%</span>
        </div>
      </div>

      <div className="dashboard-columns">
        <div className="panel">
          <div className="panel-header">
            <h2>Agenda de hoy</h2>
            <div className="tabs">
              <button className="tab active">Llegadas</button>
              <button className="tab">Salidas</button>
            </div>
          </div>

          <table className="table">
            <thead>
              <tr>
                <th>Huésped</th>
                <th>Habitación</th>
                <th>Llegada est.</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {agenda.map((row) => (
                <tr key={row.nombre}>
                  <td>
                    <div className="cell-title">{row.nombre}</div>
                    <div className="cell-subtitle">{row.tag}</div>
                  </td>
                  <td className="cell-strong">{row.hab}</td>
                  <td>{row.hora}</td>
                  <td>
                    <span className={`badge ${row.estado === 'Llegó' ? 'badge-info' : 'badge-neutral'}`}>
                      {row.estado}
                    </span>
                  </td>
                  <td className="cell-action">
                    {row.accion === 'Revisar'
                      ? <a href="#" className="link">Revisar</a>
                      : <button className="btn-small">Registrar entrada</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <a href="#" className="link view-all">Ver las 12 llegadas</a>
        </div>

        <div className="side-column">
          <div className="panel">
            <h2>Búsqueda rápida</h2>
            <input className="search-input" placeholder="N° Reserva o Nombre" />
          </div>

          <div className="panel">
            <h2>Alertas de limpieza</h2>
            <div className="alert alert-danger">
              <strong>Urgencia Habitación 402</strong>
              <p>VIP llega a las 14:30. Necesita inspección final.</p>
            </div>
            <div className="alert alert-neutral">
              <strong>3 Habitaciones pendientes</strong>
              <p>Salidas completadas, limpieza en curso.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard