FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy project files
COPY . /app/

# Make gradlew executable and build
RUN chmod +x ./gradlew && ./gradlew build --no-daemon

# Default command
CMD ["./gradlew", "gatlingRun"]