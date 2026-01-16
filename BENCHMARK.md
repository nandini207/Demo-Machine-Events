# Performance Benchmark – Machine Event Ingestion

## What I Tested

Wanted to see how well the ingestion pipeline handles different load scenarios - batch processing and concurrent requests.

---

## Test Setup

- Running on: My local laptop
- OS: Windows
- Java: 17
- Framework: Spring Boot
- Database: H2 (in-memory)
- Build: Maven

Not exactly a production environment, but good enough to validate the approach.

---

## Test 1: Batch Ingestion

### What I did

- Created 1000 events
- Each with a unique eventId
- Valid durations and event times
- Sent them all through `/events/batch`

### Results

- All 1000 events got ingested
- Took less than a second
- Zero duplicates or rejections

### Why it's fast

The batch endpoint processes everything in one transaction, so there's minimal overhead. Database constraints handle the heavy lifting.

---

## Test 2: Duplicate Detection

### What I did in Test 2

- Sent the same 100 events twice

### Results of Test 2

- First batch: all accepted
- Second batch: all deduplicated, nothing written

### Takeaway

Payload hashing works well here - we can quickly tell if we've seen an event before without comparing every single field.

---

## Test 3: Concurrent Requests

### What I did

- Spun up 10 threads
- Each trying to insert the same eventId at the same time
- Used ExecutorService in a JUnit test

### Results

- Exactly 1 record made it to the database
- No race conditions
- No weird inconsistent states

### Why it works

The unique constraint on eventId plus transactional boundaries means the database itself prevents duplicates. First one wins, others get rejected cleanly.

---

## Limitations

Some things to keep in mind:

- H2 is way faster than a real production database, so these numbers are optimistic
- No network latency since everything's local
- Didn't use proper load testing tools like JMeter
- Batch sizes are pretty small compared to real factory data

---

## What Could Be Better

For a production system, I'd want to:

- Test with PostgreSQL or MySQL to get realistic performance numbers
- Use JMeter or Gatling for proper load testing
- Measure p95/p99 latencies, not just averages
- Maybe add async processing for really large batches
- Test with much larger datasets (100k+ events)

---

## Conclusion

For this assignment, the system handles the load fine. Deduplication works, concurrent requests don't break things, and batch processing is reasonably fast. Obviously there's room for improvement if this were going to production, but it meets the requirements.
