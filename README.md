StreamSphereApplication – Microservices Backend Architecture

📌 Overview

This project is a YouTube-like backend system built using modern Microservices Architecture with Spring Boot.

It simulates real-world video platform features such as:

User & Channel Management
Secure Authentication
Video Upload Management
Asynchronous Notifications
High Scalability & Decoupled Services

Screenshots:
<img width="1920" height="1538" alt="screencapture-localhost-8080-swagger-ui-index-html-2025-12-08-01_23_23" src="https://github.com/user-attachments/assets/fafe53b5-6d75-4baa-8c80-399239272273" />

<img width="1920" height="1028" alt="screencapture-localhost-8080-swagger-ui-index-html-2025-12-08-01_22_41" src="https://github.com/user-attachments/assets/b2ef0c48-61fd-4b49-8b7c-28548851685b" />

<img width="1920" height="970" alt="screencapture-localhost-8080-swagger-ui-index-html-2025-12-08-01_22_22" src="https://github.com/user-attachments/assets/0be38dd8-1de3-4244-8f1b-ac34a02ed137" />


🏗️ Microservices Architecture

The backend is divided into three independent microservices:

🔐 Central API Service

Handles:

User Authentication & Authorization

Channel creation & management

JWT Token validation

Secure Service-to-Service communication

🎥 Video API Service

Handles:

Video uploads

Integration with ImageKit.io

Video metadata management

Storage abstractions

🔔 Notification API Service

Handles:

Subscriptions

Channel activity notifications

Async message processing via RabbitMQ

Event-driven architecture

🧩 Tech Stack
Category	Technology
Language	Java 21
Framework	Spring Boot
Architecture	Microservices
Authentication	Spring Security + JWT
Databases	PostgreSQL + MongoDB
Messaging Queue	RabbitMQ
File Storage	ImageKit.io
Build Tool	Maven
API Docs	Swagger (OpenAPI)
Deployment	Docker for containerization of RabbitMQ

🔄 Architecture Diagram (Logical View)

<img width="401" height="277" alt="image" src="https://github.com/user-attachments/assets/dc7dfb94-095b-4ca0-a2c7-4fdd36c471f8" />

🚀 Features

JWT based Authentication
Centralized User Management
Decoupled Upload Processing
Event-Driven Notifications
Secure Microservice communication
Horizontal scalability
Cloud video storage
Stateless backend design
Clean layered architecture

⚙️ How to Run
1️⃣ Clone Repository
git clone https://github.com/programer12345anwar/YouTube-Clone.git
cd YouTube-Clone

2️⃣ Start Dependencies

Make sure these are running:

PostgreSQL

MongoDB

RabbitMQ

ImageKit credentials configured

### 🔧 Environment Variables

Create a `.env` file and add:

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
IMAGEKIT_PUBLIC_KEY=
IMAGEKIT_PRIVATE_KEY=
RABBITMQ_HOST=
```


4️⃣ Start Services (in order)
cd central-api && mvn spring-boot:run
cd video-api && mvn spring-boot:run
cd notification-api && mvn spring-boot:run

📖 API Documentation

Swagger UI (Running locally):

Service	URL
Central API	http://localhost:9000/swagger-ui.html
Video API	http://localhost:9001/swagger-ui.html
Notify API	http://localhost:9002/swagger-ui.html
🧪 Sample APIs
Feature	Endpoint
Create Channel	POST /channel/create
Upload Video	POST /video/upload
Subscribe	POST /subscribe/{channelId}
Notify	Internal Message Queue
🛡️ Security

JWT Authentication

API Gateway Pattern

Stateless Sessions

Role-based access

Inter-service authorization

👨‍💻 Author

Md Anwar Alam
📧 Email: mdanwar40212@gmail.com

🔗 GitHub: https://github.com/programer12345anwar

📢 Future Enhancements

✅ API Gateway

✅ Redis Caching

✅ Service Discovery (Eureka)

✅ Kubernetes Deployment

✅ CI/CD Integration

✅ Load Balancing

⭐ Support

If you found this project useful:

✅ Give it a ⭐ on GitHub
✅ Fork & contribute
✅ Use in portfolio
