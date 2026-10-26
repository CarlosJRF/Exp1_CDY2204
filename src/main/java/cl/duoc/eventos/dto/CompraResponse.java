package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resumen de la compra que se devuelve al cliente. */
public record CompraResponse(Long compraId, String evento, int cantidad,
                             BigDecimal precioUnitario, BigDecimal precioTotal,
                             LocalDateTime fechaCompra) {

    public static CompraResponse from(Compra c) {
        return new CompraResponse(c.getId(), c.getEvento().getNombre(), c.getCantidad(),
                c.getPrecioUnitario(), c.getPrecioTotal(), c.getFechaCompra());
    }
}
