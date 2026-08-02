# Restaurant Ops Backend Roadmap

The completed scope of Restaurant Ops Backend is:

**A backend-first restaurant operations and menu CMS system where store staff can manage tables, menus, orders, checkout, menu revisions, publishing schedules, and audit history using Kotlin, Spring Boot, PostgreSQL, and automated integration tests.**

The main purpose of this project is not to build a complete restaurant SaaS product. It is to prepare for a backend-focused internship by practicing:

* Kotlin and Spring Boot API development
* modular backend architecture using controllers, services, and repositories
* PostgreSQL schema design, foreign keys, checks, and unique constraints
* partial unique indexes and deferrable constraints
* exclusion constraints and PostgreSQL range types
* index design and query-plan analysis
* transaction-safe order creation and checkout
* transaction boundaries and rollback behavior
* pessimistic row-level locking with `SELECT ... FOR UPDATE`
* optimistic locking for concurrent CMS editing
* deadlock prevention, detection, and transaction retries
* deterministic lock ordering
* concurrent job processing with `FOR UPDATE SKIP LOCKED`
* immutable order-item snapshots
* revision-based menu editing and publishing
* scheduled menu publication
* database-backed audit history using triggers and `JSONB`
* integration, transaction, rollback, and concurrency testing

The project has two centerpiece workflows:

**A transaction-safe restaurant ordering and checkout flow that prevents duplicate active orders and payments, preserves correct order and table state, and coordinates checkout with concurrent order-item modifications.**

**A revision-based menu CMS workflow that prevents lost updates, publishes menu changes atomically, supports scheduled publication, and records an append-only audit history.**

The roadmap begins with the restaurant operation features needed to learn Kotlin, Spring Boot, PostgreSQL, database migrations, and integration testing. It then progresses into database-backed business rules and advanced backend interaction patterns.

The final phases focus on:

* using PostgreSQL constraints as part of the application’s correctness model
* coordinating concurrent requests through row-level locks
* handling stale writes with optimistic locking
* handling deadlocks and retryable transaction failures
* safely publishing related database changes as one atomic operation
* processing scheduled work across multiple backend instances
* preserving historical data through snapshots and revisions
* recording database changes through transactional audit events

By the end of Task 6.6, the project demonstrates how a Spring Boot backend and PostgreSQL can work together to enforce business rules, protect shared state under concurrency, and support realistic CMS and restaurant-operation workflows.

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

### Task 5.3 — Implement atomic checkout with pessimistic locking

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

Inside one service-layer transaction:

1. Lock the order row:

```sql
SELECT
    id,
    store_id,
    table_id,
    status,
    opened_at,
    checked_out_at
FROM orders
WHERE id = :orderId
  AND store_id = :storeId
FOR UPDATE;
```

2. Verify the order is still `OPEN`.
3. Calculate the total from `order_items.unit_price_snapshot`.
4. Insert the payment.
5. Change the order to `CHECKED_OUT`.
6. Set `checked_out_at`.
7. Change the table status to `CLOSED`.

Add:

```sql
CREATE UNIQUE INDEX ux_payments_succeeded_order
ON payments (order_id)
WHERE status = 'SUCCEEDED';
```

Important rule:

```text
Adding, updating, or deleting an order item must also lock the order row
before checking that the order is OPEN.
```

This makes checkout and order-item modification compete for the same row lock. PostgreSQL row locks are held until the transaction finishes, while Spring’s `@Transactional` establishes the service transaction around the database operations.

**Test**

* Checkout calculates the total from database snapshots.
* Checkout updates the payment, order, and table atomically.
* A failed payment insert rolls back the entire checkout.
* Two concurrent checkout requests produce only one successful payment.
* An item cannot be added while another transaction is completing checkout.
* A checked-out order cannot be checked out again.

---

### Task 5.4 — Handle deadlocks and retryable transactions

Create:

```text
common/transaction/RetryingTransactionExecutor.kt
```

Use Spring `TransactionTemplate` to execute a complete transaction and retry it when PostgreSQL returns:

```text
40001 — serialization_failure
40P01 — deadlock_detected
```

