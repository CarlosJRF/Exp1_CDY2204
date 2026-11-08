package cl.duoc.eventos.service;

import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.Ticket;
import cl.duoc.eventos.repository.TicketRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manejo de los archivos de tickets:
 * EFS (almacenamiento temporal) en {efs}/tickets y S3 (almacenamiento definitivo).
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository tickets;
    private final S3Client s3;
    private final Path generados;
    private final String bucket;

    public TicketService(TicketRepository tickets, S3Client s3,
                         @Value("${app.efs.path}") String efsPath,
                         @Value("${app.s3.bucket}") String bucket) {
        this.tickets = tickets;
        this.s3 = s3;
        this.generados = Path.of(efsPath, "tickets");
        this.bucket = bucket;
    }

    /** Genera un PDF por entrada en el EFS y lo sube a S3 automáticamente. */
    public List<Ticket> generar(Compra compra) {
        List<Ticket> creados = new ArrayList<>();
        for (int i = 0; i < compra.getCantidad(); i++) {
            Ticket ticket = new Ticket();
            ticket.setCompra(compra);
            ticket.setUsuario(compra.getUsuario());
            ticket.setFechaGeneracion(LocalDateTime.now());
            ticket = tickets.save(ticket); // primero se guarda para tener el id que va en el nombre del archivo
            escribirPdf(ticket);
            intentarSubir(ticket);
            creados.add(tickets.save(ticket));
        }
        return creados;
    }

    /** Sube (o vuelve a subir) el PDF que está en el EFS. */
    public Ticket subir(Ticket ticket) {
        Path archivo = Path.of(ticket.getRutaEfs());
        if (!Files.exists(archivo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF no encontrado en EFS: " + archivo);
        }
        if (bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3_BUCKET no está configurado");
        }
        try {
            s3.putObject(b -> b.bucket(bucket).key(ticket.getClaveS3()).contentType("application/pdf"),
                    RequestBody.fromFile(archivo));
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al subir a S3: " + e.getMessage());
        }
        ticket.setSubidoS3(true);
        return tickets.save(ticket);
    }

    /** Arma un .zip con los PDF leídos desde S3, conservando la estructura evento{id}/{usuario}/ticket{nnn}.pdf. */
    public byte[] comprimirDesdeS3(List<Ticket> lista) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Ticket ticket : lista) {
                zip.putNextEntry(new ZipEntry(ticket.getClaveS3()));
                try (InputStream pdf = s3.getObject(b -> b.bucket(bucket).key(ticket.getClaveS3()))) {
                    pdf.transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al descargar de S3: " + e.getMessage());
        }
        return bytes.toByteArray();
    }

    /** Cambia el usuario titular: regenera el PDF en la nueva ruta y borra el archivo y el objeto anteriores. */
    public Ticket cambiarUsuario(Ticket ticket, String usuario) {
        Path rutaAnterior = Path.of(ticket.getRutaEfs());
        String claveAnterior = ticket.getClaveS3();
        boolean estabaEnS3 = ticket.isSubidoS3();

        ticket.setUsuario(usuario);
        ticket.setSubidoS3(false);
        escribirPdf(ticket);
        if (!rutaAnterior.toString().equals(ticket.getRutaEfs())) {
            borrarArchivo(rutaAnterior);
            if (estabaEnS3) {
                try {
                    s3.deleteObject(b -> b.bucket(bucket).key(claveAnterior));
                } catch (SdkException e) {
                    log.warn("No se pudo borrar {} de S3: {}", claveAnterior, e.getMessage());
                }
            }
        }
        intentarSubir(ticket);
        return tickets.save(ticket);
    }

    /** Elimina el ticket de S3, del EFS y de la base de datos. */
    public void eliminar(Ticket ticket) {
        if (ticket.isSubidoS3()) {
            try {
                s3.deleteObject(b -> b.bucket(bucket).key(ticket.getClaveS3()));
            } catch (SdkException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al borrar de S3: " + e.getMessage());
            }
        }
        borrarArchivo(Path.of(ticket.getRutaEfs()));
        tickets.delete(ticket);
    }

    /** La subida automática no detiene la generación: si falla, queda subidoS3=false para reintentar. */
    private void intentarSubir(Ticket ticket) {
        try {
            subir(ticket);
        } catch (ResponseStatusException e) {
            log.warn("Ticket {} no se subió a S3: {}", ticket.getId(), e.getReason());
        }
    }

    /** Ruta: evento{id}/{usuario}/ticket{id}.pdf, igual en el EFS y como clave en S3. */
    private void escribirPdf(Ticket ticket) {
        Compra compra = ticket.getCompra();
        String clave = String.format("evento%d/%s/ticket%03d.pdf",
                compra.getEvento().getId(), ticket.getUsuario(), ticket.getId());
        Path destino = generados.resolve(clave);

        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                cs.beginText();
                cs.newLineAtOffset(60, 720);
                cs.setLeading(22);
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                cs.showText("Ticket de entrada");
                cs.newLine();
                cs.newLine();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13);
                for (String linea : List.of(
                        "Evento: " + compra.getEvento().getNombre(),
                        "Fecha: " + compra.getEvento().getFecha(),
                        "Titular: " + ticket.getUsuario(),
                        "Ticket N°: " + ticket.getId(),
                        "Compra N°: " + compra.getId(),
                        "Precio: $" + compra.getPrecioUnitario(),
                        "Generado: " + LocalDateTime.now().withNano(0))) {
                    cs.showText(linea);
                    cs.newLine();
                }
                cs.endText();
            }
            Files.createDirectories(destino.getParent());
            doc.save(destino.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ticket.setRutaEfs(destino.toString());
        ticket.setClaveS3(clave);
    }

    private void borrarArchivo(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
