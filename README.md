# Eventos API

Backend simple (Java 21 + Spring Boot + PostgreSQL) para registrar eventos y vender entradas.

## Endpoints

| Método | Ruta                | Descripción                         |
|--------|---------------------|-------------------------------------|
| GET    | /api/eventos        | Lista los eventos                   |
| GET    | /api/eventos/{id}   | Obtiene un evento                   |
| POST   | /api/eventos        | Crea un evento                      |
| PUT    | /api/eventos/{id}   | Actualiza un evento                 |
| DELETE | /api/eventos/{id}   | Elimina un evento (409 si tiene compras) |
| POST   | /api/compras        | Compra entradas (`eventoId`, `cantidad`) |
| GET    | /api/compras        | Lista las compras                   |
| GET    | /api/compras/{id}   | Obtiene una compra                  |

Ejemplo de evento: `{"nombre":"Concierto Rock","fecha":"2026-12-15","precio":25000}`

## Ejecutar localmente

```bash
docker compose up -d --build
```

La API queda en `http://localhost:8080`. Importa `postman_collection.json` en Postman para probarla.

## Despliegue

Cada push a `main` ejecuta `.github/workflows/deploy.yml`, que:
1. Construye la imagen Docker y la publica en Docker Hub (`<usuario>/eventos:latest`).
2. Se conecta por SSH a la instancia EC2 y levanta la app y PostgreSQL con `docker compose`.

Secrets requeridos en el repositorio: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `DB_PASSWORD`.
