# Use a lightweight web server as the base image
FROM nginx:alpine

# Copy the files to the nginx default document root
COPY . /usr/share/nginx/html

# Expose port 80 to allow incoming traffic
EXPOSE 80

# Start the nginx web server
CMD ["nginx", "-g", "daemon off;"]
