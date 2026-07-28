# Investigation report — does `RabbitTemplate.send()` preserve message ordering?

**Date:** 2026-07-28
**Scope:** publish-side message ordering in Spring AMQP, and the exposure of `hub/dispatcher`
**Reproduction:** `tools/amqp-ordering-lab/` (`./observe-confirms.sh`, `./run-lab.sh`)
**Status:** cause identified and confirmed; mitigation applied in the working tree (§6b)

---

## 1. Question

Do sequential calls to `RabbitTemplate.send()` from a single thread result in the messages
sitting in the destination queue in that same order?

The question matters because `hub/dispatcher` publishes one message per consumed message from a
single-threaded listener, and recipient systems may rely on the arrival order of successive
messages for a given sender/recipient pair.

## 2. Answer

**No — not with the configuration the dispatcher runs.**

With `spring.rabbitmq.publisher-confirm-type=correlated`, a single-threaded loop of sequential
`send()` calls lands in the queue **out of order**. The same code with confirms disabled
preserves order exactly.

The confirm *protocol* is not at fault. The cause is that Spring's `CachingConnectionFactory`
does not return a channel to its cache while that channel still has unconfirmed messages, so a
publish loop keeps opening **new** channels — and AMQP guarantees FIFO **per channel** only.

No message loss was observed in any scenario: every run consumed exactly what it published.
This is purely a reordering finding.

## 3. Method

One JVM per run, against a single-node RabbitMQ, in three phases:

1. **Publish** N messages carrying a `seq` header (0..N-1). The order in which each `send()`
   *returned* is recorded separately from `seq`, so a race between producer threads can never be
   mistaken for broker-side reordering — verdicts compare against actual publish-completion order.
2. **Wait** until the broker reports all N messages queued. If they never all arrive, that is
   reported as *loss*, not reordering.
3. **Drain** with a listener container started only *after* publishing, with one consumer and
   `prefetch=1` — so the recorded delivery order **is** the queue's order.

Channels opened during the publish phase are counted with a `ChannelListener`, which is what
makes the cause observable rather than inferred.

**Environment:** RabbitMQ 4.1 (`rabbitmq:4.1-management`, single node), Spring Boot 4.0.6 /
spring-rabbit 4.0.3, JDK 21, macOS. Classic durable queue, persistent messages, one publishing
thread, one consumer, `prefetch=1`.

## 4. Findings

### 4.1 Core result — confirms alone break the ordering

2000 sequential sends, one thread. Only the confirm configuration differs between rows 1 and 2.

| # | configuration | new channels during publish | out-of-order steps | verdict |
|---|---|---|---|---|
| 1 | confirms **off** | **0** (cached channel reused) | 0 | **ORDER PRESERVED** |
| 2 | confirms **`correlated`** | **85** (~23 msg/channel) | 449 | **ORDER BROKEN** |
| 3 | confirms `correlated`, 5 ms between sends | 0 | 0 | **ORDER PRESERVED** |
| 4 | confirms `correlated`, inside `invoke()` | 0 (channel pinned) | 0 | **ORDER PRESERVED** |

Row 2's first inversion, verbatim — message 7 overtook message 6:

```
 new channels opened : 85 during publish   <-- MORE CHANNELS THAN THREADS (~23 msg/channel)
 vs publish order    : REORDERED  (out-of-order steps=449, max displacement=21)
 first inversion at  : index 6 — observed[3..10] = [3, 4, 5, 7, 6, 8, 9, 10]
```

Rows 1 and 2 were repeated. Row 1 gave 0 channels / 0 inversions on every run. Row 2 gave
**83–91 channels and 442–503 inversions across 7 runs** — the effect is not a fluke, and the
magnitude is stable.

### 4.2 It is a race, not a threshold

Burst-size sweep with confirms on, three runs per size:

| burst | new channels | out-of-order steps (3 runs) | outcome |
|---|---|---|---|
| 5 | 2 | 0, 0, 0 | preserved 3/3 |
| 10 | 3 | 0, 0, 2 | **broken 1/3** |
| 15 | 4 | 0, 2, 1 | **broken 2/3** |
| 25 | 6 | 6, 1, 3 | broken 3/3 |
| 100 | 13 | 7, 11, 10 | broken 3/3 |
| 1000 | 37–42 | 164, 206, 200 | broken 3/3 |

