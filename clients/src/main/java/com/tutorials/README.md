# Java code for RabbitMQ tutorials

Here you can find the Java code examples from [RabbitMQ
tutorials](https://www.rabbitmq.com/getstarted.html)
([source code](https://github.com/rabbitmq/rabbitmq-tutorials/tree/main/java-gradle)).

## Requirements
To successfully use the examples you will need a RabbitMQ instance running locally and accessible on `amqp://localhost:5672`. 
You can achieve so by running `docker run -p 5672:5672 -p 15672:15672 rabbitmq:3.11-management-alpine`.

## Code

#### [Tutorial one: "Hello World!"](https://www.rabbitmq.com/tutorials/tutorial-one-java.html):

```shell
# terminal tab 1
gradle -Pmain=com.tutorials.Recv run

# terminal tab 2
gradle -Pmain=com.tutorials.Send run
```

#### [Tutorial two: Work Queues](https://www.rabbitmq.com/tutorials/tutorial-two-java.html):

```shell
# terminal tab 1
gradle -Pmain=com.tutorials.Worker run
gradle -Pmain=com.tutorials.Worker run

# terminal tab 2
gradle -Pmain=com.tutorials.NewTask run --args "First Message"
gradle -Pmain=com.tutorials.NewTask run --args "Second Message"
gradle -Pmain=com.tutorials.NewTask run --args "Third Message"
gradle -Pmain=com.tutorials.NewTask run --args "Fourth Message"
gradle -Pmain=com.tutorials.NewTask run --args "Fifth Message"
```

#### [Tutorial three: Publish/Subscribe](https://www.rabbitmq.com/tutorials/tutorial-three-java.html)

```shell
# terminal tab 1
gradle -Pmain=com.tutorials.ReceiveLogs run

# terminal tab 2
gradle -Pmain=com.tutorials.EmitLog run
```

#### [Tutorial four: Routing](https://www.rabbitmq.com/tutorials/tutorial-four-java.html)

```shell
# terminal tab 1
gradle -Pmain=com.tutorials.ReceiveLogsDirect run --args "warning error"

# terminal tab 2
gradle -Pmain=com.tutorials.ReceiveLogsDirect run --args "info warning error"

# terminal tab 3
gradle -Pmain=com.tutorials.EmitLogDirect run --args "error Run. Run. Or it will explode"
```

#### [Tutorial five: Topics](https://www.rabbitmq.com/tutorials/tutorial-five-java.html)

```shell
# To receive all the logs:
gradle -Pmain=com.tutorials.ReceiveLogsTopic run --args "#"

# To receive all logs from the facility "kern":
gradle -Pmain=com.tutorials.ReceiveLogsTopic run --args "kern.*"

# Or if you want to hear only about "critical" logs:
gradle -Pmain=com.tutorials.ReceiveLogsTopic run --args "*.critical"

# You can create multiple bindings:
gradle -Pmain=com.tutorials.ReceiveLogsTopic run --args "kern.* *.critical"

# And to emit a log with a routing key "kern.critical" type:
gradle -Pmain=com.tutorials.EmitLogTopic run --args "kern.critical A critical kernel error"
```

#### [Tutorial six: RPC](https://www.rabbitmq.com/tutorials/tutorial-six-java.html)

```shell
# Our RPC service is now ready. We can start the server:
gradle -Pmain=com.tutorials.RPCServer run

# To request a fibonacci number run the client:
gradle -Pmain=com.tutorials.RPCClient run
```

#### [Tutorial seven: Publisher Confirms](https://www.rabbitmq.com/tutorials/tutorial-seven-java.html)

```shell
#
gradle -Pmain=com.tutorials.PublisherConfirms run
```