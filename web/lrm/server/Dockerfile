# Node base image
FROM node:19

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install dependencies via NPM
RUN npm ci --omit=dev

# Copy the project files to the container
COPY . .

# Setup ENV variable
ENV NODE_ENV=production
ENV PORT=80

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Start the Node web server
CMD ["node", "index.js"]
