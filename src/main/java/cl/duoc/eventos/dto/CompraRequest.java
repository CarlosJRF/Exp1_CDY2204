package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompraRequest(@NotNull Long eventoId, @Min(1) int cantidad) {
}
