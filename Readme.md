# CQRSApplication

## Overview

CQRSApplication is a Java-based project that implements the Command Query Responsibility
Segregation (CQRS) pattern using Spring Boot, Kafka, and JPA. The application processes user
events (creation and updates) and maintains a projection of user data in a relational database.

## Features

- **CQRS Pattern**: Separates the read and write operations for better scalability and
  maintainability.
- **Event-Driven Architecture**: Uses Kafka for event streaming and processing.
- **Spring Boot**: Provides a robust and scalable framework for building the application.
- **JPA**: Manages the persistence of user projections in a relational database.
- **Integration Tests**: Uses Testcontainers and Embedded Kafka for comprehensive integration
  testing.

## Project Structure

- `src/main/java/com/example/demo`: Contains the main application code.
    - `command`: Handles the command side of the CQRS pattern.
        - `model`: Defines the events (`UserCreatedEvent`, `UserUpdatedEvent`).
        - `exceptions`: Custom exceptions for command processing.
    - `query`: Handles the query side of the CQRS pattern.
        - `model`: Defines the `UserProjection` entity.
        - `repo`: Repository interface for accessing user projections.
    - `listener`: Contains the `KafkaEventListener` for processing Kafka events.
- `src/test/java/com/example/demo`: Contains the test code.
    - `config`: Configuration classes for testing with Kafka and Testcontainers.
    - `integration`: Integration tests for the application.
- `src/test/resources`: Contains test configuration files.
    - `application-test.yml`: Configuration for running tests.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6.3 or higher
- Docker (for running Testcontainers)

### Building the Project

To build the project, run the following command:

```sh
mvn clean install
```

### Running the Application

To run the application, use the following command:

```sh
mvn spring-boot:run
```

### Running Tests

To run the tests, use the following command:

```sh
mvn test
```

## Configuration

### Kafka Configuration

The application uses Kafka for event streaming. The Kafka configuration is defined in
`application.yml` and `application-test.yml` for testing purposes.

### Database Configuration

The application uses JPA for managing the persistence of user projections. The database
configuration is defined in `application.yml`.

## Event Handling

### UserCreatedEvent

When a `UserCreatedEvent` is received, the application creates a new `UserProjection` or updates an
existing one if the event version is higher.

### UserUpdatedEvent

When a `UserUpdatedEvent` is received, the application updates the existing `UserProjection` if the
event version is higher.

## Integration Tests

The project includes comprehensive integration tests using Testcontainers and Embedded Kafka. The
tests cover various scenarios, including:

- Creating and updating user projections.
- Handling duplicate and out-of-order events.
- Simulating Kafka downtime.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug
fixes.

## Contact

For any questions or inquiries, please contact the project maintainer at [maintainer@example.com].