import { useState } from 'react'
import { FiUser, FiArrowRight, FiCheckCircle } from 'react-icons/fi'
import { FaBed } from 'react-icons/fa'
import './Registro.css'

const habitaciones = [
  { id: 1, nombre: 'Deluxe King', hab: 'Hab. 304', detalle: '1 Cama', precio: 245, tag: null },
  { id: 2, nombre: 'Superior Twin', hab: 'Hab. 212', detalle: '2 Camas', precio: 210, tag: null },
  { id: 3, nombre: 'Executive Suite', hab: 'Hab. 501', detalle: 'Espacio', precio: 450, tag: 'ASCENSO' },
]

function Registro() {
  const [seleccion, setSeleccion] = useState(1)
  const habitacion = habitaciones.find(h => h.id === seleccion)
  const impuestos = habitacion.precio * 0.15
  const total = habitacion.precio + impuestos

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Registro de Entrada (Walk-in)</h1>
          <p className="page-subtitle">Registro de nuevo huésped y asignación inmediata de habitación.</p>
        </div>
      </div>

      <div className="registro-columns">
        <div className="panel">
          <h2><FiUser style={{ marginRight: 6 }} /> Detalles del Huésped</h2>
          <div className="form-grid">
            <div className="form-field">
              <label>Nombre</label>
              <input placeholder="ej. Eleanor" />
            </div>
            <div className="form-field">
              <label>Apellido</label>
              <input placeholder="ej. Vance" />
            </div>
            <div className="form-field">
              <label>Tipo de Documento</label>
              <select><option>Pasaporte</option></select>
            </div>
            <div className="form-field">
              <label>Número de Documento</label>
              <input placeholder="Ingrese el número de ID" />
            </div>
            <div className="form-field form-field-full">
              <label>Correo Electrónico</label>
              <input placeholder="eleanor.vance@example.com" />
            </div>
            <div className="form-field form-field-full">
              <label>Número de Teléfono</label>
              <input placeholder="+1 (555) 000-0000" />
            </div>
            <div className="form-field form-field-full">
              <label>Solicitudes Especiales / Notas</label>
              <textarea rows={3}></textarea>
            </div>
          </div>
        </div>

        <div className="panel">
          <div className="dates-row">
            <div>
              <label>Entrada</label>
              <div className="cell-strong">Hoy</div>
            </div>
            <span><FiArrowRight /></span>
            <div>
              <label>Salida</label>
              <div className="cell-strong">26 Oct</div>
            </div>
          </div>

          <div className="rooms-header">
            <h2>Habitaciones Disponibles</h2>
            <span className="cell-subtitle">4 listas ahora</span>
          </div>

          {habitaciones.map((h) => (
            <label key={h.id} className={`room-option ${seleccion === h.id ? 'selected' : ''}`}>
              <input
                type="radio"
                name="habitacion"
                checked={seleccion === h.id}
                onChange={() => setSeleccion(h.id)}
              />
              <div className="room-thumb" />
              <div className="room-info">
                <div className="cell-title">
                  {h.nombre} {h.tag && <span className="tag-ascenso">{h.tag}</span>}
                </div>
                <div className="cell-subtitle">
                  <FaBed style={{ marginRight: 4 }} />{h.hab} · {h.detalle}
                </div>
              </div>
              <div className="room-price">${h.precio}<span>/noche</span></div>
            </label>
          ))}

          <div className="summary">
            <div className="summary-row">
              <span>Tarifa de Habitación (1 noche)</span>
              <span>${habitacion.precio.toFixed(2)}</span>
            </div>
            <div className="summary-row">
              <span>Impuestos y Cargos (15%)</span>
              <span>${impuestos.toFixed(2)}</span>
            </div>
            <div className="summary-row summary-total">
              <span>Total</span>
              <span>${total.toFixed(2)}</span>
            </div>
          </div>

          <button className="btn-danger">
            <FiCheckCircle style={{ marginRight: 6 }} /> Registrar y asignar habitación
          </button>
        </div>
      </div>
    </div>
  )
}

export default Registro