package cl.duoc.eventos.service;

import cl.duoc.eventos.model.Evento;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ticket PDF de cada evento: se genera en memoria con PDFBox y se guarda en S3 con la clave evento{id}/ticket.pdf.
 * Es la única clase que toca S3.
 */
@Service
public class TicketPdfService {

    private final S3Client s3;
    private final String bucket;

    public TicketPdfService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public static String clave(Long eventoId) {
        return "evento" + eventoId + "/ticket.pdf";
    }

    /** Genera el PDF y lo sube (o sobrescribe) en S3. */
    public void subir(Evento evento) {
        validarBucket();
        byte[] pdf = generarPdf(evento);
        try {
            s3.putObject(b -> b.bucket(bucket).key(clave(evento.getId())).contentType("application/pdf"),
                    RequestBody.fromBytes(pdf));
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al subir a S3: " + e.getMessage());
        }
    }

    /** Lee el PDF desde S3. */
    public byte[] descargar(Long eventoId) {
        validarBucket();
        try {
            return s3.getObjectAsBytes(b -> b.bucket(bucket).key(clave(eventoId))).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF no encontrado en S3");
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al descargar de S3: " + e.getMessage());
        }
    }

    public void borrar(Long eventoId) {
        validarBucket();
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(clave(eventoId)));
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al borrar de S3: " + e.getMessage());
        }
    }

    private void validarBucket() {
        if (bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3_BUCKET no está configurado");
        }
    }

    private byte[] generarPdf(Evento evento) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                cs.beginText();
                cs.newLineAtOffset(60, 720);
                cs.setLeading(22);
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                cs.showText("Ticket del evento");
                cs.newLine();
                cs.newLine();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13);
                for (String linea : List.of(
                        "Evento N°: " + evento.getId(),
                        "Nombre: " + evento.getNombre(),
                        "Fecha: " + evento.getFecha(),
                        "Precio: $" + evento.getPrecio(),
                        "Generado: " + LocalDateTime.now().withNano(0))) {
                    cs.showText(linea);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
