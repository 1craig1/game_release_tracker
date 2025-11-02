import React, { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationsLink from '../components/NotificationsLink'
import './ProfilePage.css'

const emptyProfile = {
  id: null,
  username: '',
  email: '',
  enableNotifications: true
}

const emptyPasswordForm = {
  current: '',
  next: '',
  confirm: ''
}

const ProfilePage = () => {
  const navigate = useNavigate()
  const { isAuthenticated, isLoading, logout, getCsrfToken, updateUser } = useAuth()

  const [profile, setProfile] = useState(emptyProfile)
  const [profileLoading, setProfileLoading] = useState(true)
  const [profileFetchError, setProfileFetchError] = useState('')
  const [profileSubmitError, setProfileSubmitError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')
  const [profileSaving, setProfileSaving] = useState(false)

  const [passwordForm, setPasswordForm] = useState(emptyPasswordForm)
  const [passwordError, setPasswordError] = useState('')
  const [passwordSuccess, setPasswordSuccess] = useState('')
  const [passwordSaving, setPasswordSaving] = useState(false)

  const loadProfile = useCallback(async () => {
    setProfileFetchError('')
    setProfileLoading(true)
    try {
      const response = await fetch('/api/users/me', {
        credentials: 'include'
      })

      if (response.status === 401) {
        await logout()
        navigate('/login', { replace: true })
        return
      }

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Failed to load profile.')
      }

      const data = await response.json()
      setProfile({
        id: data.id,
        username: data.username ?? '',
        email: data.email ?? '',
        enableNotifications: Boolean(data.enableNotifications)
      })
    } catch (error) {
      console.error('Failed to load profile', error)
      setProfileFetchError(error.message || 'Failed to load profile.')
    } finally {
      setProfileLoading(false)
    }
  }, [logout, navigate])

  useEffect(() => {
    if (isLoading) {
      return
    }
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
      return
    }
    loadProfile()
  }, [isAuthenticated, isLoading, loadProfile, navigate])

  const handleLogout = useCallback(async () => {
    await logout()
    navigate('/login', { replace: true })
  }, [logout, navigate])

  const handleProfileChange = (event) => {
    const { name, value, type, checked } = event.target
    setProfile(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleProfileSubmit = async (event) => {
    event.preventDefault()
    setProfileSubmitError('')
    setProfileSuccess('')

    const payload = {
      ...profile,
      username: profile.username.trim(),
      email: profile.email.trim()
    }

    if (!payload.username) {
      setProfileSubmitError('Username is required.')
      return
    }
    if (!payload.email) {
      setProfileSubmitError('Email is required.')
      return
    }

    setProfileSaving(true)
    try {
      const csrfToken = getCsrfToken()
      const response = await fetch('/api/users/me', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
        },
        body: JSON.stringify(payload)
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Failed to update profile.')
      }

      const updatedUser = await response.json()
      setProfile({
        id: updatedUser.id,
        username: updatedUser.username ?? '',
        email: updatedUser.email ?? '',
        enableNotifications: Boolean(updatedUser.enableNotifications)
      })
      updateUser(updatedUser)
      setProfileSuccess('Profile updated successfully.')
    } catch (error) {
      console.error('Profile update failed', error)
      setProfileSubmitError(error.message || 'Failed to update profile.')
    } finally {
      setProfileSaving(false)
    }
  }

  const handlePasswordChange = (event) => {
    const { name, value } = event.target
    setPasswordForm(prev => ({
      ...prev,
      [name]: value
    }))
  }

  const handlePasswordSubmit = async (event) => {
    event.preventDefault()
    setPasswordError('')
    setPasswordSuccess('')

    if (!passwordForm.current) {
      setPasswordError('Current password is required.')
      return
    }
    if (passwordForm.next.length < 8) {
      setPasswordError('New password must be at least 8 characters long.')
      return
    }
    if (passwordForm.next !== passwordForm.confirm) {
      setPasswordError('New passwords do not match.')
      return
    }

    setPasswordSaving(true)
    try {
      const csrfToken = getCsrfToken()
      const response = await fetch('/api/users/me/password', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
        },
        body: JSON.stringify({
          oldPassword: passwordForm.current,
          newPassword: passwordForm.next,
          confirmPassword: passwordForm.confirm
        })
      })

      if (!response.ok) {
        let errorMessage = 'Failed to update password.';
          try {
            // Try to parse the clean JSON from your ControllerAdvice
            const errorData = await response.json();
            errorMessage = errorData.message || 'Failed to update password.';
          } catch (e) {
            // Fallback if it's not JSON
            errorMessage = (await response.text()) || 'Failed to update password.';
          }
          throw new Error(errorMessage);
      }

      setPasswordForm(emptyPasswordForm)
      setPasswordSuccess('Password updated successfully.')
    } catch (error) {
      console.error('Password update failed', error)
      setPasswordError(error.message || 'Failed to update password.')
    } finally {
      setPasswordSaving(false)
    }
  }

  return (
    <div className="profile-page">
      <header className="profile-header">
        <div>
          <h1>Account settings</h1>
          <p>Manage your profile details and change your password.</p>
        </div>
        <div className="profile-header__actions">
          <NotificationsLink className="profile-notifications" />
          <Link to="/games" className="profile-link">← Back to games</Link>
          <button type="button" className="profile-logout" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <div className="profile-grid">
        <section className="profile-card">
          <div className="profile-card__head">
            <h2>Profile</h2>
            <p>Update your username, email, and notification preferences.</p>
          </div>

          {profileLoading ? (
            <div className="profile-status">Loading profile…</div>
          ) : profileFetchError ? (
            <div className="profile-status profile-status--error">
              <p>{profileFetchError}</p>
              <button type="button" onClick={loadProfile}>
                Try again
              </button>
            </div>
          ) : (
            <form className="profile-form" onSubmit={handleProfileSubmit}>
              <label>
                <span>Username</span>
                <input
                  type="text"
                  name="username"
                  value={profile.username}
                  onChange={handleProfileChange}
                  maxLength={50}
                  required
                />
              </label>
              <label>
                <span>Email</span>
                <input
                  type="email"
                  name="email"
                  value={profile.email}
                  onChange={handleProfileChange}
                  required
                />
              </label>
              <label className="profile-toggle">
                <input
                  type="checkbox"
                  name="enableNotifications"
                  checked={profile.enableNotifications}
                  onChange={handleProfileChange}
                />
                <div>
                  <strong>Enable Notifications</strong>
                  <small>Receive updates when wishlist games change status.</small>
                </div>
              </label>

              {profileSubmitError && <p className="profile-message profile-message--error">{profileSubmitError}</p>}
              {profileSuccess && <p className="profile-message profile-message--success">{profileSuccess}</p>}

              <div className="profile-form__actions">
                <button type="submit" className="profile-save" disabled={profileSaving}>
                  {profileSaving ? 'Saving…' : 'Save changes'}
                </button>
              </div>
            </form>
          )}
        </section>

        <section className="profile-card">
          <div className="profile-card__head">
            <h2>Change password</h2>
            <p>Use a strong password with at least 8 characters.</p>
          </div>

          <form className="profile-form" onSubmit={handlePasswordSubmit}>
            <label>
              <span>Current password</span>
              <input
                type="password"
                name="current"
                value={passwordForm.current}
                onChange={handlePasswordChange}
                required
              />
            </label>
            <label>
              <span>New password</span>
              <input
                type="password"
                name="next"
                value={passwordForm.next}
                onChange={handlePasswordChange}
                required
                minLength={8}
              />
            </label>
            <label>
              <span>Confirm new password</span>
              <input
                type="password"
                name="confirm"
                value={passwordForm.confirm}
                onChange={handlePasswordChange}
                required
                minLength={8}
              />
            </label>

            {passwordError && <p className="profile-message profile-message--error">{passwordError}</p>}
            {passwordSuccess && <p className="profile-message profile-message--success">{passwordSuccess}</p>}

            <div className="profile-form__actions">
              <button type="submit" className="profile-save" disabled={passwordSaving}>
                {passwordSaving ? 'Updating…' : 'Update password'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}

export default ProfilePage
