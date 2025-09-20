# Useful Java Clients

Small collection of pragmatic Java clients and helpers to interact with common systems (Gmail API, Kafka, RabbitMQ, SQL, Temporal, Vault) with clear, minimal examples.

The repository favors simple, testable code and includes a `UsageExample` for each client package.

## Concept: Consume As A Dependency

This project is intended to be published to a private Maven registry and consumed as a library dependency from your applications. It is not a standalone application.

- Coordinates (adjust to your org if needed):
  - `groupId`: `com.example`
  - `artifactId`: `useful-java-clients`
  - `version`: `1.0.0`

### Add Dependency

Maven:
```
<dependency>
  <groupId>com.example</groupId>
  <artifactId>useful-java-clients</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle (Kotlin DSL):
```
implementation("com.example:useful-java-clients:1.0.0")
```

Publish this repository to your private registry (e.g., Nexus/Artifactory/GitHub Packages), and configure that repository in the consuming project accordingly.

## Requirements

- Java 21+
- Maven 3.8+

## Build & Test

- Build without tests: `mvn -DskipTests clean package`
- Run tests: `mvn test`

### Local Install For Development

- Install to local Maven cache for use in other projects on the same machine: `mvn -DskipTests clean install`

### Publishing

- Configure your private repository in `distributionManagement` (and credentials via `~/.m2/settings.xml`).
- Deploy: `mvn -DskipTests deploy`

Logging uses `slf4j-simple` and is configured by `src/main/resources/simplelogger.properties`.

## Project Structure

```
src/
├── main/java/
│   ├── client/
│   │   ├── gmail/          # Gmail API client + example
│   │   ├── kafka/          # Kafka producer/consumer/admin + example
│   │   ├── mq/             # RabbitMQ consumer + example
│   │   ├── sql/            # Minimal JDBC wrapper + example
│   │   ├── temporal/       # Temporal client + workflow interface + example
│   │   └── vault/          # Vault accessor + auth strategies + example
│   └── utils/
│       ├── CommonUtils     # Generic helpers (sleep, random, JSON read)
│       └── kafka/          # Kafka helpers (headers, wrappers)
├── test/java/
│   └── BaseTest            # Shared RestAssured logging setup
└── main/resources/
    ├── proto/              # Optional proto inputs for build pipeline
    └── simplelogger.properties
```

## Clients & Usage

All client packages contain a `UsageExample` class demonstrating basic usage. Below are quick references and setup notes.

### Gmail (`client.gmail`)

Files: `GmailClient`, `UsageExample`

Setup:
- Put your OAuth 2.0 client secrets as `src/main/resources/credentials.json`.
- First run opens a browser on `http://localhost:8888` to authorize; tokens are cached in `tokens/`.

Example:
```java
new client.gmail.UsageExample().example();
// Internally: var gmail = GmailClient.getService();
```

### Kafka (`client.kafka`)

Files: `KafkaProducer`, `KafkaProducerWithSchema`, `KafkaConsumer`, `KafkaConsumerWithSchema`, `GenericKafkaConsumer`, `KafkaAdmin`, `KafkaAdminClient`, `UsageExample`

Notes:
- Set `kafkaHost` (e.g., `localhost:9092`).
- Schema-aware classes use Confluent serializer/deserializer; set Schema Registry URL when needed.

Example — Admin and generic consumer:
```java
var ex = new client.kafka.UsageExample();
ex.adminExample();
ex.consumerExample();
```

To produce Protobuf messages, use `KafkaProducer` (bytes) or `KafkaProducerWithSchema` (schema registry) with `ProducerRecord<String, YourProto>`.

### RabbitMQ (`client.mq`)

Files: `RabbitConsumer`, `MqConsumer`, `MqProducer` (interface), `UsageExample`

Setup:
- Configure AMQP URI via `RabbitConsumer.setAddress("amqp://user:pass@host:5672/vhost")` before usage.

Example:
```java
new client.mq.UsageExample().example();
// Reads messages by routing key; see code comments and adjust binding.
```

### SQL (`client.sql`)

Files: `SqlDB`, `UsageExample`

Example:
```java
var db = new client.sql.SqlDB("jdbc:postgresql://host/db", "user", "pass");
var conn = db.getConnection();
client.sql.UsageExample.insertPublisherWallet(conn, "0x...", "name", new byte[]{1,2});
var dto = client.sql.UsageExample.getPublisherWallet(conn, "0x...");
db.closeConnection();
```

### Temporal (`client.temporal`)

Files: `TemporalClient`, `TemporalWorkflowExample`, `UsageExample`

Parameters for `TemporalClient` constructor:
- `target`: Temporal endpoint (e.g., `my.temporal.cloud:7233`)
- `clientKeyPathBase64`: base64-encoded PEM private key content
- `clientCertPathBase64`: base64-encoded X.509 client certificate
- `namespace`: Temporal namespace

Notes:
- Provide raw file contents encoded to base64, not file paths.
- Uses Bouncy Castle for PEM parsing and Netty tcnative BoringSSL.

Example:
```java
new client.temporal.UsageExample().example();
```

### Vault (`client.vault`)

Files: `Vault`, auth strategies: `RoleAuthStrategy`, `UserPassAuthStrategy`, `UsageExample`

Setup:
- `Vault.setAddress("http://127.0.0.1:8200");`
- `Vault.setPath("secret/data/my-app");` (KV v2 example)
- Set an auth strategy: `new RoleAuthStrategy(roleId, secretId)` or `new UserPassAuthStrategy(user, pass)`

Example:
```java
new client.vault.UsageExample().example();
```

## Utilities

### `utils.CommonUtils`
- `sleep(ms)` — simple sleep with interruption handling
- `randomChoice(list)` — pick a random element
- `generateRandomString(length)` — alphanumeric generator
- `unmarshall(value, TypeReference<T>)` — Jackson-based JSON read

### `utils.kafka`
- `KafkaUtils.getHeaders(traceId[, idempotencyId])` — common headers builder
- `KafkaMessageWrapper<T extends Message>` / `GenericKafkaMessageWrapper<T>` — message + headers + key
- `StringHeader` — `Header` implementation for string values

## Troubleshooting

- Gmail: ensure `credentials.json` exists in `src/main/resources`; first run requires browser auth; tokens stored under `tokens/`.
- Kafka: broker and (optionally) Schema Registry must be reachable; group cleanup uses `KafkaAdminClient`.
- RabbitMQ: set AMQP URI via `RabbitConsumer.setAddress(...)` before `getInstance()`; queues/exchanges must exist or be declared as in your flow.
- Temporal: pass base64-encoded key/cert contents; ensure endpoint/namespace are correct.
- Vault: set address, path and an auth strategy; KV path should match your engine/version.

## Adding a New Client

1. Create a package under `client/<name>`.
2. Implement the client with minimal external dependencies.
3. Add `UsageExample.java` in the same package with a concise, compiling demo.
4. Update this README if public-facing behavior changes.
