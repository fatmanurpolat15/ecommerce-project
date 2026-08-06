# E-Commerce Platform, Business Requirements & Intern Project Brief (2 Weeks)

**Project:** A real, working e-commerce platform.
**Role:** Backend intern — you own the analysis, the design and the delivery.
**Duration:** 10 working days. **Tech:** Spring Boot, PostgreSQL, Kafka, Redis + a tiny frontend to prove it works.

> This document is written like a real business requirements document. It answers **WHAT** the system must do and **WHY**. HOW you implement it (Spring Boot, Kafka, Redis...) is your job, section 6 maps every requirement to the technology that satisfies it.

---

## 1. Business Overview

We are building an online shop where:

- Customers browse a product catalog, add items to a cart, and place orders.
- The shop **must sell accurate stock**  two customers must never both buy the last unit.
- Payment is **processed asynchronously by a separate payment system** (a second microservice we own). The shop cannot block the customer while payment is "in progress" the payment result arrives later and the shop must react to it correctly.
- The customer must always end up in a consistent state: **paid order → paid**, **failed order → cancelled and stock returned**.
- The system must survive heavy reads (many people browsing the same product) without melting the database.

**Actors**

| Actor | Description | Can do |
|---|---|---|
| Guest | Not logged in | Browse catalog, view products |
| Customer | Registered user | Everything a guest can + cart, checkout, order history, cancel own order |
| Admin | Employee | Manage products, categories, stock; view all orders and payments |
| Payment System | External service (we mock it) | Process payment requests asynchronously, return success/failure |

**Core business promise:** "The customer's money, the stock, and the order status are **always** consistent — eventually, and within seconds."

---

## 2. Functional Business Requirements

Requirements use IDs (`BR-XXX-n`). Priority: **MUST** = not optional, **SHOULD** = expected, **COULD** = nice to have.

### 2.1 Accounts & Access — BR-AUTH

| ID | Requirement | Priority |
|---|---|---|
| BR-AUTH-1 | A guest must be able to register with email + password + name. Email must be unique. | MUST |
| BR-AUTH-2 | A registered user must be able to log in and receive a token to prove who they are on every later request. | MUST |
| BR-AUTH-3 | A customer may only see and manage **their own** cart, orders and payment results. | MUST |
| BR-AUTH-4 | Only an admin may create/update/deactivate products, categories and stock. | MUST |
| BR-AUTH-5 | Passwords must never be stored or returned in readable form. | MUST |

**Acceptance:** register → login → token works on protected calls; a customer calling `GET /orders/{id}` with another user's order id gets rejected (403/404).

### 2.2 Product Catalog — BR-PRD

| ID | Requirement | Priority |
|---|---|---|
| BR-PRD-1 | The shop must show a browsable catalog of products with: name, description, price, currency, image, category, availability. | MUST |
| BR-PRD-2 | A guest (no login) must be able to browse the catalog. | MUST |
| BR-PRD-3 | Catalog must support: pagination, category filter, keyword search, price range filter. | MUST |
| BR-PRD-4 | Products must not be physically deleted — deactivated products disappear from the customer-facing catalog but stay in history (old orders must still show what was bought). | MUST |
| BR-PRD-5 | Only admin can add/change product info, price, category and active status. | MUST |
| BR-PRD-6 | Product read requests must be **fast and cheap** — the same product is viewed thousands of times more than it is changed. The system must be able to serve repeated reads without hitting the database every time. | MUST |
| BR-PRD-7 | When admin changes a product, the change must be **immediately visible** to customers. | MUST |

**Acceptance:** browsing the same product 100x → database hit count stays at 1 until the product is changed.

### 2.3 Stock & Inventory — BR-STK

