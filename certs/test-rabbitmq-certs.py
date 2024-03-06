import os
import pika
from pika.credentials import ExternalCredentials
import logging
import ssl

logging.basicConfig(level=logging.WARNING)

def test_certificate(cert_path, key_path, ca_path, rabbitmq_url):
    try:
        # Set up SSL context
        context = ssl.SSLContext(ssl.PROTOCOL_TLS) # deprecated, should be ssl.PROTOCOL_TLS_CLIENT but has side effects as long as we have to reach preprod RabbitMQ through an IP which is not in the SAN of the RMQ server certificate
        context.load_cert_chain(cert_path, keyfile=key_path)
        context.load_verify_locations(cafile=ca_path)

        # Establish connection to RabbitMQ server
        connection_params = pika.ConnectionParameters(
            host=rabbitmq_url,
            ssl_options=pika.SSLOptions(context),
            credentials=ExternalCredentials()
        )
        connection = pika.BlockingConnection(connection_params)
        connection.close()

        logging.warning(f"Success for {rabbitmq_url} using {cert_path}")

    except pika.exceptions.AMQPConnectionError as e:
        logging.error(f"Error connecting to {rabbitmq_url} using {cert_path}: {str(e)}")


def test_all_certificates(base_folder, environment, rabbitmq_url):
    env_path = os.path.join(base_folder, environment)

    if not os.path.exists(env_path):
        logging.warning(f"No certificates found for {environment} environment.")
        return

    for client_folder in os.listdir(env_path):
        client_path = os.path.join(env_path, client_folder)

        if os.path.isdir(client_path):
            cert_file = os.path.join(client_path, f"{client_folder}.crt")
            key_file = os.path.join(client_path, f"{client_folder}.key")
            ca_path = os.path.join(env_path, "ca-bundle.pem")

            if os.path.exists(cert_file) and os.path.exists(key_file):
                test_certificate(cert_file, key_file, ca_path, rabbitmq_url)
            else:
                logging.warning(f"Certificate files not found for {client_folder} in {environment} environment.")
        else:
            logging.warning(f"Ignoring non-directory file: {client_folder}")


if __name__ == "__main__":
    base_folder = "environments"  # Replace with the actual path to your certs folder

    environments = ["sandbox", "preprod", "prod"]
    rabbitmq_urls = {
        "sandbox": "messaging.bac-a-sable.hub.esante.gouv.fr",
        "preprod": "51.210.211.163",
        "prod": "messaging.hub.esante.gouv.fr",
    }

    for environment in environments:
        rabbitmq_url = rabbitmq_urls.get(environment)
        if rabbitmq_url:
            test_all_certificates(base_folder, environment, rabbitmq_url)
        else:
            logging.warning(f"RabbitMQ URL not found for {environment} environment.")