Use:

* maximum three attempts
* small randomized backoff
* logging of the SQLSTATE and attempt number
* no HTTP calls or other external side effects inside a retryable block

Do not retry by catching an exception inside an already failed `@Transactional` method. Retry by starting an entirely new transaction.

Create a concurrency test that:

1. Creates two database rows.
2. Transaction A locks them in the order `row1 -> row2`.
3. Transaction B locks them in the order `row2 -> row1`.
4. Confirms PostgreSQL detects the deadlock.
5. Confirms the retry wrapper reruns the failed transaction.

PostgreSQL expects applications to retry complete transactions after serialization failures and may also require retries after detected deadlocks.

Also record useful inspection queries in:

```text
docs/postgres-locking.md
```

```sql
SELECT * FROM pg_stat_activity;

SELECT * FROM pg_locks;
```

**Test**

* A deadlock can be reproduced intentionally.
* One transaction is aborted instead of both requests hanging forever.
* The retry wrapper starts a new transaction.
* Retry exhaustion produces a controlled API error.
* You can explain why deterministic lock ordering is preferable to relying on retries.

---

## Phase 6 — Menu CMS and publishing workflow

### Task 6.1 — Introduce menu-item revisions

Convert menu management from direct editing to a revision-based CMS model.

Keep `menu_items` as the stable identity:

```text
menu_items

id
store_id
category_id
is_available
created_at
```

Add:

```text
menu_item_revisions

id
menu_item_id
store_id
revision_number
status
name
description
price
version
created_by
created_at
published_at
```

Revision statuses:

```text
DRAFT
PUBLISHED
ARCHIVED
```

Add database rules:

```sql
CHECK (price >= 0)

CHECK (version > 0)

UNIQUE (menu_item_id, revision_number)
```

Add CMS endpoints:

```text
POST /api/stores/{storeId}/menu-items/{menuItemId}/drafts

GET  /api/stores/{storeId}/menu-items/{menuItemId}/revisions

GET  /api/stores/{storeId}/menu-items/{menuItemId}/draft
```

Change the ordering flow so that adding an order item loads the currently published revision. Continue storing the name and price as order-item snapshots.

**Test**

* Editing a draft does not immediately change the customer-facing menu.
* Existing order-item snapshots remain unchanged.
* Revision numbers cannot be duplicated.
* A revision cannot belong to a menu item from another store.
* Ordering fails when an item has no published revision.

---

### Task 6.2 — Add optimistic locking for CMS editing

Build:

```text
PATCH /api/stores/{storeId}/menu-items/{menuItemId}/draft
```

Request:

```json
{
  "name": "Spicy Chicken",
  "description": "Updated description",
  "price": 1350,
  "expectedVersion": 4
}
```

Repository update:

```sql
UPDATE menu_item_revisions
SET
    name = :name,
    description = :description,
    price = :price,
    version = version + 1
WHERE menu_item_id = :menuItemId
  AND store_id = :storeId
  AND status = 'DRAFT'
  AND version = :expectedVersion
RETURNING
    id,
    menu_item_id,
    store_id,
    revision_number,
    status,
    name,
    description,
    price,
    version,
    created_by,
    created_at,
    published_at;
```

When no row is returned:

* return `404` if the draft does not exist
* return `409` with code `stale_version` if the version has changed

Example response:

```json
{
  "error": {
    "code": "stale_version",
    "message": "The draft was modified by another editor"
  }
}
```

**Test**

* Updating version `4` changes it to version `5`.
* Two editors can load version `4`.
* The first editor succeeds.
* The second editor receives `409 stale_version`.
* The second editor does not overwrite the first editor’s changes.
* The client cannot directly select the new version number.

---

### Task 6.3 — Implement atomic menu publishing

Build:

```text
POST /api/stores/{storeId}/menu-items/{menuItemId}/publish
```

Add:

```sql
CREATE UNIQUE INDEX ux_menu_item_published_revision
ON menu_item_revisions (menu_item_id)
WHERE status = 'PUBLISHED';

CREATE UNIQUE INDEX ux_menu_item_draft_revision
ON menu_item_revisions (menu_item_id)
WHERE status = 'DRAFT';
```

