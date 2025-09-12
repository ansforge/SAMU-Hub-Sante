# Annuaire API Flask

## Setup

```bash
# Create a virtualenv
python -m venv env

# Activate the virtual env (Linux/macOS)
source env/bin/activate
# Activate the virtual env (windows)
env/Scripts/activate

# Install the dependencies
python -m pip install -r requirements.txt
```

## Tests

To run the tests: `python -m unittest test_annuaire.py`

To run the tests and generate a coverage report: `coverage run --source=annuaire -m unittest test_annuaire.py`

To display the coverage report summary in the terminal: `coverage report -m`

To generate a html report from an existing report: `coverage html` & open [htmlcov/index.html](htmlcov/index.html) in a browser.

Coverage doc available [here](https://coverage.readthedocs.io/en/7.8.0/)
