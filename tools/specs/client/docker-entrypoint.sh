#!/bin/sh
set -e

mkdir -p /usr/share/nginx/html/config
cat <<EOF > /usr/share/nginx/html/config/env-config.js
window.__ENV__ = { VITE_SPECS_API_DOMAIN: "${VITE_SPECS_API_DOMAIN}" };
EOF

exec nginx -c /usr/share/nginx/html/nginx.conf -g "daemon off;"
