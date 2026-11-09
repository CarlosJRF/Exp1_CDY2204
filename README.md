# Eventos y Tickets API

Backend (Java 21 + Spring Boot + PostgreSQL) para registrar eventos, vender entradas y generar tickets PDF.
Los tickets se generan en **AWS EFS** (almacenamiento temporal) y se suben a **AWS S3** con la ruta
`evento{id}/{usuario}/ticket{nnn}.pdf`.

## Endpoints

| Método | Ruta                                 | Descripción                                          |
|--------|--------------------------------------|------------------------------------------------------|
| GET    | /api/eventos                         | Lista los eventos                                    |
| GET    | /api/eventos/{id}                    | Obtiene un evento                                    |
| POST   | /api/eventos                         | Crea un evento                                       |
| PUT    | /api/eventos/{id}                    | Actualiza un evento                                  |
| DELETE | /api/eventos/{id}                    | Elimina un evento (409 si tiene compras)             |
| GET    | /api/eventos/estadisticas            | Entradas vendidas y ganancias de cada evento + totales |
| GET    | /api/eventos/{id}/estadisticas       | Ventas y reservas del evento                         |
| POST   | /api/compras                         | Compra entradas (`eventoId`, `usuario`, `cantidad`)  |
| GET    | /api/compras                         | Lista las compras                                    |
| GET    | /api/compras/{id}                    | Obtiene una compra                                   |
| POST   | /api/tickets/generar                 | Genera un PDF por entrada (`compraId`) y lo sube a S3 |
| GET    | /api/tickets?usuario=&eventoId=      | Lista tickets (filtros opcionales)                   |
| POST   | /api/tickets/{id}/subir              | Sube o re-sube el PDF del EFS a S3                   |
| GET    | /api/tickets/descargar?usuario=&eventoId= | Descarga un .zip con los PDF desde S3 (al menos un filtro) |
| PUT    | /api/tickets/{id}                    | Cambia el usuario titular (`usuario`)                |
| DELETE | /api/tickets/{id}                    | Elimina el ticket de S3, EFS y BD                    |

Ejemplo de evento: `{"nombre":"Concierto Rock","fecha":"2026-12-15","precio":25000}`
Ejemplo de compra: `{"eventoId":1,"usuario":"juan.perez","cantidad":3}`

## Ejecutar localmente

```bash
docker compose up -d --build
```

La API queda en `http://localhost:8080` y la carpeta `./efs` hace de EFS. Para probar S3 en local, exporta
`S3_BUCKET` y las credenciales del Learner Lab (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`)
antes de levantar. Importa `postman_collection.json` en Postman para probarla.

## Despliegue

Cada push a `main` ejecuta `.github/workflows/deploy.yml`, que:
1. Construye la imagen Docker y la publica en Docker Hub (`<usuario>/eventos:latest`).
2. Se conecta por SSH a la instancia EC2, monta el EFS en `/mnt/efs`, instala Docker si no está y levanta la app y PostgreSQL con `docker compose`.

La EC2 debe tener el rol `LabInstanceProfile` (acceso a S3 sin llaves) y *Metadata response hop limit* = 2.

Secrets requeridos en el repositorio: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`,
`EC2_SSH_KEY`, `DB_PASSWORD`, `S3_BUCKET`, `EFS_ID`.
