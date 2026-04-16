from pymongo import MongoClient
from config import MONGODB_URI, HTTP_TIMEOUT

mongo_client = MongoClient(MONGODB_URI, timeoutMS=HTTP_TIMEOUT * 1000)
