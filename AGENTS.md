## Cursor Cloud specific instructions

### Project overview
Spring Boot 3.2.5 / Java 21 review system located at `/workspace/review-system/`. Uses Maven for builds, H2 in-memory DB for tests, MySQL for production.

### Build / Test / Run
- **Compile**: `mvn compile` (from `/workspace/review-system/`)
- **Test**: `mvn test` — uses H2 in-memory DB via `application-test.yml` (profile `test`), no external services needed
- **Run (dev)**: Requires MySQL at `localhost:3306` (see `application.yml`). Redis, Elasticsearch, Neo4j autoconfiguration is excluded so the app starts without them.

### Key conventions
- Entities: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@PrePersist`/`@PreUpdate` for timestamps
- Services: interface + impl pattern, `@Transactional` on writes
- Controllers: `@Tag`/`@Operation` OpenAPI annotations, `CommonResult<T>` wrapping
- VOs: separate `CreateReqVO`, `RespVO`, `PageReqVO` per domain object
- Package structure: `com.review.module.{system,knowledge,review,llm,agent,cases,feedback,rules,checklist,filter,parser,context,pipeline,verification,dashboard}` and `com.review.infrastructure.{vector,search,graph,port}`

### Gotchas
- The test profile excludes Redis, Elasticsearch, and Neo4j auto-configuration. If you add new Spring Data stores, add exclusions to `application-test.yml`.
- JPA `ddl-auto` is `none` in production (schema managed by `schema.sql`), `create-drop` in tests.
- Lombok + MapStruct annotation processors are configured in `pom.xml` compiler plugin.
- External integrations (Milvus, Elasticsearch, Neo4j) use `@ConditionalOnProperty` with in-memory fallback implementations. The app runs fully without them.
- MySQL must be running for dev mode. To start: ensure `mysqladmin status` succeeds, then create db/user with `mysql -u root -e "CREATE DATABASE IF NOT EXISTS review_system; CREATE USER IF NOT EXISTS 'review'@'localhost' IDENTIFIED BY 'review123'; GRANT ALL PRIVILEGES ON review_system.* TO 'review'@'localhost'; FLUSH PRIVILEGES;"`
- If port 8080 is occupied, pass `--server.port=8081` to `spring-boot:run`.
