# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview
The `compliance-review/` directory contains a Spring Boot 3.2.5 (Java 21) backend for an intelligent compliance review system. It uses MySQL 8 as its sole data store and a mock LLM model for MVP.

### Prerequisites
- Java 21 is pre-installed. Maven and MySQL 8 are installed by the update script.
- MySQL 8 must be running. Start with `sudo service mysql start`.
- Database and user must exist (see below).

### Database Setup (one-time)
```bash
sudo service mysql start
sudo mysql -u root -e "
  CREATE DATABASE IF NOT EXISTS compliance_review DEFAULT CHARSET utf8mb4;
  CREATE DATABASE IF NOT EXISTS compliance_review_test DEFAULT CHARSET utf8mb4;
  CREATE USER IF NOT EXISTS 'compliance'@'localhost' IDENTIFIED BY 'compliance123';
  GRANT ALL PRIVILEGES ON compliance_review.* TO 'compliance'@'localhost';
  GRANT ALL PRIVILEGES ON compliance_review_test.* TO 'compliance'@'localhost';
  FLUSH PRIVILEGES;
"
```

### Common Commands
All commands run from `/workspace/compliance-review/`:

| Action | Command |
|---|---|
| Compile | `mvn compile` |
| Unit tests | `mvn test` |
| Run app (dev) | `mvn spring-boot:run` |
| Package | `mvn package -DskipTests` |

The app starts on port **8080**. Schema auto-initializes via `spring.sql.init` from `src/main/resources/db/schema.sql`.

### Key Gotchas
- The JDBC URL must use `characterEncoding=UTF-8` (not `utf8mb4`) — the MySQL Connector/J driver does not recognize `utf8mb4` as a Java charset.
- The `spring.sql.init.mode=always` means schema.sql runs on every startup; all DDL uses `CREATE TABLE IF NOT EXISTS` and `ON DUPLICATE KEY UPDATE` so it's safe for repeated runs.
- JSON columns in entities are mapped as `String`; serialize/deserialize manually with Jackson `ObjectMapper`.
- The AI module uses a `MockChatModel` that detects keywords like "保本", "收益率", "稳赚" to generate realistic violation results without an actual LLM.

### API Exploration
Swagger UI is available at `http://localhost:8080/swagger-ui.html` and OpenAPI spec at `/v3/api-docs`.

### Running the App
1. `sudo service mysql start` (if not already running)
2. `cd /workspace/compliance-review && mvn spring-boot:run`
3. The app auto-creates tables from `schema.sql` on startup and inserts a default tenant + admin user.

### Testing a Review (hello world)
```bash
# Create & publish a law article
curl -X POST http://localhost:8080/api/v1/law/articles -H "Content-Type: application/json" \
  -d '{"sourceId":1,"lawName":"证券期货投资者适当性管理办法","articleId":"第二十条第一款","originalText":"禁止使用保本、无风险等宣传用语","normType":"禁止","authorityLevel":3}'
curl -X PUT "http://localhost:8080/api/v1/law/articles/1/publish?confirmedBy=1"

# Submit compliance review
curl -X POST http://localhost:8080/api/v1/review/submit -H "Content-Type: application/json" \
  -d '{"tenantId":1,"submittedBy":1,"contentType":"营销海报","productType":"公募基金","originalContent":"保本保收益，零风险！"}'
```
