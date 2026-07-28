# AMQP publish-ordering lab

**Question:** do sequential calls to `RabbitTemplate.send()` leave the messages in the queue
in that same order?

**Answer:** not with the configuration `hub/dispatcher` runs. With
`spring.rabbitmq.publisher-confirm-type=correlated`, a single-threaded loop of sequential
`send()` calls **lands in the queue out of order**, intermittently, starting from bursts of
about a dozen messages. Turning confirms off, or pinning the channel with
`rabbitTemplate.invoke(...)`, restores strict ordering.

The cause is not the confirm protocol. It is that Spring's `CachingConnectionFactory` refuses
to return a channel to the cache while that channel still has unconfirmed messages, so a
publish loop keeps opening **new** channels — and AMQP only guarantees FIFO **per channel**.

---

## Observing it

Requires Docker and JDK 21; the Gradle wrapper is included.

```bash
cd tools/amqp-ordering-lab
./observe-confirms.sh                 # the focused demonstration, ~2 min
KEEP_BROKER=1 ./observe-confirms.sh   # keep the broker to browse http://localhost:15672 (lab/lab)
```

`observe-confirms.sh` boots RabbitMQ 4.1, builds the jar, and runs five steps that isolate the
cause by changing exactly one thing at a time. Each step publishes N sequence-numbered
messages, waits until the broker reports all N queued, and only *then* starts a
single consumer with `prefetch=1` — so the delivery order it records **is** the queue's order.

| step | what changes | expected |
|---|---|---|
| 1 | confirms **off**, 2000 sequential sends | ORDER PRESERVED, 0 new channels |
| 2 | confirms **`correlated`**, same code | ORDER BROKEN, dozens of new channels |
| 3 | confirms `correlated`, **5 ms between sends** | ORDER PRESERVED |
| 4 | confirms `correlated`, wrapped in **`invoke()`** | ORDER PRESERVED |
| 5 | burst-size sweep, 3 runs each | shows it is a race, not a threshold |

To run one variant by hand:

```bash
# Step 2 on its own — the dispatcher's configuration
PUBLISHER_CONFIRM_TYPE=correlated PUBLISHER_RETURNS=true TEMPLATE_MANDATORY=true \
  java -jar build/libs/amqp-ordering-lab.jar \
  --lab.scenario=confirms --lab.messages=2000 \
  --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1
```

`./run-lab.sh` runs the wider matrix (producer threads, consumer concurrency, requeue, quorum
queues) and is summarised in the appendix. Logs land in `build/lab-logs/`.

---

## What was observed

RabbitMQ 4.1, Spring Boot 4.0.6 / spring-rabbit 4.0.3, single-node broker on localhost,
one publishing thread, one consumer, `prefetch=1`, 2000 messages.

| step | new channels during publish | out-of-order steps | verdict |
|---|---|---|---|
| 1 — confirms off | **0** (cached channel reused) | 0 | **ORDER PRESERVED** |
| 2 — confirms `correlated` | **85** (~23 msg/channel) | 449 | **ORDER BROKEN** |
| 3 — confirms + 5 ms pacing | 0 | 0 | **ORDER PRESERVED** |
| 4 — confirms + `invoke()` | 0 (channel pinned) | 0 | **ORDER PRESERVED** |

Step 2's first inversion, verbatim from the report — message 7 overtook message 6:

```
 new channels opened : 85 during publish   <-- MORE CHANNELS THAN THREADS (~23 msg/channel)
 vs publish order    : REORDERED  (out-of-order steps=449, max displacement=21)
 first inversion at  : index 6 — observed[3..10] = [3, 4, 5, 7, 6, 8, 9, 10]
```

Steps 1 and 2 were each repeated three times: step 1 gave 0 channels / 0 inversions every
time, step 2 gave 83–91 channels and 442–472 inversions every time. The effect is not a fluke.

### Step 5 — burst size sweep

| burst | new channels | out-of-order steps (3 runs) | verdict |
|---|---|---|---|
| 5 | 2 | 0, 0, 0 | preserved ×3 |
| 10 | 3 | 0, 0, 2 | **broken 1 of 3** |
| 15 | 4 | 0, 2, 1 | **broken 2 of 3** |
| 25 | 6 | 6, 1, 3 | broken ×3 |
| 100 | 13 | 7, 11, 10 | broken ×3 |
| 1000 | 37–42 | 164, 206, 200 | broken ×3 |

There is **no safe threshold**. Ten messages published back-to-back already reordered on one
of three runs; fifteen on two of three. Displacement grows with burst size (up to 21
positions at 2000 messages), but the failure starts almost immediately. This is why the
behaviour is easy to miss: it needs concurrency in flight, so it is invisible in a
low-rate functional test and appears under load.

