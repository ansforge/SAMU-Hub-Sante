#!/usr/bin/env python3
import os
import yaml
import re
import sys

SECRETS_PATH = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("SECRETS_PATH", "/etc/secrets")
CONNECTORS_DIR = os.path.join(SECRETS_PATH, "connectors")
CLIENTS_DIR = os.path.join(SECRETS_PATH, "static-clients")
OUTPUT_DIR = os.environ.get("DEX_CONFIG_OUTPUT_DIR", "/etc/dex")
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "dex.config.yaml")
MAPPING_OUTPUT_DIR = os.environ.get("DEX_MAPPING_OUTPUT_DIR", OUTPUT_DIR)
MAPPING_OUTPUT_FILE = os.path.join(MAPPING_OUTPUT_DIR, "secrets.yaml")


def parse_flat_files(secret_dir):
    data = {}
    for filename in os.listdir(secret_dir):
        # Skip files containing '_raw' which are VaultStaticSecret metadata
        if "_raw" in filename:
            continue
        path = os.path.join(secret_dir, filename)
        if os.path.isfile(path):
            with open(path, "r", encoding="utf-8") as f:
                value = f.read().strip()
                if value.lower() == "true":
                    value = True
                elif value.lower() == "false":
                    value = False
                data[filename] = value
    return data


def parse_flat_files_with_placeholders(secret_dir, prefix=""):
    """
    Parse flat files and return both the actual values and placeholders.
    Only sensitive fields use placeholders, others use direct values.
    Returns a tuple: (data_with_placeholders, actual_values_map)
    """
    data_with_placeholders = {}
    actual_values_map = {}
    
    # Define critical/sensitive field patterns that should use placeholders
    critical_fields = [
        'secret', 'clientsecret', 'password', 'token', 'key', 'clientid'
    ]
    
    # Define fields that are critical only for static-clients
    static_clients_critical_fields = ['id']
    
    for filename in os.listdir(secret_dir):
        # Skip files containing '_raw' which are VaultStaticSecret metadata
        if "_raw" in filename:
            continue
        path = os.path.join(secret_dir, filename)
        if os.path.isfile(path):
            with open(path, "r", encoding="utf-8") as f:
                value = f.read().strip()
                if value.lower() == "true":
                    value = True
                elif value.lower() == "false":
                    value = False
                
                # Check if this field is critical/sensitive
                is_critical = any(critical_field in filename.lower() for critical_field in critical_fields)
                
                # For static-clients, also check static-clients specific critical fields
                if prefix == "static-clients.":
                    is_critical = is_critical or any(sc_field in filename.lower() for sc_field in static_clients_critical_fields)
                
                if is_critical:
                    # Create placeholder key for sensitive values
                    # Remove the prefix from filename if it exists to avoid duplication
                    clean_filename = filename
                    if prefix and filename.startswith(prefix):
                        clean_filename = filename[len(prefix):]
                    placeholder_key = f"$dex.{prefix}{clean_filename}" if prefix else f"$dex.{filename}"
                    data_with_placeholders[filename] = placeholder_key
                    actual_values_map[placeholder_key] = value
                else:
                    # Use actual value directly for non-critical fields
                    data_with_placeholders[filename] = value
    
    return data_with_placeholders, actual_values_map


def build_nested_dict(flat_dict):
    nested = {}
    for flat_key, value in flat_dict.items():
        SPLIT_KEYS_ON_DOTS_REGEX = r"\.(?=\d+|\w)"
        parts = re.split(SPLIT_KEYS_ON_DOTS_REGEX, flat_key)
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
    # Parse files with selective placeholders (only for sensitive values) and collect actual values
    connectors_data, connectors_values = parse_flat_files_with_placeholders(CONNECTORS_DIR, "connectors.")
    clients_data, clients_values = parse_flat_files_with_placeholders(CLIENTS_DIR, "static-clients.")
    
    # Combine all actual values for the mapping file (only contains sensitive values now)
    all_values_map = {**connectors_values, **clients_values}

    # Remove the "connectors." prefix from connector keys to avoid double nesting
    connectors_clean = {}
    for key, value in connectors_data.items():
        if key.startswith("connectors."):
            clean_key = key[len("connectors."):]
            connectors_clean[clean_key] = value
        else:
            connectors_clean[key] = value

    # Remove the "static-clients." prefix from client keys to avoid double nesting
    clients_clean = {}
    for key, value in clients_data.items():
        if key.startswith("static-clients."):
            clean_key = key[len("static-clients."):]
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

    return config, all_values_map


def write_yaml(config, path):
    with open(path, "w", encoding="utf-8") as f:
        yaml.safe_dump(
            config,
            f,
            default_flow_style=False,
            sort_keys=False,
            allow_unicode=True,
        )


def write_mapping(values_map, path):
    """Write the sensitive values mapping to a YAML file"""
    with open(path, "w", encoding="utf-8") as f:
        yaml.safe_dump(
            values_map,
            f,
            default_flow_style=False,
            sort_keys=True,
            allow_unicode=True,
        )


if __name__ == "__main__":
    if not os.path.isdir(CONNECTORS_DIR):
        print(f"Error: Missing directory {CONNECTORS_DIR}")
        exit(1)
    if not os.path.isdir(CLIENTS_DIR):
        print(f"Error: Missing directory {CLIENTS_DIR}")
        exit(1)

    # Create output directories if they don't exist
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(MAPPING_OUTPUT_DIR, exist_ok=True)

    config, values_map = build_dex_config()
    write_yaml(config, OUTPUT_FILE)
    write_mapping(values_map, MAPPING_OUTPUT_FILE)

    print(f"✅ dex.config.yaml generated successfully at {OUTPUT_FILE}")
    print(f"✅ Sensitive values mapping generated successfully at {MAPPING_OUTPUT_FILE}")
    print(f"\nGenerated config (with direct values for non-sensitive fields):\n")
    with open(OUTPUT_FILE, encoding="utf-8") as f:
        print(f.read())
    
    print(f"\nSensitive values mapping:\n")
    with open(MAPPING_OUTPUT_FILE, encoding="utf-8") as f:
        print(f.read())
