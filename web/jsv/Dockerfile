# Base image
FROM node:20-slim as build

# Set working directory
WORKDIR /app

# Copy the project files to the container
COPY . .

# Install dependencies via NPM
RUN npm install

# Install Bower globally
RUN npm install -g bower

# Install dependencies via Bower
RUN bower install --allow-root

# Install Sass via gem
RUN apt-get update && apt-get install -y build-essential && apt-get install -y ruby-dev
RUN gem install sass

# Build project via Grunt
RUN npm install -g grunt-cli
RUN grunt prod

# Use a lightweight web server as the base image
# --platform to avoid Mac M1 build issues when deploying
# Ref.: https://stackoverflow.com/questions/65612411/forcing-docker-to-use-linux-amd64-platform-by-default-on-macos/69636473#69636473
# Not enough -> use buildx to build image
# Ref.: https://stackoverflow.com/questions/73285601/docker-exec-usr-bin-sh-exec-format-error
FROM --platform=linux/amd64 nginx:alpine as run

# Copy the production build to the nginx default document root
COPY --from=build /app/prod /usr/share/nginx/html

# Expose the desired port (e.g., 80 for HTTP)
EXPOSE 80

# Start the nginx web server
CMD ["nginx", "-g", "daemon off;"]
