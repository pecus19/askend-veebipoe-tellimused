# Web shop orders

A Spring Boot service that stores web shop orders and their products in PostgreSQL and returns them as JSON
through a REST API.

An order has a number, a status, a creation time and its products. A product has a name, a
quantity and a unit price. Products belong to their order and are not managed on their own.

## Stack

- Java 21, Spring Boot 4.1 (Spring MVC, Spring Data JPA / Hibernate 7, Bean Validation)
- PostgreSQL 17, schema managed by Liquibase (XML changelogs)
- Gradle (Groovy DSL) with the wrapper, Lombok
- springdoc-openapi (Swagger UI)
- Tests: JUnit 5, MockMvc, Testcontainers (a real PostgreSQL)

## Running

Everything in Docker (PostgreSQL and the application):

```bash
docker compose up --build
```

PostgreSQL is published on host port 5433 (not 5432), so a PostgreSQL already installed on the machine does not
get in the way. Only the database in Docker, the application from the IDE or Gradle:

```bash
docker compose up -d postgres
./gradlew bootRun
```

The API is then at <http://localhost:8080/api/orders> and Swagger UI at
<http://localhost:8080/swagger-ui.html>. A local run loads seven demo orders.

Tests need a running Docker (Testcontainers starts its own PostgreSQL):

```bash
./gradlew test
```

## API

### `GET /api/orders`

Orders with their products, newest first.

| Parameter | Default | Notes                                                    |
|-----------|---------|----------------------------------------------------------|
| `status`  | (all)   | `NEW`, `PAID`, `SHIPPED`, `DELIVERED` or `CANCELLED`     |
| `page`    | `0`     | zero-based, at most 21474835 (the row offset fits an int) |
| `size`    | `20`    | 1-100                                                    |

```bash
curl "http://localhost:8080/api/orders?status=PAID&size=1"
```

```json
{
  "content": [
    {
      "orderNumber": "ORD-2026-000005",
      "status": "PAID",
      "createdAt": "2026-10-30T17:30:00Z",
      "total": 142.80,
      "items": [
        { "name": "Jooksujalatsid", "quantity": 1, "unitPrice": 119.00, "lineTotal": 119.00 },
        { "name": "Spordisokid (3 paari)", "quantity": 2, "unitPrice": 11.90, "lineTotal": 23.80 }
      ]
    }
  ],
  "page": 0,
  "size": 1,
  "totalElements": 2,
  "totalPages": 2
}
```

### `GET /api/orders/{orderNumber}`

One order with its products, e.g. `GET /api/orders/ORD-2026-000003`.

## Project layout

```
src/main/java/ee/askend/orders
  controller/   REST controller (GET /api/orders, GET /api/orders/{orderNumber})
  service/      reads orders in a read-only transaction and maps them to responses
  repository/   Spring Data JPA repository
  entity/       JPA entities (Order, OrderItem) and OrderStatus
  dto/          response records (OrderResponse, OrderItemResponse, PageResponse)
src/main/resources/db/changelog   Liquibase changelogs (schema, demo data)
src/test/java/ee/askend/orders    integration tests (Testcontainers), one package per tested layer
```
