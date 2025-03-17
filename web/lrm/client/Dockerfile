# 1. Build
FROM node:19 AS builder

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
FROM node:19 AS runner

WORKDIR /app

ADD package.json .
ADD nuxt.config.ts .

COPY --from=builder /app/.nuxt ./.nuxt
COPY --from=builder /app/.output ./.output
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/public ./public

ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

# Expose port 3000
EXPOSE 3000

# Start the web app
CMD ["npm", "start"]
