# Misc
.DEFAULT_GOAL = help
.PHONY        : help ruff-check ruff-format test show-coverage

help: ## Outputs this help screen
	@grep -E '(^[a-zA-Z0-9\./_-]+:.*?##.*$$)|(^##)' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}{printf "\033[32m%-30s\033[0m %s\n", $$1, $$2}' | sed -e 's/\[32m##/[33m/'

ruff-check: ## Run ruff to check code against good practices & correct fixable issues
	uv run ruff check --fix

ruff-format: ## Run ruff to format code files
	uv run ruff format

test: ## Run tests with coverage
	RABBITMQ_URL=http://test RABBITMQ_MONITORING_USERNAME=test RABBITMQ_MONITORING_PASSWORD=test DISPATCHER_INSTANCES=dispatcher_instance uv run coverage run -m unittest tests/*.py

show-coverage: ## Display coverage report
	uv run coverage report -m
