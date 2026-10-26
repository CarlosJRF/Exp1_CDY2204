package cl.duoc.eventos.controller;

import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.CompraRepository;
import cl.duoc.eventos.repository.EventoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoRepository eventos;
    private final CompraRepository compras;

    public EventoController(EventoRepository eventos, CompraRepository compras) {
        this.eventos = eventos;
        this.compras = compras;
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

    private Evento buscar(Long id) {
        return eventos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }
}
