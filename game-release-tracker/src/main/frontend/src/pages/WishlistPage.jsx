import React, { useEffect, useState, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationsLink from '../components/NotificationsLink'
import './WishlistPage.css'

const WishlistPage = () => {
  const navigate = useNavigate()
  const { user, isAuthenticated, isLoading, logout, getCsrfToken } = useAuth()

  const [wishlist, setWishlist] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchWishlist = useCallback(async () => {
    if (!user) {
      return
    }

    try {
      setLoading(true)
      const response = await fetch(`/api/users/${user.id}/wishlist/games`, {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error('Failed to load wishlist')
      }

      const data = await response.json()
      setWishlist(Array.isArray(data) ? data : [])
      setError('')
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to load wishlist')
    } finally {
      setLoading(false)
    }
  }, [user?.id])

  useEffect(() => {
    if (isLoading) {
      return
    }
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
      return
    }
    fetchWishlist()
  }, [isLoading, isAuthenticated, fetchWishlist, navigate])

  const handleRemove = async (gameId) => {
    if (!user) {
      return
    }

    try {
      const csrfToken = getCsrfToken()
      const response = await fetch(`/api/users/${user.id}/wishlist`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
        },
        body: JSON.stringify({ gameId: Number(gameId) })
      })

      if (!response.ok) {
        throw new Error('Failed to remove from wishlist')
      }

      setWishlist(prev => prev.filter(item => Number(item.id) !== Number(gameId)))
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to remove from wishlist')
    }
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  if (isLoading || loading) {
    return <div className="wishlist-status">Loading your wishlist…</div>
  }

  if (error) {
    return (
      <div className="wishlist-status wishlist-error">
        <p>{error}</p>
        <button onClick={fetchWishlist}>Try again</button>
      </div>
    )
  }

  return (
    <div className="wishlist-page">
      <header className="wishlist-header">
        <div>
          <h1>My Wishlist</h1>
          <p>Games you&apos;re keeping an eye on.</p>
        </div>
        <div className="wishlist-actions">
          <NotificationsLink className="wishlist-notifications" />
          <Link to="/profile" className="wishlist-profile">Profile</Link>
          <Link to="/games" className="wishlist-link">← Back to games</Link>
          <button onClick={handleLogout} className="wishlist-logout">Logout</button>
        </div>
      </header>

      {wishlist.length === 0 ? (
        <div className="wishlist-empty">
          <p>Your wishlist is empty.</p>
          <Link to="/games" className="wishlist-cta">Browse games</Link>
        </div>
      ) : (
        <ul className="wishlist-list">
          {wishlist.map(item => (
            <li key={item.id} className="wishlist-item">
              <div className="wishlist-cover">
                <img src={item.coverImageUrl || '/placeholder-game.jpg'} alt={item.title} />
              </div>
              <div className="wishlist-info">
                <h2>
                  <Link to={`/games/${item.id}`}>{item.title}</Link>
                </h2>
                <p>Status: <span>{item.status || 'Unknown'}</span></p>
                <p>
                  Release date:{' '}
                  <span>{item.releaseDate ? new Date(item.releaseDate).toLocaleDateString() : 'TBA'}</span>
                </p>
              </div>
              <div className="wishlist-controls">
                <Link to={`/games/${item.id}`} className="wishlist-view">View details</Link>
                <button onClick={() => handleRemove(item.id)} className="wishlist-remove">
                  Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default WishlistPage
