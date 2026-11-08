package cl.duoc.eventos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Único dato modificable del ticket: el usuario titular. */
public record ActualizarTicketRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_@-][A-Za-z0-9._@-]*") String usuario) {
}
