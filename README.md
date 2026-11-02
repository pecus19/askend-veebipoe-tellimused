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

### Errors

Errors are RFC 9457 `application/problem+json`, produced by Spring's own handler
(`spring.mvc.problemdetails.enabled=true`), so there is no error-handling code in the project:

| Case                                    | Status |
|-----------------------------------------|--------|
| unknown `status`, `page` < 0, `size` outside 1-100, non-numeric value | 400 |
| order number not found                  | 404    |

```json
{ "title": "Bad Request", "status": 400,
  "detail": "Failed to convert 'status' with value: 'SENT'", "instance": "/api/orders" }
```

A `page` or `size` out of range names the parameter and its rule, e.g.
`"detail": "Invalid request parameters: size must be less than or equal to 100"`. The texts live in
`src/main/resources/messages.properties`, not in Java code. An unknown order number answers 404: the service returns an
empty `Optional` and the controller turns it into a `ResponseStatusException`, so the service knows nothing about HTTP.

## Design decisions

**Two tables.** `orders` (`order_number` unique, `status`, `created_at`) and `order_items` (`order_id`
foreign key with `ON DELETE CASCADE`, `name`, `quantity`, `unit_price`). Products are stored per order, not
in a product catalogue, because the task says they are not managed on their own; the item also keeps the
price at the time of the order.

**Constraints in the database, not only in Java.** `NOT NULL`, a unique order number, `CHECK` for the status
list, `quantity > 0` and `unit_price >= 0`. Whatever writes to the database later cannot store an invalid
row. The status is `VARCHAR` + `CHECK` rather than a PostgreSQL enum type: adding a value is a plain
migration, and it maps directly to a Java enum with `@Enumerated(STRING)`.

**Indexes for the actual reads.** `order_items(order_id)` (PostgreSQL does not index foreign keys on its own),
`orders(created_at DESC, id DESC)` for the default list and `orders(status, created_at DESC, id DESC)` for the
filtered one. They match the `ORDER BY` exactly, so a page is read from the index without a sort step.

**Money.** `NUMERIC(12, 2)` in the database and `BigDecimal` in Java, never floating point. One currency
(EUR) is assumed, so there is no currency column. `lineTotal` and `total` are calculated for the response,
not stored, so they cannot drift from the items. Totals always have two decimals.

**Time.** `TIMESTAMP WITH TIME ZONE` and `Instant`; the API returns ISO-8601 in UTC
(`2026-10-30T17:30:00Z`). The client decides how to show it in a local time zone.

**Liquibase owns the schema.** Hibernate runs with `ddl-auto=validate`: it never changes the schema, it only
fails the start-up when the entities and the tables disagree. Demo data is a separate changeset with
`contextFilter="demo"`; the application runs with the `demo` context, tests run with `test`, so tests start
from an empty database and write their own rows. Demo data is loaded into a new, empty database, so its orders get ids 1-7 and the items refer to
them by those numbers.

**No N+1.** A page of orders is read with at most three queries whatever its size: the page, the count (Spring
Data skips it when the first page is not full), and the items of all orders on the page in one
`WHERE order_id IN (...)` query (`@BatchSize` on `Order.items`, the API's page limit `MAX_PAGE_SIZE` is defined from
the same `Order.ITEMS_BATCH_SIZE` constant, so the two cannot drift apart). A `JOIN FETCH` was not used, because combined with paging Hibernate
would load every matching row and page in memory. `OrderServiceQueryCountTest` reads the largest page and checks the
number of statements with Hibernate statistics.

**Entities stay inside the service.** The service maps entities to response records inside a read-only
transaction, and `spring.jpa.open-in-view` is off, so no lazy loading happens while JSON is written and the
JSON shape does not depend on the entity classes. The page is our own `PageResponse` record rather than
Spring Data's `Page`, whose serialized form is not a stable contract.

**Stable paging.** Sorted by `created_at DESC, id DESC`, written into the repository method names
(`findAllByOrderByCreatedAtDescIdDesc`, `findByStatusOrderByCreatedAtDescIdDesc`), so two orders with the same timestamp never swap
between pages. The page size is capped at 100, and the page number so that the row offset fits an `int` (a larger
one would otherwise end in a 500).

**Lombok, carefully.** Entities use only `@Getter` and a protected no-args constructor (JPA needs it).
`getItems()` returns a read-only view, because items are not managed on their own.
`@Data`, `@EqualsAndHashCode` and `@ToString` are avoided on entities: they would touch the lazy `items`
collection and give an identity that changes once the row is saved. Response types are Java records.

**Read-only API.** The task asks for querying the list, so there is no create/update endpoint and no
authentication. Writing would be the natural next step (`POST /api/orders` with validation of the items).

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
