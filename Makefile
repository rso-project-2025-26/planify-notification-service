.PHONY: build run test clean docker-build docker-run build-core build-functions

# Build all modules
build:
	./mvnw clean package -DskipTests

# Build only Spring Boot core
build-core:
	./mvnw clean package -DskipTests -pl notification-core -am

# Build only Azure Functions
build-functions:
	./mvnw clean package -DskipTests -pl notification-functions -am

# Build with tests
build-test:
	./mvnw clean package

# Run the Spring Boot application locally
run:
	cd notification-core && ../mvnw spring-boot:run

# Run Azure Functions locally
run-functions:
	cd notification-functions && mvn azure-functions:run

# Run tests
test:
	./mvnw test

# Clean build artifacts
clean:
	./mvnw clean

# Build Docker image (Spring Boot only)
docker-build:
	docker build -t planify/notification-service:latest .

# Run Docker container
docker-run:
	docker run -p 8084:8084 \
		-e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/planify_notifications \
		-e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
		-e SENDGRID_API_KEY=$$SENDGRID_API_KEY \
		planify/notification-service:latest

# Deploy Azure Functions
deploy-functions:
	cd notification-functions && mvn azure-functions:deploy
