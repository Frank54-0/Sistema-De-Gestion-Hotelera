package com.sv.grupo5.suiteflow.dto;

/**
 * Resultado de una solicitud de reserva dentro de un lote: trae la reserva creada
 * o el motivo del rechazo.
 */
public class ResultadoSolicitudDTO {

    private final NuevaReservaDTO solicitud;
    private final boolean exitosa;
    private final ReservaResumenDTO reserva;
    private final String motivoRechazo;

    private ResultadoSolicitudDTO(NuevaReservaDTO solicitud, boolean exitosa,
                                  ReservaResumenDTO reserva, String motivoRechazo) {
        this.solicitud = solicitud;
        this.exitosa = exitosa;
        this.reserva = reserva;
        this.motivoRechazo = motivoRechazo;
    }

    /**
     * @param solicitud solicitud procesada
     * @param reserva   reserva creada
     * @return resultado exitoso
     */
    public static ResultadoSolicitudDTO exito(NuevaReservaDTO solicitud, ReservaResumenDTO reserva) {
        return new ResultadoSolicitudDTO(solicitud, true, reserva, null);
    }

    /**
     * @param solicitud     solicitud procesada
     * @param motivoRechazo por qué se rechazó
     * @return resultado rechazado
     */
    public static ResultadoSolicitudDTO rechazo(NuevaReservaDTO solicitud, String motivoRechazo) {
        return new ResultadoSolicitudDTO(solicitud, false, null, motivoRechazo);
    }

    public NuevaReservaDTO getSolicitud() {
        return solicitud;
    }

    public boolean isExitosa() {
        return exitosa;
    }

    /** @return resumen de la reserva creada, o null si fue rechazada */
    public ReservaResumenDTO getReserva() {
        return reserva;
    }

    /** @return motivo del rechazo, o null si fue exitosa */
    public String getMotivoRechazo() {
        return motivoRechazo;
    }
}
