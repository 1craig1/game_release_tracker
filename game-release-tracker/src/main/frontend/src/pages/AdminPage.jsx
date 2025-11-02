import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import './AdminPage.css';

const ROLE_TYPES = ['ROLE_USER', 'ROLE_ADMIN'];

const AdminPage = () => {
  const { user, isAuthenticated, isLoading, logout, getCsrfToken } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const [searchId, setSearchId] = useState('');
  const [searchUsername, setSearchUsername] = useState('');

  // Fetch all users
  const fetchUsers = useCallback(async () => {
    setError(null);
    try {
      const response = await fetch('/api/users/admin', { credentials: 'include' });
      if (!response.ok) throw new Error('Failed to fetch users');
      const data = await response.json();
      setUsers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated || user.role !== 'ROLE_ADMIN') {
      navigate('/games', { replace: true });
      return;
    }
    fetchUsers();
  }, [isAuthenticated, isLoading, user, navigate, fetchUsers]);

  // Search by ID
  const handleSearchById = async () => {
    if (!searchId) return;
    setBusy(true);
    setError(null);
    try {
      const response = await fetch(`/api/users/admin/id/${searchId}`, { credentials: 'include' });
      if (!response.ok) throw new Error('User not found');
      const data = await response.json();
      setUsers([data]);
    } catch (err) {
      setError(err.message);
      setUsers([]);
    } finally {
      setBusy(false);
    }
  };

  // Search by username
  const handleSearchByUsername = async () => {
    if (!searchUsername) return;
    setBusy(true);
    setError(null);
    try {
      const response = await fetch(`/api/users/admin/username/${searchUsername}`, { credentials: 'include' });
      if (!response.ok) throw new Error('User not found');
      const data = await response.json();
      setUsers([data]);
    } catch (err) {
      setError(err.message);
      setUsers([]);
    } finally {
      setBusy(false);
    }
  };

  // Reset to all users
  const handleReset = () => {
    setSearchId('');
    setSearchUsername('');
    fetchUsers();
  };

  // Update user role
  const handleRoleChange = async (userId, newRole) => {
    setBusy(true);
    setError(null);
    const csrfToken = getCsrfToken();
    try {
      const response = await fetch(`/api/users/admin/${userId}/role`, {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
        },
        body: JSON.stringify(newRole),
      });
      if (!response.ok) throw new Error('Failed to update role');
      await fetchUsers();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  // Delete user
  const handleDelete = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    setBusy(true);
    setError(null);
    const csrfToken = getCsrfToken();
    try {
      const response = await fetch(`/api/users/admin/${userId}`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
        },
      });
      if (!response.ok) throw new Error('Failed to delete user');
      setUsers(users.filter((u) => u.id !== userId));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  if (isLoading || loading) return <div className="gd-loading">Loading…</div>;

  return (
    <div className="gd-page">
      <header className="gd-header">
        <button className="gd-btn gd-btn-back" onClick={() => navigate('/games')}>← Back to Games</button>
        <div className="gd-controls">
          <span>{user.username}</span>
          <button className="gd-btn" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <h1 className="gd-title">Admin Dashboard</h1>

      {error && <div className="gd-error">{error}</div>}

      {/* Search Section */}
      <div className="gd-search-row">
        <input
          type="number"
          placeholder="Search by ID"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
          className="gd-input-small"
          disabled={busy}
        />
        <button className="gd-btn" onClick={handleSearchById} disabled={busy}>Search</button>
      </div>

      <div className="gd-search-row">
        <input
          type="text"
          placeholder="Search by Username"
          value={searchUsername}
          onChange={(e) => setSearchUsername(e.target.value)}
          className="gd-input-small"
          disabled={busy}
        />
        <button className="gd-btn" onClick={handleSearchByUsername} disabled={busy}>Search</button>
        <button className="gd-btn gd-btn-reset" onClick={handleReset} disabled={busy}>Reset</button>
      </div>

      <table className="gd-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Notifications</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
        {users.length > 0 ? (
            users.map((u) => (
                <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.username}</td>
                    <td>{u.email}</td>
                    <td>{u.enableNotifications ? '✅' : '❌'}</td>
                    <td>
                        <select
                            className="gd-select"
                            value={u.role}
                            onChange={(e) => handleRoleChange(u.id, e.target.value)}
                            disabled={busy || u.id === user.id}
                        >
                            {ROLE_TYPES.map((role) => (
                                <option key={role} value={role}>
                                    {role.replace('ROLE_', '')}
                                </option>
                            ))}
                        </select>
                    </td>
                    <td>
                        <button
                            className="gd-btn-delete"
                            onClick={() => handleDelete(u.id)}
                            disabled={busy || u.id === user.id}
                        >
                            Delete
                        </button>
                    </td>
                </tr>
            ))
        ) : (
            /* Only show "No users found" if there is NO error */
            !error && <tr><td colSpan="6">No users found.</td></tr>
        )}
        </tbody>
      </table>
    </div>
  );
};

export default AdminPage;