| ID | Requirement | Priority |
|---|---|---|
| BR-STK-1 | Every product has a sellable stock level, managed by admin. | MUST |
| BR-STK-2 | Stock is the **source of truth in the database** — no stock decision may rely on a cache. | MUST |
| BR-STK-3 | When a customer places an order, the ordered quantities must be **reserved** so other customers cannot buy them. | MUST |
| BR-STK-4 | Two customers ordering the last unit at the same moment: **exactly one order succeeds, the other is rejected** ("insufficient stock"). No over-selling, ever. | MUST |
| BR-STK-5 | When an order is cancelled or payment fails, the reservation is **released** and the units become sellable again. | MUST |
| BR-STK-6 | The shop should be able to quickly reject obviously impossible requests (asking for 500 units when 2 exist) **without expensive database work** on every request. | SHOULD |

**Acceptance:** product with stock=1; two simultaneous checkouts → one 200, one rejected; rejected reservation released; stock on the successful side is exactly correct after every scenario.

### 2.4 Shopping Cart (Redis) — BR-CART

| ID | Requirement | Priority |
|---|---|---|
| BR-CART-1 | A logged-in customer has **one persistent cart**. It survives logout, browser close, and restarts of the shop. | MUST |
| BR-CART-2 | Customer can add items, change quantities, remove items, and view the cart with line totals + total price. | MUST |
| BR-CART-3 | Adding an item to the cart does **not** reserve stock — reservation happens only at checkout. Cart is a wishlist, stock is shared. | MUST |
| BR-CART-4 | If a product is deactivated, it must be marked/removed in the cart (customer must not check out a dead product). | MUST |
| BR-CART-5 | The cart is **very frequently written and read** and lives only while relevant. It must be fast and must **not** bloat the main database with throwaway rows. | MUST |
| BR-CART-6 | An abandoned cart should automatically disappear after 7 days of inactivity. | SHOULD |
| BR-CART-7 | Cart price display must use current product price; the price used at checkout is the **snapshot at order time** (see BR-ORD-5). | MUST |

**Acceptance:** add items, close browser, reopen, items still there; after 7 days (configurable for demo, e.g. 60s) cart is gone.

### 2.5 Orders & Checkout — BR-ORD

| ID | Requirement | Priority |
|---|---|---|
| BR-ORD-1 | Checkout = customer provides a shipping address; the system creates an order from the cart with all items, quantities and a **total amount**. | MUST |
| BR-ORD-2 | Checkout must reserve stock in the same atomic step as creating the order: if stock is insufficient, the **whole order fails** and nothing is half-created. | MUST |
| BR-ORD-3 | Every order has a unique human-readable order number, and a status. | MUST |
| BR-ORD-4 | Order status lifecycle (business rules): `CREATED → PAID → FULFILLED`; `CREATED → CANCELLED` (customer cancels); `CREATED → PAYMENT_FAILED → CANCELLED` (payment rejected). **No illegal transitions** (e.g. a paid order may never go back to CREATED). | MUST |
| BR-ORD-5 | Order items must snapshot product name + price **at order time**, so later product changes never rewrite history. | MUST |
| BR-ORD-6 | Customer can list their orders and see the detail of each: items, total, status, timeline (what happened when). | MUST |
| BR-ORD-7 | Customer can cancel an order only while it is still `CREATED` (payment not yet decided). | MUST |
| BR-ORD-8 | The shop must react to the payment result when it arrives — even if it arrives after a restart or minutes later — and **never** lose or double-apply a payment result. | MUST |
| BR-ORD-9 | Once the order is created, the customer must not be blocked waiting for payment: checkout returns immediately with status `CREATED`, and the order moves forward **on its own** when the payment system answers. | MUST |

**Acceptance:** full lifecycle demonstrated without touching the database manually; duplicate/delayed payment results do not corrupt the order.

### 2.6 Payments (async, separate microservice) — BR-PAY

**Business context:** payment is handled by a separate system (the "Payment Service") for two reasons: (a) in the real world it belongs to a bank/PSP we don't control; (b) it is slow — seconds — and we must not block the shop. This is why payments are **asynchronous events**, not a HTTP call inside checkout.

