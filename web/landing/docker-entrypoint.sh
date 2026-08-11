#!/bin/sh
set -e

# Génère le fichier de config à partir de la variable d'environnement
cat > /usr/share/nginx/html/script/annuaire/env-config.js << EOF
export const API_URL = "${PUBLIC_ANNUAIRE_API_URL}";
EOF

# Lance la commande normale (nginx)
exec "$@"