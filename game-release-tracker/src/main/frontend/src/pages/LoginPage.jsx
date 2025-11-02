
import React, { useEffect, useState } from 'react'

import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './LoginPage.css'

const LoginPage = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    remember: false
  })

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  const { login, isAuthenticated, isLoading: authLoading } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      navigate('/games')
    }
  }, [isAuthenticated, authLoading, navigate])


  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    setIsSubmitting(true)

    try {
      await login(formData.username, formData.password, formData.remember)

      navigate('/games')
    } catch (err) {
      setError(err.message || 'Login failed. Please try again.')
    } finally {

      setIsSubmitting(false)

    }
  }

  return (
    <div className="login-container">

      <div className="login-box">
        <h2>Game Tracker Login</h2>
        {error && <div className="login-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Enter your username"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Enter your password"
              required
            />
          </div>

          <div className="checkbox">
            <label htmlFor="remember">
              <input
                id="remember"
                type="checkbox"
                name="remember"
                checked={formData.remember}
                onChange={handleChange}
              />
              Remember me
            </label>
          </div>

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Logging in…' : 'Login'}
          </button>
        </form>

        <p className="signup-link">
          Don't have an account? <Link to="/signup">Register</Link>
        </p>
      </div>
    </div>
  )
}


export default LoginPage
