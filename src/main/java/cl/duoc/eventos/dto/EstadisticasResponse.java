package cl.duoc.eventos.dto;

import java.math.BigDecimal;

/** Ventas y reservas de un evento (cada compra cuenta como una reserva). */
public record EstadisticasResponse(Long eventoId, String evento, long reservas, long entradasVendidas,
                                   BigDecimal ganancias, long ticketsGenerados, long ticketsSubidosS3) {
}
