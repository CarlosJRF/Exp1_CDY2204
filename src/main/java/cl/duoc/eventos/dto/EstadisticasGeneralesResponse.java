package cl.duoc.eventos.dto;

import java.math.BigDecimal;
import java.util.List;

/** Detalle de ventas por evento y totales de todos los eventos. */
public record EstadisticasGeneralesResponse(List<VentasEventoResponse> eventos,
                                            long totalEntradasVendidas, BigDecimal totalGanancias) {
}
