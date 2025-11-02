# Game Release Tracker - Frontend

This is the React frontend for the Game Release Tracker application.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

3. Build for production:
```bash
npm run build
```

4. Preview production build:
```bash
npm run preview
```

## Features

- **Login Page**: User authentication with username/password
- **Sign Up Page**: User registration with form validation
- **Game List Page**: Browse games with search and filter functionality
- **Game Detail Page**: View detailed game information and manage wishlist

## Tech Stack

- React 18
- React Router DOM for navigation
- Vite for build tooling
- Axios for API calls
- CSS3 with modern styling

## Project Structure

```
src/
├── components/          # Reusable React components
├── pages/              # Page components
│   ├── LoginPage.jsx
│   ├── SignUpPage.jsx
│   ├── GameListPage.jsx
│   └── GameDetailPage.jsx
├── styles/             # CSS files
│   ├── index.css       # Global styles
│   ├── App.css         # App-specific styles
│   └── [Page].css      # Page-specific styles
├── App.jsx             # Main App component
└── main.jsx            # Entry point
```

## API Integration

The frontend expects the backend API to be running on `http://localhost:8080`. The Vite configuration includes a proxy to forward `/api` requests to the backend.

### API Endpoints Used

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/games` - Get all games
- `GET /api/games/:id` - Get game details
- `GET /api/wishlist/check/:id` - Check wishlist status
- `POST /api/wishlist` - Add to wishlist
- `DELETE /api/wishlist` - Remove from wishlist

## Development

The development server runs on `http://localhost:3000` and includes:
- Hot module replacement
- API proxy to backend
- Modern ES6+ support
- CSS preprocessing

## Production Build

The production build creates optimized static files in the `dist/` directory that can be served by any static file server.

