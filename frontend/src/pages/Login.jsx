// Copied login UI adapted for BCrypt verification, HTTP Basic, CSRF and current-user lookup.
import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { api, clearCredentials } from "../api.js";
import { useAuth } from "../auth.jsx";
import { Alert } from "../components/ui.jsx";

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    api.login(username.trim().toLowerCase(),password).then(async identity=>{
      const profile=await api.me().catch(error=>{
        if(error.status===404 && identity.role==='ADMIN') return null;
        throw error;
      });
      login({...identity,empId:profile?.empId || null,name:profile?.empName || null,profile});
      setPassword('');navigate('/');
    }).catch(error=>{clearCredentials();setError(error.message);}).finally(()=>setBusy(false));
  }

  return (
    <div className="row g-0 min-vh-100">
      <div className="col-lg-6 auth-hero d-none d-lg-flex">
        <span className="brand-logo mb-4">L</span>
        <h1 className="display-5 fw-bold">Your people, connected.</h1>
        <p className="fs-5 opacity-75">Manage employee profiles, account access and reporting teams.</p>
      </div>

      <div className="col-lg-6 auth-side d-flex align-items-center justify-content-center p-4">
        <form className="card auth-card p-4" onSubmit={submit}>
          <div className="d-flex align-items-center gap-2 mb-3 fw-bold fs-4" style={{ color: "#064e3b" }}>
            <span className="brand-logo">L</span> LeaveHub
          </div>
          <h2 className="h4">Welcome back</h2>
          <p className="text-secondary">Sign in with your email and password.</p>
          <Alert>{error}</Alert>

          <div className="mb-3">
            <label htmlFor="loginEmail" className="form-label fw-semibold">Username (email)</label>
            <input id="loginEmail" autoComplete="username" type="email" className="form-control" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="you@company.com" required />
          </div>
          <div className="mb-3">
            <label htmlFor="loginPassword" className="form-label fw-semibold">Password</label>
            <input id="loginPassword" autoComplete="current-password" type="password" className="form-control" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>

          <button className="btn btn-primary w-100" disabled={busy}>{busy ? "Signing in..." : "Sign in"}</button>
          <p className="text-center text-secondary mt-3 mb-0">
            Contact HR to create your employee profile and login account.
          </p>
        </form>
      </div>
    </div>
  );
}
