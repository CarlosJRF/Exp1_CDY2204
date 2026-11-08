package cl.duoc.eventos.dto;

import jakarta.validation.constraints.NotNull;

public record GenerarTicketsRequest(@NotNull Long compraId) {
}