| ID | Requirement | Priority |
|---|---|---|
| BR-PAY-1 | After order creation, the shop must **hand the payment over to the Payment Service asynchronously** and continue serving other requests. | MUST |
| BR-PAY-2 | The Payment Service is a **separate Spring Boot microservice**. It receives payment requests, processes them (mock: random success/fail, sometimes slow), and returns the result asynchronously. | MUST |
| BR-PAY-3 | The shop must not trust that a payment message arrives — the handover must survive a crash of the shop right after checkout (message must not be lost). | MUST |
| BR-PAY-4 | When the payment result arrives: **SUCCESS** → shop marks order `PAID` (and notifies the customer). **FAILURE** → shop marks order `CANCELLED`, **releases the reserved stock** (BR-STK-5) and notifies the customer. | MUST |
| BR-PAY-5 | Every result is processed **exactly once** in effect — if the Payment Service retries or the shop re-processes, the order must not be double-paid / stock must not be double-released. | MUST |
| BR-PAY-6 | If the payment system never answers, the order must not stay stuck forever: an order still `CREATED` after N minutes is auto-cancelled and stock released. | SHOULD |
| BR-PAY-7 | Each attempt must be recorded (payment record per order) so admin can see: requested, in-progress, success/failed, amount, payment reference. | SHOULD |
| BR-PAY-8 | The stock release on payment failure and the order status update must happen **atomically** — the customer must never see "order cancelled but stock still reserved" or the reverse. | MUST |
| BR-PAY-9 | Admin must be able to see all payments and their statuses in one screen. | SHOULD |

**Acceptance:** place order → checkout returns immediately → within seconds order becomes `PAID` or `CANCELLED` on its own; failure case returns stock to sellable; no double-processing when the mock payment service sends the result twice.

### 2.7 Notifications — BR-NOT

| ID | Requirement | Priority |
|---|---|---|
| BR-NOT-1 | Customer must be notified when: order is created, payment succeeded, payment failed / order cancelled. | MUST |
| BR-NOT-2 | Notification delivery (email mock) must not slow down or block any core operation — it is fire-and-forget. | MUST |
| BR-NOT-3 | Notifications must be recorded (who, what, when, status) so admin can inspect them. | SHOULD |

### 2.8 Admin Back Office — BR-ADM

| ID | Requirement | Priority |
|---|---|---|
| BR-ADM-1 | Admin dashboard: products (create/edit/deactivate), categories, stock levels (adjust), orders (all, filter by status), payments, notifications. | MUST |
| BR-ADM-2 | Admin must be able to inspect the **event history of an order** (order created → payment request sent → payment result → status changes) — the shop must keep an audit trail of what happened and when. | SHOULD |

---

## 3. Cross-Cutting Business Rules

1. **Money rule:** all amounts are exact decimal values (e.g. `49.99`). Floating-point math is forbidden; totals must always match the sum of line items to the cent.
2. **Price snapshot rule:** order items freeze name+price at order time; cart always shows live price; history is immutable.
3. **Status machine rule:** every order status change is validated against a legal transition table; anything else is rejected.
4. **Single-winner rule:** stock quantity may never go negative; concurrent orders compete and exactly one wins per unit.
5. **Eventual consistency promise:** after any scenario (success, failure, crash, duplicate message, slow payment), the system must settle into a consistent state within seconds **without manual intervention**.
6. **Caching rule:** caches may only speed up reads, never decide business outcomes (stock, payments, status changes are never decided from cache).
7. **Idempotency rule:** any automated retry (Kafka redelivery, payment re-answer) must be safe to apply twice.
8. **Audit rule:** every state-changing business event is recorded with a timestamp.

---

## 4. Business Scenarios (the flows that must work end-to-end)

### Scenario 1 — Happy path
Guest browses → registers → logs in → adds 2 items to cart → checkout with address → order `CREATED`, stock reserved, payment handed to Payment Service → payment succeeds → order `PAID` → customer notified. Customer sees the whole timeline in order detail.

