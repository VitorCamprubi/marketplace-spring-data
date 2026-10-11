# Marketplace — Spring Data com múltiplos bancos

Projeto desenvolvido durante o **Bootcamp CI&T – Java com IA (DIO)**, no módulo de Spring Data.

Um marketplace de ingressos dividido em **bounded contexts**, e cada contexto usa a tecnologia de persistência que faz mais sentido para ele, sem perder a mesma arquitetura em camadas.

## Contextos

| Contexto       | Responsabilidade                               | Persistência                         |
|----------------|------------------------------------------------|--------------------------------------|
| `registration` | Cadastro de clientes                           | MySQL (porta 3307)                   |
| `catalog`      | Vitrine de eventos e metadados (setores/assentos) | MySQL (3308) + MongoDB (27018) + Redis como cache (6380) |
| `ticketing`    | Seleção de assentos                            | PostgreSQL (5433) + Redis para lock (6381) |

Os contextos conversam por **eventos de aplicação** (`CustomerCreated`, `EventUpdated`): quando um cliente é cadastrado ou os metadados de um evento mudam, o `ticketing` recebe o evento de forma assíncrona e cria a sua própria cópia dos dados.

## Tecnologias

- Java 25 + Spring Boot 4
- Spring Data JPA (MySQL e PostgreSQL), Spring Data MongoDB, Spring Data Redis (Jedis)
- Spring Data REST + HAL Explorer
- Spring Cache, `@Async` e virtual threads
- Spring Boot Actuator
- Docker Compose
- Lombok

## Estrutura

```text
src/main/java/dio/marketplace
├── common/        -> DTOs de eventos compartilhados entre contextos
├── registration/  -> domain + infrastructure (JPA, Data REST, projeção, event handler)
├── catalog/       -> domain + application + infrastructure (JPA, Mongo, cache, listeners)
└── ticketing/     -> domain + application + infrastructure (JPA, Redis, HTTP, listeners)
```

Cada contexto segue o mesmo padrão: `domain/` (entidades, value objects e contratos), `application/` (casos de uso) e `infrastructure/` (adaptadores).

## O que aprendi

- **Vários datasources na mesma aplicação**: cada contexto tem sua própria `*Configuration` com `DataSource`, `EntityManagerFactory` e `TransactionManager`, separados com `@Qualifier` e `@EnableJpaRepositories(basePackages = ...)`.
- **Escolher o banco pelo uso**: dados relacionais em MySQL/PostgreSQL, documentos flexíveis (setores, assentos, requisitos técnicos) no MongoDB, dados temporários no Redis.
- **Spring Data REST**: expõe repositórios como API HAL sem escrever controller, com projeções (`CustomerExcerpt`) e controle do que é exportado (`@RestResource(exported = false)`).
- **Cache com Redis**: `@Cacheable` na vitrine, ignorando resultados vazios.
- **Lock de assento com TTL**: a chave `eventId:seatId` fica 30 segundos no Redis (`@RedisHash(timeToLive = 30)`), o que evita duas pessoas reservarem o mesmo assento ao mesmo tempo.
- **Diferentes formas de reagir a mudanças**: `@EntityListeners` do JPA (`@PostPersist`...), `AbstractMongoEventListener` do Mongo, `@RepositoryEventHandler` do Data REST e `@EventListener` + `@Async` do Spring.
- **Enriquecimento assíncrono**: `EventEnricher` busca os metadados no Mongo em paralelo usando `CompletableFuture`.

## Como rodar

Pré-requisito: Docker em execução. O `spring-boot-docker-compose` sobe os 6 containers do `compose.yml` automaticamente no `bootRun`.

```bash
./gradlew bootRun
```

Endpoints úteis:

- `http://localhost:8080/` — HAL Explorer (Spring Data REST)
- `http://localhost:8080/customers` — clientes (registration)
- `http://localhost:8080/events` — eventos do catálogo (os metadados do Mongo também aparecem no HAL Explorer)
- `GET /showcase` — vitrine de eventos com metadados (com cache)
- `POST /ticketing/events/{eventId}/seats/select` — seleciona um assento (header `X-CUSTOMER-ID`)
- `http://localhost:8080/actuator/health` — saúde da aplicação e dos bancos

Exemplo de seleção de assento:

```bash
curl -i -X POST http://localhost:8080/ticketing/events/{eventId}/seats/select \
  -H "Content-Type: application/json" \
  -H "X-CUSTOMER-ID: {customerId}" \
  -d '{"id":"A1"}'
```

> O teste `contextLoads` precisa dos bancos no ar, então suba o `compose.yml` (`docker compose up -d`) antes de rodar `./gradlew test`.
