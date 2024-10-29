import os
import requests
from flask import Flask, jsonify

app = Flask(__name__)

PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://prometheus-server.default.svc:9090")  # Default value
QUERY_INTERVAL_SECONDS = 60  # Polling interval

def query_prometheus(query):
    print({PROMETHEUS_URL})
    response = requests.get(f"{PROMETHEUS_URL}/api/v1/query", params={"query": query})

    # Log the response status and content
    print("Response Status Code:", response.status_code)
    print("Response Content:", response.text)

    # Check if response is successful
    response.raise_for_status()  # This will raise an error for HTTP errors

    data = response.json()
    return data.get("data", {}).get("result", [])

def check_service_health():
    # Define your queries and criteria
    health_checks = {
        "up": 'up'
    }
    # health_checks = {
    #     "rabbitmq_up": 'up{job="rabbitmq"} == 1',
    #     "dispatchers_up": 'up{job=~"dispatcher-.*-service"} == 1',
    #     "all_dispatchers_connected": 'sum(rabbitmq_queue_consumers{queue="dispatch"})/sum(up{job=~"dispatcher-.*-service"}) == 1'
    # }
    results = {}
    for check_name, query in health_checks.items():
        query_result = query_prometheus(query)
        results[check_name] = len(query_result) > 0 and all(
            float(result["value"][1]) for result in query_result
        )
    return results

@app.route('/health', methods=['GET'])
def health():
    # Aggregate health status based on queries
    health_status = check_service_health()
    global_status = "healthy" if all(health_status.values()) else "unhealthy"
    return jsonify({"status": global_status, "details": health_status})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
