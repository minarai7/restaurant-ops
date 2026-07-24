# Restaurant Ops Backend Roadmap

The MVP for the app Restaurant Ops Backend is:

**A backend-first restaurant operation system where store staff can manage tables, menus, orders, and checkout using Kotlin, Spring Boot, PostgreSQL, Redis, and automated integration tests.**

The main purpose of this project is not to build a complete restaurant SaaS product. The purpose is to prepare for a backend-focused internship by practicing:

* Kotlin and Spring Boot API development
* PostgreSQL schema design and database constraints
* index design and query-plan analysis
* transaction-safe order checkout
* transaction isolation and row-level locking
* idempotency and duplicate-request protection
* integration and concurrency testing
* basic load testing and bottleneck investigation
* Redis caching and cache invalidation
* backend observability through logs and metrics
* realistic restaurant operation and peak-traffic scenarios

The centerpiece feature is:

**An idempotent and transaction-safe restaurant checkout flow that prevents duplicate payments and preserves correct order and table state during concurrent requests and peak traffic.**

The roadmap begins with the basic restaurant operation features needed to learn Kotlin, Spring Boot, and PostgreSQL development. After those foundations are complete, the remaining tasks focus on deeper backend topics that are directly relevant to the internship, especially scalability, database consistency, concurrency, caching, testing, and performance investigation.

---

## Phase 0 — Project scope and delivery rules

### Task 0.1 — Define the project goal

Write the project goal clearly in `README.md`.

Example:

```text
Restaurant Ops Backend is a backend-first preparation project for a restaurant operation platform.

The main goal is to implement an idempotent checkout flow using Kotlin, Spring Boot,

PostgreSQL transactions, row-level locking, and integration tests.
```

**Test**

* You can explain the project goal in one sentence.
* The README clearly says this is backend-first.
* The README clearly says what is not included.

---

### Task 0.2 — Define non-goals

Write a list of features that will not be built before the internship.

Non-goals:

* real LINE Messaging API integration
* real payment integration
* real POS hardware integration
* real customer mobile order UI
* Kubernetes deployment
* microservices
* advanced frontend design
* advanced campaign builder
* advanced reservation system

**Test**

* The non-goals are written in the README.
* When tempted to add a feature, you can check whether it is listed as a non-goal.

---

### Task 0.3 — Define the main deliverable

The main deliverable should be:

```text
An idempotent checkout API that safely checks out a restaurant order,

creates a payment, updates the table status, updates customer visit history,

and reflects the result in sales analytics.
```

**Test**

* You can clearly identify the most important feature.
* The project does not become a vague full-product clone.
* Every later task supports this main deliverable.

---

### Task 0.4 — Create a development log

Create:

```text
docs/dev-log.md
```

Each day, write:

```md
# YYYY-MM-DD

## Goal

## What I implemented

## Problems

## Tomorrow
```

**Test**

* You update the development log at the end of each work session.
* The log can later be reused for the final report.

---

## Phase 1 — Kotlin and Spring Boot foundation

Target dates: **7/04–7/07**

### Task 1.1 — Learn Kotlin basics needed for backend work

Focus only on the Kotlin features needed for Spring Boot APIs:

* `val` and `var`
* data classes
* nullable types
* functions
* `when`
* collections
* exceptions
* basic package/import structure

Practice with a small calculation example:

```kotlin
data class MenuItem(

    val id: String,

    val name: String,

    val price: Int,

    val isAvailable: Boolean

)

fun calculateSubtotal(items: List<Pair<MenuItem, Int>>): Int {

    return items.sumOf { (item, quantity) -> item.price * quantity }

}
```

**Test**

* You can read and write simple Kotlin data classes.
* You understand why Kotlin nullable types help avoid null-related bugs.
* You can write a simple function that calculates an order subtotal.

---

### Task 1.2 — Create a Spring Boot project

Create a Kotlin + Spring Boot backend project.

Use:

* Kotlin
* Spring Boot
* Spring Web
* Spring Validation
* PostgreSQL Driver
* Flyway
* Spring Boot Test

Add one route:

```text
GET /health
```

Response:

```json
{

  "status": "ok"

}
```

**Test**

* Backend starts successfully.
* `GET /health` returns HTTP 200.
* The response body is correct.

---

### Task 1.3 — Learn basic Spring Boot annotations

Understand and use:

