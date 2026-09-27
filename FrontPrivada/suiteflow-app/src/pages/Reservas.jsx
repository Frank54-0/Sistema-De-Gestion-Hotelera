import './Reservas.css'

const reservas = [
  { id: '#SF-8924', nombre: 'Eleanor Astor', tag: 'Huésped VIP', hab: 'Suite 402', habTag: 'Vista al Océano', fechas: '12 Oct - 15 Oct', noches: '3 Noches', total: '$1,450.00', estado: 'Confirmada' },
  { id: '#SF-8925', nombre: 'James Davies', tag: 'Corporativo', hab: 'Room 215', habTag: 'King Clásica', fechas: '13 Oct - 14 Oct', noches: '1 Noche', total: '$320.00', estado: 'Pendiente' },
  { id: '#SF-8922', nombre: 'Sarah Lin', tag: 'Reserva Directa', hab: 'Suite 501', habTag: 'Penthouse', fechas: '10 Oct - 15 Oct', noches: '5 Noches', total: '$3,250.00', estado: 'Check-in realizado' },
  { id: '#SF-8920', nombre: 'Marcus Reed', tag: 'Reserva OTA', hab: 'Room 112', habTag: 'Queen Estándar', fechas: '14 Oct - 16 Oct', noches: '2 Noches', total: '$480.00', estado: 'Cancelada' },
]

const badgeClass = {
  'Confirmada': 'badge-success',
  'Pendiente': 'badge-warning',
  'Check-in realizado': 'badge-info',
  'Cancelada': 'badge-danger',
}

function Reservas() {
  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Reservas</h1>
          <p className="page-subtitle">Administra todas las reservas de huéspedes actuales y entrantes.</p>
        </div>
      </div>

      <div className="filters-bar">
        <input type="text" placeholder="mm/dd/yyyy" className="filter-input" readOnly />
        <select className="filter-input">
          <option>Todos los estados</option>
        </select>
        <input type="text" placeholder="Buscar por nombre o ID..." className="filter-input filter-search" />
        <button className="btn-primary">+ Nueva reserva</button>
      </div>

      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>ID Reserva</th>
              <th>Huésped</th>
              <th>Habitación</th>
              <th>Fechas</th>
              <th>Total</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {reservas.map((r) => (
              <tr key={r.id}>
                <td className="cell-strong">{r.id}</td>
                <td>
                  <div className="huesped-cell">
                    <div className="avatar-sm">{r.nombre.split(' ').map(w => w[0]).join('').slice(0,2)}</div>
                    <div>
                      <div className="cell-title">{r.nombre}</div>
                      <div className="cell-subtitle">{r.tag}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="cell-title">{r.hab}</div>
                  <div className="cell-subtitle">{r.habTag}</div>
                </td>
                <td>
                  <div>{r.fechas}</div>
                  <div className="cell-subtitle">{r.noches}</div>
                </td>
                <td className="cell-strong">{r.total}</td>
                <td><span className={`badge ${badgeClass[r.estado]}`}>{r.estado}</span></td>
                <td className="cell-action">⋮</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="pagination">
          <span>Mostrando 1 a 4 de 48 registros</span>
          <div className="pagination-controls">
            <button>‹</button>
            <button className="active">1</button>
            <button>2</button>
            <button>3</button>
            <span>…</span>
            <button>›</button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Reservas