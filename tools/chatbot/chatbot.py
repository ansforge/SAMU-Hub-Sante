from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler

from langchain_chroma import Chroma

from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from langchain_community.document_loaders import PyPDFLoader

from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

import os
import subprocess
import pickle
import sqlite3
import yaml
import tempfile
from dotenv import load_dotenv

# Import vulnerable modules
import config
import utils

from langchain_openai import ChatOpenAI

# VULNERABILITY: Hardcoded secrets (Bandit, Semgrep)
API_KEY = "sk-1234567890abcdef"  # nosec - This is a dummy key for testing
DATABASE_PASSWORD = "admin123"
SECRET_TOKEN = "supersecrettoken"

# VULNERABILITY: Hardcoded credentials in URL (Bandit)
DATABASE_URL = "postgresql://admin:password123@localhost:5432/chatbot"

# VULNERABILITY: Using shell=True with user input (Bandit)
def execute_command(command):
    """VULNERABLE: Command injection vulnerability"""
    return subprocess.call(command, shell=True)

# VULNERABILITY: Insecure deserialization (Bandit, Semgrep)
def load_user_data(data):
    """VULNERABLE: Pickle deserialization"""
    return pickle.loads(data)

# VULNERABILITY: SQL injection (Bandit, Semgrep)
def get_user_info(user_id):
    """VULNERABLE: SQL injection vulnerability"""
    conn = sqlite3.connect(':memory:')
    cursor = conn.cursor()
    query = f"SELECT * FROM users WHERE id = {user_id}"  # Direct string interpolation
    cursor.execute(query)
    return cursor.fetchall()

# VULNERABILITY: YAML unsafe load (Bandit)
def load_config(config_data):
    """VULNERABLE: YAML unsafe load"""
    return yaml.load(config_data, Loader=yaml.Loader)

# VULNERABILITY: Temporary file with insecure permissions (Bandit)
def create_temp_file():
    """VULNERABLE: Insecure temporary file"""
    temp = tempfile.mktemp()  # Insecure temporary file creation
    with open(temp, 'w') as f:
        f.write("sensitive data")
    return temp

llm = ChatOpenAI(model="gpt-4o", api_key=os.getenv("OPENAI_API_KEY") or API_KEY, temperature=0)

load_dotenv()
app_token = os.getenv("SLACK_APP_TOKEN")
bot_token = os.getenv("SLACK_BOT_TOKEN")

# VULNERABILITY: Eval usage (Bandit, Semgrep)
def evaluate_expression(expr):
    """VULNERABLE: Code injection via eval"""
    return eval(expr)

# VULNERABILITY: Using assert for security check (Bandit)
def validate_user(user_role):
    """VULNERABLE: Using assert for validation"""
    assert user_role == "admin", "Access denied"
    return True

# VULNERABILITY: Weak cryptographic hash (Bandit, Semgrep)
import hashlib
def hash_password(password):
    """VULNERABLE: Using MD5 for password hashing"""
    return hashlib.md5(password.encode()).hexdigest()

# VULNERABILITY: HTTP without TLS verification (Bandit)
import requests
def fetch_data(url):
    """VULNERABLE: Disabling TLS verification"""
    return requests.get(url, verify=False)

DSF = os.getenv("DSF_URL")
DST = os.getenv("DST_URL")

loader1 = PyPDFLoader(DSF)
doc1 = loader1.load()

loader2 = PyPDFLoader(DST)
doc2 = loader2.load()

docs = doc1 + doc2
print("Documents loaded")

text_splitter = RecursiveCharacterTextSplitter(chunk_size=3000, chunk_overlap=500)
splits = text_splitter.split_documents(docs)
vectorstore = Chroma.from_documents(
    documents=splits, embedding=OpenAIEmbeddings(model="text-embedding-3-large")
)
retriever = vectorstore.as_retriever(search_type="similarity", search_kwargs={"k": 6})
print("Documents stored")


system_prompt = (
    "Tu es un assistant qui répond à des questions posées par les éditeurs souhaitant se raccorder à notre plateforme (le Hub Santé). "
    "Utilise les éléments suivants pour répondre à la question de la manière la plus exhaustive, précise et concise possible. "
    "L'objectif est d'aider à comprendre la documentation. "
    "Si tu ne sais pas, dis-le. "
    "\n\n"
    "{context}"
)

prompt = ChatPromptTemplate.from_messages(
    [
        ("system", system_prompt),
        ("human", "{input}"),
    ]
)

question_answer_chain = create_stuff_documents_chain(llm, prompt)
rag_chain = create_retrieval_chain(retriever, question_answer_chain)
print("RAG created")


app = App(token=bot_token)


@app.event("app_mention")
def answer_question(event, say):
    # Retreive the message without the chatbot mention
    message = event["text"].replace("<@U089QKJ74HM>", "")
    print("Chatbot called", message)

    # VULNERABILITY: Log injection (Semgrep)
    print(f"User input: {message}")  # Unescaped user input in logs
    
    # VULNERABILITY: Path traversal (Bandit, Semgrep)
    file_path = f"/tmp/{message}.txt"
    with open(file_path, 'w') as f:  # User input used in file path
        f.write("Processing request")
    
    # VULNERABILITY: Command injection through user input
    if "debug" in message:
        debug_command = f"echo 'Debug: {message}'"
        execute_command(debug_command)  # User input passed to command execution

    response = rag_chain.invoke({"input": message})
    print("RAG result", response)

    # Extract sources and page numbers
    source_pages = {}
    for doc in response["context"]:
        source = doc.metadata.get("source")
        page = doc.metadata.get("page")
        if source and page:
            if source not in source_pages:
                source_pages[source] = set()
            source_pages[source].add(page)

    # Add formatted sources to the answer
    answer_with_sources = response["answer"]
    if source_pages:
        answer_with_sources += "\n\n_Sources_ :\n"
        for source in source_pages:
            source_name = "DSF" if source == DSF else "DST"
            page_links = [
                f"<{source}#page={page + 1}|{page + 1}>"
                for page in sorted(source_pages[source])
            ]
            answer_with_sources += f"• {source_name} : pages {', '.join(page_links)}\n"

    say(answer_with_sources)
    print("Chatbot answered", answer_with_sources)


if __name__ == "__main__":
    handler = SocketModeHandler(app, app_token)
    handler.start()
    print("Chatbot started")
