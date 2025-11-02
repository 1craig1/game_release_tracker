import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const NotificationsLink = ({ className = '' }) => {
  const { isAuthenticated, isLoading } = useAuth()
  const [unreadCount, setUnreadCount] = useState(null)
  const [isFetching, setIsFetching] = useState(false)

  const fetchCount = useCallback(async () => {
    if (!isAuthenticated) {
      setUnreadCount(null)
      return
    }
    setIsFetching(true)
    try {
      const response = await fetch('/api/notifications/unread/count', {
        credentials: 'include'
      })
      if (!response.ok) {
        throw new Error('Failed to load notification count')
      }
      const data = await response.json()
      const value = typeof data?.count === 'number' ? data.count : 0
      setUnreadCount(value)
    } catch (error) {
      console.error('Unable to fetch notification count', error)
      setUnreadCount(null)
    } finally {
      setIsFetching(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    if (isLoading || !isAuthenticated) {
      return
    }

    let active = true
    const load = async () => {
      if (!active) return
      await fetchCount()
    }
    load()

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        fetchCount()
      }
    }

    const handleNotificationsUpdated = () => {
      fetchCount()
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('notifications:updated', handleNotificationsUpdated)

    return () => {
      active = false
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('notifications:updated', handleNotificationsUpdated)
    }
  }, [isAuthenticated, isLoading, fetchCount])

  if (!isAuthenticated) {
    return null
  }

  const badgeValue = typeof unreadCount === 'number' && unreadCount > 99 ? '99+' : unreadCount

  return (
    <Link to="/notifications" className={`notification-link ${className}`}>
      Notifications
      {typeof unreadCount === 'number' && unreadCount > 0 && (
        <span className="notification-link__badge">
          {badgeValue}
        </span>
      )}
      {isFetching && unreadCount === null && <span className="notification-link__spinner" aria-hidden="true" />}
    </Link>
  )
}

export default NotificationsLink
