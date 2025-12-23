.PHONY: build run test clean docker-build docker-run

# Build the application
build:
	./mvnw clean package -DskipTests

# Build with tests
build-test:
	./mvnw clean package

# Run the application locally
run:
	./mvnw spring-boot:run

# Run tests
test:
	./mvnw test

# Clean build artifacts
clean:
	./mvnw clean

# Build Docker image
docker-build:
	docker build -t planify/notification-service:latest .

# Run Docker container
docker-run:
	docker run -p 8084:8084 \
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify_notifications \
		-e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
		-e SENDGRID_API_KEY=$$SENDGRID_API_KEY \
		planify/notification-service:latest

# Build Lambda deployment package
lambda-build:
	./mvnw clean package
	cp target/notification-service-1.0.0-SNAPSHOT-lambda.jar lambda-deployment.jar