A partial unique index is the PostgreSQL mechanism for enforcing uniqueness over only the rows matching a condition.

Inside one transaction:

1. Lock the `menu_items` row with `FOR UPDATE`.
2. Load the current draft.
3. Validate all publish-time rules.
4. Change the current `PUBLISHED` revision to `ARCHIVED`.
5. Change the draft to `PUBLISHED`.
6. Set `published_at`.
7. Create a new `DRAFT` copied from the newly published revision.

Rules:

* only a `DRAFT` can be published
* at most one draft may exist
* at most one published revision may exist
* publishing must not expose an intermediate state with no published revision
* concurrent publish requests must serialize on the menu-item row

**Test**

* Publishing makes the draft customer-visible.
* The previous published revision becomes archived.
* A new editable draft is created.
* Two concurrent publish requests do not publish two revisions.
* A failure halfway through publishing rolls back every state change.
* Orders created before and after publication use the appropriate price snapshot.

---

### Task 6.4 — Implement deadlock-safe CMS reordering

Build:

```text
PUT /api/stores/{storeId}/menu-categories/{categoryId}/menu-item-order
```

Request:

```json
{
  "menuItemIds": [
    "item-id-3",
    "item-id-1",
    "item-id-2"
  ]
}
```

Add a position column and a deferrable constraint:

```sql
ALTER TABLE menu_items
ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

ALTER TABLE menu_items
ADD CONSTRAINT uq_menu_items_category_position
UNIQUE (category_id, display_order)
DEFERRABLE INITIALLY IMMEDIATE;
```

Inside the transaction:

1. Lock the category row.
2. Validate that every submitted item belongs to that category and store.
3. Defer the position constraint:

```sql
SET CONSTRAINTS uq_menu_items_category_position DEFERRED;
```

4. Update all positions.
5. Let PostgreSQL validate uniqueness when the transaction commits.

Deferring the constraint allows temporary duplicate positions while several rows are being rearranged, while still requiring the final committed state to be valid.

Also build:

```text
PATCH /api/stores/{storeId}/menu-items/{menuItemId}/placement
```

When moving an item between categories:

* lock both category rows
* always lock them in ascending UUID order
* renumber the source and destination categories
* never lock them according to request order

**Test**

* Swapping two item positions succeeds.
* Duplicate final positions are rejected at commit.
* An item from another store is rejected.
* Two concurrent reorders of the same category serialize.
* Two concurrent cross-category moves do not deadlock.
* Reversing the lock order in a test demonstrates why deterministic ordering matters.

---

### Task 6.5 — Add scheduled menu publication

Add support for publishing a menu-item revision once, at a specific future time, without anyone needing to be online when it happens.

Add:

```text
menu_publication_schedules

id
store_id
menu_item_id
revision_id
publish_at
status
created_by
created_at
processed_at
```

Schedule statuses:

```text
SCHEDULED
PROCESSED
CANCELLED
FAILED
```

Widen the existing revision lifecycle to include a frozen, unpublished state:

```text
MenuItemRevisionStatus:
DRAFT
SCHEDULED
PUBLISHED
ARCHIVED
```

`revision_id` must reference an immutable snapshot, not "whatever the draft happens to be" at the scheduled time. Merely storing the current draft's id while leaving it as `DRAFT` is not a real snapshot, because a later edit could silently change what eventually gets published. Creating a schedule therefore transitions the current draft itself.

Inside one transaction:

1. Lock the `menu_items` row.
2. Lock the current `DRAFT` revision.
3. Verify `expectedVersion`; return `409` with code `stale_version` if it changed.
4. Validate that `publishAt` is in the future.
5. Change the revision from `DRAFT` to `SCHEDULED`.
6. Insert the schedule, using that revision's `id` as `revision_id`.
7. Create a new `DRAFT`, copied from the now-`SCHEDULED` revision.

The resulting state:

```text
revision 1: PUBLISHED
revision 2: SCHEDULED  <- referenced by the schedule, no longer editable
revision 3: DRAFT      <- current editable draft
```

Build:

