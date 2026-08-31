# Node base image
FROM node:24.18-alpine AS build

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install dependencies via NPM (use latest npm in build stage)
RUN npm ci 

# Copy the project files to the container
COPY . .

# Install dependencies via NPM
RUN npm run build

# Stage 2 : run
FROM node:24.18-alpine AS production

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install production dependencies via NPM, then remove npm to avoid shipping its bundled packages
RUN npm ci --omit=dev && \
	npm cache clean --force && \
	rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx

# Copy build directory from stage 1
COPY --from=build /app/dist .

# Create non-root user for security
RUN addgroup -g 1001 -S app
RUN adduser -S app -u 1001 -G app

# Change ownership of the app directory to the non-root user
RUN chown -R app:app /app

# Setup ENV variable
ENV NODE_ENV=production
ENV PORT=80

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Switch to non-root user
USER app

# Start the Node web server
CMD ["node", "index.js"]