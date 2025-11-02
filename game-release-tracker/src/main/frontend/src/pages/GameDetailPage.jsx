import React, { useCallback, useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './GameDetailPage.css'

const GameDetailPage = () => {
    const { id } = useParams()
    const [game, setGame] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [inWishlist, setInWishlist] = useState(false)
    const [wishlistBusy, setWishlistBusy] = useState(false)
    const [wishlistError, setWishlistError] = useState('')
    const [countdown, setCountdown] = useState('')
    const navigate = useNavigate()
    const { user, isAuthenticated, isLoading, logout, getCsrfToken } = useAuth()
    const fetchGame = useCallback(async () => {
        try {
            const response = await fetch(`/api/games/${id}`, {
                credentials: 'include'
            })
            if (response.ok) {
                const data = await response.json()
                setGame(data)
            } else {
                setError('Failed to fetch game details')
            }
        } catch (err) {
            console.error(err)
            setError('Failed to fetch game details')
        } finally {
            setLoading(false)
        }
    }, [id])

    const fetchWishlistStatus = useCallback(async () => {
        if (!user) {
            return
        }
        try {
            const response = await fetch(`/api/users/${user.id}/wishlist/games/${id}/exists`, {
                credentials: 'include'
            })
            if (response.ok) {
                const data = await response.json()
                setInWishlist(Boolean(data?.exists))
            }
        } catch (err) {
            console.error('Unable to determine wishlist status', err)
        }
    }, [user?.id, id])

    useEffect(() => {
        if (!game?.releaseDate) return

        const releaseDate = new Date(game.releaseDate)

        const updateCountdown = () => {
            const now = new Date()
            const diff = releaseDate - now

            if (diff <= 0) {
                setCountdown('Released!')
                return
            }

            const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
            setCountdown(`${days} day${days !== 1 ? 's' : ''} left`)
        }

        updateCountdown()
        const timer = setInterval(updateCountdown, 3600000) // every hour
        return () => clearInterval(timer)
    }, [game?.releaseDate])

    useEffect(() => {
        if (isLoading) {
            return
        }
        if (!isAuthenticated) {
            navigate('/login', { replace: true })
            return
        }
        fetchGame()
        fetchWishlistStatus()
    }, [id, isAuthenticated, isLoading, navigate, fetchGame, fetchWishlistStatus])

    const handleWishlistToggle = async () => {
        if (!user || wishlistBusy) {
            return
        }
        setWishlistBusy(true)
        setWishlistError('')
        const csrfToken = getCsrfToken()
        const payload = { gameId: Number(id) }

        try {
            const response = await fetch(`/api/users/${user.id}/wishlist`, {
                method: inWishlist ? 'DELETE' : 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
                },
                body: JSON.stringify(payload)
            })

            if (!response.ok) {
                throw new Error('Wishlist request failed')
            }

            setInWishlist(!inWishlist)
        } catch (err) {
            console.error(err)
            setWishlistError('Unable to update wishlist. Please try again.')
        } finally {
            setWishlistBusy(false)
        }
    }

    const handleLogout = async () => {
        await logout()
        navigate('/login', { replace: true })
    }

    if (isLoading || loading) return <div className="gd-loading">Loading…</div>
    if (error) return <div className="gd-error">{error}</div>
    if (!game) return <div className="gd-empty">No game found.</div>

    return (
        <div className="gd-page">
            <header className="gd-header">
                <Link to="/games" className="gd-back">← Back to Games</Link>
                <div className="gd-controls">
                    <Link to="/profile" className="gd-profile">Profile</Link>
                    <button
                        className="gd-logout"
                        onClick={handleLogout}
                    >
                        Logout
                    </button>
                </div>
            </header>

            <div className="gd-card">
                <div className="gd-media">
                    <img src={game.coverImageUrl || '/placeholder-game.jpg'} alt={game.title} />
                </div>

                <div className="gd-body">
                    <h1 className="gd-title">{game.title}</h1>

                    <div className="gd-meta">
                        <div><strong>Release Date:</strong> {game.releaseDate ? new Date(game.releaseDate).toLocaleDateString() : 'TBA'}</div>
                        <div><strong>Status:</strong> {game.status || 'Unknown'}</div>
                        <div><strong>Developer:</strong> {game.developer || 'Unknown'}</div>
                        <div><strong>Publisher:</strong> {game.publisher || 'Unknown'}</div>
                    </div>

                    {/* Countdown Timer */}
                    {game.releaseDate && countdown && (
                        <div className="gd-countdown">
                            <strong>Countdown:</strong> <span>{countdown}</span>
                        </div>
                    )}

                    <p className="gd-desc">{game.description}</p>

                    <div className="gd-section">
                        <h3>Genres</h3>
                        <div className="gd-tags">
                            {(game.genres || []).map(genre => (
                                <span key={genre.id || genre.name} className="gd-pill">{genre.name}</span>
                            ))}
                        </div>
                    </div>

                    <div className="gd-section">
                        <h3>Platforms</h3>
                        <div className="gd-tags">
                            {(game.platforms || []).map(p => (
                                <span key={p.id || p.name} className="gd-chip">{p.name}</span>
                            ))}
                        </div>
                    </div>

                    <div className="gd-section">
                        <h3>Where to Buy / Preorder</h3>
                        {(!game.preorderLinks || game.preorderLinks.length === 0) ? (
                            <div className="gd-empty">No store links available.</div>
                        ) : (
                            <div className="gd-preorder-buttons">
                                {game.preorderLinks.map(link => (
                                    <a
                                        key={link.id || link.url}
                                        href={link.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="gd-preorder-btn"
                                    >
                                        {link.storeName}
                                    </a>
                                ))}
                            </div>
                        )}
                    </div>

                    <button
                        className="gd-btn"
                        onClick={handleWishlistToggle}
                        disabled={wishlistBusy}
                    >
                        {inWishlist ? '✓ In Wishlist' : '♡ Add to Wishlist'}
                    </button>
                    {wishlistError && <div className="gd-error">{wishlistError}</div>}
                </div>
            </div>
        </div>
    )
}

export default GameDetailPage