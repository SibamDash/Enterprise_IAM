# Performance & Scalability Log

## Current Benchmarks
- **Database:** PostgreSQL configured for generic multi-tenancy. Need to evaluate row-level security performance vs index performance at scale.
- **Stateless Tokens:** Using JWTs removes database lookup overhead for standard API requests, significantly improving response times compared to stateful session lookups, though Refresh Tokens do hit the database (acceptable trade-off since they are infrequent).
