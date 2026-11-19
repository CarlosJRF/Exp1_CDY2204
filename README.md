# Eventos API (Exp2)

Microservicio (Java 21 + Spring Boot + PostgreSQL) con CRUD de eventos. Al crear un evento se genera su
**ticket PDF** y se guarda en **AWS S3** con la clave `evento{id}/ticket.pdf`; el PDF se puede descargar desde la API.

## Endpoints

| Método | Ruta                       | Descripción                                   | Respuestas            |
|--------|----------------------------|-----------------------------------------------|-----------------------|
| GET    | /api/eventos               | Lista los eventos                             | 200                   |
| GET    | /api/eventos/{id}          | Obtiene un evento                             | 200 / 404             |
| POST   | /api/eventos               | Crea un evento y sube su ticket PDF a S3      | 201 / 400 / 502       |
| PUT    | /api/eventos/{id}          | Actualiza el evento y regenera el PDF en S3   | 200 / 400 / 404 / 502 |
| DELETE | /api/eventos/{id}          | Elimina el PDF de S3 y el evento              | 204 / 404 / 502       |
| GET    | /api/eventos/{id}/ticket   | Descarga el ticket PDF desde S3               | 200 / 404 / 502       |

Ejemplo de evento: `{"nombre":"Concierto Rock","fecha":"2026-12-15","precio":25000}`

Si S3 falla (o `S3_BUCKET` no está configurado), la API responde **502** y no guarda cambios en la base de datos.

## Ejecutar localmente

```bash
export S3_BUCKET=<tu-bucket>
export AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_SESSION_TOKEN=...   # credenciales del Learner Lab
docker compose up -d --build
```

La API queda en `http://localhost:8080`.

## Pruebas con Postman

- `postman_collection.json`: requests sueltos del CRUD y casos de error (`baseUrl` = `http://localhost:8080`).
- `postman_collection_Exp2.json`: ciclo completo encadenado contra la EC2 (`baseUrl` = `http://100.61.249.100:8080`).
  Crear → listar → obtener → descargar PDF → actualizar → descargar PDF actualizado → eliminar → 404.
  Valida el PDF (`Content-Type`, `Content-Disposition`, firma `%PDF`, que cambie tras el PUT).
  Ejecutar con *Run collection* o `postman collection run postman_collection_Exp2.json`.

## Despliegue

Cada push a la rama **`Exp2`** ejecuta `.github/workflows/deploy.yml`, que:
1. Construye la imagen Docker y la publica en Docker Hub (`<usuario>/eventos:latest`).
2. Se conecta por SSH a la EC2, instala Docker si falta y levanta la app y PostgreSQL con `docker compose`.
   En cada despliegue se ejecuta `docker compose down -v`, por lo que **la base de datos se reinicia**.

La EC2 debe tener el rol `LabInstanceProfile` (acceso a S3 sin llaves) y *Metadata response hop limit* = 2.

Secrets requeridos en el repositorio: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`,
`EC2_SSH_KEY`, `DB_PASSWORD`, `S3_BUCKET`.
