# Notification and event flows

StreamSphere currently uses RabbitMQ for asynchronous email notifications and
Spring WebSocket/STOMP for real-time in-app notifications. They serve different
purposes and have different persistence and delivery behavior.

## RabbitMQ email notifications

### Topology

Central publishes JSON-serialized `NotificationMessage` objects to a configured
direct exchange and routing key. Notification service consumes from the
configured durable queue. A dead-letter exchange and queue are configured for
rejected messages.

The queue name, exchange name, and routing key must match between Central and
Notification. The Render Blueprint expects RabbitMQ TLS settings and broker
credentials to be configured for both services.

### Message shape

Current shared message fields:

| Field | Meaning |
| --- | --- |
| `type` | Notification type string |
| `email` | Recipient email |
| `name` | Context value used by the matching notification handler |

The field called `name` is context-dependent; for example, the current producer
may place a media URL or video ID there. When changing this contract, update
both producer and consumer and document the type-specific meaning.

### Supported message types

The Notification service recognizes these enum values:

| Type | Intent |
| --- | --- |
| `user_registration` | Welcome/registration email |
| `subscriber_added` | Subscriber-related email |
| `create_channel` | Channel creation notification |
| `new_video` | Notify a subscriber about a new video |
| `video_liked` | Notify a creator about a like |
| `new_comment` | Notify a creator about a comment |
| `video_processed` | Notify the creator that video processing/completion occurred |

Message types are case-sensitive strings and should match the enum spelling.

### Producer, consumer, and failure behavior

1. Central creates a `NotificationMessage` and publishes it to RabbitMQ.
2. Notification service deserializes it and dispatches by `type`.
3. The matching handler sends email using the configured SMTP provider.
4. The listener has bounded retry settings; rejected messages are not endlessly
   requeued and may be routed to the dead-letter queue.

Current reliability limitations to understand:

- Central's RabbitMQ producer catches publish-time runtime exceptions and logs
  a warning, allowing the primary operation to continue. This is best-effort
  publication, not a transactional outbox.
- Notification email exceptions are logged by the listener. Current code does
  not rethrow these exceptions, so an SMTP failure can be treated as handled
  rather than reaching broker retry/dead-letter behavior.
- Duplicate suppression is an in-memory FIFO map capped at 1,000 keys. It is
  not durable across restart and is not coordinated across multiple
  Notification instances.
- These behaviors are reasonable points for production-hardening discussion;
  do not claim guaranteed exactly-once email delivery.

### Configuration

Shared broker settings:

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VIRTUAL_HOST`
- `SPRING_RABBITMQ_SSL_ENABLED`
- `RABBITMQ_EXCHANGE_NAME`
- `RABBITMQ_QUEUE_NAME`
- `RABBITMQ_ROUTING_KEY`

Notification retry configuration also supports:

- `RABBITMQ_RETRY_ENABLED`
- `RABBITMQ_RETRY_INITIAL_INTERVAL`
- `RABBITMQ_RETRY_MULTIPLIER`
- `RABBITMQ_RETRY_MAX_ATTEMPTS`
- `RABBITMQ_REQUEUE_REJECTED`

SMTP and branding values are documented in
[Development and deployment](../development-and-deployment.md) and
`backend/.env.example`.

## In-app notifications over WebSocket

Central persists in-app notification records in PostgreSQL and then publishes
them to a user topic:

```text
/topic/notifications/{userId}
```

The SockJS/STOMP endpoint is:

```text
/api/v1/central/ws
```

The broker prefix is `/topic`; the application destination prefix is `/app`.
Persisted notification records can be listed, counted, and marked read through
the Central notifications REST API. This path is not the same as the email
RabbitMQ consumer.

## Adding or changing an event

1. Decide whether the feature needs a durable in-app record, an email, or both.
2. Define the type and any context carried in `NotificationMessage`.
3. Update the Central producer and Notification consumer together.
4. Add/update handler tests and keep the routing key and broker topology
   consistent in both services.
5. Document failure, retry, and duplicate behavior; avoid sensitive data in
   message bodies and logs.
6. For stronger delivery guarantees, consider a transactional outbox and a
   durable idempotency key/store before describing delivery as reliable.
