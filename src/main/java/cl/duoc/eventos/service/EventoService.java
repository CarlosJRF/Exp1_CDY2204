package cl.duoc.eventos.service;

import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.EventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD de eventos. Cada escritura en BD va junto con la operación en S3:
 * si S3 falla se lanza ResponseStatusException (502) y la transacción hace rollback.
 */
@Service
public class EventoService {

    private final EventoRepository eventos;
    private final TicketPdfService tickets;

    public EventoService(EventoRepository eventos, TicketPdfService tickets) {
        this.eventos = eventos;
        this.tickets = tickets;
    }

    public List<Evento> listar() {
        return eventos.findAll();
    }

    public Evento buscar(Long id) {
        return eventos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    @Transactional
    public Evento crear(Evento evento) {
        evento.setId(null);
        Evento guardado = eventos.saveAndFlush(evento); // primero se guarda para tener el id de la clave S3
        tickets.subir(guardado);
        return guardado;
    }

    @Transactional
    public Evento actualizar(Long id, Evento datos) {
        Evento evento = buscar(id);
        evento.setNombre(datos.getNombre());
        evento.setFecha(datos.getFecha());
        evento.setPrecio(datos.getPrecio());
        Evento guardado = eventos.saveAndFlush(evento);
        tickets.subir(guardado); // regenera y sobrescribe el PDF
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        tickets.borrar(id);
        eventos.deleteById(id);
    }

    public byte[] descargarTicket(Long id) {
        buscar(id);
        return tickets.descargar(id);
    }
}