* `@RestController`
* `@RequestMapping`
* `@GetMapping`
* `@PostMapping`
* `@PatchMapping`
* `@RequestBody`
* `@PathVariable`
* `@Service`
* `@Repository`

Create a simple test controller:

```text
GET /api/debug/ping

POST /api/debug/echo
```

**Test**

* You can explain the difference between controller, service, and repository.
* You can create a simple request/response API.
* You can call the API from curl, Postman, or browser.

---

### Task 1.4 — Set up PostgreSQL with Docker Compose

Create `docker-compose.yml` with PostgreSQL.

Example services:

```text
postgres

backend
```

At first, running only PostgreSQL is enough.

**Test**

* `docker compose up` starts PostgreSQL.
* You can connect to PostgreSQL from terminal or GUI.
* You can run `SELECT 1;`.

---

### Task 1.5 — Set up Flyway migrations

Add Flyway migration support.

Create the first migration:

```sql
CREATE TABLE stores (

    id UUID PRIMARY KEY,

    name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL

);
```

**Test**

* Starting the backend runs the migration automatically.
* The `stores` table exists in PostgreSQL.
* If the database is recreated, Flyway creates the table again.

---

### Task 1.6 — Set up basic backend testing

Add:

* JUnit 5
* Spring Boot Test
* Testcontainers PostgreSQL

Write one integration test:

```text
Create store -> fetch store -> assert the name is correct
```

**Test**

* Tests run successfully.
* Tests use a real PostgreSQL container.
* You can explain why database integration tests are useful for backend work.

---

## Phase 2 — Project foundation and database schema

Target dates: **7/08–7/09**

### Task 2.1 — Create backend package structure

Use a modular monolith structure:

```text
backend/

  src/main/kotlin/com/example/restaurantops/

    RestaurantOpsApplication.kt

    common/

      error/

      security/

      idempotency/

      transaction/

    store/

      controller/

      service/

      repository/

      model/

    table/

      controller/

      service/

      repository/

      model/

    menu/

      controller/

      service/

      repository/

      model/

    order/

      controller/

      service/

      repository/

      model/

    checkout/

      controller/

      service/

      repository/

      model/

    customer/

      controller/

      service/

      repository/

      model/

    analytics/

      controller/

      service/

      repository/

      model/
```

**Test**

* Code is separated by feature.
* Controllers do not directly contain SQL.
* Services contain business logic.
* Repositories contain database access.

---

### Task 2.2 — Create core database tables

Create migrations for:

```text
stores

restaurant_tables

menu_categories

menu_items

customers

orders

order_items

payments

customer_visits

idempotency_keys
```

**Test**

* Migration runs successfully.
* All tables exist in PostgreSQL.
* Foreign keys are correctly created.
* You can explain why each table exists.

---

### Task 2.3 — Add common error response format

Create a standard error response.

Example:

```json
{

  "error": {

    "code": "invalid_input",

    "message": "Name must not be blank"

  }

}
```

Handle common cases:

* validation error
* not found
* conflict
* unauthorized or forbidden, if auth is added
* unexpected server error

**Test**

* Invalid requests return a consistent JSON shape.
* Missing resources return 404.
* Business rule conflicts return 409.
* Raw stack traces are not exposed to the client.

---

## Phase 3 — Store and table operation backend

Target dates: **7/10–7/11**

### Task 3.1 — Implement store APIs

Build:

```text
POST /api/stores

GET  /api/stores

GET  /api/stores/{storeId}
```

Store fields:

* id
* name
* created_at

**Test**

* Creating a store inserts a row in `stores`.
* Listing stores returns created stores.
* Fetching a missing store returns 404.

---

### Task 3.2 — Implement table APIs

Build:

```text
POST  /api/stores/{storeId}/tables

GET   /api/stores/{storeId}/tables

PATCH /api/stores/{storeId}/tables/{tableId}/status
```

Table fields:

* id
* store_id
* table_name
* seat_count
* status
* created_at

Table statuses:

```text
EMPTY

SEATED

CALLING

CHECKOUT_WAITING

CLOSED
```

**Test**

* Creating a table inserts a row in `restaurant_tables`.
* Listing tables only returns tables for the selected store.
* Updating table status works.
* Invalid status is rejected.

---

### Task 3.3 — Implement operation board API

Build:

```text
GET /api/stores/{storeId}/tables/operation-board
```

Example response:

