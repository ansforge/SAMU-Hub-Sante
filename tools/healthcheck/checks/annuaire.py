from prometheus_client import Gauge

from checks.checker import IChecker
from checks.status import Status
from http_client import http_session

ANNUAIRE_HEALTH_URL = (
    "http://annuaire-service.annuaire.svc.cluster.local/annuaire/health"
)


class AnnuaireHealthcheck(IChecker):
    annuaire_status_metric = Gauge(
        "annuaire_status", "Statut de l'annuaire (1=UP, 0=DOWN)"
    )

    def perform_checks(self):
        response = http_session.get(ANNUAIRE_HEALTH_URL)
        response.raise_for_status()
        data = response.json()
        status = data.get("status", Status.UNKNOWN.value)
        self.annuaire_status_metric.set(1 if status == Status.UP.value else 0)
        return {
            "annuaire": (
                {"status": Status.UP.value}
                if status == Status.UP.value
                else {"status": Status.DOWN.value}
            )
        }

    def check_failure_fallback(self):
        self.annuaire_status_metric.set(0)
        return {"annuaire": {"status": Status.DOWN.value}}
