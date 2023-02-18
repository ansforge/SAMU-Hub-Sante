# publish.py
import pika
params = pika.URLParameters("amqp://login-client:password-client@{POC_HUB_URL}")
connection = pika.BlockingConnection(params)
channel = connection.channel() # start a channel
channel.queue_declare(queue='{IdentifiantClient}.out.message') # Declare a queue
channel.basic_publish(exchange='',
                      routing_key='{IdentifiantClient}.out.message',
                      body='Hello World!')
print(" [x] Sent 'Hello World!'")
connection.close()
