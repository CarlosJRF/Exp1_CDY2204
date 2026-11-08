package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.EstadisticasGeneralesResponse;
import cl.duoc.eventos.dto.EstadisticasResponse;
import cl.duoc.eventos.dto.VentasEventoResponse;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.CompraRepository;
import cl.duoc.eventos.repository.EventoRepository;
import cl.duoc.eventos.repository.TicketRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoRepository eventos;
    private final CompraRepository compras;
    private final TicketRepository tickets;

    public EventoController(EventoRepository eventos, CompraRepository compras, TicketRepository tickets) {
        this.eventos = eventos;
        this.compras = compras;
        this.tickets = tickets;
    }

    @GetMapping
    public List<Evento> listar() {
        return eventos.findAll();
    }

    @GetMapping("/{id}")
    public Evento obtener(@PathVariable Long id) {
        return buscar(id);
    }

    @PostMapping
    public ResponseEntity<Evento> crear(@Valid @RequestBody Evento evento) {
        evento.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventos.save(evento));
    }

    @PutMapping("/{id}")
    public Evento actualizar(@PathVariable Long id, @Valid @RequestBody Evento datos) {
        Evento evento = buscar(id);
        evento.setNombre(datos.getNombre());
        evento.setFecha(datos.getFecha());
        evento.setPrecio(datos.getPrecio());
        return eventos.save(evento);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        buscar(id);
        if (compras.existsByEventoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento tiene compras asociadas");
        }
        eventos.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Entradas vendidas y ganancias (ingresos por ventas) de cada evento, más los totales generales. */
    @GetMapping("/estadisticas")
    public EstadisticasGeneralesResponse estadisticasGenerales() {
        List<VentasEventoResponse> detalle = eventos.findAll().stream()
                .map(e -> new VentasEventoResponse(e.getId(), e.getNombre(),
                        compras.sumarEntradas(e.getId()), compras.sumarGanancias(e.getId())))
                .toList();
        long totalEntradas = detalle.stream().mapToLong(VentasEventoResponse::entradasVendidas).sum();
        BigDecimal totalGanancias = detalle.stream().map(VentasEventoResponse::ganancias)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EstadisticasGeneralesResponse(detalle, totalEntradas, totalGanancias);
    }

    @GetMapping("/{id}/estadisticas")
    public EstadisticasResponse estadisticas(@PathVariable Long id) {
        Evento evento = buscar(id);
        return new EstadisticasResponse(id, evento.getNombre(),
                compras.countByEventoId(id), compras.sumarEntradas(id), compras.sumarGanancias(id),
                tickets.countByCompraEventoId(id), tickets.countByCompraEventoIdAndSubidoS3True(id));
    }

    private Evento buscar(Long id) {
        return eventos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }
}
