# Use a lightweight web server as the base image
FROM nginx:alpine

# Copy the static files to the nginx default document root
COPY . /usr/share/nginx/html

# Fix permissions for nginx cache directory when running with non-root user
RUN mkdir -p /var/cache/nginx && chown -R nginx:nginx /var/cache/nginx

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Start the nginx web server
CMD ["nginx", "-g", "daemon off;"]

# Run as non-root user for better security
USER nginx