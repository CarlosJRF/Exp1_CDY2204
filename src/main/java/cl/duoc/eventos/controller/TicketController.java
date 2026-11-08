package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.ActualizarTicketRequest;
import cl.duoc.eventos.dto.GenerarTicketsRequest;
import cl.duoc.eventos.dto.TicketResponse;
import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.Ticket;
import cl.duoc.eventos.repository.CompraRepository;
import cl.duoc.eventos.repository.TicketRepository;
import cl.duoc.eventos.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository tickets;
    private final CompraRepository compras;
    private final TicketService servicio;

    public TicketController(TicketRepository tickets, CompraRepository compras, TicketService servicio) {
        this.tickets = tickets;
        this.compras = compras;
        this.servicio = servicio;
    }

    @PostMapping("/generar")
    public ResponseEntity<List<TicketResponse>> generar(@Valid @RequestBody GenerarTicketsRequest request) {
        Compra compra = compras.findById(request.compraId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
        if (compra.getUsuario() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La compra no tiene usuario");
        }
        if (tickets.existsByCompraId(compra.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra ya tiene tickets generados");
        }
        List<TicketResponse> creados = servicio.generar(compra).stream().map(TicketResponse::from).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(creados);
    }

    @GetMapping
    public List<TicketResponse> listar(@RequestParam(required = false) String usuario,
                                       @RequestParam(required = false) Long eventoId) {
        return filtrar(usuario, eventoId).stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/subir")
    public TicketResponse subir(@PathVariable Long id) {
        return TicketResponse.from(servicio.subir(buscar(id)));
    }

    /** Descarga en un .zip los tickets subidos a S3, filtrando por usuario, por evento o por ambos. */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(@RequestParam(required = false) String usuario,
                                            @RequestParam(required = false) Long eventoId) {
        if (usuario == null && eventoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique usuario o eventoId");
        }
        List<Ticket> enS3 = filtrar(usuario, eventoId).stream().filter(Ticket::isSubidoS3).toList();
        if (enS3.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay tickets en S3 para el filtro indicado");
        }
        String nombre = "tickets" + (eventoId != null ? "_evento" + eventoId : "")
                + (usuario != null ? "_" + usuario : "") + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(servicio.comprimirDesdeS3(enS3));
    }

    @PutMapping("/{id}")
    public TicketResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarTicketRequest request) {
        return TicketResponse.from(servicio.cambiarUsuario(buscar(id), request.usuario()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(buscar(id));
        return ResponseEntity.noContent().build();
    }

    private List<Ticket> filtrar(String usuario, Long eventoId) {
        if (usuario != null && eventoId != null) return tickets.findByUsuarioAndCompraEventoId(usuario, eventoId);
        if (usuario != null) return tickets.findByUsuario(usuario);
        if (eventoId != null) return tickets.findByCompraEventoId(eventoId);
        return tickets.findAll();
    }

    private Ticket buscar(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }
}
