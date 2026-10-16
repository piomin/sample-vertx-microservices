### AI Agents Best Practices for Code Generation

This document outlines best practices for generating code within this repository using AI agents.

#### 1. Project Overview
*   **Stack:** Vert.x 5.x, Java 21, Maven multi-module project.
*   **Architecture:** Two asynchronous microservices communicating via HTTP, with service discovery through Consul and MongoDB as the data store.
*   **Modules:**
    *   `account-vertx-service` — CRUD API for accounts (port 2222)
    *   `customer-vertx-service` — CRUD API for customers, calls Account Service to enrich responses (port 3333)
*   **Key patterns used:**
    *   **Verticles** (`AbstractVerticle`) — each service deploys a `MongoVerticle` (database setup + service proxy registration) and a main server verticle (`AccountServer` / `CustomerServer`) with HTTP routing.
    *   **Service Proxy** (`@ProxyGen`) — repository interfaces generate event bus proxies via `vertx-codegen`. Communication between verticles happens over the Vert.x event bus, not direct method calls.
    *   **Data Objects** (`@DataObject`) — domain classes with a `JsonObject` constructor and `toJson()` method, required by the codegen/service-proxy layer.
    *   **Module Gen** (`@ModuleGen` in `package-info.java`) — declares the codegen module for each service.
    *   **Config** — loaded from `src/main/resources/application.json` via `vertx-config` (`ConfigRetriever`).
    *   **Consul integration** — Account Service registers itself in Consul; Customer Service imports services from Consul via `ConsulServiceImporter`.
    *   **REST routing** via `vertx-web` `Router` with `BodyHandler` and `ResponseContentTypeHandler`.
    *   **Inter-service calls** — Customer Service discovers Account Service through Consul `ServiceDiscovery` and calls it with `WebClient` (see `AccountClient`).
*   **Package structure:** `pl.piomin.services.vertx.account.*` and `pl.piomin.services.vertx.customer.*`.

#### 2. Code Generation (vertx-codegen)
*   The `vertx-codegen` annotation processor generates event bus proxy classes (`*VertxEBProxy`, `*VertxProxyHandler`) into `src/main/generated/`.
*   These files are **generated artifacts** — do not edit them manually. They are regenerated on every `mvn clean compile`.
*   The `maven-clean-plugin` is configured to delete `src/main/generated/` on clean, and `build-helper-maven-plugin` adds it as a source directory.
*   When adding a new `@ProxyGen` interface, you must also create a `package-info.java` with `@ModuleGen` in the same package.

#### 3. Running the Application
Prerequisites: MongoDB on port 27017, Consul agent on port 8500.

```bash
# Build
mvn clean package

# Run (each in a separate terminal)
java -jar account-vertx-service/target/account-vertx-service-*-runner.jar
java -jar customer-vertx-service/target/customer-vertx-service-*-runner.jar
```

Configuration is loaded from each module's `src/main/resources/application.json`:
```json
{
  "port": 2222,
  "discovery": { "host": "localhost", "port": 8500 },
  "datasource": { "host": "localhost", "port": 27017, "db_name": "test" }
}
```

#### 4. Code Generation Guidelines
*   **Be Specific:** Instead of "add a new endpoint," use "add a GET endpoint `/account/balance/{id}` to `AccountServer` that returns only the balance field."
*   **Reference Existing Patterns:**
    *   For new domain classes — follow `Account` / `Customer` (must be `@DataObject` with `JsonObject` constructor and `toJson()`).
    *   For new repositories — follow `AccountRepository` / `CustomerRepository` (`@ProxyGen` interface + `*Impl` with `MongoClient`, plus `package-info.java` with `@ModuleGen`).
    *   For new verticles — follow `MongoVerticle` (database + `ServiceBinder` registration) and `AccountServer` / `CustomerServer` (HTTP routing + config).
    *   For inter-service communication — follow `AccountClient` (Consul discovery + `WebClient`).
