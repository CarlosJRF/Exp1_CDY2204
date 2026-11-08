package cl.duoc.eventos.dto;

import java.math.BigDecimal;

/** Ventas de un evento: ganancias = suma de los totales de sus compras. */
public record VentasEventoResponse(Long eventoId, String evento, long entradasVendidas, BigDecimal ganancias) {
}
