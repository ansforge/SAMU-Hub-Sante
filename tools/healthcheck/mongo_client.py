import os
from pymongo import MongoClient

MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")

mongo_client = MongoClient(MONGODB_URI, serverSelectionTimeoutMS=1000)
