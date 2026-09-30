#!/bin/sh
set -e

cat <<EOF > /usr/share/nginx/html/env-config.js
window.__ENV__ = {
  VITE_SPECS_API_DOMAIN: "${VITE_SPECS_API_DOMAIN}",
  // raw JSON array, not a string, e.g. ["3.4.2","2.4.0"]
  VITE_SPECS_PUBLIC_VERSIONS: ${VITE_SPECS_PUBLIC_VERSIONS:-undefined},
  // raw JSON object from hubsante-topology's lrm-editors-configmap vhost.map
  VITE_SPECS_VHOST_MAP: ${VITE_SPECS_VHOST_MAP:-undefined},
};
EOF

exec nginx -c /usr/share/nginx/html/nginx.conf -g "daemon off;"
