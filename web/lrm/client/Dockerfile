# 1. Build
FROM node:24.11-alpine AS builder

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy the project files to the container
COPY . .

# Build the project
RUN npm run build

# 2. Start
FROM node:24.11-alpine AS runner

WORKDIR /app

ADD package.json .
ADD nuxt.config.ts .

COPY --from=builder /app/.nuxt ./.nuxt
COPY --from=builder /app/.output ./.output
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/public ./public

# Create non-root user for security
RUN addgroup -g 1001 -S app
RUN adduser -S app -u 1001 -G app

# Change ownership of the app directory to the non-root user
RUN chown -R app:app /app

ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

# Expose port 3000
EXPOSE 3000

# Switch to non-root user
USER app

# Start the web app
CMD ["npm", "start"]