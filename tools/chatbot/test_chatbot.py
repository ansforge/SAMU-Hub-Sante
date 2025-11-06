"""
Test file with security vulnerabilities for testing security scanners
"""
import unittest
import tempfile
import os
import subprocess
import yaml

# VULNERABILITY: Hardcoded test credentials (Bandit)
TEST_API_KEY = "test-api-key-123456"
TEST_DATABASE_PASSWORD = "testpass123"

class VulnerableTests(unittest.TestCase):
    
    def setUp(self):
        # VULNERABILITY: Hardcoded sensitive data in tests (Bandit)
        self.secret_token = "hardcoded-test-token"
        self.admin_password = "admin123"
    
    def test_command_injection(self):
        """VULNERABLE: Testing command injection"""
        user_input = "test; rm -rf /"
        command = f"echo {user_input}"
        result = subprocess.call(command, shell=True)
        self.assertIsNotNone(result)
    
    def test_insecure_temp_file(self):
        """VULNERABLE: Creating insecure temporary files"""
        temp_file = tempfile.mktemp()
        with open(temp_file, 'w') as f:
            f.write("sensitive test data")
        os.chmod(temp_file, 0o777)
    
    def test_yaml_loading(self):
        """VULNERABLE: Unsafe YAML loading in tests"""
        yaml_data = "test: value"
        result = yaml.load(yaml_data, Loader=yaml.Loader)
        self.assertEqual(result['test'], 'value')
    
    def test_eval_usage(self):
        """VULNERABLE: Using eval in tests"""
        expression = "2 + 2"
        result = eval(expression)
        self.assertEqual(result, 4)
    
    def test_hardcoded_crypto_key(self):
        """VULNERABLE: Hardcoded cryptographic material"""
        crypto_key = "1234567890abcdef"
        self.assertEqual(len(crypto_key), 16)
    
    def test_sql_injection_setup(self):
        """VULNERABLE: SQL injection in test setup"""
        import sqlite3
        conn = sqlite3.connect(':memory:')
        user_id = "1' OR '1'='1"
        query = f"SELECT * FROM users WHERE id = {user_id}"
        # This would be vulnerable if executed
    
    def test_assert_for_security(self):
        """VULNERABLE: Using assert for security checks in tests"""
        user_role = "admin"
        assert user_role == "admin", "Security check failed"
    
    def test_weak_random(self):
        """VULNERABLE: Using weak random in tests"""
        import random
        token = random.randint(1000, 9999)
        self.assertGreater(token, 999)

if __name__ == '__main__':
    # VULNERABILITY: Running tests with debug information
    unittest.main(verbosity=2)