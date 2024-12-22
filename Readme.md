# Event-Driven CQRS Example

This project demonstrates an event-driven architecture using the Command Query Responsibility Segregation (CQRS) pattern with Apache Kafka, Spring Boot, and Java.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Technologies Used](#technologies-used)
- [Setup and Running](#setup-and-running)
- [Project Structure](#project-structure)
- [Usage](#usage)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Overview
This project showcases how to implement an event-driven system using CQRS. It includes:
- Command side for handling user creation and updates.
- Query side for reading user data.
- Kafka for event streaming.

## Architecture
- **Command Side**: Handles user creation and updates, publishes events to Kafka.
- **Query Side**: Listens to Kafka events, updates the read model.

## Technologies Used
- Java 21
- Spring Boot 3.1.4
- Apache Kafka
- Spring Kafka
- Spring Data JPA
- H2 Database (for testing)
- Lombok
- Maven

## Setup and Running
### Prerequisites
- Java 21
- Maven
- Docker and Docker Compose

### Steps
1. **Clone the repository**:
    ```sh
    git clone https://github.com/joaccenture1234/event-driven-cqrs.git
    cd event-driven-cqrs
    ```

2. **Build the project**:
    ```sh
    mvn clean install
    ```

3. **Start Kafka and Zookeeper**:
   Use Docker Compose to start Kafka and Zookeeper:
    ```sh
    docker-compose up -d
    ```
   This will start the following services:
   - Zookeeper on port 2182
   - Kafka on port 9093

4. **Run the application**:
    ```sh
    mvn spring-boot:run
    ```

## Project Structure
```plaintext
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── command/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── producer/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── listener/
│   │   ├── query/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── repo/
│   │   │   └── service/
│   └── resources/
│       └── application.yml
└── test/
    ├── java/com/example/demo/
    │   ├── listener/
    └── resources/
        └── application-test.yml