---

## Why it happens

`spring-rabbit-4.0.3`, `CachingConnectionFactory.CachedChannelInvocationHandler.returnToCache`
(lines 1310–1347). When a proxied channel is closed — which `RabbitTemplate.send()` does after
every single call — this runs:

```java
private void returnToCache(ChannelProxy proxy) {
    if (CachingConnectionFactory.this.active
            && this.publisherConfirms
            && proxy instanceof PublisherCallbackChannel publisherCallbackChannel) {

        this.theConnection.channelsAwaitingAcks.put(this.target, proxy);   // parked, NOT cached
        AtomicBoolean ackCallbackCalledImmediately = new AtomicBoolean();
        publisherCallbackChannel.setAfterAckCallback(c -> {
            ackCallbackCalledImmediately.set(true);
            doReturnToCache(this.theConnection.channelsAwaitingAcks.remove(c));  // cached only now
        });
        ...
    }
    else {
        doReturnToCache(proxy);           // no confirms: straight back to the cache
    }
}
```

So the two configurations behave completely differently:

- **Confirms off** — the channel goes straight back into the cache. The next `send()` finds it
  there and reuses it. All 2000 publishes travel over **one** channel, and AMQP's per-channel
  FIFO guarantee means the queue order is exactly the publish order. Measured: 0 new channels,
  0 inversions.

- **Confirms on** — the channel is parked in `channelsAwaitingAcks` and only returned to the
  cache later, from the broker's ack callback. The next `send()` therefore finds the cache
  empty and `createBareChannel(...)` opens a **fresh** channel (line ~1219). Under a burst,
  publishing runs ahead of the acks, so the loop keeps minting channels: 85 of them for 2000
  messages, ~23 messages each. Once messages for the same queue are in flight on several
  channels concurrently, the broker is free to enqueue them in any interleaving — nothing in
  AMQP orders writes *across* channels.

Steps 3 and 4 confirm this is the mechanism rather than a plausible story:

- **Step 3 (pacing)** keeps confirms enabled and only inserts 5 ms between sends. Each ack now
  arrives before the next publish, `doReturnToCache` runs in time, the same channel is reused,
  and ordering is perfect. Confirms are therefore *not* inherently unordered — the reordering
  needs unconfirmed messages outstanding when the next send happens.
- **Step 4 (`invoke()`)** keeps the burst rate and pins one channel to the thread for the whole
  scope, bypassing the borrow-return cycle entirely. 0 new channels, 0 inversions.

Note the channel count is not the whole story: a channel *switch* is only harmful when writes
overlap. In step 3, 1–3 channels were still created across runs, yet order held, because each
message was fully acked before the next was published.

---

## What this means for `hub/dispatcher`

The dispatcher matches step 2's shape exactly:

- `src/main/resources/application.properties:15` sets
  `spring.rabbitmq.publisher-confirm-type=correlated` (plus `publisher-returns=true` and
  `template.mandatory=true`).
- The `dispatch` `@RabbitListener` (`service/Dispatcher.java:191`) runs with Boot's default
  `concurrentConsumers=1` — no `spring.rabbitmq.listener.*` property is set anywhere — so it
  is a single thread issuing sequential publishes.
- Each consumed message triggers a plain `rabbitTemplate.send(...)`:
  `Dispatcher.java:274` (recipient queue), `Dispatcher.java:319` (transfer exchange),
  `MessageHandler.java:254` (error reports), plus `publishMetrics`. None of these is inside an
  `invoke()`.

So a burst of messages for the **same recipient queue** can be enqueued out of order, and the
recipient consumes them out of order. Two mitigating facts and one aggravating one:

- Reordering only affects messages published close together in time; the dispatcher publishes
  one message per consumed message, so its publish rate is bounded by its own consumption rate.
- Messages for *different* recipient queues are unaffected in any way that matters — they go to
  different queues.
- But nothing prevents a burst: replaying a backlog, a DLQ drain, or a busy sender all produce
  exactly the back-to-back pattern that step 5 shows failing from ~10 messages.

Worth flagging separately: `correlated` confirms are configured but never used. There is no
`setConfirmCallback`, no `CorrelationData` passed to any send, and no `waitForConfirms` anywhere
in `src/main/java` — only a `ReturnsCallback` (`Dispatcher.java:110`), which is driven by
`publisher-returns`/`mandatory` and works independently of the confirm type. As configured, the
confirm setting buys no delivery guarantee that the code acts on, while costing the publish
ordering demonstrated above.

### Options

