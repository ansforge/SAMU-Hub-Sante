Ideally, we should use the same image in tests than in cluster.

But we use bitnami images of RabbitMQ in prod, as they are specially packaged for the Bitnami Helm chart, and not RabbitMQ official images.

Bitnami registry: https://hub.docker.com/r/bitnami/rabbitmq/tags

RabbitMQ official registry: https://hub.docker.com/_/rabbitmq/tags

So we should substitute the bitnami registry in the tests initialization, as described here :

https://java.testcontainers.org/features/image_name_substitution/#manual-substitution

```java
protected static final String RABBITMQ_IMAGE = "bitnami/rabbitmq:3.12.13-debian-12-r2";

@Container
public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE).asCompatibleSubstituteFor("rabbitmq:3.12.13-management-alpine"));
```

But for some reason it doesn't work.

Here's the stack trace:
```text
20:28:09.184 ERROR d.12.13-debian-12-r3] -- Could not start container
org.testcontainers.containers.ContainerLaunchException: Timed out waiting for log output matching '.*Server startup complete.*'
	at org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy.waitUntilReady(LogMessageWaitStrategy.java:47)
	at org.testcontainers.containers.wait.strategy.AbstractWaitStrategy.waitUntilReady(AbstractWaitStrategy.java:52)
	at org.testcontainers.containers.GenericContainer.waitUntilContainerStarted(GenericContainer.java:953)
	at org.testcontainers.containers.GenericContainer.tryStart(GenericContainer.java:485)
	at org.testcontainers.containers.GenericContainer.lambda$doStart$0(GenericContainer.java:344)
	at org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess(Unreliables.java:81)
	at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:334)
	at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:322)
	at org.testcontainers.junit.jupiter.TestcontainersExtension$StoreAdapter.start(TestcontainersExtension.java:242)
	at org.testcontainers.junit.jupiter.TestcontainersExtension$StoreAdapter.access$200(TestcontainersExtension.java:229)
	at org.testcontainers.junit.jupiter.TestcontainersExtension.lambda$null$1(TestcontainersExtension.java:56)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore.lambda$getOrComputeIfAbsent$5(NamespacedHierarchicalStore.java:147)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore$MemoizingSupplier.computeValue(NamespacedHierarchicalStore.java:372)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore$MemoizingSupplier.get(NamespacedHierarchicalStore.java:361)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore$StoredValue.evaluate(NamespacedHierarchicalStore.java:308)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore$StoredValue.access$200(NamespacedHierarchicalStore.java:287)
	at org.junit.platform.engine.support.store.NamespacedHierarchicalStore.getOrComputeIfAbsent(NamespacedHierarchicalStore.java:149)
	at org.junit.jupiter.engine.execution.NamespaceAwareStore.lambda$getOrComputeIfAbsent$2(NamespaceAwareStore.java:57)
	at org.junit.jupiter.engine.execution.NamespaceAwareStore.accessStore(NamespaceAwareStore.java:90)
	at org.junit.jupiter.engine.execution.NamespaceAwareStore.getOrComputeIfAbsent(NamespaceAwareStore.java:57)
	at org.testcontainers.junit.jupiter.TestcontainersExtension.lambda$beforeAll$2(TestcontainersExtension.java:56)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at org.testcontainers.junit.jupiter.TestcontainersExtension.beforeAll(TestcontainersExtension.java:55)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeBeforeAllCallbacks$12(ClassBasedTestDescriptor.java:396)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeBeforeAllCallbacks(ClassBasedTestDescriptor.java:396)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.before(ClassBasedTestDescriptor.java:212)
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.before(ClassBasedTestDescriptor.java:85)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:148)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:107)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:88)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:67)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:52)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:114)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	at org.junit.platform.launcher.core.DefaultLauncherSession$DelegatingLauncher.execute(DefaultLauncherSession.java:86)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.processAllTestClasses(JUnitPlatformTestClassProcessor.java:110)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.access$000(JUnitPlatformTestClassProcessor.java:90)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor.stop(JUnitPlatformTestClassProcessor.java:85)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:62)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
	at jdk.proxy2/jdk.proxy2.$Proxy5.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker$3.run(TestWorker.java:193)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.executeAndMaintainThreadName(TestWorker.java:129)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:100)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:60)
	at org.gradle.process.internal.worker.child.ActionExecutionWorker.execute(ActionExecutionWorker.java:56)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:113)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:65)
	at worker.org.gradle.process.internal.worker.GradleWorkerMain.run(GradleWorkerMain.java:69)
	at worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
20:28:09.238 ERROR d.12.13-debian-12-r3] -- Log output from the failed container:
rabbitmq 19:27:08.97 INFO  ==> 
rabbitmq 19:27:08.97 INFO  ==> Welcome to the Bitnami rabbitmq container
rabbitmq 19:27:08.97 INFO  ==> Subscribe to project updates by watching https://github.com/bitnami/containers
rabbitmq 19:27:08.97 INFO  ==> Submit issues and feature requests at https://github.com/bitnami/containers/issues
rabbitmq 19:27:08.97 INFO  ==> Upgrade to Tanzu Application Catalog for production environments to access custom-configured and pre-packaged software components. Gain enhanced features, including Software Bill of Materials (SBOM), CVE scan result reports, and VEX documents. To learn more, visit https://bitnami.com/enterprise
rabbitmq 19:27:08.98 INFO  ==> 
rabbitmq 19:27:08.98 INFO  ==> ** Starting RabbitMQ setup **
rabbitmq 19:27:09.00 INFO  ==> Validating settings in RABBITMQ_* env vars..
rabbitmq 19:27:09.03 INFO  ==> Initializing RabbitMQ...
rabbitmq 19:27:09.04 INFO  ==> Generating random cookie
rabbitmq 19:27:09.05 INFO  ==> Starting RabbitMQ in background...
rabbitmq 19:27:18.92 INFO  ==> No custom scripts in /docker-entrypoint-initdb.d
rabbitmq 19:27:18.92 INFO  ==> Stopping RabbitMQ...
rabbitmq 19:27:27.14 INFO  ==> ** RabbitMQ setup finished! **

rabbitmq 19:27:27.15 INFO  ==> ** Starting RabbitMQ **
2024-03-24 19:27:29.581005+00:00 [notice] <0.44.0> Application syslog exited with reason: stopped
2024-03-24 19:27:29.589001+00:00 [notice] <0.235.0> Logging: switching to configured handler(s); following messages may not be visible in this log output

  ##  ##      RabbitMQ 3.12.13
  ##  ##
  ##########  Copyright (c) 2007-2024 Broadcom Inc and/or its subsidiaries
  ######  ##
  ##########  Licensed under the MPL 2.0. Website: https://rabbitmq.com

  Erlang:      26.2.3 [jit]
  TLS Library: OpenSSL - OpenSSL 3.0.11 19 Sep 2023
  Release series support status: supported

  Doc guides:  https://rabbitmq.com/documentation.html
  Support:     https://rabbitmq.com/contact.html
  Tutorials:   https://rabbitmq.com/getstarted.html
  Monitoring:  https://rabbitmq.com/monitoring.html

  Logs: /opt/bitnami/rabbitmq/var/log/rabbitmq/rabbit@localhost.log
        <stdout>

  Config file(s): /etc/rabbitmq/rabbitmq-custom.conf

  Starting broker... completed with 3 plugins.
```

It seems that the lib testcontainers with RabbitMQ scans output logs of the Docker image to determine is the startup is complete.

When I run
```bash
docker pull bitnami/rabbitmq:3.12.13-debian-r12-r12
```

in a terminal the container starts, logs deeper than the stack trace and eventually reaches the expected log "Server startup complete".

I can't figure out why.

At this point, I assume that it should be related to the base image (testcontainers expects generally alpine images and I am trying to pull a debian one),
and that the rabbitmq server underneath behave similarly as long as they have the same tag (3.12.13 in this example), so I run the tests with an alpine image