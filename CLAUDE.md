# CLAUDE.md — Sistema de Gestión de Reservas de Eventos con Tickets

Instrucciones para trabajar en este repositorio. Responde y documenta siempre en español.

## Contexto

Actividad sumativa "Construyendo y desplegando una solución Cloud Native" (CDY2204, semana 3).
Partimos de una API Spring Boot de eventos y compras (PostgreSQL, Docker, GitHub Actions → Docker Hub → EC2)
y la extendemos con tickets PDF almacenados en **EFS** (temporal) y **S3** (definitivo).

Repositorio: https://github.com/CarlosJRF/Exp1_CDY2204 (rama `main`).

## Requisitos del instructivo (fuente de verdad)

1. **EFS para almacenamiento temporal**: los tickets personalizados se generan y guardan en el EFS montado en la EC2.
2. **Subida automática a S3**: los tickets se guardan en S3 por evento y usuario, ej. `evento123/usuarioX/ticket001.pdf`.
3. **Endpoints REST**: generar tickets, subir tickets a S3, descargar tickets por usuario o evento,
   modificar detalles del ticket, eliminar tickets específicos, estadísticas de ventas y reservas por evento.
4. **Despliegue automatizado**: un push a `main` construye y publica la imagen en Docker Hub y despliega
   en EC2 con GitHub Actions, **sin pasos manuales** (incluido el montaje del EFS).
5. Demostración en vivo con **Postman**.

**No agregar funcionalidades fuera de esta lista** (sin login, sin tabla de usuarios, sin reservas separadas,
sin microservicios extra, sin LocalStack/MinIO, sin Swagger).

## Decisiones tomadas con el usuario

| Tema | Decisión |
|------|----------|
| Cuenta AWS | AWS Academy Learner Lab, región `us-east-1`. Sin usuarios IAM ni access keys: la EC2 usa el rol **LabInstanceProfile** y el AWS SDK obtiene credenciales del rol (cadena por defecto). |
| EFS | EFS real de AWS, montado en la EC2 en `/mnt/efs` por el propio workflow; se pasa al contenedor como volumen en `/data/efs`. |
| Arquitectura | Un solo servicio Spring Boot (se agrega el módulo de tickets a la API existente). |
| Usuario | Campo de texto `usuario` en la compra (body de `POST /api/compras`). Solo `[A-Za-z0-9._@-]` y sin empezar con punto, porque forma parte de la ruta. |
| Tickets por compra | Uno por entrada: una compra de 3 entradas genera 3 PDF. |
| Generación | Endpoint explícito `POST /api/tickets/generar` con `compraId`. Comprar no genera tickets. |
| Subida a S3 | Automática al generar. Si falla, el ticket queda con `subidoS3=false` y se reintenta con `POST /api/tickets/{id}/subir`. |
| Descarga | `.zip` armado con los PDF leídos desde **S3** (solo tickets con `subidoS3=true`), con carpetas `evento{id}/{usuario}/ticket{nnn}.pdf`. Filtros `usuario` y/o `eventoId` (al menos uno, combinables); 404 si no hay tickets. Ya no se copian descargas al EFS. |
| Modificar | Solo el **usuario titular**: el PDF se regenera en la nueva ruta del EFS, se sube a S3 y se borran el archivo y el objeto anteriores. |
| Estadísticas | Compra = reserva. **Ganancias = suma de `precioTotal` de las compras** (no hay costos). Por evento (`/api/eventos/{id}/estadisticas`): reservas, entradas vendidas, ganancias, tickets generados y subidos. Todos los eventos (`/api/eventos/estadisticas`): solo entradas vendidas y ganancias de cada evento, más los totales generales. |
| Formato | PDF generado con Apache PDFBox. |
| Git | Commit en `main`; el push lo hace el usuario cuando la infraestructura esté lista. |

## Diseño

### Rutas de archivos

- Clave S3 / ruta relativa: `evento{eventoId}/{usuario}/ticket{id con 3 dígitos}.pdf` (ej. `evento1/juan.perez/ticket001.pdf`).
  Se usa el id del ticket, así no hay choques si un usuario compra dos veces para el mismo evento.
- EFS (generados): `${EFS_PATH}/tickets/<ruta relativa>` → en la EC2: `/mnt/efs/tickets/...`

### Endpoints nuevos o modificados

