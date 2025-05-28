# Node base image
FROM node:20.19-alpine AS build

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install dependencies via NPM
RUN npm install

# Copy the project files to the container
COPY . .

# Install dependencies via NPM
RUN npm run build && npm run postbuild

# Stage 2 : run
FROM node:20.19-alpine AS production

# Set working directory
WORKDIR /app

# Copy the npm project files needed for install to the container
COPY package*.json ./

# Install production dependencies via NPM
RUN npm ci --omit=dev

# Copy build directory from stage 1
COPY --from=build /app/dist ./dist

# Setup ENV variable
ENV NODE_ENV=production
ENV PORT=80

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Start the Node web server
CMD ["node", "dist/index.js"]
