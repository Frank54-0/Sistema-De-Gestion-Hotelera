import { FiCalendar } from 'react-icons/fi'
import { FaBed } from 'react-icons/fa'
import './Disponibilidad.css'

const planta1 = [
  { num: 101, tipo: 'Suite King', estado: 'Lista', nota: 'Reservar / Check-in' },
  { num: 102, tipo: null, estado: 'Ocupada', nota: 'J. Doe · Salida: Mañana' },
  { num: 103, tipo: null, estado: 'Limpieza', nota: 'En curso · Estimado: 30 mins' },
  { num: 104, tipo: 'Doble Queen', estado: 'Lista', nota: 'Reservar / Check-in' },
  { num: 105, tipo: 'Doble Queen', estado: 'Lista', nota: 'Reservar / Check-in' },
  { num: 106, tipo: null, estado: 'Incidencia', nota: 'Fontanería · Bloqueada' },
]

const planta2 = [
  { num: 201, tipo: null, estado: 'Ocupada', nota: 'A. Smith · Salida: 26 Oct' },
  { num: 202, tipo: null, estado: 'Ocupada', nota: 'M. Johnson · Salida: 28 Oct' },
  { num: 203, tipo: 'Ático', estado: 'Lista', nota: 'Reservar / Check-in' },
]

const badgeClass = {
  'Lista': 'badge-info',
  'Ocupada': 'badge-danger',
  'Limpieza': 'badge-neutral',
  'Incidencia': 'badge-neutral',
}

function RoomCard({ room }) {
  return (
    <div className="room-card">
      <div className="room-card-top">
        <span className="room-num">{room.num}</span>
        <span className={`badge ${badgeClass[room.estado]}`}>{room.estado}</span>
      </div>
      {room.tipo && (
        <div className="cell-subtitle">
          <FaBed style={{ marginRight: 4 }} />{room.tipo}
        </div>
      )}
      <div className="room-note">{room.nota}</div>
    </div>
  )
}

function Disponibilidad() {
  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Disponibilidad de Habitaciones</h1>
          <p className="page-subtitle">Resumen de estado en tiempo real para operaciones fluidas.</p>
        </div>
        <div className="date-nav">
          <button>‹</button>
          <span><FiCalendar style={{ marginRight: 6 }} />Hoy, 24 Oct</span>
          <button>›</button>
        </div>
      </div>

      <div className="legend">
        <span><i className="dot dot-info" /> Disponible (12)</span>
        <span><i className="dot dot-danger" /> Ocupada (24)</span>
        <span><i className="dot dot-neutral" /> Limpieza (4)</span>
        <span><i className="dot dot-dark" /> Mantenimiento (1)</span>
      </div>

      <h2 className="section-title">Planta 1</h2>
      <div className="rooms-grid">
        {planta1.map((r) => <RoomCard key={r.num} room={r} />)}
      </div>

      <h2 className="section-title">Planta 2 - Premium</h2>
      <div className="rooms-grid">
        {planta2.map((r) => <RoomCard key={r.num} room={r} />)}
      </div>
    </div>
  )
}

export default Disponibilidad