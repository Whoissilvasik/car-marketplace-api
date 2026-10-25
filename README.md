# Car Marketplace API

REST API for managing car sale listings. A learning project at Junior Java Developer level: layered architecture, Spring Boot, PostgreSQL, Swagger, unit tests and Docker support.

The project grew out of a console application with `Car` and `CarService` classes (stored in an `ArrayList`). The business logic is preserved: adding cars, finding by id, brand, price and year, combined search, updating the price, deleting, and validation of brand, model, price and year. Storage was replaced with PostgreSQL.

## Technologies

- Java 21
- Spring Boot 3.5.6 (Spring Web, Spring Data JPA, Bean Validation)
- Hibernate + PostgreSQL 16
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito
- Maven, Docker, Docker Compose

## Project structure

```
car-marketplace/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── .env.example
└── src
    ├── main
    │   ├── java/com/example/carmarketplace
    │   │   ├── CarMarketplaceApplication.java
    │   │   ├── config/OpenApiConfig.java
    │   │   ├── controller/CarController.java
    │   │   ├── service/CarService.java
    │   │   ├── repository/CarRepository.java
    │   │   ├── entity/Car.java
    │   │   ├── dto/
    │   │   └── exception/
    │   └── resources/application.yml
    └── test/java/com/example/carmarketplace
        ├── service/CarServiceTest.java
        └── controller/CarControllerTest.java
```

## Quick start with Docker Compose

Only Docker and Docker Compose are required.

```bash
cp .env.example .env
docker compose up --build
```

Compose starts PostgreSQL and the application on the same network. The application starts only after the database passes its health check (`pg_isready`).

After startup:

- API: http://localhost:8080/cars
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Useful commands:

```bash
docker compose up --build -d     # run in the background
docker compose logs -f app       # application logs
docker compose down              # stop (database data is kept)
docker compose down -v           # stop and delete database data
```

## Running locally without Docker for the application

1. Start only the database:

   ```bash
   docker compose up -d db
   ```

2. Start the application:

   ```bash
   mvn spring-boot:run
   ```

By default the application connects to `jdbc:postgresql://localhost:5432/carmarketplace` with the username and password `carmarketplace`. If you changed `POSTGRES_PASSWORD` in `.env`, set the same value for the application:

```bash
export DB_PASSWORD=your_password
mvn spring-boot:run
```

## Environment variables

Application:

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | JDBC database URL | `jdbc:postgresql://localhost:5432/carmarketplace` |
| `DB_USERNAME` | Database user | `carmarketplace` |
| `DB_PASSWORD` | Database password | `carmarketplace` |
| `SERVER_PORT` | Application port | `8080` |

Docker Compose (`.env` file):

| Variable | Purpose | Default |
|---|---|---|
| `POSTGRES_DB` | Database name | `carmarketplace` |
| `POSTGRES_USER` | Database user | `carmarketplace` |
| `POSTGRES_PASSWORD` | Database password | `carmarketplace` |
| `DB_PORT` | PostgreSQL port on the host | `5432` |
| `APP_PORT` | Application port on the host | `8080` |

The `.env` file is listed in `.gitignore`, so real passwords are not committed. The defaults are for local development only: set your own values for any shared environment.

## Tests

```bash
mvn test
```

The tests do not require a running PostgreSQL server:

- `CarServiceTest` uses Mockito with a mocked repository: creating, retrieving, updating, deleting, missing cars, invalid data and search.
- `CarControllerTest` uses `@WebMvcTest` and MockMvc: status codes 200, 201, 204, 400 and 404, JSON error format and malformed JSON.

The project has no integration tests against a real database.

## Endpoints

| Method | Path | Description | Status codes |
|---|---|---|---|
| GET | `/cars` | List with pagination and sorting | 200, 400 |
| GET | `/cars/{id}` | Get a car by id | 200, 400, 404 |
| POST | `/cars` | Create a car | 201, 400 |
| PUT | `/cars/{id}` | Update a car | 200, 400, 404 |
| PATCH | `/cars/{id}/price` | Update only the price | 200, 400, 404 |
| DELETE | `/cars/{id}` | Delete a car | 204, 404 |
| GET | `/cars/search` | Search by optional filters | 200, 400 |

List and search parameters: `page` (zero-based), `size` (default 20, maximum 100), `sort` (for example `sort=price,desc`).

`/cars/search` filters (all optional, combined with AND): `brand` (case-insensitive), `minPrice`, `maxPrice`, `year` (exact year), `minYear`.

## Validation rules

- `brand`, `model`: not blank, up to 100 characters (leading and trailing spaces are trimmed)
- `price`: greater than zero, at most 10 integer digits and 2 fraction digits
- `year`: from 1886 to (current year + 1)

## Sample JSON

Create or update request:

```json
{
  "brand": "Toyota",
  "model": "Camry",
  "price": 18500.00,
  "year": 2020
}
```

Response:

```json
{
  "id": 1,
  "brand": "Toyota",
  "model": "Camry",
  "price": 18500.00,
  "year": 2020
}
```

List response:

```json
{
  "content": [
    { "id": 1, "brand": "Toyota", "model": "Camry", "price": 18500.00, "year": 2020 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Error response (for example, invalid input):

```json
{
  "timestamp": "2026-10-25T00:10:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/cars",
  "details": [
    "brand: Brand must not be blank",
    "price: Price must be greater than zero"
  ]
}
```

## Example requests (curl)

Create a car:

```bash
curl -i -X POST http://localhost:8080/cars \
  -H "Content-Type: application/json" \
  -d '{"brand":"Toyota","model":"Camry","price":18500.00,"year":2020}'
```

List cars (second page, 5 per page, most expensive first):

```bash
curl "http://localhost:8080/cars?page=1&size=5&sort=price,desc"
```

Get by id:

```bash
curl http://localhost:8080/cars/1
```

Update a car:

```bash
curl -X PUT http://localhost:8080/cars/1 \
  -H "Content-Type: application/json" \
  -d '{"brand":"Toyota","model":"Camry","price":17900.00,"year":2020}'
```

Update only the price:

```bash
curl -X PATCH http://localhost:8080/cars/1/price \
  -H "Content-Type: application/json" \
  -d '{"price":17000.00}'
```

Search: brand Toyota, price up to 20000, year 2015 or later:

```bash
curl "http://localhost:8080/cars/search?brand=toyota&maxPrice=20000&minYear=2015"
```

Delete a car:

```bash
curl -i -X DELETE http://localhost:8080/cars/1
```

Example of a 404 error:

```bash
curl -i http://localhost:8080/cars/9999
```

## Implementation notes

- The table schema is created by Hibernate (`ddl-auto: update`), which is convenient for a learning project. Real projects usually use migrations (Flyway or Liquibase).
- The price is stored as `BigDecimal` (`NUMERIC(12,2)`) to avoid `double` rounding errors.
- The year column is named `production_year` to avoid conflicts with SQL keywords.
- Validation happens twice: Bean Validation annotations on the DTOs (fields and formats) and `CarService` (business rules, including the upper year bound that depends on the current date).
