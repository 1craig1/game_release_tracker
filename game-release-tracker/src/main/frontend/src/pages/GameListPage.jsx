import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import NotificationsLink from '../components/NotificationsLink'
import './GameListPage.css'

const ITEMS_PER_PAGE = 16

const GameListPage = () => {
  const [games, setGames] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  //Fitlers
  const [searchTerm, setSearchTerm] = useState('')
  const [filterGenre, setFilterGenre] = useState(new Set())
  const [filterPlatform, setFilterPlatform] = useState(new Set())
  const [filterStatus, setFilterStatus] = useState(new Set())
  const [releaseDateFrom, setReleaseDateFrom] = useState('')

  const [genreSearch, setGenreSearch] = useState('')
  const [platformSearch, setPlatformSearch] = useState('')

  const [wishlistIds, setWishlistIds] = useState(() => new Set())
  const [wishlistError, setWishlistError] = useState('')
  const [wishlistPendingId, setWishlistPendingId] = useState(null)

  const [openDropdown, setOpenDropdown] = useState(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageInput, setPageInput] = useState('1')

  const navigate = useNavigate()
  const { user, isAuthenticated, isLoading, logout, getCsrfToken } = useAuth()

  const toggleDropdown = (name) => {setOpenDropdown(openDropdown === name ? null : name)}
  const toggleStatus = (status) => {
    setFilterStatus(prev => {
      const next = new Set(prev)
      next.has(status) ? next.delete(status) : next.add(status)
      return next
    })
  }

  const loadGames = useCallback(async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/games', {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error('Failed to fetch games')
      }

      const data = await response.json()
      const raw = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : [])
      const normalized = raw.map(g => ({
        ...g,
        id: g.id,
        title: g.title ?? 'Untitled',
        description: g.description ?? '',
        coverImageUrl: g.coverImageUrl ?? '/placeholder-game.jpg',
        releaseDate: g.releaseDate ?? null,
        genres: Array.isArray(g.genres) ? g.genres : [],
        platforms: Array.isArray(g.platforms) ? g.platforms : [],
        mature: g.mature ?? false
      }))
      setGames(normalized)
      setError(null)
    } catch (err) {
      console.error('Error fetching games:', err)
      setError(err.message || 'Failed to fetch games')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadWishlist = useCallback(async () => {
    if (!user) {
      setWishlistIds(new Set())
      return
    }

    try {
      const response = await fetch(`/api/users/${user.id}/wishlist/games`, {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error('Failed to load wishlist')
      }

      const data = await response.json()
      const ids = Array.isArray(data) ? data.map(item => Number(item.id)) : []
      setWishlistIds(new Set(ids))
      setWishlistError('')
    } catch (err) {
      console.error(err)
      setWishlistError('Failed to sync wishlist status.')
    }
  }, [user?.id])

  useEffect(() => {
    setCurrentPage(1)
    setPageInput('1')
  }, [searchTerm, filterGenre, filterPlatform, filterStatus, releaseDateFrom])

  useEffect(() => {
    if (isLoading) {
      return
    }
    if (!isAuthenticated) {
      navigate('/login', { replace: true })
      return
    }
    loadGames()
    loadWishlist()
  }, [isAuthenticated, isLoading, navigate, loadGames, loadWishlist])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const handleToggleWishlist = async (gameId, currentlySaved) => {
    if (!user) {
      return
    }

    setWishlistError('')
    setWishlistPendingId(gameId)

    try {
      const csrfToken = getCsrfToken()
      const response = await fetch(`/api/users/${user.id}/wishlist`, {
        method: currentlySaved ? 'DELETE' : 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
        },
        body: JSON.stringify({ gameId })
      })

      if (!response.ok) {
        throw new Error('Wishlist update failed')
      }

      setWishlistIds(prev => {
        const next = new Set(prev)
        if (currentlySaved) {
          next.delete(gameId)
        } else {
          next.add(gameId)
        }
        return next
      })
    } catch (err) {
      console.error(err)
      setWishlistError('Could not update wishlist. Please try again.')
    } finally {
      setWishlistPendingId(null)
    }
  }
  const isSafe = (game) => !game.mature;
  // const [filterStatus, setFilterStatus] = useState(new Set());
  // const [releaseDateFrom, setReleaseDateFrom] = useState('');
  // const toggleStatus = (status) => {
  //   const next = new Set(filterStatus);
  //   next.has(status) ? next.delete(status) : next.add(status);
  //   setFilterStatus(next);
  // };

  const uniqueGenres = [...new Set(games.flatMap(game => (game.genres ?? []).map(genre => genre.name)))]
  const uniquePlatforms = [...new Set(games.flatMap(game => (game.platforms ?? []).map(platform => platform.name)))]

  const filteredGames = games.filter(game => {
    const matchesSearch = (game.title || '').toLowerCase().includes(searchTerm.toLowerCase())
    const matchesGenre = filterGenre.size === 0 || (game.genres ?? []).some(g => filterGenre.has(g.name))
    const matchesPlatform = filterPlatform.size === 0 || (game.platforms ?? []).some(p => filterPlatform.has(p.name))
    const matchesStatus = filterStatus.size === 0 || filterStatus.has(game.status);
    const matchesDate = (!releaseDateFrom || new Date(game.releaseDate) >= new Date(releaseDateFrom));
    const safeForWork = isSafe(game)
    return matchesSearch && matchesGenre && matchesPlatform && matchesStatus && matchesDate && safeForWork;
  })

  useEffect(() => {
    const total = Math.max(1, Math.ceil(filteredGames.length / ITEMS_PER_PAGE))
    if (currentPage > total) {
      setCurrentPage(total)
      setPageInput(String(total))
    }
  }, [filteredGames.length, currentPage])

  const hasResults = filteredGames.length > 0
  const totalPages = hasResults ? Math.ceil(filteredGames.length / ITEMS_PER_PAGE) : 0
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE
  const paginatedGames = hasResults
    ? filteredGames.slice(startIndex, startIndex + ITEMS_PER_PAGE)
    : []

  const pageNumbers = useMemo(() => {
    if (totalPages === 0) {
      return []
    }
    if (totalPages === 1) {
      return [1]
    }
    const maxButtons = 5
    let start = Math.max(1, currentPage - 2)
    let end = Math.min(totalPages, start + maxButtons - 1)
    if (end - start < maxButtons - 1) {
      start = Math.max(1, end - maxButtons + 1)
    }
    const pages = []
    for (let i = start; i <= end; i += 1) {
      pages.push(i)
    }
    return pages
  }, [currentPage, totalPages])

  const handlePageChange = (page) => {
    if (!hasResults) {
      setCurrentPage(1)
      setPageInput('1')
      return
    }
    const total = Math.max(1, Math.ceil(filteredGames.length / ITEMS_PER_PAGE))
    const next = Math.min(Math.max(page, 1), total)
    setCurrentPage(next)
    setPageInput(String(next))
  }

  const handlePageSubmit = (event) => {
    event.preventDefault()
    const parsed = parseInt(pageInput, 10)
    if (Number.isNaN(parsed)) {
      setPageInput(String(currentPage))
      return
    }
    handlePageChange(parsed)
  }

  const toggleGenre = genre =>{
    const next = new Set(filterGenre)
    next.has(genre) ? next.delete(genre) : next.add(genre)
    setFilterGenre(next)
  }

  const togglePlatform = platform => {
    const next = new Set(filterPlatform)
    next.has(platform) ? next.delete(platform) : next.add(platform)
    setFilterPlatform(next)
  }

  const filteredGenreOptions = uniqueGenres.filter(g =>
    g.toLowerCase().includes(genreSearch.toLowerCase())
  )
  const filteredPlatformOptions = uniquePlatforms.filter(p =>
    p.toLowerCase().includes(platformSearch.toLowerCase())
  )

  if (isLoading || loading) {
    return <div className="loading">Loading games...</div>
  }

  if (error) {
    return <div className="error">Error: {error}</div>
  }

  return (
    <div className="dl-page">
    <header className="dl-topbar">
      {/* Left Section (Unchanged) */}
      <div className="dl-brand">
        <span className="dot" />
        <div className="dl-brand-text">
          <h1>Coming Soon</h1>
          <small>Hand-picked upcoming releases across all platforms.</small>
        </div>
      </div>

      {/* New Wrapper for Filters and User Buttons */}
      <div className="dl-actions-wrapper">

        {/* Row 1: Filters */}
        <div className="dl-filter-actions">
          <input
            type="text"
            className="dl-search"
            placeholder="Search games..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />

          <div className="dropdown">
            <button className="dl-select" onClick={() => toggleDropdown('genre')}>Genres ▾</button>
            {openDropdown === 'genre' && (
              <div className="dropdown-content">
                <input
                  type="text"
                  className="dropdown-search"
                  placeholder="Search genres..."
                  value={genreSearch}
                  onChange={(e) => setGenreSearch(e.target.value)}
                />
                {filteredGenreOptions.map(ge => (
                  <label key={ge}>
                    <input
                      type="checkbox"
                      checked={filterGenre.has(ge)}
                      onChange={() => toggleGenre(ge)}
                    />
                    {ge}
                  </label>
                ))}
              </div>
            )}
          </div>

          <div className="dropdown">
            <button className="dl-select" onClick={() => toggleDropdown('platform')}>Platforms ▾</button>
            {openDropdown === 'platform' && (
              <div className="dropdown-content">
                <input
                  type="text"
                  className="dropdown-search"
                  placeholder="Search platforms..."
                  value={platformSearch}
                  onChange={(e) => setPlatformSearch(e.target.value)}
                />
                {filteredPlatformOptions.map(p => (
                  <label key={p}>
                    <input
                      type="checkbox"
                      checked={filterPlatform.has(p)}
                      onChange={() => togglePlatform(p)}
                    />
                    {p}
                  </label>
                ))}
              </div>
            )}
          </div>

          <div className="dropdown">
            <button className="dl-select" onClick={() => toggleDropdown('status')}>Status ▾</button>
            {openDropdown === 'status' && (
              <div className="dropdown-content">
                {['UPCOMING', 'RELEASED'].map(status => (
                  <label key={status}>
                    <input
                      type="checkbox"
                      checked={filterStatus.has(status)}
                      onChange={() => toggleStatus(status)}
                    />
                    {status}
                  </label>
                ))}
              </div>
            )}
          </div>

          <div className="dropdown">
            <button className="dl-select" onClick={() => toggleDropdown('releaseDate')}>Release Date ▾</button>
            {openDropdown === 'releaseDate' && (
              <div className="dropdown-content">
                <label>
                  From: <input
                    type="date"
                    value={releaseDateFrom}
                    onChange={(e) => setReleaseDateFrom(e.target.value)}
                  />
                </label>
              </div>
            )}
          </div>
        </div>

        {/* Row 2: User Buttons (with new classes and text) */}
        <div className="dl-user-actions">
          <NotificationsLink className="dl-btn dl-btn-notifications" />
          <Link to="/profile" className="dl-btn dl-btn-profile">Profile</Link>
          <Link to="/wishlist" className="dl-btn dl-btn-wishlist">Wishlist</Link>
          {user?.role === 'ROLE_ADMIN' && (
            <button className="dl-btn dl-btn-admin" onClick={() => navigate('/admin')}>Admin Page</button>
          )}
          <button className="dl-btn dl-btn-logout" onClick={handleLogout}>Logout</button>
        </div>
      </div>
    </header>


      {wishlistError && (
        <div className="dl-wishlist-error">{wishlistError}</div>
      )}

      <main className="dl-grid">
        {paginatedGames.map(game => {
          const firstPlatform = (game.platforms ?? [])[0]?.name
          const gameId = Number(game.id)
          const isSaved = wishlistIds.has(gameId)

          return (
            <article key={game.id} className="dl-card">
              <div className="dl-media">
                <img src={game.coverImageUrl || '/placeholder-game.jpg'} alt={game.title} />
                {firstPlatform && <span className="dl-chip">On {firstPlatform}</span>}
              </div>

              <div className="dl-body">
                <h3 className="dl-title">
                  <Link to={`/games/${game.id}`}>{game.title}</Link>
                </h3>

                <p className="dl-desc">{game.description}</p>

                <div className="dl-meta">
                  <div className="dl-author">
                    <span className="dl-curator-icon" aria-hidden="true">🎮</span>
                    <span>curated</span>
                    <span className="dot-sep">•</span>
                    <time>
                      {game.releaseDate ? new Date(game.releaseDate).toLocaleDateString() : 'TBA'}
                    </time>
                  </div>

                  <div className="dl-tags">
                    {(game.genres ?? []).slice(0, 2).map(g => (
                      <span className="dl-pill" key={g.id || g.name}>{g.name}</span>
                    ))}
                  </div>
                </div>

                <div className="dl-footer">
                  <Link to={`/games/${game.id}`} className="dl-btn">View details</Link>
                  <button
                    className={`dl-btn-secondary${isSaved ? ' active' : ''}`}
                    onClick={() => handleToggleWishlist(gameId, isSaved)}
                    disabled={wishlistPendingId === gameId}
                  >
                    {isSaved ? 'Remove' : 'Add to wishlist'}
                  </button>
                </div>
              </div>
            </article>
          )
        })}
      </main>

      {filteredGames.length === 0 && (
        <div className="dl-empty">No games found.</div>
      )}

      {hasResults && totalPages > 1 && (
        <nav className="dl-pagination" aria-label="Game list pagination">
          <span className="dl-pagination__summary">
            Page {currentPage} of {totalPages}
          </span>

          <div className="dl-pagination__controls">
            <button
              type="button"
              className="dl-page-btn"
              onClick={() => handlePageChange(1)}
              disabled={currentPage === 1}
            >
              «
            </button>
            <button
              type="button"
              className="dl-page-btn"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
            >
              ‹
            </button>

            {pageNumbers[0] > 1 && (
              <>
                <button
                  type="button"
                  className={`dl-page-btn${currentPage === 1 ? ' active' : ''}`}
                  onClick={() => handlePageChange(1)}
                >
                  1
                </button>
                {pageNumbers[0] > 2 && <span className="dl-pagination__ellipsis">…</span>}
              </>
            )}

            {pageNumbers.map(number => (
              <button
                type="button"
                key={number}
                className={`dl-page-btn${number === currentPage ? ' active' : ''}`}
                onClick={() => handlePageChange(number)}
              >
                {number}
              </button>
            ))}

            {pageNumbers.length > 0 && pageNumbers[pageNumbers.length - 1] < totalPages && (
              <>
                {pageNumbers[pageNumbers.length - 1] < totalPages - 1 && (
                  <span className="dl-pagination__ellipsis">…</span>
                )}
                <button
                  type="button"
                  className={`dl-page-btn${currentPage === totalPages ? ' active' : ''}`}
                  onClick={() => handlePageChange(totalPages)}
                >
                  {totalPages}
                </button>
              </>
            )}

            <button
              type="button"
              className="dl-page-btn"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
            >
              ›
            </button>
            <button
              type="button"
              className="dl-page-btn"
              onClick={() => handlePageChange(totalPages)}
              disabled={currentPage === totalPages}
            >
              »
            </button>
          </div>

          <form className="dl-pagination__jump" onSubmit={handlePageSubmit}>
            <label htmlFor="dl-pagination-input">Go to</label>
            <input
              id="dl-pagination-input"
              type="number"
              min="1"
              max={totalPages}
              value={pageInput}
              onChange={(e) => setPageInput(e.target.value)}
            />
            <button type="submit" className="dl-page-btn">
              Go
            </button>
          </form>
        </nav>
      )}
    </div>
  )
}

export default GameListPage