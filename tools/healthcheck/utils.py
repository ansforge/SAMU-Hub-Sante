import logging

def remove_error_keys(d, component_name='root'):
    if isinstance(d, dict):
        # Check if "error" key exists and log it
        if "error" in d:
            logging.error(f"Error encountered in component '{component_name}': {d['error']}")
            del d["error"]

        # Recursively remove "error" from nested dictionaries
        for key, value in d.items():
            remove_error_keys(value, component_name=f"{component_name}.{key}")

    # If d is a list, iterate through each element
    elif isinstance(d, list):
        for item in d:
            remove_error_keys(item, component_name=component_name)

    return d