package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByCompraId(Long compraId);
    List<Ticket> findByUsuario(String usuario);
    List<Ticket> findByCompraEventoId(Long eventoId);
    List<Ticket> findByUsuarioAndCompraEventoId(String usuario, Long eventoId);
    long countByCompraEventoId(Long eventoId);
    long countByCompraEventoIdAndSubidoS3True(Long eventoId);
}