### Scenario 2 — Payment rejected
Same until checkout → payment fails → order `CANCELLED` (via `PAYMENT_FAILED`), **stock released**, customer notified "payment failed". The units are immediately sellable again.

### Scenario 3 — Race on last unit
Stock = 1. Two customers check out at the same moment → one `CREATED`, the other rejected with "insufficient stock". No negative stock, no double sale.

### Scenario 4 — Shop crashes right after checkout
Customer checks out, the shop process dies before the payment message is sent → after restart, the payment is still delivered to the Payment Service. The order is not silently forgotten.

### Scenario 5 — Payment Service sends the result twice
The result must be applied once. Order stays `PAID` once; stock released once (in the failure variant).

### Scenario 6 — Payment never answers
After timeout (demo-friendly, e.g. 2 minutes), the order auto-cancels and stock is released.

### Scenario 7 — Hot product
A single product page is hit by thousands of browsers → database load stays flat (cache absorbs reads) and data stays correct after the admin edits the product.

---

## 5. Non-Functional Business Requirements (the "why Kafka/Redis" part)

| NFR | Business meaning | Requirement |
|---|---|---|
| NFR-1 — Async decoupling | Slow work (payment, email) must not block fast work (checkout response) | Checkout returns in < 1s regardless of payment speed; failures in payment/notification must never take the shop down |
| NFR-2 — Reliability | Business events (order created, payment result) must not be lost | Handover survives restart; delivery is at-least-once; retries with backoff; poison messages quarantined for manual review, not dropped |
| NFR-3 — Read performance | Catalog is read-dominated | Repeated product reads served from cache; cache consistent within seconds of changes |
| NFR-4 — Write scalability for carts | Carts are many, small, short-lived | Cart lives in fast storage, not in the main database |
| NFR-5 — Consistency | Money + stock + status must agree | Atomic local transactions; event-driven coordination between the two services; idempotent handling |
| NFR-6 — Observability | Anyone (including a beginner) must be able to see what's happening | Every service has health endpoints; events visible in a UI; order timeline shows business history |
| NFR-7 — Clean failure | Customers see friendly errors, not stack traces | Validation errors, insufficient stock, not-found, conflict — each with the right status and a clear message |

---

## 6. Technology Mapping — every tech must justify itself with a business requirement

| Technology | Business requirements it satisfies | How (business-level contract, not code) |
|---|---|---|
| **PostgreSQL** | BR-STK-2 (stock truth), BR-PAY-8, NFR-5 | Owns the **state**: users, products, stock, orders, payments records, notifications, audit trail, outbox (event ledger) |
| **Redis** | BR-PRD-6/7, BR-CART-1/5/6, BR-STK-6 | Holds: product catalog cache (BR-PRD-6), the live cart (BR-CART-5), a fast stock pre-check counter (BR-STK-6). It is explicitly **not** the source of truth (business rule 6) |
| **Kafka** | BR-PAY-1/3/5, BR-ORD-8/9, BR-NOT-1/2, NFR-1/2/5 | Carries business events between the shop and the Payment Service, and inside the shop (notifications). Decouples the systems, survives restarts, redelivers on failure, quarantines bad messages |
| **Payment Service (2nd Spring Boot app)** | BR-PAY-1/2/4/7, Scenario 5/6 | Owns the **payment domain**: receives requests via Kafka, simulates bank processing (success/failure/delay), publishes results via Kafka. The shop reacts to results in its own transactions |
| **JWT auth** | BR-AUTH-1..5 | Stateless proof of identity for customers/admins; role checks at API level |
| **Spring Data JPA + Flyway** | NFR-5, BR-ORD-5 | Schema as code; transactions as the atomicity tool |
| **Thymeleaf + minimal JS frontend** | every scenario | Proves each requirement through the UI: browse, cart, checkout, live order status, admin screens, payment/order timeline |
| **Docker Compose** | NFR-2/6 | One command runs postgres, redis, kafka (+kafka-ui) so failures are reproducible |