| Método | Ruta | Descripción | OK / errores |
|--------|------|-------------|--------------|
| POST | `/api/compras` | Ahora exige `usuario` además de `eventoId` y `cantidad` | 201 / 400 / 404 |
| POST | `/api/tickets/generar` | Body `{"compraId":1}`. Genera un PDF por entrada en EFS y lo sube a S3 | 201 / 404 / 409 si ya tiene tickets |
| GET | `/api/tickets?usuario=&eventoId=` | Lista tickets (filtros opcionales), para conocer ids y estado | 200 |
| POST | `/api/tickets/{id}/subir` | Sube (o re-sube) el PDF del EFS a S3 | 200 / 404 / 502 |
| GET | `/api/tickets/descargar?usuario=&eventoId=` | `.zip` con los PDF desde S3 (filtros combinables) | 200 / 400 sin filtro / 404 sin tickets / 502 |
| PUT | `/api/tickets/{id}` | Body `{"usuario":"nuevo"}`. Cambia titular, mueve archivo en EFS y S3 | 200 / 400 / 404 |
| DELETE | `/api/tickets/{id}` | Borra el ticket de BD, EFS y S3 | 204 / 404 |
| GET | `/api/eventos/estadisticas` | Entradas vendidas y ganancias de cada evento + totales generales | 200 |
| GET | `/api/eventos/{id}/estadisticas` | Estadísticas de ventas y reservas del evento | 200 / 404 |

Los endpoints existentes de eventos y compras se mantienen.

### Código (paquete `cl.duoc.eventos`)

- `model/Ticket`: id, compra (`@ManyToOne`), usuario, rutaEfs, claveS3, subidoS3, fechaGeneracion.
- `model/Compra` + `dto/CompraRequest`/`CompraResponse`: campo `usuario`.
- `repository/TicketRepository`: búsquedas por compra, usuario y evento; conteos para estadísticas.
- `config/S3Config`: bean `S3Client` con la región de `AWS_REGION` y la cadena de credenciales por defecto.
- `service/TicketService`: genera PDF, escribe en EFS, sube/descarga/borra en S3. Es la única clase que toca S3 y disco.
- `controller/TicketController`, `dto/Ticket*`, `dto/EstadisticasResponse`, `dto/EstadisticasGeneralesResponse`, `dto/VentasEventoResponse`.
- Mantener el estilo actual: inyección por constructor, `ResponseStatusException` para errores, records para DTOs,
  sin Lombok, comentarios breves en español.

### Configuración (variables de entorno)

| Variable | Uso | Default local |
|----------|-----|---------------|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL (sin cambios) | localhost |
| `EFS_PATH` | Carpeta raíz del EFS dentro del contenedor | `./efs` |
| `S3_BUCKET` | Bucket de tickets | (vacío → la subida falla con 502 y `subidoS3=false`) |
| `AWS_REGION` | Región del cliente S3 | `us-east-1` |

En la EC2 nunca se configuran access keys: las credenciales vienen del rol de la instancia (IMDS).
Para que el contenedor alcance el IMDS, la instancia debe tener *Metadata response hop limit* = 2.

### Despliegue (`.github/workflows/deploy.yml`)

- Job `build-push`: igual que antes (imagen `<usuario>/eventos:latest`).
- Job `deploy`: copia `docker-compose.yml`, instala el cliente NFS si falta, monta el EFS en `/mnt/efs` si no está
  montado, crea `tickets/`, instala Docker y el plugin compose si faltan, y ejecuta `docker compose pull` / `up -d`.
  Funciona en Ubuntu (`apt-get`) y en Amazon Linux 2023 (`dnf`); en este último el usuario SSH es `ec2-user`.
- Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DB_PASSWORD`,
  **`S3_BUCKET`**, **`EFS_ID`** (ej. `fs-0123456789abcdef0`).

## Comandos útiles

```bash
./mvnw -q package -DskipTests          # compilar
docker compose up -d --build           # levantar app + PostgreSQL en local
ls -R /mnt/efs                         # (en la EC2) ver tickets generados
aws s3 ls s3://$S3_BUCKET --recursive  # (en la EC2 o CloudShell) ver objetos en S3
```

## Documentación

- `README.md` y `postman_collection.json` se actualizan con cada cambio de endpoints.
- `Guia_Exp1_CDY2204.odt` es material de estudio personal (ignorado por git): explica conceptos cloud aplicados
  y el paso a paso de pruebas con Postman. Se mantiene sincronizada con el sistema.