**There is no safe burst size.** Ten messages published back-to-back already reordered on one of
three runs. An earlier independent sweep agreed qualitatively but with different flip points
(15 broke 3/3, 25 broke 2/3), confirming the non-determinism.

Displacement grows with burst size (up to 21 positions at 2000 messages) but failures begin
almost immediately. Because the reordering requires messages in flight concurrently, it is
**invisible at low publish rates and appears under load** — which is why it can go unnoticed.

### 4.3 Root cause

`spring-rabbit-4.0.3`, `CachingConnectionFactory.CachedChannelInvocationHandler.returnToCache`
(lines 1310–1347). This runs every time a proxied channel is closed, which `RabbitTemplate.send()`
does after **every single call**:

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

Consequences of the two branches:

- **Confirms off** — the channel returns to the cache immediately; the next `send()` reuses it.
  All publishes travel over one channel, and per-channel FIFO makes queue order equal publish
  order.
- **Confirms on** — the channel is parked in `channelsAwaitingAcks` and returned only from the
  broker's ack callback. During a burst, publishing outruns the acks, so each `send()` finds an
  empty cache and `createBareChannel(...)` opens a fresh channel. With messages for the same
  queue in flight on several channels at once, the broker may enqueue them in any interleaving —
  nothing in AMQP orders writes *across* channels.

Two controls establish this as the mechanism rather than a plausible narrative:

- **Pacing (row 3)** keeps confirms enabled and only inserts 5 ms between sends. Each ack now
  lands before the next publish, the same channel is reused, ordering is perfect. Confirms are
  therefore not inherently unordered — the reordering needs unconfirmed messages outstanding
  when the next send fires.
- **`invoke()` (row 4)** keeps the full burst rate but pins one channel for the scope, bypassing
  the borrow/return cycle. 0 new channels, 0 inversions.

Nuance: channel count alone is not the predictor. Row 3 still created 1–3 channels across runs
yet held order, because a channel switch is only harmful when writes actually overlap.

### 4.4 Ruled out

Measured as **not** causes of publish-side reordering:

| factor | result |
|---|---|
| Consumer prefetch (tested at 250) | no effect — 0 inversions |
| Queue type (classic vs quorum) | no effect — 0 inversions on quorum |
| Message persistence | no effect |
| Message loss | none anywhere; consumed == published in every run |

### 4.5 Secondary findings

From the wider matrix (`./run-lab.sh`):

| scenario | result |
|---|---|
| 4 publishing threads | Order broken globally **and within each single thread** — consecutive sends from one thread land on different cached channels. **"One thread per key" does not give per-key ordering.** |
| 4 publishing threads, inside `invoke()` | Global order still interleaved (expected), but **each thread's own order preserved** — the correct shape when ordering matters per entity |
| `concurrentConsumers=4` | Queue order fine, **processing order broken** — ordered delivery buys nothing if the consumer is concurrent |
| Reject-with-requeue, prefetch 50 | **Broken** — the requeued message returns to the head of the queue but behind everything already prefetched; it reappeared **51 positions late**. Any retry/requeue or DLQ-with-TTL loop destroys ordering even with a single consumer |

End-to-end ordering therefore requires *all* of: a pinned publishing channel, one queue per
ordering key, exactly one consumer per queue, and no requeue/retry path that can reinject a
message behind newer ones.

## 5. Exposure of `hub/dispatcher`

At the time of the investigation (commit `337eec58`), the dispatcher matched the failing
configuration exactly. Verified in the code:

| fact | location |
|---|---|
| `spring.rabbitmq.publisher-confirm-type=correlated` (plus `publisher-returns=true`, `template.mandatory=true`) | `hub/dispatcher/src/main/resources/application.properties:15-17` |
| `dispatch` listener runs at Boot's default `concurrentConsumers=1` — no `spring.rabbitmq.listener.*` property is set anywhere in the module | `service/Dispatcher.java:191` |
| Plain sequential `rabbitTemplate.send(...)`, none inside an `invoke()` | `Dispatcher.java:274` (recipient queue), `Dispatcher.java:319` (transfer exchange), `MessageHandler.java:254` (error reports), plus `publishMetrics` |

