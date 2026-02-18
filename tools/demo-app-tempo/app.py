from flask import Flask, jsonify
import time
import random

from opentelemetry import trace

app = Flask(__name__)
tracer = trace.get_tracer(__name__)

@app.get("/health")
def health():
    return {"ok": True}

@app.get("/work")
def work():
    # A manual span (you'll also get automatic spans for Flask/requests once instrumented)
    with tracer.start_as_current_span("do_work") as span:
        ms = random.randint(50, 250)
        time.sleep(ms / 1000.0)
        span.set_attribute("work.ms", ms)
        return jsonify({"slept_ms": ms})
