import React from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './contexts/AuthContext'
import LoginPage from './pages/LoginPage'
import SignUpPage from './pages/SignUpPage'
import GameListPage from './pages/GameListPage'
import GameDetailPage from './pages/GameDetailPage'
import WishlistPage from './pages/WishlistPage'
import NotificationsPage from './pages/NotificationsPage'
import ProfilePage from './pages/ProfilePage'
import AdminPage from './pages/AdminPage'
import './styles/App.css'

function App() {
  const { isAuthenticated, user, isLoading } = useAuth()
  const ProtectedRoute = ({ element, role }) => {
    if (isLoading) return <div>Loading…</div>
    if (!isAuthenticated) return <Navigate to="/login" replace />
    if (role && user?.role !== role) return <Navigate to="/games" replace />
    return element
  }

  if (isLoading) return <div>Loading…</div>

  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<LoginPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/games" element={<GameListPage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
          <Route path="/wishlist" element={<WishlistPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/admin" element={<ProtectedRoute element={<AdminPage />} role="ROLE_ADMIN" />} />
        </Routes>
      </div>
    </Router>
  )
}

export default App
