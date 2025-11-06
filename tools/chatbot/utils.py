"""
Utility functions with security vulnerabilities
"""
import os
import subprocess
import pickle
import xml.etree.ElementTree as ET
import tempfile
import requests
from urllib.parse import urlparse

# VULNERABILITY: Command injection (Bandit, Semgrep)
def execute_system_command(user_input):
    """VULNERABLE: Direct command execution with user input"""
    command = f"ls -la {user_input}"
    return os.system(command)

def run_shell_command(cmd):
    """VULNERABLE: Shell injection via subprocess"""
    return subprocess.call(cmd, shell=True)

# VULNERABILITY: Deserialization attacks (Bandit, Semgrep)
def load_pickle_data(data):
    """VULNERABLE: Unsafe pickle deserialization"""
    return pickle.loads(data)

def save_pickle_data(obj, filename):
    """VULNERABLE: Creating pickle files"""
    with open(filename, 'wb') as f:
        pickle.dump(obj, f)

# VULNERABILITY: XML External Entity (XXE) attacks (Bandit, Semgrep)
def parse_xml(xml_string):
    """VULNERABLE: XXE attack via XML parsing"""
    return ET.fromstring(xml_string)

def parse_xml_file(filename):
    """VULNERABLE: XXE attack via XML file parsing"""
    parser = ET.XMLParser(resolve_entities=True)
    return ET.parse(filename, parser=parser)

# VULNERABILITY: Path traversal (Bandit, Semgrep)
def read_user_file(filename):
    """VULNERABLE: Path traversal vulnerability"""
    file_path = f"/app/uploads/{filename}"
    with open(file_path, 'r') as f:
        return f.read()

def write_user_file(filename, content):
    """VULNERABLE: Arbitrary file write"""
    file_path = f"/tmp/{filename}"
    with open(file_path, 'w') as f:
        f.write(content)

# VULNERABILITY: Server-Side Request Forgery (SSRF) (Semgrep)
def fetch_url(url):
    """VULNERABLE: SSRF attack via unvalidated URL"""
    return requests.get(url).text

def proxy_request(target_url):
    """VULNERABLE: Open redirect/SSRF"""
    parsed = urlparse(target_url)
    return requests.get(target_url, timeout=30)

# VULNERABILITY: Insecure random number generation (Bandit)
import random
def generate_session_id():
    """VULNERABLE: Weak random number generation"""
    return random.randint(100000, 999999)

def generate_password():
    """VULNERABLE: Predictable password generation"""
    chars = "abcdefghijklmnopqrstuvwxyz123456"
    return ''.join(random.choice(chars) for i in range(8))

# VULNERABILITY: Insecure temporary files (Bandit)
def create_temp_config():
    """VULNERABLE: Insecure temporary file creation"""
    temp_file = tempfile.mktemp(suffix='.conf')
    with open(temp_file, 'w') as f:
        f.write("admin_password=secret123")
    os.chmod(temp_file, 0o777)  # World-readable/writable
    return temp_file

# VULNERABILITY: SQL injection helpers (Bandit, Semgrep)
def build_sql_query(table, user_input):
    """VULNERABLE: SQL injection via string formatting"""
    return f"SELECT * FROM {table} WHERE name = '{user_input}'"

def execute_raw_sql(query):
    """VULNERABLE: Direct SQL execution"""
    import sqlite3
    conn = sqlite3.connect(':memory:')
    return conn.execute(query).fetchall()

# VULNERABILITY: Hardcoded cryptographic keys (Bandit)
ENCRYPTION_KEY = b'1234567890123456'  # 16 byte key
HMAC_SECRET = "hardcoded-hmac-secret"

def encrypt_data(data):
    """VULNERABLE: Using hardcoded encryption key"""
    from cryptography.fernet import Fernet
    import base64
    key = base64.urlsafe_b64encode(ENCRYPTION_KEY)
    f = Fernet(key)
    return f.encrypt(data.encode())

# VULNERABILITY: Assert statements for security checks (Bandit)
def validate_admin_access(user_role):
    """VULNERABLE: Using assert for security validation"""
    assert user_role == 'admin', "Access denied"
    return True

# VULNERABILITY: Weak hash functions (Bandit, Semgrep)
import hashlib
def hash_password(password):
    """VULNERABLE: Using SHA1 for password hashing"""
    return hashlib.sha1(password.encode()).hexdigest()

def verify_checksum(data, checksum):
    """VULNERABLE: Using MD5 for integrity check"""
    return hashlib.md5(data).hexdigest() == checksum