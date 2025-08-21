# Tuniv 🇹🇳

A collaborative Q&A and community platform designed for university students in Tunisia. Find reliable answers, connect with peers, and share knowledge within your university and module.

![Build Status](https://img.shields.io/github/actions/workflow/status/Ahmed-Mhiri/Tuniv/main.yml?branch=main&style=for-the-badge)
![License](https://img.shields.io/github/license/Ahmed-Mhiri/Tuniv?style=for-the-badge)

---

## About The Project

Struggling to find clear answers for your university modules? Wasting time in noisy and disorganized Facebook or WhatsApp groups? **Tuniv** is the solution.

This platform provides a structured environment where students can ask technical and academic questions, get high-quality answers from their peers and professors, and build a lasting knowledge base for their specific university and field of study. It's built to function like a focused, academic "Reddit" for the Tunisian university ecosystem, fostering a community of learning and collaboration.



### Key Features

* 🎓 **University-Specific Hubs:** Content is organized by university and module, making it easy to find relevant information.
* ❓ **Robust Q&A System:** Ask questions, provide detailed answers with code snippets, vote on content, and mark the best solution.
* 📈 **Reputation System:** Earn points and build credibility by providing helpful answers.
* 👤 **Rich User Profiles:** Showcase your major, university role (student, professor, alumni), and contributions.
* 💬 **Direct Messaging:** Connect privately with other users for mentorship or collaboration.
* 🔍 **Powerful Search:** Instantly find questions and answers across the entire platform, powered by Algolia.
* 📱 **Cross-Platform:** Accessible via a responsive web app and native mobile apps for Android & iOS.

---

## Tech Stack

This project is built with a modern, scalable, and maintainable technology stack, designed for high performance and a great developer experience.

| Category           | Technology                                           |
| :----------------- | :--------------------------------------------------- |
| **Backend** | Spring Boot (Java), Spring Security (JWT)            |
| **Frontend (Web)** | Angular, Tailwind CSS                              |
| **Mobile** | Kotlin (Jetpack Compose) & Swift (SwiftUI)           |
| **Database** | PostgreSQL, Redis (for caching)                      |
| **Search** | Algolia                                              |
| **File Storage** | S3-compatible Object Storage (e.g., DigitalOcean Spaces) |
| **DevOps** | Docker, GitHub Actions, Terraform                    |
| **Real-time** | WebSockets (for Chat Service)                        |

---

## Architecture & Design

This project is guided by a comprehensive set of product design and software architecture documents. These diagrams provide a complete blueprint of the platform's vision, user experience, and technical implementation.

### Product Design

These documents define the **"who, what, and why"** of the platform from a user's perspective. They ensure we are building the right product for the right people.

* ➡️ **[User Persona](./docs/product-design/user-persona.md)** - An in-depth look at our target user, Amine.
* ➡️ **[User Journey Map](./docs/product-design/user-journey-map.md)** - Visualizing the "before and after" experience.
* ➡️ **[Site Map](./docs/product-design/site-map.md)** - The structural blueprint of all screens and pages.
* ➡️ **[User Flows](./docs/product-design/user-flows.md)** - Step-by-step paths for key tasks like asking questions and messaging.

---

### Software Architecture

These documents outline the **"how"**—the technical blueprint for the system's construction, operation, and scalability.

* ➡️ **[System Architecture](./docs/software-architecture/system-architecture.md)** - A high-level view of the entire cloud infrastructure.
* ➡️ **[Microservices Model](./docs/software-architecture/microservices-model.md)** - The backend service-oriented architecture.
* ➡️ **[Database Schema (ERD)](./docs/software-architecture/database-schema.md)** - The complete structure of our PostgreSQL database.
* ➡️ **[API Contract](./docs/software-architecture/api-contract.md)** - The formal specification for our REST API.
* ➡️ **[CI/CD Pipeline](./docs/software-architecture/ci-cd-pipeline.md)** - The automated build, test, and deployment process.

---

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

* Java 17+
* Node.js and npm
* Docker and Docker Compose
* Android Studio / Xcode for mobile development

### Installation

1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/Ahmed-Mhiri/Tuniv.git](https://github.com/Ahmed-Mhiri/Tuniv.git)
    ```
2.  **Backend Setup:**
    * Navigate to the `backend` directory.
    * Create an `application.properties` file based on the example.
    * Run the database using Docker Compose: `docker-compose up -d`
    * Build and run the Spring Boot application.
3.  **Frontend Setup:**
    * Navigate to the `frontend` directory.
    * Install dependencies: `npm install`
    * Start the development server: `npm start`

---

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

Please see `CONTRIBUTING.md` for details on our code of conduct and the process for submitting pull requests.

---

## License

Distributed under the MIT License. See `LICENSE.md` for more information.

---

## Contact

Ahmed Mhiri - [@your_twitter](https://twitter.com/your_twitter) - ahmedmhiri2004@gmail.com

Project Link: [https://github.com/Ahmed-Mhiri/Tuniv](https://github.com/Ahmed-Mhiri/Tuniv)