FROM python:3.11-slim

# Install required Python packages
RUN pip install --no-cache-dir pyyaml

# Create working directory
WORKDIR /app

# Copy the script
COPY generate-dex-config.py /app/generate-dex-config.py

# Make script executable
RUN chmod +x /app/generate-dex-config.py

# Set the default command
ENTRYPOINT ["python3", "/app/generate-dex-config.py"]