**System context diagram (who talks to whom):**

```
                      ┌──────────────┐   HTTP/JSON   ┌──────────────────────────┐
                      │  FRONTEND    │ ────────────▶ │ SHOP (e-commerce API)    │
                      │  (Thymeleaf) │ ◀──────────── │  Spring Boot #1          │
                      └──────────────┘               │  users, catalog, cart,   │
                                                     │  orders, stock, outbox   │
                                                     └──────┬─────────┬─────────┘
                                                            │         │
                                      reads/writes state    │         │ events
                                                            ▼         ▼
                                                     ┌──────────┐  ┌──────────────┐
                                                     │PostgreSQL│  │    Kafka     │
                                                     │ truth    │  │ payment.requests
                                                     └──────────┘  │ payment.results
                                                                  │ order.* , dlt
                                                                  └──────┬─────────┘
                                                     ┌──────────┐         │ consume/produce
                                                     │  Redis   │         ▼
                                                     │ cache,   │  ┌──────────────────────────┐
                                                     │ carts,   │  │ PAYMENT SERVICE          │
                                                     │ counters │  │  Spring Boot #2 (mock)   │
                                                     └──────────┘  │  banks don't respond now │
                                                                   └──────────────────────────┘
```

---

## 7. Entities — Business-Level Definition (what exists, and why)

Defined by **business purpose**; field-level schema is your job (Flyway). Think about relationships, uniqueness, and what breaks if a field is missing.

| Entity | Business purpose | Key business facts it must hold | Owned by |
|---|---|---|---|
| **User** | Identify customers & admins | unique email, password (hashed), name, role, timestamps | Shop |
| **Category** | Organize the catalog | name (unique), description | Shop |
| **Product** | Sellable thing | unique SKU, name, description, price (exact decimal), currency, category, status (active/deactivated), image, timestamps | Shop |
| **Inventory** | Sellable quantity of a product | product (1:1), available quantity, reserved quantity, version (concurrency guard — required by BR-STK-4) | Shop |
| **Cart** | Customer's wishlist before checkout | user (1:1), items (product, quantity), expiry — **lives in Redis**, no DB table (BR-CART-5); checkout consumes and deletes it | Shop (Redis) |
| **Order** | The customer's commitment to buy | unique order number, user, status + legal transitions (BR-ORD-4), total, currency, shipping address, timestamps, version | Shop |
| **OrderItem** | What was bought at that moment | order, product, **snapshot** of name+price (BR-ORD-5), quantity, line total | Shop |
| **StockReservation** | The claim on stock made by an order | order, product, quantity, state (active/released) — makes release idempotent (BR-PAY-5) | Shop |
| **Payment** | The record of a payment attempt | order (1:1), amount, method, status (requested/in-progress/succeeded/failed), payment reference from Payment Service, timestamps | Shop (record) |
| **PaymentRequest / PaymentResult (events)** | The contract between the two services | order number/id, amount, currency; result: status, reference, timestamp | Kafka messages |
| **OutboxEvent** | The shop's promise that an event was handed over (BR-PAY-3) | aggregate type+id, event type, payload, processed flag/timestamp | Shop |
| **Notification** | Record of a delivered message | user, type, channel, subject/body, status, timestamps | Shop |
| **OrderEventLog** | Audit trail for the timeline (BR-ADM-2) | order, event type, detail, timestamp | Shop |

**Data ownership rule (important):**
- The **Shop** owns: users, catalog, stock, orders, cart, notifications, its own payment records.
- The **Payment Service** owns: the payment execution domain (its own DB is optional — a mock may be stateless; if it keeps state, it owns it).
- The two systems **never share a database**; they only share Kafka events. That is why BR-PAY-8 (atomicity) is solved with the shop's local transaction + idempotent handling, not with a distributed lock.

---

## 8. Business Contracts (the surfaces you design)

