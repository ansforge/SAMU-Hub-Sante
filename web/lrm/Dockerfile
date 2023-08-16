# Node base image
# --platform to avoid Mac M1 build issues when deploying
# Ref.: https://stackoverflow.com/questions/65612411/forcing-docker-to-use-linux-amd64-platform-by-default-on-macos/69636473#69636473
# Not enough -> use buildx to build image
# Ref.: https://stackoverflow.com/questions/73285601/docker-exec-usr-bin-sh-exec-format-error
FROM --platform=linux/amd64 node:19

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY server/package*.json ./

# Install dependencies via NPM
RUN npm ci --omit=dev

# Copy the project files to the container
COPY server/ .

# Setup ENV variable
ENV NODE_ENV=production
ENV PORT=80
ENV HUB_URL=amqps://messaging.hub.esante.gouv.fr

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Start the Node web server
CMD ["node", "index.js"]