```json
[

  {

    "tableId": "table-id",

    "tableName": "T-01",

    "seatCount": 4,

    "status": "CHECKOUT_WAITING",

    "orderId": "order-id",

    "orderTotal": 12750,

    "elapsedMinutes": 62

  }

]
```

**Test**

* The API returns every table in the store.
* Tables with active orders include order information.
* Tables without active orders still appear.
* The result can support a simple table operation board UI.

---

## Phase 4 — Menu backend

Target dates: **7/12–7/13**

### Task 4.1 — Implement menu category APIs

Build:

```text
POST /api/stores/{storeId}/menu-categories

GET  /api/stores/{storeId}/menu-categories
```

Menu category fields:

* id
* store_id
* name
* display_order
* created_at

**Test**

* Creating a category inserts a row.
* Listing categories returns categories in display order.
* Blank category name is rejected.

---

### Task 4.2 — Implement minimal menu item APIs

Build:

```text
POST  /api/stores/{storeId}/menu-items

GET   /api/stores/{storeId}/menu-items

PATCH /api/stores/{storeId}/menu-items/{menuItemId}/availability
```

Menu item fields:

* id
* store_id
* category_id
* name
* price
* is_available
* created_at
* updated_at

Rules:

* price must be zero or greater
* name must not be blank
* category must belong to the selected store
* unavailable items cannot be added to orders

Database constraints:

```sql
CHECK (price >= 0)

UNIQUE (store_id, name)
```

**Test**

* Creating a valid menu item inserts a row.
* A negative price is rejected.
* A blank name is rejected.
* A category from another store is rejected.
* Updating availability works.

---

### Task 4.3 — Add and examine a menu index

Use this main query:

```sql
SELECT
    id,
    store_id,
    category_id,
    name,
    price,
    is_available,
    created_at,
    updated_at
FROM menu_items
WHERE store_id = :storeId
ORDER BY
    created_at ASC,
    id ASC;
```

Generate enough menu-item rows to examine the query plan.

Run:

```sql
EXPLAIN ANALYZE
```

before and after adding an index.

Possible index:

```sql
CREATE INDEX idx_menu_items_store_created
ON menu_items (
    store_id,
    created_at,
    id
);
```

Record the result in:

```text
docs/index-experiment.md
```

Explain:

* whether PostgreSQL used the index
* why the index columns match the filter and ordering
* whether execution improved
* why adding unnecessary indexes can slow inserts and updates

**Test**

* The query returns the correct store's items.
* The query plan before and after the index is recorded.
* You can explain the reason for the index column order.

---

## Phase 5 — Core ordering flow

### Task 5.1 — Implement order creation

Build:

```text
POST /api/stores/{storeId}/orders

GET  /api/stores/{storeId}/orders/{orderId}
```

Order fields:

* id
* store_id
* table_id
* status
* opened_at
* checked_out_at

Order statuses:

```text
OPEN

CHECKED_OUT

CANCELLED
```

When an order is created:

* verify that the table belongs to the store
* verify that the table has no active order
* create an `OPEN` order
* update the table status to `SEATED`

Add a PostgreSQL partial unique index:

```sql
CREATE UNIQUE INDEX ux_orders_active_table
ON orders (table_id)
WHERE status = 'OPEN';
```

**Test**

* Creating an order inserts a row.
* Creating an order updates the table status.
* A table from another store is rejected.
* A table cannot have two open orders.
* Two concurrent attempts create only one open order.

---

### Task 5.2 — Implement order item APIs

Build:

```text
POST   /api/stores/{storeId}/orders/{orderId}/items

PATCH  /api/stores/{storeId}/orders/{orderId}/items/{orderItemId}

DELETE /api/stores/{storeId}/orders/{orderId}/items/{orderItemId}
```

Order item fields:

* id
* order_id
* menu_item_id
* item_name_snapshot
* unit_price_snapshot
* quantity
* created_at

Rules:

* quantity must be greater than zero
* the server loads the item name and price
* the client does not provide the price
* unavailable items cannot be added
* items cannot be changed after checkout
* the order and menu item must belong to the selected store

Important design:

```text
Save item_name_snapshot and unit_price_snapshot when the item is added.
```

**Test**

* Adding an item creates an order-item row.
* The stored price comes from the server.
* Changing the menu price does not alter an existing order item.
* An unavailable item is rejected.
* An item from another store is rejected.
* A checked-out order cannot be changed.

---

### Task 5.3 — Add order total calculation

