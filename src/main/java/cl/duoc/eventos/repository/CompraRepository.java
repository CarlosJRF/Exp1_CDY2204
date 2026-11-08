package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    boolean existsByEventoId(Long eventoId);
    long countByEventoId(Long eventoId);

    @Query("select coalesce(sum(c.cantidad), 0) from Compra c where c.evento.id = :eventoId")
    long sumarEntradas(Long eventoId);

    @Query("select coalesce(sum(c.precioTotal), 0) from Compra c where c.evento.id = :eventoId")
    BigDecimal sumarGanancias(Long eventoId);
}
