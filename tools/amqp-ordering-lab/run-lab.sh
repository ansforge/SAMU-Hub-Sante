#!/usr/bin/env bash
# Reproducible end-to-end run: boots a RabbitMQ broker, builds the lab jar, then runs
# every scenario in sequence and prints a summary table.
#
#   ./run-lab.sh              # all scenarios
#   ./run-lab.sh baseline     # a single scenario by name
#   KEEP_BROKER=1 ./run-lab.sh  # leave the broker running afterwards
set -euo pipefail
cd "$(dirname "$0")"

LOG_DIR="build/lab-logs"
mkdir -p "$LOG_DIR"

# scenario | env overrides | --lab.* args
DISPATCHER_CONFIRMS="PUBLISHER_CONFIRM_TYPE=correlated PUBLISHER_RETURNS=true TEMPLATE_MANDATORY=true"

SCENARIOS=(
  # --- the dispatcher's configuration: publisher confirms + sequential send() ---
  "baseline|--lab.messages=2000 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.expect=preserved"
  "confirms|$DISPATCHER_CONFIRMS|--lab.messages=2000 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.expect=broken --lab.expect-per-producer=broken"
  "confirms-paced|$DISPATCHER_CONFIRMS|--lab.messages=500 --lab.publish-pause-ms=5 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.expect=preserved"
  "confirms-invoke|$DISPATCHER_CONFIRMS|--lab.messages=2000 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.use-invoke=true --lab.expect=preserved"
  # --- other publisher-side variants -------------------------------------------
  "quorum-queue|--lab.messages=2000 --lab.queue-type=quorum --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.expect=preserved"
  "producer-threads-4|--lab.messages=2000 --lab.producer-threads=4 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.expect=broken --lab.expect-per-producer=broken"
  "producer-threads-4-invoke|--lab.messages=2000 --lab.producer-threads=4 --lab.consumer-concurrency=1 --lab.prefetch=1 --lab.use-invoke=true --lab.expect=broken --lab.expect-per-producer=preserved"
  # --- consumer side: does PROCESSING follow the queue order? -----------------
  "high-prefetch|--lab.messages=2000 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=250 --lab.expect=preserved"
  "consumer-concurrency-4|--lab.messages=400 --lab.producer-threads=1 --lab.consumer-concurrency=4 --lab.prefetch=1 --lab.work-jitter-ms=5 --lab.expect=broken --lab.expect-per-producer=broken"
  "requeue-on-reject|--lab.messages=400 --lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=50 --lab.requeue-seq=10 --lab.expect=broken --lab.expect-per-producer=broken"
)

only="${1:-}"

echo "==> starting RabbitMQ (docker compose)"
docker compose up -d --wait

echo "==> building lab jar"
./gradlew --quiet --console=plain bootJar
JAR=$(ls build/libs/amqp-ordering-lab-*.jar 2>/dev/null | head -1 || true)
[ -z "$JAR" ] && JAR=$(ls build/libs/*.jar | head -1)

overall=0
results=()
for entry in "${SCENARIOS[@]}"; do
  IFS='|' read -r -a parts <<< "$entry"
  name="${parts[0]}"
  if [ "${#parts[@]}" -eq 3 ]; then
    envs="${parts[1]}"; args="${parts[2]}"
  else
    envs=""; args="${parts[1]}"
  fi

  if [ -n "$only" ] && [ "$only" != "$name" ]; then
    continue
  fi

  echo
  echo "==> scenario: $name"
  set +e
  # shellcheck disable=SC2086
  env $envs java -jar "$JAR" --lab.scenario="$name" $args 2>&1 | tee "$LOG_DIR/$name.log"
  rc=${PIPESTATUS[0]}
  set -e
  [ "$rc" -ne 0 ] && overall=1
  results+=("$(grep -h 'LAB-RESULT|' "$LOG_DIR/$name.log" | tail -1 | sed 's/.*LAB-RESULT|//')")
done

echo
echo "════════════════════════════════ SUMMARY ════════════════════════════════"
printf "%-26s %-8s %6s %4s %4s %5s %4s %6s %6s %-20s %s\n" \
  SCENARIO QUEUE MSGS PROD CONS PREF CHAN CONSUM STEPS PER-PRODUCER VERDICT
for r in "${results[@]}"; do
  IFS='|' read -r s q m p c pf ch cs st pp v <<< "$r"
  printf "%-26s %-8s %6s %4s %4s %5s %4s %6s %6s %-20s %s\n" \
    "$s" "$q" "$m" "$p" "$c" "$pf" "$ch" "$cs" "$st" "$pp" "$v"
done
echo "═════════════════════════════════════════════════════════════════════════"
echo "PROD=producer threads  CONS=consumer concurrency  PREF=prefetch"
echo "CHAN=new AMQP channels opened during publish (0 = reused the cached one; FIFO is per-channel only)"
echo "STEPS=adjacent pairs consumed out of publish order"

if [ "${KEEP_BROKER:-0}" != "1" ]; then
  echo
  echo "==> stopping RabbitMQ (KEEP_BROKER=1 to keep it)"
  docker compose down -v
fi

exit "$overall"
