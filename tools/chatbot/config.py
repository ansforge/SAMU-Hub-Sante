"""
Configuration file with security vulnerabilities for testing
"""
import os
import hashlib
import subprocess

# VULNERABILITY: Hardcoded secrets and API keys (Bandit, Semgrep)
SECRET_KEY = "django-insecure-hardcoded-secret-key-12345"
API_KEY = "sk-1234567890abcdefghijklmnopqrstuvwxyz"
DATABASE_PASSWORD = "admin123"
JWT_SECRET = "supersecretjwtkey"

# VULNERABILITY: SQL connection string with hardcoded credentials (Bandit)
DATABASE_URL = "mysql://root:password@localhost:3306/chatbot"
REDIS_URL = "redis://:password123@localhost:6379/0"

# VULNERABILITY: Weak cryptographic practices (Bandit, Semgrep)
def generate_token():
    """VULNERABLE: Using weak random number generation"""
    import random
    return str(random.randint(100000, 999999))

def hash_sensitive_data(data):
    """VULNERABLE: Using MD5 for sensitive data"""
    return hashlib.md5(data.encode()).hexdigest()

# VULNERABILITY: Command injection (Bandit, Semgrep)
def backup_database(db_name):
    """VULNERABLE: Command injection via string formatting"""
    command = f"mysqldump -u root -p{DATABASE_PASSWORD} {db_name} > backup.sql"
    return subprocess.call(command, shell=True)

# VULNERABILITY: Path traversal (Bandit, Semgrep)
def read_config_file(filename):
    """VULNERABLE: Path traversal vulnerability"""
    config_path = f"/etc/chatbot/{filename}"
    with open(config_path, 'r') as f:
        return f.read()

# VULNERABILITY: Unsafe YAML loading (Bandit)
import yaml
def load_yaml_config(yaml_data):
    """VULNERABLE: Unsafe YAML loading"""
    return yaml.load(yaml_data, Loader=yaml.Loader)

# VULNERABILITY: Using eval (Bandit, Semgrep)
def evaluate_config(expression):
    """VULNERABLE: Code injection via eval"""
    return eval(expression)

# VULNERABILITY: Debug mode enabled (Bandit, Semgrep)
DEBUG = True
TESTING = True

# VULNERABILITY: Insecure SSL/TLS settings (Bandit)
SSL_VERIFY = False
VERIFY_SSL = False

# VULNERABILITY: Weak session configuration
SESSION_COOKIE_SECURE = False
SESSION_COOKIE_HTTPONLY = False
CSRF_COOKIE_SECURE = False

# VULNERABILITY: Allowing all hosts (Django-specific)
ALLOWED_HOSTS = ['*']

# VULNERABILITY: Disabled security headers
SECURE_BROWSER_XSS_FILTER = False
SECURE_CONTENT_TYPE_NOSNIFF = False
X_FRAME_OPTIONS = 'ALLOWALL'