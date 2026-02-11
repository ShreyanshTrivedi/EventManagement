# Event Management System

A comprehensive event management platform built with a modern tech stack to facilitate event creation, booking, and management.

## Tech Stack

### Frontend
- React.js
- Tailwind CSS
- React Router
- Axios for API calls

### Backend
- Node.js
- Express.js
- JWT Authentication
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
├── frontend/          # React frontend application
├── backend/           # Node.js backend API
├── .gitignore         # Git ignore file
└── README.md          # This file
```

## Getting Started

### Prerequisites
- Node.js (v14 or higher)
- npm or yarn

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd EventManagement
```

2. Install backend dependencies:
```bash
cd backend
npm install
```

3. Install frontend dependencies:
```bash
cd ../frontend
npm install
```

### Running the Application

1. Start the backend server:
```bash
cd backend
npm start
```

2. Start the frontend development server:
```bash
cd frontend
npm start
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:5000

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
