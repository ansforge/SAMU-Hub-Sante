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
	RABBITMQ_HEALTH_URL=http://test \
	SHOVEL_STATUS_URL=http://shovel_test \
	ANNUAIRE_HEALTH_URL=http://annuaire_test \
	CONVERTER_HEALTH_URL=http://converter_test \
	RABBITMQ_MONITORING_USERNAME=test \
	RABBITMQ_MONITORING_PASSWORD=test \
	DISPATCHER_CONFIG_FILE_PATH=dispatchers_config_file_path_example.txt \
	uv run coverage run -m unittest tests/*.py

show-coverage: ## Display coverage report
	uv run coverage report -m
