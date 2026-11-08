package cl.duoc.eventos.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Un ticket por entrada comprada. El PDF vive en EFS (temporal) y en S3 (definitivo). */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    private String usuario;
    private String rutaEfs;
    private String claveS3;
    private boolean subidoS3;
    private LocalDateTime fechaGeneracion;

    public Long getId() { return id; }
    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRutaEfs() { return rutaEfs; }
    public void setRutaEfs(String rutaEfs) { this.rutaEfs = rutaEfs; }
    public String getClaveS3() { return claveS3; }
    public void setClaveS3(String claveS3) { this.claveS3 = claveS3; }
    public boolean isSubidoS3() { return subidoS3; }
    public void setSubidoS3(boolean subidoS3) { this.subidoS3 = subidoS3; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
