import requests

from config import HTTP_TIMEOUT


class TimeoutSession(requests.Session):
    def request(self, method, url, **kwargs):
        kwargs.setdefault("timeout", HTTP_TIMEOUT)
        return super().request(method, url, **kwargs)


http_session = TimeoutSession()
