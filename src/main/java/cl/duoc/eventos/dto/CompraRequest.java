package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** El usuario forma parte de la ruta del ticket en EFS/S3, por eso solo admite caracteres seguros. */
public record CompraRequest(@NotNull Long eventoId,
                            @NotBlank @Pattern(regexp = "[A-Za-z0-9_@-][A-Za-z0-9._@-]*") String usuario,
                            @Min(1) int cantidad) {
}
