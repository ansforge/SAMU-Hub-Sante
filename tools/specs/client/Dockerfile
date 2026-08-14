# 1. Build
FROM dhi.io/node:24.19-alpine3.24-dev AS builder

WORKDIR /app

# Enable pnpm via corepack (bundled with Node)
RUN corepack enable

# Copy the pnpm project files needed for install to the container
COPY package.json pnpm-lock.yaml ./

# Install dependencies
RUN pnpm install --frozen-lockfile

# Copy the project files to the container
COPY . .

# Build the project
RUN pnpm build

# 2. Serve
FROM dhi.io/nginx:1.31.3-alpine3.24

# The hardened nginx image runs as a non-root user by default; switch to root
# for the setup steps below, then drop back to non-root before running.
USER 0

# Copy the static build output to the nginx document root
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy the nginx configuration files
COPY nginx.conf /usr/share/nginx/html/nginx.conf
COPY conf.d/default.conf /etc/nginx/conf.d/default.conf

# Vite bakes VITE_* vars at build time, but this image is built once and
# deployed to multiple environments with per-env values injected as container
# env vars. This entrypoint writes those values to a small script, loaded by
# index.html before the app bundle, so they're readable at container runtime.
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

RUN export VITE_SPECS_API_DOMAIN=https://integration.hub.esante.gouv.fr/specs/api

# Expose port 8080 to allow incoming traffic
EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]

# Run as non-root user for better security
USER nginx
