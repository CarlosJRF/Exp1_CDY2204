package cl.duoc.eventos.controller;

import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventos;

    public EventoController(EventoService eventos) {
        this.eventos = eventos;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventos.listar();
    }

    @GetMapping("/{id}")
    public Evento obtener(@PathVariable Long id) {
        return eventos.buscar(id);
    }

    /** Crea el evento y sube su ticket PDF a S3. */
    @PostMapping
    public ResponseEntity<Evento> crear(@Valid @RequestBody Evento evento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventos.crear(evento));
    }

    /** Actualiza el evento y regenera su ticket PDF en S3. */
    @PutMapping("/{id}")
    public Evento actualizar(@PathVariable Long id, @Valid @RequestBody Evento datos) {
        return eventos.actualizar(id, datos);
    }

    /** Elimina el ticket PDF de S3 y el evento. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eventos.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Descarga el ticket PDF del evento leído desde S3. */
    @GetMapping("/{id}/ticket")
    public ResponseEntity<byte[]> descargarTicket(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evento" + id + "-ticket.pdf\"")
                .body(eventos.descargarTicket(id));
    }
}