Create a pure Kotlin service that calculates:

* subtotal
* tax
* total

Use the order-item snapshots rather than current menu-item prices.

**Test**

* Subtotal is `unit_price_snapshot * quantity`.
* Multiple items are added correctly.
* Tax rounding is consistent.
* Total is subtotal plus tax.
* The calculation is covered by unit tests.

---

## Phase 6 — Transaction-safe checkout

### Task 6.1 — Implement checkout API

Build:

```text
POST /api/stores/{storeId}/orders/{orderId}/checkout
```

Request:

```json
{
  "paymentMethod": "CASH"
}
```

Request header:

```text
Idempotency-Key: client-generated-key
```

Response:

```json
{
  "orderId": "order-id",
  "paymentId": "payment-id",
  "subtotal": 4200,
  "tax": 420,
  "total": 4620,
  "orderStatus": "CHECKED_OUT"
}
```

**Test**

* Valid checkout creates a payment.
* Checkout returns the calculated amounts.
* An empty order is rejected.
* A missing order returns 404.
* An order from another store is rejected.

---

### Task 6.2 — Implement checkout transaction and row locking

Run the complete checkout inside one transaction.

Flow:

```text
1. Lock the order row

2. Verify that the order is OPEN

3. Load order items

4. Calculate subtotal, tax, and total

5. Insert payment

6. Update order to CHECKED_OUT

7. Update table status to CLOSED

8. Commit
```

Use:

```sql
SELECT
    id,
    store_id,
    table_id,
    status
FROM orders
WHERE id = :orderId
  AND store_id = :storeId
FOR UPDATE;
```

Add:

```sql
UNIQUE (order_id)
```

to the payments table.

Create a controlled exception after payment insertion to test rollback.

**Test**

* Payment, order, and table changes commit together.
* A controlled failure rolls back every change.
* No payment remains after rollback.
* Two simultaneous checkout attempts create one payment.
* A checked-out order cannot be checked out again.

---

### Task 6.3 — Add idempotency key support

Use the existing `idempotency_keys` table.

Store:

* store_id
* idempotency_key
* request_hash
* response_body
* status
* created_at

Behavior:

```text
First request:

- process checkout
- save the response

Same key with the same request:

- return the saved response

Same key with a different request:

- return 409 Conflict
```

Protect against:

* double-clicking checkout
* retry after a slow response
* network disconnection after checkout
* two staff devices submitting checkout

**Test**

* Repeating the same request returns the same response.
* A repeated request does not create another payment.
* The same key with a different request returns 409.
* Different keys still cannot create multiple payments for one order.
* Concurrent requests with the same key create one payment.

---

### Task 6.4 — Perform one transaction isolation experiment

Add a temporary `remaining_quantity` field or create a small test-only stock table.

Initial state:

```text
remaining_quantity = 1
```

Run two concurrent transactions that both attempt to purchase the last item.

Compare:

```text
READ COMMITTED without locking

READ COMMITTED with SELECT FOR UPDATE

SERIALIZABLE
```

Record:

* what each transaction reads
* whether either transaction waits
* whether both transactions commit
* whether one transaction fails
* whether retry is required

Write:

```text
docs/transaction-isolation.md
```

**Test**

* The concurrency scenario is reproducible.
* Stock never becomes negative in the protected implementation.
* You can explain the difference between row locking and isolation level.
* You can explain which approach you would use for this scenario.

---

### Task 6.5 — Complete checkout integration tests

Write integration tests for:

* successful checkout
* correct total calculation
* payment creation
* order update
* table update
* complete rollback
* duplicate checkout
* repeated idempotency key
* conflicting idempotency key
* concurrent checkout

Use PostgreSQL through Testcontainers.

**Test**

* All checkout integration tests pass.
* Tests verify database rows, not only HTTP status codes.
* The concurrency test proves that one payment is created.

---

## Phase 7 — Peak-traffic scalability basics

### Task 7.1 — Create a focused load test

Use k6 or Gatling.

Test:

```text
GET  /api/stores/{storeId}/menu-items

POST /api/stores/{storeId}/orders/{orderId}/items

POST /api/stores/{storeId}/orders/{orderId}/checkout
```

Run:

```text
10 concurrent users

50 concurrent users

100 concurrent users
```

Measure:

* requests per second
* p50 latency
* p95 latency
* error rate

After the test, verify:

* no duplicate payments exist
* no invalid order states exist
* no negative quantities exist

