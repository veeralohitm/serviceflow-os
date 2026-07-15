import { useEffect, useState } from "react";
import Login from "./Login";
import { getMe } from "./api";
import "./App.css";

const TOKEN_KEY = "serviceflow_token";

function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }
    getMe(token)
      .then(setUser)
      .catch((err) => {
        setError(err.message);
        handleLogout();
      });
  }, [token]);

  function handleLogin(newToken) {
    localStorage.setItem(TOKEN_KEY, newToken);
    setToken(newToken);
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
  }

  if (!token || !user) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div className="page">
      <div className="card dashboard-card">
        <div className="dashboard-row">
          <h1 className="wordmark">
            ServiceFlow OS
            <span className="role-pill">{user.role}</span>
          </h1>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <dl className="info-list">
          <div>
            <dt>Signed in as</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Tenant</dt>
            <dd>
              <code>{user.tenantId}</code>
            </dd>
          </div>
        </dl>

        <button type="button" className="btn-secondary" onClick={handleLogout}>
          Log out
        </button>
      </div>
    </div>
  );
}

export default App;
