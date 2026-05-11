import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from config import HTTP_TIMEOUT


class TimeoutSession(requests.Session):
    def __init__(self) -> None:
        super().__init__()
        retry = Retry(
            total=1,
            connect=1,
            read=0,
            status=0,
            allowed_methods={"GET"},
            backoff_factor=0,
            raise_on_status=False,
        )
        adapter = HTTPAdapter(max_retries=retry)
        self.mount("http://", adapter)
        self.mount("https://", adapter)

    def request(self, method, url, **kwargs):
        kwargs.setdefault("timeout", HTTP_TIMEOUT)
        return super().request(method, url, **kwargs)


http_session = TimeoutSession()
