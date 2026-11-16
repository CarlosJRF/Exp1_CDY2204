# CLAUDE.md — Microservicio de Eventos con Ticket PDF en S3 (Exp2)

Instrucciones para trabajar en este repositorio. Responde y documenta siempre en español.

## Contexto

Proyecto estudiantil (CDY2204). En Exp1 (rama `main`) se construyó una API de eventos, compras y tickets por entrada
con PDF en EFS y S3. En **Exp2** (rama `Exp2`) se simplifica a un **microservicio con CRUD completo de eventos**:
al crear un evento se genera su ticket PDF y se guarda en un bucket S3; el PDF se puede descargar.

Objetivo: tener un microservicio funcional y estable sobre el cual practicar, en desarrollos futuros,
**API Gateway** e **IDaaS** (proveedor de identidad). Esos componentes **no** forman parte de Exp2.

Repositorio: https://github.com/CarlosJRF/Exp1_CDY2204 — se trabaja y se hace push solo en la rama **`Exp2`**.
`main` conserva la versión Exp1 y no se modifica.

## Alcance

- CRUD de eventos + descarga del ticket PDF. **No agregar otros endpoints** ni funcionalidades
  (sin compras, tickets por usuario, estadísticas, EFS, login, Swagger, LocalStack/MinIO ni microservicios extra).
- La seguridad (autenticación/autorización) se resolverá después con API Gateway + IDaaS, no en el código del servicio.

## Decisiones tomadas con el usuario

| Tema | Decisión |
|------|----------|
| Cuenta AWS | AWS Academy Learner Lab, región `us-east-1`. Sin access keys en la EC2: el SDK usa el rol **LabInstanceProfile** (cadena de credenciales por defecto). |
| Persistencia | Datos del evento (`nombre`, `fecha`, `precio`) en PostgreSQL; solo el PDF se guarda en S3. |
| EFS | Eliminado (código, compose, workflow). El PDF se genera en memoria y se sube directo a S3. |
| Generación del PDF | Al crear (POST). En PUT se regenera y sobrescribe. En DELETE se borra de S3. |
| Clave S3 | `evento{id}/ticket.pdf` (ej. `evento1/ticket.pdf`). |
| Descarga | `GET /api/eventos/{id}/ticket` lee el PDF desde S3 y lo devuelve como `application/pdf` (attachment `evento{id}-ticket.pdf`). |
| Fallo de S3 | POST/PUT/DELETE responden **502** y la operación en BD se revierte (`@Transactional`): BD y S3 siempre consistentes. |
| Bucket | Ya existe; se pasa por el secret `S3_BUCKET`. |
| BD en la EC2 | El workflow ejecuta `docker compose down -v` en **cada** deploy: la BD queda vacía tras desplegar (aceptado; los PDF antiguos quedan en S3). |
| Formato PDF | Apache PDFBox. |
| Git | Rama `Exp2`; Claude hace commit y `git push origin Exp2`. |

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|--------|------|-------------|-----------|
| GET | `/api/eventos` | Lista eventos | 200 |
| GET | `/api/eventos/{id}` | Obtiene un evento | 200 / 404 |
| POST | `/api/eventos` | Crea el evento, genera el PDF y lo sube a S3 | 201 / 400 / 502 |
| PUT | `/api/eventos/{id}` | Actualiza el evento y regenera el PDF en S3 | 200 / 400 / 404 / 502 |
| DELETE | `/api/eventos/{id}` | Borra el PDF de S3 y el evento | 204 / 404 / 502 |
| GET | `/api/eventos/{id}/ticket` | Descarga el PDF desde S3 | 200 / 404 / 502 |

Body de POST/PUT: `{"nombre":"Concierto Rock","fecha":"2026-12-15","precio":25000}`.

## Código (paquete `cl.duoc.eventos`)

- `model/Evento`: entidad JPA (tabla `eventos`) con validaciones (`@NotBlank`, `@NotNull`, `@PositiveOrZero`).
- `repository/EventoRepository`: `JpaRepository<Evento, Long>`.
- `config/S3Config`: bean `S3Client` con la región de `AWS_REGION`, sin llaves en el código.
- `service/TicketPdfService`: genera el PDF en memoria y hace put/get/delete en S3. **Única clase que toca S3.**
- `service/EventoService`: CRUD transaccional; coordina BD + `TicketPdfService`.
- `controller/EventoController`: `/api/eventos`, delega en `EventoService`.
- Estilo: inyección por constructor, `ResponseStatusException` para errores, sin Lombok, comentarios breves en español.

## Configuración (variables de entorno)

| Variable | Uso | Default local |
|----------|-----|---------------|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL | localhost / postgres |
| `S3_BUCKET` | Bucket de tickets | vacío → POST/PUT/DELETE/descarga responden 502 |
| `AWS_REGION` | Región del cliente S3 | `us-east-1` |

En la EC2 nunca se configuran access keys: las credenciales vienen del rol de la instancia (IMDS).
Para que el contenedor alcance el IMDS, la instancia debe tener *Metadata response hop limit* = 2.

## Despliegue (`.github/workflows/deploy.yml`)

- Se dispara con push a **`Exp2`**.
- Job `build-push`: construye y publica `<usuario>/eventos:latest` en Docker Hub.
- Job `deploy`: copia `docker-compose.yml` a `~/eventos`, instala Docker y compose si faltan (Ubuntu `apt-get` /
  Amazon Linux 2023 `dnf`), escribe `~/eventos/.env` (permisos 600) con las variables para compose,
  `docker compose pull app`, `down -v` (reinicia la BD) y `up -d`.
  No usar `sudo -E`: el `sudo-rs` de Ubuntu 26.04 lo ignora y las variables no llegan a compose.
- EC2 actual: Ubuntu 26.04 (usuario `ubuntu`), IP elástica en el secret `EC2_HOST`.
- Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DB_PASSWORD`, `S3_BUCKET`.
  (`EFS_ID` ya no se usa.)

## Comandos útiles

```bash
./mvnw -q package -DskipTests          # compilar
docker compose up -d --build           # levantar app + PostgreSQL en local
aws s3 ls s3://$S3_BUCKET --recursive  # (en la EC2 o CloudShell) ver PDF en S3
```

## Documentación

- `README.md` y `postman_collection.json` se actualizan con cada cambio de endpoints.
- `Guia_Exp2_CDY2204.odt`: material de estudio personal (ignorado por git, `*.odt`). Explica los conceptos cloud
  aplicados, las pruebas con Postman y una introducción a API Gateway e IDaaS. Se mantiene sincronizada con el sistema.
  `Guia_Exp1_CDY2204.odt` queda como referencia de Exp1.
- `PLAN_Exp2.md`: plan de trabajo de la refactorización (ignorado por git).
