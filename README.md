# Backend Intern Assignment – Buyogo

## Overview

This backend system processes machine events from a factory floor. It handles incoming events, removes duplicates, updates records when needed, and generates statistics for a given time period.

Built with **Java + Spring Boot** and uses **H2 in-memory database** for local development.

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- H2 Database (in-memory)
- Maven
- JUnit 5

---

## Architecture

Pretty standard layered approach here:

Controller → Service → Repository → Database

- **Controller layer**: REST endpoints that handle requests and responses
- **Service layer**: Where the actual business logic lives - validation, deduplication, updates, stats
- **Repository layer**: Database operations through Spring Data JPA
- **Database**: H2 running in-memory for quick local testing

Keeping things separated like this makes debugging and testing way easier.

---

## Data Model

### MachineEvent Entity

Events get stored in the `machine_event` table:

- `id` - auto-generated primary key
- `eventId` - unique identifier for each event
- `eventTime` - when the event actually happened (used for time-based queries)
- `receivedTime` - when we got the event (helps decide which update is newer)
- `machineId` - which machine this came from
- `durationMs` - how long the event lasted
- `defectCount` - number of defects detected
- `payloadHash` - SHA-256 hash to quickly check if events are identical

There's a unique constraint on `eventId` so we don't accidentally store the same event twice.

---

## How Ingestion Works

When an event comes in, here's what happens:

1. **Validation checks**
   - Duration must be between 0 and 6 hours
   - Event time can't be more than 15 minutes in the future

2. **Deduplication**
   - If we've seen this exact event before (same eventId and payload), we skip it

3. **Updates**
   - If the eventId exists but the payload is different:
     - We update it only if this new event has a newer receivedTime
     - Otherwise, we ignore it (the existing one is more recent)

Using SHA-256 hashes makes comparing payloads pretty fast instead of checking every field.

---

## Thread Safety

Handling concurrent requests safely through:

- Database unique constraint on `eventId` prevents duplicates at the DB level
- `@Transactional` on service methods
- Letting the database handle atomicity instead of manual locking

Tested with multiple threads hitting the same endpoint simultaneously - works without issues.

---

## Performance

Keeping things fast by:

- Processing batches in a single transaction
- Pushing aggregation logic to the database
- Using H2 in-memory for speed during development

Can handle 1000 events in under a second on my laptop.

---

## API Endpoints

### Machine Stats

`GET /events/stats`

Shows:

- How many events happened in the time window
- Total defects (ignoring -1 values)
- Average defect rate per hour
- Health status (Healthy or Warning)

Time window is:

- `start` - inclusive
- `end` - exclusive

---

### Top Defect Lines

`GET /events/stats/top-defect-lines`

Note: Treating each `machineId` as a separate production line

Returns for each line:

- Line identifier
- Total defects found
- Number of events
- Defects per 100 events

---

## Testing

Wrote 8+ unit tests covering:

- Deduplication works correctly
- Updates only happen with newer receivedTime
- Validation catches bad data
- Defect calculations follow the rules
- Time windows work as expected
- Concurrent requests don't cause issues

All tests use SpringBootTest with H2.

---

## Setup & Run

### What you need

- Java 17
- Maven

### Running it

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`

---

## What I Learned

This was my first time really diving into:

- Handling concurrent database operations properly
- Using payload hashing for efficient deduplication
- Writing transactional code that's actually thread-safe

The trickiest part was getting the update logic right - making sure we only accept newer events without creating race conditions.
