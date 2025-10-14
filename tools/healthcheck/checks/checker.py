from abc import ABC, abstractmethod


class IChecker(ABC):
    @abstractmethod
    def perform_checks(self):
        pass