*   **Async-first:** All repository and service methods must return `Future<T>`. Never use blocking calls. Compose futures with `.compose()` / `.map()`, handle results with `.onComplete()`.
*   **Iterative Development:** For complex features, break down: domain model -> repository interface + impl -> service proxy registration in MongoVerticle -> HTTP routes in server verticle -> tests.

#### 5. Code Quality and Style
*   **Java 21:** Use Java 21 features where appropriate (records for simple DTOs if no `@DataObject` is needed, text blocks, pattern matching).
*   **Vert.x conventions:** Use `Future`-based APIs (Vert.x 5 style), not callback-based `Handler<AsyncResult<T>>`. Use `Json.encodePrettily()` for responses. Use `JsonObject.mapFrom()` for object-to-JSON conversion.
*   **Logging:** Use SLF4J (`LoggerFactory.getLogger()`), not `System.out`.
*   **No blocking:** Never perform blocking operations on the event loop. If unavoidable, use `vertx.executeBlocking()`.

#### 6. Testing

##### Obowiązkowa weryfikacja po każdej zmianie
**Po każdej zmianie w kodzie agent MUSI uruchomić testy i upewnić się, że przechodzą.** Nie zgłaszaj zakończenia zadania, dopóki testy nie będą zielone. Jeśli testy nie przechodzą — napraw problem i uruchom je ponownie.

##### Uruchamianie testów
```bash
# Wszystkie moduły
mvn clean test

# Pojedynczy moduł (preferowane przy zmianach w jednym module)
mvn clean test -pl account-vertx-service
mvn clean test -pl customer-vertx-service
```

##### Wymagania
*   **Docker musi być uruchomiony** — testy korzystają z Testcontainers do podniesienia MongoDB i Consul. Bez Dockera testy nie wystartują.
*   Testy są w pełni samowystarczalne — nie wymagają ręcznego uruchamiania żadnych zewnętrznych usług.

##### Framework i wzorce
*   **JUnit 5** z `@ExtendWith(VertxExtension.class)` i `VertxTestContext`.
*   **Testcontainers:** `MongoDBContainer` (mongo:5.0) i `ConsulContainer` (consul:1.14) — uruchamiane w `@BeforeAll`, zatrzymywane w `@AfterAll`.
*   **Kolejność testów:** `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` z `@Order` — testy tworzące dane (`shouldAddNew`) muszą mieć niższy order niż testy je odczytujące.
*   **Wzorzec testu:**
    1.  `@BeforeAll` — start kontenerów, deploy `MongoVerticle` z portem z Testcontainers, deploy Server verticle.
    2.  Każdy `@Test` — tworzenie `WebClient`, wysyłanie requestu HTTP, asercje w callback `onSuccess`, zakończenie przez `testContext.completeNow()`, obsługa błędów przez `testContext::failNow`.
*   **Referencja:** `AccountServerTests` (port 2222), `CustomerServerTests` (port 3333).

##### Pisanie nowych testów
*   Każdy nowy endpoint lub metoda repozytorium **musi** mieć odpowiedni test.
*   Nowe testy dodawaj do istniejącej klasy testowej danego modułu, zachowując konwencję `@Order`.
*   Używaj `WebClient` do testowania endpointów (testy integracyjne), nie mockuj repozytorium.

##### CI
*   CircleCI uruchamia `mvn test` na maszynie z Dockerem (Ubuntu). Konfiguracja w `.circleci/config.yml`.
*   SonarCloud analizuje jakość kodu (osobny job `analyze`).

#### 7. Critical Restrictions
*   **Do not commit `target/`** or `src/main/generated/` — both are generated artifacts.
*   **Do not edit generated files** in `src/main/generated/` — they will be overwritten on next build.

#### 8. Build Verification
*   **Compile check:** `mvn clean compile` — ensures code compiles and codegen runs successfully.
*   **Run tests:** `mvn test` — requires Docker for Testcontainers.
*   **CI:** CircleCI (`.circleci/config.yml`). SonarCloud is used for code quality analysis.
