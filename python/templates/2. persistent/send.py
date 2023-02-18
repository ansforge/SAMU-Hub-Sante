# publish.py
import pika
params = pika.URLParameters("amqp://login-client:password-client@{POC_HUB_URL}")
connection = pika.BlockingConnection(params)
channel = connection.channel()
channel.queue_declare(queue='{IdentifiantClient}.out.message', durable=True)
channel.basic_publish(exchange='',
                      routing_key='{IdentifiantClient}.out.message',
                      body='Hello World!',
                      properties=pika.BasicProperties(
                         delivery_mode = pika.spec.PERSISTENT_DELIVERY_MODE
                      ))
print(" [x] Sent 'Hello World!'")
connection.close()
