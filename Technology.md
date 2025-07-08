# ThingsBoard IoT Platform

## Overview
ThingsBoard is an open-source IoT platform for data collection, processing, visualization, and device management. It enables device connectivity via industry standard IoT protocols and supports both cloud and on-premises deployments.

## Core Features
- **Device Management**: Register, monitor and control IoT devices
- **Asset Management**: Model and track physical assets
- **Data Collection**: Collect and store telemetry data
- **Rule Engine**: Process and react to device data
- **Visualization**: Create interactive dashboards
- **Alarms**: Generate and manage alarms
- **Notifications**: Send notifications via multiple channels
- **Edge Computing**: Extend platform capabilities to edge devices
- **Multi-tenancy**: Support for multiple tenants and customers

## Technology Stack
- **Backend**: 
  - Java 17, Spring Boot 3.2.12
  - Cassandra, PostgreSQL
  - Kafka, RabbitMQ
  - Redis, Caffeine
- **Protocols**: 
  - MQTT, CoAP, LWM2M, SNMP
- **Frontend**: 
  - Angular (ui-ngx module)
- **Security**: 
  - Spring Security, JWT

## Project Structure
### Core Modules
- **application**: Main application module with controllers and services
- **common**: Common components and utilities
- **dao**: Data access layer
- **transport**: Protocol implementations (MQTT, CoAP, LWM2M, SNMP)
- **rule-engine**: Rule processing engine
- **netty-mqtt**: MQTT protocol implementation
- **msa**: Microservice architecture support
- **ui-ngx**: Angular-based web UI

### Key Directories
- **application/src/main/java/org/thingsboard/server**
  - `controller`: REST API endpoints
  - `service`: Business logic implementation
  - `actors`: Actor model implementation
  - `config`: Application configuration

## Getting Started
### Prerequisites
- Java 17
- Maven 3.6+
- Node.js 16+ (for UI development)
- Cassandra 3.11+ or PostgreSQL 12+

### Build and Run
1. Clone the repository:
   ```bash
   git clone https://github.com/thingsboard/thingsboard.git
   ```
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   java -jar application/target/application-*.jar
   ```

## Development Guide
### Code Style
- Follow Google Java Style Guide
- Use Lombok for boilerplate code reduction
- Keep controller methods clean and delegate to services

### Testing
- Unit tests should cover critical business logic
- Integration tests for API endpoints
- Test containers for database integration tests

## License
Apache License 2.0 - See [LICENSE](LICENSE) for details.
