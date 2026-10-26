package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.CompraRequest;
import cl.duoc.eventos.dto.CompraResponse;
import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.CompraRepository;
import cl.duoc.eventos.repository.EventoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraRepository compras;
    private final EventoRepository eventos;

    public CompraController(CompraRepository compras, EventoRepository eventos) {
        this.compras = compras;
        this.eventos = eventos;
    }

    @GetMapping
    public List<CompraResponse> listar() {
        return compras.findAll().stream().map(CompraResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return compras.findById(id).map(CompraResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra no encontrada"));
    }

    @PostMapping
    public ResponseEntity<CompraResponse> comprar(@Valid @RequestBody CompraRequest request) {
        Evento evento = eventos.findById(request.eventoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        Compra compra = new Compra();
        compra.setEvento(evento);
        compra.setCantidad(request.cantidad());
        compra.setPrecioUnitario(evento.getPrecio());
        compra.setPrecioTotal(evento.getPrecio().multiply(BigDecimal.valueOf(request.cantidad())));
        compra.setFechaCompra(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(CompraResponse.from(compras.save(compra)));
    }
}