So a burst of messages destined for the **same recipient queue** can be enqueued out of order,
and the recipient will consume them out of order.

Bounding the risk:

- The dispatcher publishes one message per consumed message, so its publish rate is bounded by
  its own consumption rate.
- Messages for *different* recipient queues are unaffected in any meaningful way.
- **But** nothing prevents a burst: a backlog replay, a DLQ drain, or a busy sender all produce
  exactly the back-to-back pattern that fails from ~10 messages.

### Separate observation — confirms are configured but never used

There is no `setConfirmCallback`, no `CorrelationData` passed to any send, and no
`waitForConfirms` anywhere in `hub/dispatcher/src/main/java`. The only registered callback is a
`ReturnsCallback` (`Dispatcher.java:110`), which is driven by `publisher-returns` /
`template.mandatory` and works **independently of the confirm type**.

As configured, `publisher-confirm-type=correlated` costs the publish ordering demonstrated above
while providing no delivery guarantee that the code acts on.

## 6. Options

| option | effect on ordering | notes |
|---|---|---|
| Set `publisher-confirm-type=none`, keep returns + mandatory | **restores strict per-queue ordering** | Unroutable-message reporting via `ReturnsCallback` keeps working. Loses confirms, which are currently unobserved anyway. Smallest change. |
| Wrap publish sites in `rabbitTemplate.invoke(ops -> ...)` | **restores ordering** | Keeps confirms available; `waitForConfirms()` becomes usable inside the scope. Requires touching each publish site. |
| Register a `ConfirmCallback` + pass `CorrelationData` | **no effect on ordering** | Makes confirms genuinely useful, but does not fix this. Combine with one of the above. |
| Accept reordering | — | Requires recipients to tolerate out-of-order delivery, or sequence numbers + reordering in the consumer. |

**Suggested direction:** decide first whether recipient systems depend on the arrival order of
successive messages for one sender/recipient pair — this investigation cannot answer that
functional question. If they do, `invoke()` at the publish sites keeps both properties. If they
do not, the setting is still worth revisiting, since it pays an ordering cost for a
guarantee nothing consumes.

## 6b. Change applied (working tree, not yet committed)

Option 1 has been applied — `publisher-confirm-type` switched from `correlated` to `none` in
both places:

- `hub/dispatcher/src/main/resources/application.properties:15`
- `hub/dispatcher/src/test/java/com/hubsante/hub/service/RabbitIntegrationAbstract.java:150`

`publisher-returns=true` and `template.mandatory=true` are unchanged, so the `ReturnsCallback`
path (`Dispatcher.java:110`) that reports unroutable messages keeps working.

**Verified:** the full dispatcher test suite passes with the change — **106 tests, 0 failures**
(`./gradlew test` in `hub/dispatcher`).

One note on the test file: the comment above the changed line reads *"must be set to handle
PublisherConfirms in other RabbitTemplates, even if we don't use it in Dispatcher"*. That
comment is inaccurate. The custom mTLS template built by `getCustomRabbitTemplate` constructs its
own `CachingConnectionFactory` and calls `ccf.setPublisherConfirmType(ConfirmType.CORRELATED)`
directly (`RabbitIntegrationAbstract.java:109-113`), which is independent of the
`spring.rabbitmq.publisher-confirm-type` property — that property only configures the
auto-configured connection factory. The passing suite confirms it. The comment is now misleading
and worth updating or removing.

## 7. Reproducing

```bash
cd tools/amqp-ordering-lab
./observe-confirms.sh     # the five steps of §4.1 and §4.2, ~2 min
./run-lab.sh              # the wider matrix of §4.4 and §4.5
```

Both scripts boot their own broker, build the jar, declare their own expected verdict per
scenario, and exit non-zero on any mismatch — so they double as regression checks.
`tools/amqp-ordering-lab/README.md` documents the knobs and the measurement design.
