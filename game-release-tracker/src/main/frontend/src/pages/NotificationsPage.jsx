import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './NotificationsPage.css'

const NotificationsPage = () => {
  const { isAuthenticated, isLoading, logout, getCsrfToken } = useAuth()
  const navigate = useNavigate()

  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [markingAll, setMarkingAll] = useState(false)

  const fetchNotifications = useCallback(async () => {
    const response = await fetch('/api/notifications', {
      credentials: 'include'
    })

    if (!response.ok) {
      const message = await response.text()
      throw new Error(message || 'Failed to load notifications')
    }

    const data = await response.json()
    if (!Array.isArray(data)) {
      return []
    }
    return data
  }, [])

  const loadNotifications = useCallback(async () => {
    setError('')
    setLoading(true)
    try {
      const data = await fetchNotifications()
      setNotifications(data)
      window.dispatchEvent(new CustomEvent('notifications:updated'))
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to load notifications.')
    } finally {
      setLoading(false)
    }
  }, [fetchNotifications])

  useEffect(() => {
    if (isLoading) {
      return
    }
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
      return
    }
    loadNotifications()
  }, [isLoading, isAuthenticated, loadNotifications, navigate])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const announceChange = () => {
    window.dispatchEvent(new CustomEvent('notifications:updated'))
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    try {
      const data = await fetchNotifications()
      setNotifications(data)
      setError('')
      announceChange()
    } catch (err) {
      console.error(err)
      setError(err.message || 'Unable to refresh notifications.')
    } finally {
      setRefreshing(false)
    }
  }

  const sendMutation = async (url, method = 'PUT') => {
    const csrfToken = getCsrfToken()
    const response = await fetch(url, {
      method,
      credentials: 'include',
      headers: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : undefined
    })
    if (!response.ok) {
      const message = await response.text()
      throw new Error(message || 'Notification update failed')
    }
  }

  const handleToggleRead = async (id, nextReadState) => {
    setBusyId(id)
    try {
      await sendMutation(`/api/notifications/${id}/${nextReadState ? 'read' : 'unread'}`)
      setNotifications(prev => prev.map(item => (
        item.id === id ? { ...item, isRead: nextReadState } : item
      )))
      announceChange()
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to update notification status.')
    } finally {
      setBusyId(null)
    }
  }

  const handleDelete = async (id) => {
    setBusyId(id)
    try {
      await sendMutation(`/api/notifications/${id}`, 'DELETE')
      setNotifications(prev => prev.filter(item => item.id !== id))
      announceChange()
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to delete notification.')
    } finally {
      setBusyId(null)
    }
  }

  const handleMarkAllRead = async () => {
    setMarkingAll(true)
    try {
      await sendMutation('/api/notifications/read-all')
      setNotifications(prev => prev.map(item => ({ ...item, isRead: true })))
      announceChange()
    } catch (err) {
      console.error(err)
      setError(err.message || 'Failed to mark all as read.')
    } finally {
      setMarkingAll(false)
    }
  }

  const unreadCount = useMemo(() => (
    notifications.reduce((total, item) => total + (item.isRead ? 0 : 1), 0)
  ), [notifications])

  const renderTimestamp = (value) => {
    if (!value) {
      return 'Unknown time'
    }
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      return 'Unknown time'
    }
    return date.toLocaleString()
  }

  if (isLoading || loading) {
    return (
      <div className="notifications-status">
        Loading notifications…
      </div>
    )
  }

  return (
    <div className="notifications-page">
      <header className="notifications-header">
        <div>
          <h1>Notifications</h1>
          <p>
            {unreadCount > 0
              ? `You have ${unreadCount} unread notification${unreadCount > 1 ? 's' : ''}.`
              : 'All caught up – no unread notifications.'}
          </p>
        </div>
        <div className="notifications-controls">
          <button
            type="button"
            className="notifications-btn"
            onClick={handleRefresh}
            disabled={refreshing}
          >
            {refreshing ? 'Refreshing…' : 'Refresh'}
          </button>
          <button
            type="button"
            className="notifications-btn"
            onClick={handleMarkAllRead}
            disabled={markingAll || unreadCount === 0}
          >
            {markingAll ? 'Marking…' : 'Mark all read'}
          </button>
          <Link to="/profile" className="notifications-profile">
            Account settings
          </Link>
          <Link to="/games" className="notifications-link">
            ← Back to games
          </Link>
          <button type="button" className="notifications-btn secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      {error && (
        <div className="notifications-error">
          <p>{error}</p>
          <button type="button" onClick={handleRefresh}>Try again</button>
        </div>
      )}

      {notifications.length === 0 ? (
        <div className="notifications-empty">
          <p>No notifications yet. We&apos;ll let you know when games you watch change status.</p>
          <Link to="/games" className="notifications-empty__cta">Browse games</Link>
        </div>
      ) : (
        <ul className="notifications-list">
          {notifications.map(notification => (
            <li
              key={notification.id}
              className={`notification-card ${notification.isRead ? 'is-read' : 'is-unread'}`}
            >
              <div className="notification-card__body">
                <div className="notification-card__meta">
                  <span className={`notification-card__status ${notification.isRead ? 'read' : 'unread'}`}>
                    {notification.isRead ? 'Read' : 'Unread'}
                  </span>
                  <time>{renderTimestamp(notification.createdAt)}</time>
                </div>
                <h2>{notification.gameTitle || 'Game update'}</h2>
                <p className="notification-card__message">
                  {notification.message || 'A game on your wishlist has been updated.'}
                </p>
                <div className="notification-card__actions">
                  {notification.gameId && (
                    <Link
                      to={`/games/${notification.gameId}`}
                      className="notification-card__view"
                    >
                      View game
                    </Link>
                  )}
                  <button
                    type="button"
                    className="notification-card__button"
                    onClick={() => handleToggleRead(notification.id, !notification.isRead)}
                    disabled={busyId === notification.id}
                  >
                    {notification.isRead ? 'Mark as unread' : 'Mark as read'}
                  </button>
                  <button
                    type="button"
                    className="notification-card__button danger"
                    onClick={() => handleDelete(notification.id)}
                    disabled={busyId === notification.id}
                  >
                    Delete
                  </button>
                </div>
              </div>
              <div className="notification-card__art">
                <div className="notification-card__glow" />
                <img
                  src={notification.gameCoverImageUrl || '/placeholder-game.jpg'}
                  alt={notification.gameTitle || 'Game cover'}
                  loading="lazy"
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default NotificationsPage
