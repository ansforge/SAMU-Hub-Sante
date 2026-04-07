from abc import ABC, abstractmethod

import logging


class IChecker(ABC):
    @abstractmethod
    def perform_checks(self):
        pass

    @abstractmethod
    def check_failure_fallback(self):
        pass

    def check_wrapper(self):
        component_name = type(self).__name__
        try:
            logging.info(f"Performing {component_name} check")
            return self.perform_checks()
        except Exception:
            logging.exception(f"Error occurred when performing {component_name} check")
            return self.check_failure_fallback()
