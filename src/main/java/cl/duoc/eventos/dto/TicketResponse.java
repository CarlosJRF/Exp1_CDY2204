package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.Ticket;

import java.time.LocalDateTime;

public record TicketResponse(Long ticketId, Long compraId, Long eventoId, String evento, String usuario,
                             String rutaEfs, String claveS3, boolean subidoS3, LocalDateTime fechaGeneracion) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getCompra().getId(), t.getCompra().getEvento().getId(),
                t.getCompra().getEvento().getNombre(), t.getUsuario(), t.getRutaEfs(), t.getClaveS3(),
                t.isSubidoS3(), t.getFechaGeneracion());
    }
}
