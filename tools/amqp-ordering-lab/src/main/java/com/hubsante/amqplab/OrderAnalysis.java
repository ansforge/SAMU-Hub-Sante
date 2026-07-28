package com.hubsante.amqplab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure comparison helpers — no AMQP involved, so the verdicts are easy to audit. */
public final class OrderAnalysis {

  private OrderAnalysis() {}

  /**
   * @param descents number of adjacent pairs delivered in the wrong relative order
   * @param maxDisplacement largest distance a message moved from its expected position
   * @param firstDescentAt index of the first wrong adjacent pair, or -1
   * @param detail human-readable snippet around the first descent
   */
  public record Comparison(int descents, int maxDisplacement, int firstDescentAt, String detail) {
    public boolean preserved() {
      return descents == 0;
    }
  }

  /** Compares {@code observed} against {@code reference}; both must be permutations of each other. */
  public static Comparison compare(List<Integer> observed, List<Integer> reference) {
    Map<Integer, Integer> positionInReference = new HashMap<>(reference.size() * 2);
    for (int i = 0; i < reference.size(); i++) {
      positionInReference.put(reference.get(i), i);
    }

    int[] mapped = new int[observed.size()];
    for (int i = 0; i < observed.size(); i++) {
      Integer p = positionInReference.get(observed.get(i));
      mapped[i] = p == null ? -1 : p;
    }

    int descents = 0;
    int firstDescentAt = -1;
    int maxDisplacement = 0;
    for (int i = 0; i < mapped.length; i++) {
      maxDisplacement = Math.max(maxDisplacement, Math.abs(mapped[i] - i));
      if (i + 1 < mapped.length && mapped[i + 1] < mapped[i]) {
        descents++;
        if (firstDescentAt < 0) {
          firstDescentAt = i;
        }
      }
    }

    String detail = "—";
    if (firstDescentAt >= 0) {
      int from = Math.max(0, firstDescentAt - 3);
      int to = Math.min(observed.size(), firstDescentAt + 5);
      detail = "observed[" + from + ".." + (to - 1) + "] = " + observed.subList(from, to);
    }
    return new Comparison(descents, maxDisplacement, firstDescentAt, detail);
  }

  /**
   * Per-publisher-thread check. AMQP only guarantees FIFO <em>per channel</em>; each producer
   * thread gets its own channel from the {@code CachingConnectionFactory}. So even when the
   * global order is broken, every single thread's own messages must still arrive in order.
   */
  public static Map<String, Comparison> comparePerProducer(
      List<Integer> observed, List<Integer> reference, String[] producerOfSeq) {
    Map<String, List<Integer>> observedByProducer = new LinkedHashMap<>();
    Map<String, List<Integer>> referenceByProducer = new LinkedHashMap<>();
    for (Integer seq : reference) {
      referenceByProducer.computeIfAbsent(producerOfSeq[seq], k -> new ArrayList<>()).add(seq);
    }
    for (Integer seq : observed) {
      observedByProducer.computeIfAbsent(producerOfSeq[seq], k -> new ArrayList<>()).add(seq);
    }

    Map<String, Comparison> result = new LinkedHashMap<>();
    referenceByProducer.forEach(
        (producer, refSeqs) ->
            result.put(producer, compare(observedByProducer.getOrDefault(producer, List.of()), refSeqs)));
    return result;
  }
}
