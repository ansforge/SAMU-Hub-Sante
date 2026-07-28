#!/usr/bin/env bash
# Focused demonstration of the ONE case that matters for hub/dispatcher:
# publisher confirms (`publisher-confirm-type=correlated`) + sequential RabbitTemplate.send().
#
# Runs four controlled variants that isolate the cause, then a burst sweep that shows the
# reordering is an intermittent race rather than a fixed threshold.
#
#   ./observe-confirms.sh                # everything
#   KEEP_BROKER=1 ./observe-confirms.sh  # leave the broker up for the management UI
set -euo pipefail
cd "$(dirname "$0")"

CONFIRMS="PUBLISHER_CONFIRM_TYPE=correlated PUBLISHER_RETURNS=true TEMPLATE_MANDATORY=true"
JAR=build/libs/amqp-ordering-lab.jar
COMMON="--lab.producer-threads=1 --lab.consumer-concurrency=1 --lab.prefetch=1"

echo "==> starting RabbitMQ"
docker compose up -d --wait
echo "==> building"
./gradlew --quiet --console=plain bootJar

# Prints only the report block of a run. Never aborts the script: a scenario that
# disagrees with its expectation exits non-zero, and we still want the remaining steps.
run_step() {
  local title="$1" envs="$2" args="$3"
  echo
  echo "┏━━ $title"
  set +e
  # shellcheck disable=SC2086
  env $envs java -jar "$JAR" $COMMON $args 2>&1 \
    | grep -E "^ (scenario|new channels|published /|vs publish|first inversion|VERDICT)" \
    | sed 's/^/┃ /'
  set -e
  echo "┗━━"
}

echo
echo "#############################################################################"
echo "# STEP 1 — control: confirms OFF. 2000 sequential send() calls."
echo "#          Expect ORDER PRESERVED, and 0 new channels (the cached one is reused)."
echo "#############################################################################"
run_step "confirms OFF" "" "--lab.scenario=1-confirms-off --lab.messages=2000 --lab.expect=preserved"

echo
echo "#############################################################################"
echo "# STEP 2 — the dispatcher's configuration: confirms=correlated. SAME code."
echo "#          Expect ORDER BROKEN, and dozens of new channels opened."
echo "#############################################################################"
run_step "confirms=correlated (burst)" "$CONFIRMS" \
  "--lab.scenario=2-confirms-on --lab.messages=2000 --lab.expect=broken --lab.expect-per-producer=broken"

echo
echo "#############################################################################"
echo "# STEP 3 — confirms are NOT inherently unordered: same config, 5ms between"
echo "#          sends so each ack lands before the next publish."
echo "#          Expect ORDER PRESERVED with almost no new channels."
echo "#############################################################################"
run_step "confirms=correlated (paced 5ms)" "$CONFIRMS" \
  "--lab.scenario=3-confirms-paced --lab.messages=500 --lab.publish-pause-ms=5 --lab.expect=preserved"

echo
echo "#############################################################################"
echo "# STEP 4 — the fix: same burst, same confirms, publishing inside"
echo "#          rabbitTemplate.invoke() so one channel is pinned for the loop."
echo "#          Expect ORDER PRESERVED."
echo "#############################################################################"
run_step "confirms=correlated + invoke()" "$CONFIRMS" \
  "--lab.scenario=4-confirms-invoke --lab.messages=2000 --lab.use-invoke=true --lab.expect=preserved"

echo
echo "#############################################################################"
echo "# STEP 5 — how big a burst is needed? Three runs each. Note that this is a"
echo "#          RACE: the same burst size flips between preserved and broken."
echo "#############################################################################"
printf "%8s %6s %10s %8s %s\n" BURST RUN CHANNELS STEPS RESULT
set +e
for n in 5 10 15 25 100 1000; do
  for i in 1 2 3; do
    # shellcheck disable=SC2086
    env $CONFIRMS java -jar "$JAR" $COMMON --lab.scenario=sweep-$n --lab.messages="$n" 2>&1 \
      | grep -oE "LAB-RESULT.*" \
      | awk -F'|' -v n="$n" -v i="$i" \
          '{sub(/ \(.*\)$/, "", $12); printf "%8s %6s %10s %8s %s\n", n, i, $8, $10, $12}'
  done
done
set -e

echo
echo "Full logs of the four steps are printed above; the queue is 'lab.order.classic'."
if [ "${KEEP_BROKER:-0}" != "1" ]; then
  echo "==> stopping RabbitMQ (KEEP_BROKER=1 to keep it and browse http://localhost:15672)"
  docker compose down -v
else
  echo "==> broker kept. Management UI: http://localhost:15672 (lab/lab) — the Connections"
  echo "    tab shows the channel count the publish phase churned through."
fi
