# receive.py
import pika
import ssl
from pika.credentials import ExternalCredentials

context = ssl.create_default_context(cafile='./cert/AC_POC_HUB.crt')
context.load_cert_chain(certfile='./cert/ans.poc-hub.local.crt', keyfile='./cert/ans.poc-hub.local.key')

ssl_options = pika.SSLOptions(context, 'srv1.poc-hub.local')
conn_params = pika.ConnectionParameters(host='srv1.poc-hub.local',
                                        port=5671,
                                        virtual_host='vhv1', 
                                        ssl_options=ssl_options,
                                        credentials=ExternalCredentials(),
                                        heartbeat=0)
 
def callback(ch, method, properties, body):
    print(" [x] Received %r" % body)
 
with pika.BlockingConnection(conn_params) as conn:
    ch = conn.channel()
    ch.basic_consume(
        '{IdentifiantClient}.in.create',
        callback,
        auto_ack=True
    )
    print(' [*] Waiting for messages:')
    ch.start_consuming()

# In etc/hosts
# Hub_IP srv1.poc-hub.local