### 8.1 Frontend ↔ Shop (API the UI needs, business view)
- register/login
- browse catalog (paged, filtered, searchable)
- product detail
- cart: view, add, change qty, remove
- checkout (shipping address)
- my orders, order detail (+ timeline), cancel order
- admin: product CRUD + deactivate, stock adjust, all orders, all payments, event history

### 8.2 Shop ↔ Payment Service (event contract)
```
Shop ──▶ Kafka topic payment.requests:  {orderId, amount, currency, customerEmail}
PaymentService ──▶ Kafka topic payment.results: {orderId, paymentReference, status: SUCCESS|FAILED, reason?, processedAt}
```
- The Payment Service must reply to **every** request it accepts (success **or** failure) — silence is handled by the shop's timeout rule (BR-PAY-6).
- The contract must carry enough info for the shop to match the result to exactly one order.

---

## 9. Acceptance & Demo Scenarios (what "done" looks like)

1. Register customer + admin; admin-only screens reject the customer. ✔ BR-AUTH
2. Admin creates 3 products + stock; one product has **exactly 1 unit**. Catalog shows them. ✔ BR-PRD, BR-STK
3. Add to cart, close browser, reopen → cart persists. ✔ BR-CART
4. Two browsers checkout the last unit simultaneously → exactly one `CREATED`, one rejected; stock is correct (0) and never negative. ✔ BR-STK-3/4
5. Order page shows `CREATED`, then flips to `PAID` **on its own** within seconds — kafka-ui shows the request/result events moving. ✔ BR-ORD-9, BR-PAY-1/4
6. Buy a second product with forced payment failure → order `CANCELLED`, stock released and re-sellable, customer notified. ✔ BR-PAY-4/8
7. Product detail requested repeatedly → visible cache hits (HIT/MISS header); admin edits product → change appears immediately. ✔ BR-PRD-6/7
8. Kill the shop right after checkout → after restart the payment still gets processed; kill the Payment Service mid-processing → restart → result eventually applied, once. ✔ BR-PAY-3/5
9. Payment Service sends the same result twice → applied once. ✔ BR-ORD-8
10. Payment never answers → order auto-cancels after timeout, stock released. ✔ BR-PAY-6
11. Admin inspects one order's event timeline and payment list. ✔ BR-ADM-2, BR-PAY-7

---

## 10. Delivery Plan (10 working days) — business milestone view

| Day | Business milestone you must hit | Main topics you'll learn to get there |
|---|---|---|
| 1–2 | You can boot the shop, and the catalog reads from PostgreSQL | Project setup, Docker, JPA entities, migrations |
| 3 | Catalog API + clean errors; guest can browse | DTOs, validation, exception handling |
| 4 | Users can register/login; admin vs customer enforced | Spring Security, JWT, roles |
| 5 | Checkout works, stock never over-sold, orders have legal statuses | Transactions, pessimistic/optimistic locking, status machine |
| 6–7 | Payment handover works over Kafka; results arrive and the shop reacts; nothing lost on crash; duplicates harmless | Kafka basics, outbox pattern, idempotent consumers, retries + DLT |
| 8 | Hot-product scenario: cache absorbs reads, cart lives in Redis, carts expire | Redis cache-aside, invalidation, TTL, Redis cart |
| 9 | Payment microservice + full end-to-end demo incl. failure & timeout paths | Second Spring Boot app, event contract, boundary handling |
| 10 | Tests, README, mentor demo of scenarios 1–11 | Unit + Testcontainers integration tests, documentation |

---

## 11. Definition of Done (the mentor checks these)

- [ ] All business scenarios in section 4 work end-to-end through the UI.
- [ ] No over-selling, no negative stock, no illegal status transitions — proven by scenario 4.
- [ ] Payment flows async through a **separate** payment service; results applied exactly-once-in-effect; crash of either service loses nothing.
- [ ] Redis demonstrably serves catalog reads and the cart; it never decides business outcomes.
- [ ] Audit trail exists (order timeline + payment records).
- [ ] `mvn clean verify` green for both services; README explains run steps and the demo script.
