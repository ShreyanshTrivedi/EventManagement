# Event Management System

A comprehensive event management platform built with a modern tech stack to facilitate event creation, booking, and management.

## Tech Stack

### Frontend
- React.js
- Tailwind CSS
- React Router
- Axios for API calls

### Backend
- Java 17
- Spring Boot 3.3.4
- Spring Security (JWT Authentication)
- Spring Data JPA
- MySQL Database
- Maven
- Role-Based Access Control (RBAC)

## Features

- **User Authentication**: Login, registration with role-based access
- **Event Management**: Create, view, and manage events
- **Room Booking**: Book rooms for events
- **Role-Based Access Control**: 
  - GENERAL_USER: Basic access
  - FACULTY: Enhanced permissions
  - CLUB_ASSOCIATE: Club management features
  - ADMIN: Full system access

## Project Structure

```
EventManagement/
├── frontend/                    # React frontend application
├── backend/event-management/     # Spring Boot backend API
├── .gitignore                   # Git ignore file
└── README.md                    # This file
```

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Node.js (v14 or higher)
- npm or yarn
- MySQL Database

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd EventManagement
```

2. Set up the backend:
```bash
cd backend/event-management
# Configure database connection in src/main/resources/application.yml
mvn clean install
```

3. Install frontend dependencies:
```bash
cd ../frontend
npm install
```

### Running the Application

1. Start the backend server:
```bash
cd backend/event-management
mvn spring-boot:run
```

2. Start the frontend development server:
```bash
cd frontend
npm run dev
```

The application will be available at:
- Frontend: http://localhost:5173 (Vite default)
- Backend API: http://localhost:8080 (Spring Boot default)

## Authentication Flow

- New users register with default role: GENERAL_USER
- Login redirects based on user role:
  - GENERAL_USER → Home page (/)
  - FACULTY/CLUB_ASSOCIATE/ADMIN → Dashboard (/dashboard)

## Protected Routes

- `/dashboard` - User dashboard
- `/bookings` - Booking management
- `/register/:eventId` - Event registration
- `/book-room` - Room booking

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.