| option | effect | cost |
|---|---|---|
| `publisher-confirm-type=none` (keep returns + mandatory) | restores strict per-queue ordering; unroutable-message reporting keeps working | loses confirms — currently unobserved anyway |
| Wrap publishes in `rabbitTemplate.invoke(ops -> ...)` | keeps confirms, restores ordering | small refactor of each publish site; `waitForConfirms` becomes usable inside the scope |
| Register a `ConfirmCallback` and pass `CorrelationData` | makes confirms actually useful | does **not** fix ordering on its own |
| Accept reordering | — | recipients must tolerate out-of-order delivery, or messages need sequence numbers |

Which to pick depends on whether recipient systems rely on the arrival order of successive
messages for one sender/recipient pair — a functional question this lab cannot answer.

---

## Appendix: the wider matrix (`./run-lab.sh`)

The same jar covers the other ways ordering can break. Full details are in the
scenario list at the top of `run-lab.sh`; measured verdicts:

| scenario | verdict |
|---|---|
| `baseline` (1 thread, no confirms) | **PRESERVED** — the control |
| `confirms` / `confirms-paced` / `confirms-invoke` | see above |
| `quorum-queue` | **PRESERVED** — queue type is irrelevant to this question |
| `high-prefetch` (prefetch 250) | **PRESERVED** — prefetch alone reorders nothing |
| `producer-threads-4` | BROKEN globally **and per thread** — consecutive sends from one thread land on different cached channels, so "one thread per key" does not give per-key ordering |
| `producer-threads-4-invoke` | broken globally, **per-thread order preserved** — the shape to use if ordering matters per entity |
| `consumer-concurrency-4` | queue order fine, **processing order broken** — ordered delivery buys nothing if the consumer is concurrent |
| `requeue-on-reject` | **BROKEN** — a requeued message returns to the head of the queue but behind everything already prefetched; it reappeared 51 positions late. Any retry/requeue or DLQ-with-TTL loop destroys ordering even with one consumer |

End-to-end ordering therefore requires *all* of: a pinned publishing channel, one queue per
ordering key, exactly one consumer per queue, and no requeue/retry path that can reinject a
message behind newer ones. Anything less needs sequence numbers and reordering — or
order-insensitive handling — in the consumer.

---

## Knobs

| argument | default | meaning |
|---|---|---|
| `--lab.scenario` | `baseline` | label used in the report |
| `--lab.messages` | 2000 | messages published |
| `--lab.publish-pause-ms` | 0 | sleep between sends — the decisive knob for the confirms case |
| `--lab.use-invoke` | false | publish inside `invoke()` (pins one channel per thread) |
| `--lab.producer-threads` | 1 | threads calling `send()` |
| `--lab.queue-type` | `classic` | `classic` or `quorum` |
| `--lab.consumer-concurrency` | 1 | `concurrentConsumers` |
| `--lab.prefetch` | 1 | `basic.qos` |
| `--lab.work-jitter-ms` | 0 | listener sleep on even `seq`, to expose concurrent processing |
| `--lab.requeue-seq` | -1 | reject this `seq` with requeue, once |
| `--lab.expect` / `--lab.expect-per-producer` | `preserved` | expected verdict; drives the exit code |

Connection settings come from env vars — `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`,
`RABBIT_PASS`, `RABBIT_VHOST`, `PUBLISHER_CONFIRM_TYPE`, `PUBLISHER_RETURNS`,
`TEMPLATE_MANDATORY` — so the lab can be pointed at any broker instead of the bundled one.

### How the measurement avoids lying to you

- The order each `send()` *returned* is recorded separately from the `seq` header, so a race
  between producer threads is never mistaken for broker-side reordering. The verdict compares
  against actual publish-completion order.
- After publishing, the runner polls the broker until the queue really holds all N messages. If
  they never all arrive that is reported as **loss**, not reordering.
- The consumer starts only after publishing finishes, with `prefetch=1` and one consumer, so
  delivery order equals queue order.
- Channel creations during the publish phase are counted via a `ChannelListener`, which is what
  makes the cause visible instead of inferred.
- Every scenario declares its expected verdict and the exit code is non-zero on any mismatch,
  so the whole thing doubles as a regression check.

## Layout

```
compose.yaml                              RabbitMQ 4.1 + management UI
observe-confirms.sh                       focused demonstration of the confirms behaviour
run-lab.sh                                the wider scenario matrix
src/main/java/.../Producer.java           publish phase, plain send() and invoke() variants
src/main/java/.../Consumer.java           drain phase, records delivery order
src/main/java/.../ChannelUsageProbe.java  counts channels opened while publishing
src/main/java/.../OrderAnalysis.java      pure order comparison, no AMQP
src/main/java/.../LabRunner.java          orchestration + report
```
