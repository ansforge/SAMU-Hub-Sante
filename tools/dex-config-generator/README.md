# Dex Config Generator

This script generates Dex configuration YAML from flat secret files stored in a directory structure.

## Overview

The `generate-dex-config.py` script reads secret files from two directories:
- `connectors/` - Contains connector configuration files
- `staticClients/` - Contains static client configuration files

It processes these flat files and generates a structured `dex.config.yaml` file.

## Usage

### Local Development

```bash
# Run the script locally
python3 scripts/generate-dex-config.py /path/to/secrets

# Or with default path (/etc/secrets)
python3 scripts/generate-dex-config.py
```

### Container Usage

The script is containerized and available as a Docker image:

```bash
# Pull and run the image
docker run --rm -v /path/to/secrets:/etc/secrets ghcr.io/ansforge/dex-config-generator:latest

# Or with custom secrets path
docker run --rm -v /path/to/secrets:/custom/path ghcr.io/ansforge/dex-config-generator:latest /custom/path
```

## Input Format

### Connectors Directory Structure
```
connectors/
├── connectors.0.type
├── connectors.0.id
├── connectors.0.config.clientID
├── connectors.0.config.clientSecret
└── ...
```

### Static Clients Directory Structure
```
staticClients/
├── clients.0.id
├── clients.0.secret
├── clients.0.redirectURIs.0
├── clients.1.id
└── ...
```

## Output

The script generates a `dex.config.yaml` file with the following structure:

```yaml
connectors:
  - type: oauth2
    id: example-connector
    config:
      clientID: example-client-id
      clientSecret: example-secret

oauth2:
  skipApprovalScreen: true

staticClients:
  - id: example-client
    secret: client-secret
    redirectURIs:
      - http://localhost:8080/callback
```

## Features

- Converts flat key-value files to nested YAML structure
- Handles boolean string conversion ("true"/"false" → true/false)
- Skips files containing "_raw" in their names
- Supports indexed arrays (e.g., `clients.0`, `clients.1`)
- Automatic prefix removal for cleaner configuration

## Container Image

The container image is automatically built and published via GitHub Actions when creating a release with the `dex-` prefix (e.g., `dex-1.0.0`).

Image: `ghcr.io/ansforge/hub-dex-config-generator:latest`
