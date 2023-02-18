# consume.py
import pika

params = pika.URLParameters("amqp://login-client:password-client@{POC_HUB_URL}")
connection = pika.BlockingConnection(params)
channel = connection.channel()
channel.queue_declare(queue='{IdentifiantClient}.in.message', durable=True)
def callback(ch, method, properties, body):
  print(" [x] Received " + str(body))

channel.basic_consume('{IdentifiantClient}.in.message',
                      callback,
                      auto_ack=True)

print(' [*] Waiting for messages:')
channel.start_consuming()
connection.close()