**Test**

* The load-test script can run repeatedly.
* Results for each load level are recorded.
* Database correctness is checked after the test.

---

### Task 7.2 — Diagnose and improve one bottleneck

Use the load-test results to identify one bottleneck.

Possible examples:

* missing database index
* slow SQL
* connection-pool exhaustion
* lock contention
* long transaction
* unnecessary repeated queries

Use:

* `EXPLAIN ANALYZE`
* application logs
* HikariCP metrics
* request latency

Record:

```text
Problem

Evidence

Change

Result
```

**Test**

* One measured bottleneck is identified.
* One change is made.
* Before-and-after results are recorded.
* You can explain why the change helped or did not help.

---

### Task 7.3 — Run one peak-traffic spike test

Use this pattern:

```text
20 seconds normal traffic

10 seconds sudden high traffic

20 seconds normal traffic
```

Observe:

* latency during the spike
* failed requests
* database connections
* recovery after the spike
* checkout correctness

Write a short note explaining:

* which requests could return 429
* which operations could be delayed
* which operations must remain strongly consistent
* why checkout must never be duplicated
* why menu reads are easier to scale than checkout writes

**Test**

* The application recovers after the traffic spike.
* Checkout remains consistent.
* Peak behavior and limitations are documented.

---

## Phase 8 — Redis cache basics

### Task 8.1 — Cache menu-item reads

Add Redis through Docker Compose.

Cache:

```text
GET /api/stores/{storeId}/menu-items
```

Use cache-aside behavior:

```text
1. Read from Redis

2. On cache miss, read from PostgreSQL

3. Store the result with a TTL

4. Return the result
```

Use a separate cache key for each store.

If Redis is unavailable, fall back to PostgreSQL.

**Test**

* The first request reads from PostgreSQL.
* A later request reads from Redis.
* Different stores use different keys.
* Redis failure does not prevent menu reads.
* The returned data remains correct.

---

### Task 8.2 — Implement cache invalidation

When a menu item's price or availability changes:

* invalidate the store's menu cache
* allow the next request to reload current data

**Test**

* A menu read populates the cache.
* Updating an item invalidates the cache.
* The next menu read returns the updated value.
* Stale availability is not returned after an update.

---

## Phase 9 — Minimum observability

### Task 9.1 — Add structured checkout logs

Log:

* request ID
* store ID
* order ID
* checkout started
* checkout succeeded
* checkout conflict
* idempotent response returned
* operation duration

Do not log:

* full request bodies
* sensitive customer information
* raw credentials

**Test**

* Successful checkout produces useful logs.
* Failed checkout can be followed using the request ID.
* Concurrent requests have different request IDs.
* Sensitive information is not present.

---

### Task 9.2 — Add basic application metrics

Use Spring Boot Actuator and Micrometer.

Inspect:

* HTTP request count
* HTTP latency
* checkout success count
* checkout conflict count
* HikariCP active connections
* Redis cache hit and miss count

Run the load test and inspect the metrics.

**Test**

* Metrics are available.
* Checkout success and conflicts are visible.
* Connection-pool usage is visible.
* Metrics help explain the selected bottleneck.

---

## Phase 10 — Code-quality automation

### Task 10.1 — Add static analysis

Configure:

```text
ktlint or Spotless

Detekt
```

Run them through Gradle.

**Test**

* Formatting violations fail the check.
* Static-analysis violations are reported.
* Important warnings are fixed.
* Disabled rules are documented when necessary.

---

### Task 10.2 — Create one automated verification command

Configure:

```text
./gradlew check
```

or an equivalent private CI workflow to run:

* compilation
* static analysis
* unit tests
* PostgreSQL integration tests
* Redis integration tests

**Test**

* One command runs the complete verification process.
* A failing unit test causes the command to fail.
* A failing integration test causes the command to fail.
* A static-analysis problem causes the command to fail.

---

## Phase 11 — Store-operation review

### Task 11.1 — Document important restaurant scenarios

Create:

```text
docs/store-scenarios.md
```

Cover:

```text
Staff presses checkout twice because the response is slow.

Two devices attempt to check out the same table.

Wi-Fi disconnects after the server processes checkout.

A menu item becomes unavailable during a busy period.

Redis becomes unavailable during peak traffic.
```

For each scenario, write:

* what staff experience
* what customers experience
* technical protection
* remaining limitation

**Test**

