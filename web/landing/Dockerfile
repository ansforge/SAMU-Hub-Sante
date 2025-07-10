# Use a lightweight web server as the base image
FROM nginx:alpine

# Copy the static files to the nginx default document root
COPY . /usr/share/nginx/html

# Copy custom server config to use port 8080
COPY conf.d/default.conf /etc/nginx/conf.d/default.conf

# Fix permissions for nginx cache directory when running with non-root user
RUN mkdir -p /var/cache/nginx && chown -R nginx:nginx /var/cache/nginx /usr/share/nginx/html

# Expose port 8080 to allow incoming traffic
EXPOSE 8080

# Start the nginx web server with custom config and pid path
CMD ["nginx", "-g", "pid /var/cache/nginx/nginx.pid; daemon off;"]

# Run as non-root user for better security
USER nginx