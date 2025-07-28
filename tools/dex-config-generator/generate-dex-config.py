#!/usr/bin/env python3
import os
import yaml
import re
import sys

SECRETS_PATH = (
    sys.argv[1]
    if len(sys.argv) > 1
    else os.environ.get("SECRETS_PATH", "/etc/secrets")
)
CONNECTORS_DIR = os.path.join(SECRETS_PATH, "connectors")
CLIENTS_DIR = os.path.join(SECRETS_PATH, "staticClients")
OUTPUT_FILE = "dex.config.yaml"


def parse_flat_files(secret_dir):
    data = {}
    for filename in os.listdir(secret_dir):
        # Skip files containing '_raw' in their name
        if "_raw" in filename:
            continue
        path = os.path.join(secret_dir, filename)
        if os.path.isfile(path):
            with open(path, "r", encoding="utf-8") as f:
                value = f.read().strip()
                # Convert string boolean values to actual booleans
                if value.lower() == "true":
                    value = True
                elif value.lower() == "false":
                    value = False
                data[filename] = value
    return data


def build_nested_dict(flat_dict):
    nested = {}
    for flat_key, value in flat_dict.items():
        # Split keys on dot, but keep numbers and words as parts
        parts = re.split(r"\.(?=\d+|\w)", flat_key)
        d = nested
        for i, part in enumerate(parts):
            # Convert numeric keys to int to distinguish from strings
            if part.isdigit():
                part = int(part)
            if i == len(parts) - 1:
                d[part] = value
            else:
                if part not in d:
                    d[part] = {}
                d = d[part]
    return nested


def flatten_indexed_dict(d):
    """
    Recursively convert dicts with all-integer keys to lists.
    """
    if not isinstance(d, dict):
        return d
    if all(isinstance(k, int) for k in d.keys()):
        # Sort keys numerically and recurse
        return [flatten_indexed_dict(d[k]) for k in sorted(d.keys())]
    else:
        return {k: flatten_indexed_dict(v) for k, v in d.items()}


def build_dex_config():
    connectors_flat = parse_flat_files(CONNECTORS_DIR)
    clients_flat = parse_flat_files(CLIENTS_DIR)

    # Remove the "connectors." prefix from connector keys
    connectors_clean = {}
    for key, value in connectors_flat.items():
        if key.startswith("connectors."):
            clean_key = key[11:]  # Remove "connectors." prefix
            connectors_clean[clean_key] = value
        else:
            connectors_clean[key] = value

    # Remove the "clients." prefix from client keys  
    clients_clean = {}
    for key, value in clients_flat.items():
        if key.startswith("clients."):
            clean_key = key[8:]  # Remove "clients." prefix
            clients_clean[clean_key] = value
        else:
            clients_clean[key] = value

    connectors_nested = flatten_indexed_dict(build_nested_dict(connectors_clean))
    clients_nested = flatten_indexed_dict(build_nested_dict(clients_clean))

    config = {
        "connectors": connectors_nested,
        "oauth2": {
            "skipApprovalScreen": True
        },
        "staticClients": clients_nested
    }

    return config


def write_yaml(config, path):
    with open(path, "w", encoding="utf-8") as f:
        yaml.safe_dump(
            config,
            f,
            default_flow_style=False,
            sort_keys=False,
            allow_unicode=True,
        )


if __name__ == "__main__":
    if not os.path.isdir(CONNECTORS_DIR):
        print(f"Error: Missing directory {CONNECTORS_DIR}")
        exit(1)
    if not os.path.isdir(CLIENTS_DIR):
        print(f"Error: Missing directory {CLIENTS_DIR}")
        exit(1)

    config = build_dex_config()
    write_yaml(config, OUTPUT_FILE)

    print(f"✅ dex.config.yaml generated successfully:\n")
    with open(OUTPUT_FILE, encoding="utf-8") as f:
        print(f.read())