* Each technical feature is connected to a restaurant problem.
* Idempotency, transactions, locking, and caching are explained operationally.
* You can explain why correctness matters during busy periods.

---

## Phase 12 — Final review

### Task 12.1 — Prepare scalability explanations

Prepare answers for:

* How would the backend react to a sudden traffic spike?
* Why can menu reads be cached?
* Why must checkout use PostgreSQL as the authoritative source?
* Why are application instances usually stateless?
* Why can PostgreSQL become a bottleneck?
* Why are database connections limited?
* How do indexes improve queries?
* Why can indexes slow writes?
* What does `SELECT FOR UPDATE` protect?
* What do transaction isolation levels change?
* Why is idempotency necessary?
* How would you investigate high p95 latency?
* How would you prevent duplicate payment during peak traffic?

**Test**

* Each answer takes no more than two minutes.
* Answers refer to experiments from this project.
* You distinguish measured results from hypothetical large-scale design.

---

### Task 12.2 — Final project check

Final checklist:

* menu-item APIs work
* order creation works
* order-item snapshots work
* checkout is transactional
* checkout uses row locking
* payment uniqueness is enforced
* idempotency works
* concurrent checkout creates one payment
* isolation-level experiment is documented
* index experiment is documented
* load and spike tests run
* Redis caching and invalidation work
* logs and metrics are available
* static analysis runs
* automated tests pass

**Test**

* The project starts from a clean database.
* All migrations run.
* The complete order and checkout flow works.
* One verification command passes.
* You can explain every protected technical feature.

---

# Recommended build order

Do it in this order:

1. Phase 0 through Task 4.1
2. minimal menu item APIs
3. order creation
4. order-item APIs and price snapshots
5. order total calculation
6. checkout API
7. checkout transaction and row locking
8. idempotency
9. transaction-isolation experiment
10. checkout integration tests
11. menu index experiment
12. focused load test
13. one bottleneck improvement
14. traffic-spike test
15. Redis caching and invalidation
16. structured logs and metrics
17. static analysis and automated verification
18. restaurant-operation review
19. final scalability review
20. final project check

This order protects the transaction and concurrency work before optional performance and observability improvements.

---

# Minimal definition of “MVP finished”

You can say the revised MVP is finished when all of these are true:

* backend is written in Kotlin and Spring Boot
* PostgreSQL schema is managed by Flyway
* store, table, menu, order, order-item, and checkout APIs exist
* order items store name and price snapshots
* checkout is transactional
* checkout uses row-level locking
* checkout uses an idempotency key
* duplicate checkout does not create duplicate payment
* a unique database constraint protects one payment per order
* integration tests cover rollback, idempotency, and concurrent checkout
* one transaction-isolation experiment is documented
* one index experiment is documented
* one repeatable load test exists
* one measured bottleneck has been investigated
* Redis caches menu reads and is invalidated after updates
* logs and metrics help investigate checkout behavior
* static analysis and automated verification run successfully

---

# Very important implementation rules

Do not add another ordinary CRUD feature unless it is necessary for checkout, concurrency, indexing, caching, or load testing.

Do not optimize before measuring.

Do not rely only on application checks for important database invariants.

Do not treat HTTP 200 as sufficient proof of data consistency.

Do not postpone tests until the end of the project.

The backend and database behavior are the main preparation targets.

---

# Feature cut order if behind schedule

Cut these remaining tasks first:

1. Task 9.2 — Basic application metrics
2. Task 8.2 — Detailed cache invalidation tests
3. Task 7.3 — Peak-traffic spike test
4. Task 9.1 — Structured checkout logging improvements
5. Task 8.1 — Redis caching
6. Advanced Detekt cleanup
7. Task 11.1 — Written restaurant scenarios

Protect these at all costs:

1. order creation
2. order items and price snapshots
3. order total calculation
4. checkout
5. transaction rollback
6. row locking
7. payment uniqueness
8. idempotency
9. concurrent checkout testing
10. transaction-isolation experiment
11. index experiment
12. basic load testing
13. integration tests

---

# Busy day fallback plan

On days when there is not enough time to implement a full task, do only one small action.

Examples:

* write one repository method
* write one integration test
* add one database constraint
* inspect one query with `EXPLAIN ANALYZE`
* fix one transaction bug
* record one load-test result
* add one Redis integration test
* improve one checkout log
* update the development log

The goal of a busy day is not major progress.

The goal is to avoid losing context.