```text
POST   /api/stores/{storeId}/menu-items/{menuItemId}/publication-schedules

GET    /api/stores/{storeId}/publication-schedules

DELETE /api/stores/{storeId}/publication-schedules/{scheduleId}
```

Request:

```json
{
  "publishAt": "2026-08-01T09:00:00",
  "expectedVersion": 4
}
```

`DELETE` is a soft cancel (`SCHEDULED` to `CANCELLED`), not a physical delete — history stays intact for Task 6.6's audit trail. Cancelling also archives the now-orphaned `SCHEDULED` revision, since it can otherwise never be published or edited again. Cancelling an already-`CANCELLED` schedule returns `204` (idempotent); cancelling a `PROCESSED` or `FAILED` schedule returns `409`.

Add a scheduled worker that claims due rows using:

```sql
SELECT id
FROM menu_publication_schedules
WHERE status = 'SCHEDULED'
  AND publish_at <= CURRENT_TIMESTAMP
ORDER BY publish_at, id
FOR UPDATE SKIP LOCKED
LIMIT 50;
```

Multiple backend instances may run the worker, but `SKIP LOCKED` prevents them from claiming the same schedule row.

For each claimed schedule, lock the `menu_items` row and the target revision, then dispatch on the revision's status:

* `SCHEDULED` (expected case) — archive the currently published revision, promote this one to `PUBLISHED`, mark the schedule `PROCESSED` with `processed_at`.
* `PUBLISHED`, and it is already the item's current published revision — idempotent completion; mark `PROCESSED`.
* `PUBLISHED`, but a different revision is now live — this schedule can never apply; mark `FAILED`.
* `DRAFT` — schedule creation should have frozen it; mark `FAILED`.
* `ARCHIVED` — permanently superseded; mark `FAILED`.

A `FAILED` outcome must still commit (return normally, do not throw), or the worker would re-claim the same permanently invalid row forever. Only a genuinely transient failure should throw and roll back, leaving the schedule `SCHEDULED` so the next tick retries it.

**Test**

* A valid future schedule freezes the current draft into `SCHEDULED` and creates a new editable `DRAFT`.
* A past or missing `publishAt` is rejected.
* A stale `expectedVersion` returns `409 stale_version`.
* Editing the draft after scheduling does not change the scheduled revision.
* Two worker instances do not process the same schedule.
* The worker publishes a due schedule and archives the previously published revision.
* An already-published target is treated as idempotent completion.
* An archived or draft target is marked `FAILED` and not re-claimed.
* A transient failure leaves the schedule `SCHEDULED` and retryable.
* Cancelling a `SCHEDULED` schedule archives its revision and returns `204`; cancelling again is idempotent; cancelling a `PROCESSED` or `FAILED` schedule returns `409`.
* Listing never returns another store's schedules.

---

### Task 6.6 — Add database-backed CMS audit history

Add:

```text
cms_audit_events

id
store_id
entity_type
entity_id
action
before_data
after_data
actor_id
request_id
created_at
```

Use `JSONB` for:

```text
before_data
after_data
```

At the beginning of a write transaction, set request context:

```sql
SELECT set_config('app.actor_id', :actorId, true);

SELECT set_config('app.request_id', :requestId, true);
```

Create audit triggers for:

```text
menu_item_revisions
menu_publication_schedules
```

The trigger reads:

```sql
current_setting('app.actor_id', true)

current_setting('app.request_id', true)
```

Using the local form of `set_config` limits the values to the current transaction. PostgreSQL triggers execute within the same transaction as the statement that fired them, so an audit event is rolled back when the business operation is rolled back.

Build:

```text
GET /api/stores/{storeId}/cms-audit-events
```

Rules:

* audit rows are append-only
* application code cannot update or delete audit rows
* database changes are audited even when they bypass the normal service repository
* the trigger records facts, while the service remains responsible for business decisions

**Test**

* Draft edits record old and new values.
* Publishing records the status transition.
* Rolling back an edit also rolls back its audit event.
* A direct SQL update still creates an audit event.
* Audit events cannot be modified through the application.
* Audit queries cannot return another store’s events.
