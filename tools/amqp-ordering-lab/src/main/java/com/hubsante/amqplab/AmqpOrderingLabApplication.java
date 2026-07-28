package com.hubsante.amqplab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * One-shot lab: publishes N sequence-numbered messages, then drains the queue and
 * compares the consumed order with the publish order.
 *
 * <p>Everything is driven by {@code --lab.*} arguments so a single jar covers every
 * scenario; see README.md.
 */
@SpringBootApplication
@EnableConfigurationProperties(LabProperties.class)
public class AmqpOrderingLabApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(AmqpOrderingLabApplication.class, args);
    int exitCode = ctx.getBean(LabRunner.class).exitCode();
    ctx.close();
    System.exit(exitCode);
  }
}
