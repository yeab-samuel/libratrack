import { useState, useEffect, useCallback } from "react";

/* ─── CONFIG ─────────────────────────────────────────────── */
const BASE = "";

/* ─── API HELPER ─────────────────────────────────────────── */
async function api(path, opts = {}, token = null) {
  const headers = { "Content-Type": "application/json", ...(opts.headers || {}) };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(BASE + path, { ...opts, headers });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || body.error || `Error ${res.status}`);
  return body;
}

/* ─── GLOBAL STYLES ──────────────────────────────────────── */
const CSS = `
@import url('https://fonts.googleapis.com/css2?family=EB+Garamond:ital,wght@0,400;0,500;0,600;1,400&family=DM+Mono:wght@300;400;500&display=swap');
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
:root {
  --ink: #0E0D0B; --paper: #F8F6F2; --accent: #A04E1F; --muted: #7A7570;
  --border: #DDD9D2; --ok: #2C6E49; --bad: #9B2226; --warn: #8E6000;
  --info: #144272; --sw: 228px;
}
html { font-size: 16px; }
body { min-height: 100vh; background: var(--paper); color: var(--ink); font-family: 'EB Garamond', Georgia, serif; -webkit-font-smoothing: antialiased; }
#root { min-height: 100vh; }
.auth-shell { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; padding: 3rem 2rem 4rem; overflow-y: auto; }
.auth-card { width: 100%; max-width: 400px; animation: fadeUp .35s ease both; }
.auth-wordmark { font-size: .8rem; font-weight: 600; letter-spacing: .22em; text-transform: uppercase; margin-bottom: 3rem; display: flex; align-items: baseline; gap: .35rem; }
.auth-wordmark em { color: var(--accent); font-style: normal; }
.auth-heading { font-size: 2rem; font-weight: 400; line-height: 1.15; margin-bottom: 2rem; }
.field { margin-bottom: 1.1rem; }
.field label { display: block; font-family: 'DM Mono', monospace; font-size: .63rem; letter-spacing: .1em; text-transform: uppercase; color: var(--muted); margin-bottom: .35rem; }
.field input, .field select, .field textarea { width: 100%; border: 1px solid var(--border); background: var(--paper); color: var(--ink); font-family: 'EB Garamond', serif; font-size: 1rem; padding: .55rem .75rem; outline: none; border-radius: 0; appearance: none; -webkit-appearance: none; transition: border-color .15s; }
.field input:focus, .field select:focus, .field textarea:focus { border-color: var(--ink); }
.field textarea { resize: vertical; min-height: 80px; }
.field select { background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%237A7570'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right .75rem center; padding-right: 2rem; }
.field-hint { font-family: 'DM Mono', monospace; font-size: .6rem; color: var(--muted); margin-top: .3rem; letter-spacing: .02em; }
.btn { display: inline-flex; align-items: center; gap: .4rem; cursor: pointer; font-family: 'DM Mono', monospace; font-size: .66rem; letter-spacing: .1em; text-transform: uppercase; padding: .6rem 1.2rem; border: 1px solid var(--ink); background: var(--ink); color: var(--paper); border-radius: 0; transition: background .15s, border-color .15s, color .15s; white-space: nowrap; }
.btn:hover:not(:disabled) { background: var(--accent); border-color: var(--accent); }
.btn:disabled { opacity: .38; cursor: not-allowed; }
.btn-ghost { background: transparent; color: var(--ink); }
.btn-ghost:hover:not(:disabled) { background: var(--ink); color: var(--paper); }
.btn-sm { font-size: .6rem; padding: .3rem .65rem; }
.btn-danger { border-color: var(--bad); background: transparent; color: var(--bad); }
.btn-danger:hover:not(:disabled) { background: var(--bad); color: white; }
.btn-ok { border-color: var(--ok); background: transparent; color: var(--ok); }
.btn-ok:hover:not(:disabled) { background: var(--ok); color: white; }
.btn-borrow { border-color: var(--info); background: transparent; color: var(--info); }
.btn-borrow:hover:not(:disabled) { background: var(--info); color: white; }
.link-action { background: none; border: none; cursor: pointer; font-family: 'EB Garamond', serif; font-size: .95rem; color: var(--accent); text-decoration: underline; padding: 0; }
.msg { font-family: 'DM Mono', monospace; font-size: .68rem; letter-spacing: .03em; margin-top: .75rem; padding: .6rem .8rem; border-left: 2px solid; }
.msg-err { color: var(--bad); border-color: var(--bad); background: #fdf3f3; }
.msg-ok  { color: var(--ok);  border-color: var(--ok);  background: #f0faf4; }
.app { display: flex; min-height: 100vh; overflow-x: hidden; }
.sidebar { width: var(--sw); min-height: 100vh; border-right: 1px solid var(--border); display: flex; flex-direction: column; padding: 1.75rem 1.5rem; position: fixed; top: 0; left: 0; bottom: 0; background: var(--paper); z-index: 20; overflow-y: auto; }
.sidebar-wm { font-size: .72rem; font-weight: 600; letter-spacing: .2em; text-transform: uppercase; margin-bottom: 2.5rem; line-height: 1.5; }
.sidebar-wm em { color: var(--accent); font-style: normal; }
.nav-section-label { font-family: 'DM Mono', monospace; font-size: .58rem; letter-spacing: .14em; text-transform: uppercase; color: var(--border); margin: 1.25rem 0 .4rem; }
.nav-btn { display: block; width: 100%; text-align: left; background: none; border: none; cursor: pointer; font-family: 'EB Garamond', serif; font-size: .975rem; color: var(--muted); padding: .35rem 0; transition: color .12s; letter-spacing: .01em; }
.nav-btn:hover { color: var(--ink); }
.nav-btn.active { color: var(--ink); font-weight: 500; }
.nav-btn.active::before { content: '→ '; color: var(--accent); }
.sidebar-foot { margin-top: auto; padding-top: 1.25rem; border-top: 1px solid var(--border); }
.user-chip { font-family: 'DM Mono', monospace; font-size: .62rem; letter-spacing: .04em; color: var(--muted); line-height: 1.7; margin-bottom: .7rem; word-break: break-all; }
.user-chip strong { display: block; color: var(--ink); font-weight: 400; font-size: .7rem; }
.user-chip .uid { display: block; color: var(--accent); font-size: .68rem; letter-spacing: .06em; }
.main { margin-left: var(--sw); flex: 1; padding: 2.5rem 3rem; width: calc(100vw - var(--sw)); max-width: 1040px; min-width: 0; overflow-x: hidden; animation: fadeUp .3s ease both; }
.page-head { margin-bottom: 1.75rem; padding-bottom: .9rem; border-bottom: 1px solid var(--border); display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.page-title { font-size: 1.8rem; font-weight: 400; letter-spacing: -.01em; }
.page-sub { font-family: 'DM Mono', monospace; font-size: .65rem; letter-spacing: .08em; text-transform: uppercase; color: var(--muted); flex: 1; padding-left: 1rem; }
.filter-row { display: flex; gap: .65rem; flex-wrap: wrap; align-items: flex-end; margin-bottom: 1.5rem; }
.filter-row .field { margin: 0; min-width: 140px; flex: 1; }
.tbl-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th { font-family: 'DM Mono', monospace; font-size: .6rem; letter-spacing: .1em; text-transform: uppercase; color: var(--muted); text-align: left; padding: .55rem 1rem .55rem 0; border-bottom: 1px solid var(--border); font-weight: 400; white-space: nowrap; }
td { padding: .8rem 1rem .8rem 0; font-size: .95rem; border-bottom: 1px solid var(--border); vertical-align: middle; line-height: 1.35; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: rgba(160,78,31,.03); }
.mono { font-family: 'DM Mono', monospace; font-size: .78rem; }
.td-actions { display: flex; gap: .4rem; flex-wrap: wrap; }
.badge { font-family: 'DM Mono', monospace; font-size: .58rem; letter-spacing: .07em; text-transform: uppercase; padding: .18rem .42rem; border: 1px solid currentColor; display: inline-block; line-height: 1.4; }
.b-active{color:var(--ok)} .b-overdue{color:var(--bad)} .b-returned{color:var(--muted)}
.b-pending{color:var(--warn)} .b-unpaid{color:var(--warn)} .b-paid{color:var(--ok)} .b-waived{color:var(--muted)}
.b-notified{color:var(--info)} .b-cancelled{color:var(--muted)} .b-fulfilled{color:var(--ok)} .b-expired{color:var(--bad)}
.b-waiting{color:var(--info)} .b-admin{color:var(--accent)} .b-librarian{color:var(--info)} .b-student{color:var(--ok)} .b-faculty{color:var(--warn)}
.b-science{color:var(--info)} .b-engineering{color:var(--accent)} .b-humanities{color:var(--ok)} .b-law{color:var(--bad)} .b-medicine{color:var(--warn)} .b-other{color:var(--muted)}
.b-computer_science{color:var(--info)} .b-history{color:var(--accent)} .b-literature{color:var(--ok)}
.pager { display: flex; align-items: center; gap: .6rem; margin-top: 1.5rem; font-family: 'DM Mono', monospace; font-size: .65rem; color: var(--muted); }
.empty { text-align: center; padding: 5rem 2rem; color: var(--muted); font-style: italic; font-size: 1.15rem; }
.loading { text-align: center; padding: 4rem 2rem; font-family: 'DM Mono', monospace; font-size: .68rem; letter-spacing: .12em; text-transform: uppercase; color: var(--border); animation: pulse 1.4s infinite; }
.overlay { position: fixed; inset: 0; background: rgba(14,13,11,.45); display: grid; place-items: center; z-index: 200; padding: 1rem; animation: fadeIn .18s ease; overflow-y: auto; }
.dialog { background: var(--paper); border: 1px solid var(--border); padding: 2rem; width: 100%; max-width: 440px; max-height: 90vh; overflow-y: auto; animation: fadeUp .22s ease; }
.dialog-title { font-size: 1.3rem; font-weight: 400; margin-bottom: 1.5rem; }
.dialog-actions { display: flex; gap: .65rem; margin-top: 1.5rem; justify-content: flex-end; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 1px; background: var(--border); border: 1px solid var(--border); margin-bottom: 2rem; }
.stat-cell { background: var(--paper); padding: 1.25rem 1.5rem; }
.stat-n { font-family: 'DM Mono', monospace; font-size: 2rem; font-weight: 300; color: var(--accent); line-height: 1; margin-bottom: .25rem; }
.stat-l { font-family: 'DM Mono', monospace; font-size: .6rem; text-transform: uppercase; letter-spacing: .1em; color: var(--muted); }
.books-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr)); gap: 0; border-top: 1px solid var(--border); border-left: 1px solid var(--border); margin-bottom: 1.5rem; }
.book-card { border-right: 1px solid var(--border); border-bottom: 1px solid var(--border); padding: 1.5rem; display: flex; flex-direction: column; gap: .75rem; transition: background .15s; }
.book-card:hover { background: rgba(160,78,31,.025); }
.book-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: .5rem; }
.book-card-year { font-family: 'DM Mono', monospace; font-size: .65rem; color: var(--muted); white-space: nowrap; padding-top: .1rem; }
.book-card-title { font-size: 1.15rem; font-weight: 500; line-height: 1.3; color: var(--ink); letter-spacing: -.01em; }
.book-card-author { font-size: .9rem; color: var(--muted); font-style: italic; }
.book-card-isbn { font-family: 'DM Mono', monospace; font-size: .62rem; color: var(--border); letter-spacing: .04em; }
.book-card-meta { display: flex; align-items: center; gap: .6rem; flex-wrap: wrap; margin-top: auto; padding-top: .5rem; border-top: 1px solid var(--border); }
.book-card-copies { font-family: 'DM Mono', monospace; font-size: .62rem; color: var(--muted); letter-spacing: .04em; margin-left: auto; }
.book-card-copies.available { color: var(--ok); }
.book-avail-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: var(--ok); flex-shrink: 0; }
.book-avail-dot.none { background: var(--border); }
.book-card-actions { display: flex; gap: .4rem; flex-wrap: wrap; margin-top: .5rem; }
.book-rating-row { display:flex; align-items:center; gap:.4rem; margin-top:.25rem; }
.book-avg-stars { color:#c8a84b; font-size:.8rem; letter-spacing:.05em; }
.book-avg-count { font-size:.65rem; color:var(--muted); font-family:'DM Mono',monospace; }
.stars { display:inline-flex; gap:.1rem; }
.star { font-size:1rem; color:var(--border); cursor:pointer; transition:color .1s; user-select:none; line-height:1; }
.star.filled { color:#c8a84b; }
.star.readonly { cursor:default; }
.td-rating { display:flex; flex-direction:column; gap:.15rem; align-items:flex-start; }
.rating-hint { font-size:.65rem; color:var(--muted); }
.priority-badge { font-family: 'DM Mono', monospace; font-size: .55rem; letter-spacing: .06em; padding: .12rem .35rem; border: 1px solid var(--warn); color: var(--warn); text-transform: uppercase; display: inline-block; }
.welcome-toast {
  position: fixed; top: 1.25rem; left: 50%; transform: translateX(-50%);
  z-index: 999; background: rgba(44,110,73,0.93); color: white;
  padding: .75rem 2rem; font-family: 'DM Mono', monospace;
  font-size: .72rem; letter-spacing: .08em; box-shadow: 0 4px 20px rgba(0,0,0,.15);
  white-space: nowrap; animation: fadeIn .3s ease both;
  border-left: 3px solid rgba(255,255,255,.4);
}
@keyframes fadeUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes pulse { 0%, 100% { opacity: .6; } 50% { opacity: .2; } }
`;

/* ─── UTILS ───────────────────────────────────────────────── */
const fmt   = (d) => d ? new Date(d).toLocaleDateString("en-GB", { day:"2-digit", month:"short", year:"numeric" }) : "—";
const money = (n) => n != null ? `$${Number(n).toFixed(2)}` : "—";

/**
 * Spring Data with @EnableSpringDataWebSupport(VIA_DTO) wraps page metadata
 * under a "page" key: { content:[...], page:{ number, totalPages, totalElements } }
 * This helper reads either format safely.
 */
const parsePage = (data) => {
  const p = data.page ?? data;
  return {
    number:        p.number        ?? 0,
    totalPages:    p.totalPages    ?? 0,
    totalElements: p.totalElements ?? 0,
  };
};

function Badge({ status }) {
  const cls = {
    ACTIVE:"b-active", OVERDUE:"b-overdue", RETURNED:"b-returned",
    UNPAID:"b-unpaid", PAID:"b-paid", WAIVED:"b-waived",
    NOTIFIED:"b-notified", CANCELLED:"b-cancelled", FULFILLED:"b-fulfilled",
    EXPIRED:"b-expired", WAITING:"b-waiting",
    ADMIN:"b-admin", LIBRARIAN:"b-librarian", STUDENT:"b-student", FACULTY:"b-faculty",
    SCIENCE:"b-science", ENGINEERING:"b-engineering", HUMANITIES:"b-humanities",
    LAW:"b-law", MEDICINE:"b-medicine", OTHER:"b-other",
  }[status] || "b-returned";
  return <span className={`badge ${cls}`}>{status?.replace(/_/g," ")}</span>;
}

function Pager({ page, totalPages, onPage }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => onPage(page-1)}>← prev</button>
      <span>page {page+1} / {totalPages}</span>
      <button className="btn btn-ghost btn-sm" disabled={page >= totalPages-1} onClick={() => onPage(page+1)}>next →</button>
    </div>
  );
}

function Modal({ title, onClose, children }) {
  return (
    <div className="overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="dialog">
        <div className="dialog-title">{title}</div>
        {children}
      </div>
    </div>
  );
}

/* ─── LOGIN ───────────────────────────────────────────────── */
function LoginPage({ onLogin, onRegister, successMessage }) {
  const [form, setForm]       = useState({ identifier: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [err, setErr]         = useState("");

  const handle = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      const data = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ identifier: form.identifier, password: form.password }),
      });
      onLogin({
        token: data.token,
        role: data.role,
        username: data.fullName || form.identifier,
        universityId: form.identifier,
      });
    } catch(e) { setErr(e.message); }
    finally { setLoading(false); }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-wordmark">LIBRA<em>TRACK</em></div>
        <h1 className="auth-heading">Sign in to<br/>your account</h1>

        {successMessage && <div className="msg msg-ok" style={{ marginBottom: "1.25rem" }}>{successMessage}</div>}

        <form onSubmit={handle}>
          <div className="field">
            <label>University ID</label>
            <input
              type="text"
              autoFocus
              autoComplete="username"
              placeholder="e.g. UGR/9305/23 · FAC/1234/20 · LIB/0042/19"
              value={form.identifier}
              onChange={e => setForm(f => ({ ...f, identifier: e.target.value.toUpperCase() }))}
              required
            />
            <div className="field-hint">Format: TYPE/SERIAL/YEAR — use the ID assigned by your institution</div>
          </div>
          <div className="field">
            <label>Password</label>
            <input
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              required
            />
          </div>
          <button
            className="btn"
            style={{ width: "100%", justifyContent: "center", marginTop: ".5rem" }}
            disabled={loading}
          >
            {loading ? "Signing in…" : "Sign in"}
          </button>
          {err && <div className="msg msg-err">{err}</div>}
        </form>

        <p style={{ marginTop: "1.5rem", fontSize: ".9rem", color: "var(--muted)" }}>
          No account? <button className="link-action" onClick={onRegister}>Register here</button>
        </p>
      </div>
    </div>
  );
}

/* ─── REGISTER ────────────────────────────────────────────── */
// FIX 2: Replaced local `ok` state + success screen with `onSuccess(msg)` callback.
//         After successful registration the root component switches back to login
//         and passes a success message that LoginPage displays above the form.
function RegisterPage({ onBack, onSuccess }) {
  const [form, setForm] = useState({ fullName: "", email: "", password: "", role: "STUDENT", universityId: "" });
  const [loading, setLoading] = useState(false);
  const [err, setErr]   = useState("");

  const handle = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      await api("/api/auth/register", { method: "POST", body: JSON.stringify(form) });
      // Delegate success handling to the root — no local ok-screen anymore.
      onSuccess("Account created successfully. Please sign in.");
    } catch(e) { setErr(e.message); }
    finally { setLoading(false); }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-wordmark">LIBRA<em>TRACK</em></div>
        <h1 className="auth-heading">Create<br/>an account</h1>

        <form onSubmit={handle}>
          <div className="field">
            <label>Full Name</label>
            <input autoFocus value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} required />
          </div>
          <div className="field">
            <label>University ID</label>
            <input
              value={form.universityId}
              onChange={e => setForm(f => ({ ...f, universityId: e.target.value.toUpperCase() }))}
              placeholder="e.g. ATE/9305/14"
              pattern="^[A-Z]{2,5}/\d{3,6}/\d{2}$"
              title="Format: DEPT/SERIAL/YEAR e.g. ATE/9305/14"
              required
            />
            <div className="field-hint">Format: DEPT/SERIAL/YEAR — must match campus registry</div>
          </div>
          <div className="field">
            <label>Role</label>
            <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
              <option value="STUDENT">Student</option>
              <option value="FACULTY">Faculty</option>
            </select>
            <div className="field-hint">Admin and Librarian accounts are created by administrators only.</div>
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
          </div>
          <div className="field">
            <label>Password (min 8 chars)</label>
            <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required minLength={8} />
          </div>
          <button
            className="btn"
            style={{ width: "100%", justifyContent: "center", marginTop: ".5rem" }}
            disabled={loading}
          >
            {loading ? "Creating…" : "Create account"}
          </button>
          {err && <div className="msg msg-err">{err}</div>}
        </form>

        <p style={{ marginTop: "1.5rem", fontSize: ".9rem", color: "var(--muted)" }}>
          Have an account? <button className="link-action" onClick={onBack}>Sign in</button>
        </p>
      </div>
    </div>
  );
}

/* ─── BOOK CARD ───────────────────────────────────────────── */
function StarRating({ value, onChange, readonly }) {
  const [hover, setHover] = React.useState(0);
  const effective = hover || value || 0;
  return (
    <span className="stars">
      {[1,2,3,4,5].map(n => (
        <span key={n}
          className={`star${effective >= n ? " filled" : ""}${readonly ? " readonly" : ""}`}
          onClick={() => !readonly && onChange?.(n)}
          onMouseEnter={() => !readonly && setHover(n)}
          onMouseLeave={() => !readonly && setHover(0)}
          title={readonly ? `${value} out of 5` : `Rate ${n} star${n > 1 ? "s" : ""}`}
        >★</span>
      ))}
    </span>
  );
}

function BookCard({ book, role, onBorrow, onReserve }) {
  const copies     = book.totalCopies ?? 0;
  const hasAvailable = copies > 0;
  const isMember   = ["STUDENT","FACULTY"].includes(role);
  const isFaculty  = role === "FACULTY";

  return (
    <div className="book-card">
      <div className="book-card-top">
        <Badge status={book.category} />
        <span className="book-card-year">{book.publishedYear || "—"}</span>
      </div>
      <div>
        <div className="book-card-title">{book.title}</div>
        <div className="book-card-author">{book.author}</div>
      </div>
      {book.isbn && <div className="book-card-isbn">{book.isbn}</div>}
      <div className="book-card-meta">
        <span className={`book-avail-dot ${hasAvailable ? "" : "none"}`} />
        <span className={`book-card-copies ${hasAvailable ? "available" : ""}`}>
          {hasAvailable ? `${copies} cop${copies === 1 ? "y" : "ies"} available` : "all copies on loan"}
        </span>
        {isFaculty && <span className="priority-badge">Priority</span>}
      </div>
      {book.ratingCount > 0 && (
        <div className="book-rating-row">
          <StarRating value={Math.round(book.averageRating)} readonly />
          <span className="book-avg-stars">{Number(book.averageRating).toFixed(1)}</span>
          <span className="book-avg-count">({book.ratingCount} rating{book.ratingCount !== 1 ? "s" : ""})</span>
        </div>
      )}
      {isMember && (
        <div className="book-card-actions">
          {hasAvailable ? (
            <button className="btn btn-borrow btn-sm" onClick={() => onBorrow(book)}>Borrow</button>
          ) : (
            <button className="btn btn-ghost btn-sm" onClick={() => onReserve(book)}>Reserve</button>
          )}
        </div>
      )}
    </div>
  );
}

/* ─── BORROW MODAL ────────────────────────────────────────── */
function BorrowModal({ book, token, role, onClose, onDone }) {
  const today = new Date();
  const defaultDue = new Date(today);
  defaultDue.setDate(defaultDue.getDate() + (role === "FACULTY" ? 30 : 14));
  const defaultDueStr = defaultDue.toISOString().split("T")[0];

  const [copyId, setCopyId] = useState("");
  const [dueDate, setDueDate] = useState(defaultDueStr);
  const [copies, setCopies]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr]         = useState("");

  useEffect(() => {
    api(`/api/books/${book.id}/copies/available`, {}, token)
      .then(d => setCopies(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, [book.id, token]);

  const submit = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      await api("/api/loans/borrow", { method: "POST", body: JSON.stringify({ bookCopyId: Number(copyId), dueDate }) }, token);
      onDone(`"${book.title}" borrowed successfully. Due: ${fmt(dueDate)}`);
    } catch(e) { setErr(e.message); setLoading(false); }
  };

  return (
    <Modal title={`Borrow: ${book.title}`} onClose={onClose}>
      <p style={{ fontSize: ".9rem", color: "var(--muted)", marginBottom: "1.25rem", lineHeight: 1.5 }}>
        {book.author} · {role === "FACULTY" ? "Faculty — up to 30 days, extendable" : "Student — up to 14 days"}
      </p>
      <form onSubmit={submit}>
        <div className="field">
          <label>Select copy</label>
          <select required value={copyId} onChange={e => setCopyId(e.target.value)}>
            <option value="">— choose a copy —</option>
            {copies.map(c => <option key={c.id} value={c.id}>{c.copyNumber} ({c.condition})</option>)}
          </select>
        </div>
        <div className="field">
          <label>Due date</label>
          <input type="date" required value={dueDate} min={new Date().toISOString().split("T")[0]} onChange={e => setDueDate(e.target.value)} />
        </div>
        {err && <div className="msg msg-err">{err}</div>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
          <button className="btn btn-sm" disabled={loading || !copyId}>{loading ? "Borrowing…" : "Confirm borrow"}</button>
        </div>
      </form>
    </Modal>
  );
}

/* ─── BOOKS PAGE ──────────────────────────────────────────── */
function BooksPage({ auth }) {
  const { token, role } = auth;
  const canManage = ["ADMIN","LIBRARIAN"].includes(role);
  const [rows, setRows]     = useState([]);
  const [pg, setPg]         = useState({ number:0, totalPages:0, totalElements:0 });
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({ title:"", author:"", category:"", publishedYear:"", available:"" });
  const [modal, setModal]   = useState(null);
  const [msg, setMsg]       = useState(null);

  const load = useCallback(async (page = 0) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, size: 18 });
      if (filters.title)     params.set("title", filters.title);
      if (filters.author)    params.set("author", filters.author);
      if (filters.category)    params.set("category", filters.category);
      if (filters.publishedYear) params.set("publishedYear", filters.publishedYear);
      if (filters.available)    params.set("available", filters.available);
      const data = await api(`/api/books/search?${params}`, {}, token);
      setRows(data.content);
      setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token, filters]);

  useEffect(() => { load(0); }, [load]);

  const reserve = async (book) => {
    try {
      await api("/api/reservations", { method:"POST", body: JSON.stringify({ bookId: book.id }) }, token);
      setModal(null);
      setMsg({ type:"ok", text: `Reservation placed for "${book.title}". You'll be notified when a copy is available.` });
    } catch(e) { setMsg({ type:"err", text: e.message }); setModal(null); }
  };

  const CATEGORIES = ["SCIENCE","ENGINEERING","HUMANITIES","LAW","MEDICINE","OTHER"];

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">Catalogue</h1>
        <span className="page-sub">{pg.totalElements} titles</span>
        {canManage && <button className="btn btn-sm" onClick={() => setModal("create")}>+ Add book</button>}
      </div>
      {msg && (
        <div className={`msg msg-${msg.type}`} style={{ marginBottom:"1rem" }}>
          {msg.text}
          <button style={{ marginLeft:"1rem", cursor:"pointer", background:"none", border:"none", color:"inherit", fontSize:"inherit" }} onClick={() => setMsg(null)}>✕</button>
        </div>
      )}
      <div className="filter-row">
        <div className="field"><label>Title</label><input placeholder="Search…" value={filters.title} onChange={e => setFilters(f => ({ ...f, title: e.target.value }))} /></div>
        <div className="field"><label>Author</label><input placeholder="Search…" value={filters.author} onChange={e => setFilters(f => ({ ...f, author: e.target.value }))} /></div>
        <div className="field"><label>Year</label><input type="number" min="1000" max="2100" placeholder="e.g. 2008" value={filters.publishedYear} onChange={e => setFilters(f => ({ ...f, publishedYear: e.target.value }))} /></div>
        <div className="field">
          <label>Category</label>
          <select value={filters.category} onChange={e => setFilters(f => ({ ...f, category: e.target.value }))}>
            <option value="">All</option>
            {CATEGORIES.map(c => <option key={c} value={c}>{c.replace(/_/g," ")}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Availability</label>
          <select value={filters.available} onChange={e => setFilters(f => ({ ...f, available: e.target.value }))}>
            <option value="">Any</option>
            <option value="true">Available</option>
            <option value="false">Unavailable</option>
          </select>
        </div>
        <button className="btn btn-sm" onClick={() => load(0)}>Search</button>
      </div>

      {loading ? <div className="loading">Loading catalogue…</div> : (
        <>
          {rows.length === 0 ? <div className="empty">No books found.</div> : (
            <div className="books-grid">
              {rows.map(b => (
                <BookCard
                  key={b.id} book={b} role={role}
                  onBorrow={(book) => setModal({ type:"borrow", book })}
                  onReserve={(book) => setModal({ type:"reserve", book })}
                />
              ))}
            </div>
          )}
          <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
        </>
      )}

      {modal === "create" && (
        <CreateBookModal
          onSubmit={async (form) => { await api("/api/books", { method:"POST", body: JSON.stringify(form) }, token); setModal(null); load(0); }}
          onClose={() => setModal(null)}
          categories={CATEGORIES}
        />
      )}
      {modal?.type === "borrow" && (
        <BorrowModal book={modal.book} token={token} role={role} onClose={() => setModal(null)}
          onDone={(txt) => { setModal(null); setMsg({ type:"ok", text: txt }); load(0); }} />
      )}
      {modal?.type === "reserve" && (
        <Modal title="Reserve book" onClose={() => setModal(null)}>
          <p style={{ fontSize:".95rem", marginBottom:"1.25rem", lineHeight:1.6 }}>
            Reserve <em>{modal.book.title}</em>?<br/>
            <span style={{ fontSize:".85rem", color:"var(--muted)" }}>
              You'll be added to the waitlist.{role === "FACULTY" ? " As faculty, you'll receive priority over students." : ""} An email notification will be sent when a copy is ready.
            </span>
          </p>
          <div className="dialog-actions">
            <button className="btn btn-ghost btn-sm" onClick={() => setModal(null)}>Cancel</button>
            <button className="btn btn-sm" onClick={() => reserve(modal.book)}>Confirm reservation</button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function CreateBookModal({ onSubmit, onClose, categories }) {
  const [form, setForm] = useState({ isbn:"", title:"", author:"", category:"OTHER", publisher:"", publishedYear:"", totalCopies:"1", description:"" });
  const [loading, setLoading] = useState(false);
  const [err, setErr]         = useState("");
  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      const body = { ...form, publishedYear: form.publishedYear ? Number(form.publishedYear) : undefined, totalCopies: Number(form.totalCopies) };
      await onSubmit(body);
    } catch(e) { setErr(e.message); setLoading(false); }
  };
  return (
    <Modal title="Add new book" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field"><label>ISBN</label><input value={form.isbn} onChange={set("isbn")} /></div>
        <div className="field"><label>Title *</label><input required value={form.title} onChange={set("title")} /></div>
        <div className="field"><label>Author *</label><input required value={form.author} onChange={set("author")} /></div>
        <div className="field"><label>Category</label><select value={form.category} onChange={set("category")}>{categories.map(c => <option key={c} value={c}>{c.replace(/_/g," ")}</option>)}</select></div>
        <div className="field"><label>Publisher</label><input value={form.publisher} onChange={set("publisher")} /></div>
        <div className="field"><label>Published year</label><input type="number" value={form.publishedYear} onChange={set("publishedYear")} /></div>
        <div className="field"><label>Total copies *</label><input type="number" min="1" required value={form.totalCopies} onChange={set("totalCopies")} /></div>
        <div className="field"><label>Description</label><textarea value={form.description} onChange={set("description")} /></div>
        {err && <div className="msg msg-err">{err}</div>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
          <button className="btn btn-sm" disabled={loading}>{loading ? "Saving…" : "Add book"}</button>
        </div>
      </form>
    </Modal>
  );
}

/* ─── LOANS PAGE ──────────────────────────────────────────── */
function LoansPage({ auth }) {
  const { token, role } = auth;
  const isMember  = ["STUDENT","FACULTY"].includes(role);
  const isFaculty = role === "FACULTY";
  const endpoint  = isMember ? "/api/loans/mine" : "/api/loans";
  const [rows, setRows]         = useState([]);
  const [pg, setPg]             = useState({ number:0, totalPages:0, totalElements:0 });
  const [loading, setLoading]   = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [modal, setModal]       = useState(null);
  const [msg, setMsg]           = useState(null);
  const [ratings, setRatings]   = useState({});  // bookId → submitted star value

  const submitRating = async (bookId, stars) => {
    try {
      await api(`/api/books/${bookId}/ratings`, {
        method: "POST",
        body: JSON.stringify({ stars })
      }, token);
      setRatings(r => ({ ...r, [bookId]: stars }));
    } catch(e) {
      setMsg({ type: "err", text: "Rating failed: " + e.message });
    }
  };

  const load = useCallback(async (page = 0) => {
    setLoading(true); setMsg(null);
    try {
      const params = new URLSearchParams({ page, size: 15 });
      if (statusFilter) params.set("status", statusFilter);
      const data = await api(`${endpoint}?${params}`, {}, token);
      setRows(data.content);
      setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token, endpoint, statusFilter]);

  useEffect(() => { load(0); }, [load]);

  const returnLoan = async (id) => {
    try {
      await api(`/api/loans/${id}/return`, { method:"PATCH" }, token);
      setMsg({ type:"ok", text:"Loan returned successfully." }); load(pg.number);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  const extendLoan = async (id, newDueDate) => {
    try {
      await api(`/api/loans/${id}/extend`, { method:"PATCH", body: JSON.stringify({ newDueDate }) }, token);
      setModal(null); setMsg({ type:"ok", text:"Loan extended." }); load(pg.number);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">{isMember ? "My Loans" : "All Loans"}</h1>
        <span className="page-sub">{pg.totalElements} records</span>
        {!isMember && <button className="btn btn-sm" onClick={() => setModal("create")}>+ Issue loan</button>}
      </div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <div className="filter-row">
        <div className="field"><label>Status</label>
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="OVERDUE">Overdue</option>
            <option value="RETURNED">Returned</option>
          </select>
        </div>
        <button className="btn btn-sm" onClick={() => load(0)}>Filter</button>
      </div>
      {loading ? <div className="loading">Loading loans…</div> : (
        <>
          <div className="tbl-wrap">
            <table>
              <thead><tr>
                <th>#</th>{!isMember && <th>Member</th>}
                <th>Book</th><th>Copy</th><th>Issued</th><th>Due</th><th>Status</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {rows.length === 0 && <tr><td colSpan={8}><div className="empty">No loans found.</div></td></tr>}
                {rows.map(l => (
                  <tr key={l.id}>
                    <td className="mono">{l.id}</td>
                    {!isMember && <td>{l.memberName}<br/><span className="mono" style={{ color:"var(--muted)", fontSize:".65rem" }}>#{l.memberId}</span></td>}
                    <td style={{ maxWidth:200 }}>{l.bookTitle}</td>
                    <td className="mono">{l.copyNumber}</td>
                    <td className="mono" style={{ fontSize:".75rem" }}>{fmt(l.issuedAt)}</td>
                    <td className="mono" style={{ fontSize:".75rem", color: l.status==="OVERDUE" ? "var(--bad)" : "inherit" }}>{fmt(l.dueDate)}</td>
                    <td><Badge status={l.status} /></td>
                    <td><div className="td-actions">
                      {!isMember && l.status !== "RETURNED" && <button className="btn btn-ok btn-sm" onClick={() => returnLoan(l.id)}>Return</button>}
                      {isFaculty && l.status === "ACTIVE" && <button className="btn btn-ghost btn-sm" onClick={() => setModal({ type:"extend", loan:l })}>Extend</button>}
                      {isMember && l.status === "RETURNED" && (
                        <div className="td-rating">
                          <StarRating value={ratings[l.bookId]} onChange={stars => submitRating(l.bookId, stars)} />
                          <span className="rating-hint">
                            {ratings[l.bookId] ? `Rated ★${ratings[l.bookId]}` : "Rate this book"}
                          </span>
                        </div>
                      )}
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
        </>
      )}
      {modal === "create" && <IssueLoanModal token={token} onClose={() => setModal(null)} onDone={() => { setModal(null); load(0); }} />}
      {modal?.type === "extend" && <ExtendModal loan={modal.loan} onClose={() => setModal(null)} onExtend={(d) => extendLoan(modal.loan.id, d)} />}
    </div>
  );
}

function IssueLoanModal({ token, onClose, onDone }) {
  const [lookup, setLookup]       = useState("");
  const [form, setForm]           = useState({ memberId:"", bookCopyId:"", dueDate:"" });
  const [memberInfo, setMemberInfo] = useState(null);
  const [loading, setLoading]     = useState(false);
  const [err, setErr]             = useState("");
  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const lookupMember = async () => {
    try {
      const u = await api(`/api/admin/users/by-university-id/${encodeURIComponent(lookup)}`, {}, token);
      setMemberInfo(u);
      setForm(f => ({ ...f, memberId: String(u.id) }));
      setErr("");
    } catch(e) { setErr("Member not found: " + e.message); setMemberInfo(null); }
  };

  const submit = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      await api("/api/loans", { method:"POST", body: JSON.stringify({ memberId: Number(form.memberId), bookCopyId: Number(form.bookCopyId), dueDate: form.dueDate }) }, token);
      onDone();
    } catch(e) { setErr(e.message); setLoading(false); }
  };

  return (
    <Modal title="Issue loan at counter" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <label>Look up member by University ID</label>
          <div style={{ display:"flex", gap:".5rem" }}>
            <input value={lookup} onChange={e => setLookup(e.target.value.toUpperCase())} placeholder="ATE/9305/14" style={{ flex:1 }} />
            <button type="button" className="btn btn-ghost btn-sm" onClick={lookupMember}>Look up</button>
          </div>
          {memberInfo && <div className="msg msg-ok" style={{ marginTop:".4rem" }}>✓ {memberInfo.fullName} — {memberInfo.role}</div>}
        </div>
        <div className="field"><label>Member ID (auto-filled)</label>
          <input required type="number" value={form.memberId} onChange={set("memberId")} /></div>
        <div className="field"><label>Book Copy ID</label>
          <input required type="number" value={form.bookCopyId} onChange={set("bookCopyId")} /></div>
        <div className="field"><label>Due date</label>
          <input required type="date" value={form.dueDate} onChange={set("dueDate")} /></div>
        {err && <div className="msg msg-err">{err}</div>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
          <button className="btn btn-sm" disabled={loading}>{loading ? "Issuing…" : "Issue loan"}</button>
        </div>
      </form>
    </Modal>
  );
}

function ExtendModal({ loan, onClose, onExtend }) {
  const [newDueDate, setNewDueDate] = useState("");
  return (
    <Modal title="Extend loan" onClose={onClose}>
      <p style={{ fontSize:".95rem", marginBottom:"1.25rem", lineHeight:1.5, color:"var(--muted)" }}>
        Extending <strong style={{ color:"var(--ink)" }}>{loan.bookTitle}</strong>. Current due: {fmt(loan.dueDate)}.
      </p>
      <div className="field"><label>New due date</label>
        <input type="date" value={newDueDate} onChange={e => setNewDueDate(e.target.value)} /></div>
      <div className="dialog-actions">
        <button className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
        <button className="btn btn-sm" disabled={!newDueDate} onClick={() => onExtend(newDueDate)}>Extend</button>
      </div>
    </Modal>
  );
}

/* ─── RESERVATIONS PAGE ───────────────────────────────────── */
function ReservationsPage({ auth }) {
  const { token, role } = auth;
  const isMember = ["STUDENT","FACULTY"].includes(role);
  const endpoint = isMember ? "/api/reservations/mine" : "/api/reservations";
  const [rows, setRows]         = useState([]);
  const [pg, setPg]             = useState({ number:0, totalPages:0, totalElements:0 });
  const [loading, setLoading]   = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [msg, setMsg]           = useState(null);

  const load = useCallback(async (page = 0) => {
    setLoading(true); setMsg(null);
    try {
      const params = new URLSearchParams({ page, size: 15 });
      if (statusFilter) params.set("status", statusFilter);
      const data = await api(`${endpoint}?${params}`, {}, token);
      setRows(data.content);
      setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token, endpoint, statusFilter]);

  useEffect(() => { load(0); }, [load]);

  const cancel = async (id) => {
    try {
      await api(`/api/reservations/${id}`, { method:"DELETE" }, token);
      setMsg({ type:"ok", text:"Reservation cancelled." }); load(pg.number);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">{isMember ? "My Reservations" : "All Reservations"}</h1>
        <span className="page-sub">{pg.totalElements} records</span>
      </div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <div className="filter-row">
        <div className="field"><label>Status</label>
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            {["WAITING","NOTIFIED","FULFILLED","CANCELLED","EXPIRED"].map(s => <option key={s}>{s}</option>)}
          </select>
        </div>
        <button className="btn btn-sm" onClick={() => load(0)}>Filter</button>
      </div>
      {loading ? <div className="loading">Loading reservations…</div> : (
        <>
          <div className="tbl-wrap">
            <table>
              <thead><tr>
                <th>#</th>{!isMember && <th>Member</th>}
                <th>Book</th><th>Queue pos.</th><th>Reserved</th><th>Notified</th><th>Collect by</th><th>Status</th><th></th>
              </tr></thead>
              <tbody>
                {rows.length === 0 && <tr><td colSpan={9}><div className="empty">No reservations found.</div></td></tr>}
                {rows.map(r => (
                  <tr key={r.id}>
                    <td className="mono">{r.id}</td>
                    {!isMember && <td>{r.memberName}</td>}
                    <td>{r.bookTitle}</td>
                    <td className="mono" style={{ textAlign:"center" }}>#{r.queuePosition}</td>
                    <td className="mono" style={{ fontSize:".75rem" }}>{fmt(r.reservedAt)}</td>
                    <td className="mono" style={{ fontSize:".75rem" }}>{fmt(r.notifiedAt)}</td>
                    <td className="mono" style={{ fontSize:".75rem", color: r.status==="NOTIFIED" ? "var(--warn)" : "inherit", fontWeight: r.status==="NOTIFIED" ? 600 : 400 }}>
                      {r.expiresAt ? fmt(r.expiresAt) : "—"}
                    </td>
                    <td><Badge status={r.status} /></td>
                    <td>{["WAITING","NOTIFIED"].includes(r.status) && (
                      <button className="btn btn-danger btn-sm" onClick={() => cancel(r.id)}>Cancel</button>
                    )}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
        </>
      )}
    </div>
  );
}

/* ─── FINES PAGE ──────────────────────────────────────────── */
function FinesPage({ auth }) {
  const { token, role } = auth;
  const isMember = ["STUDENT","FACULTY"].includes(role);
  const canPay   = ["LIBRARIAN","ADMIN"].includes(role);
  const canWaive = role === "ADMIN";
  const endpoint = isMember ? "/api/fines/mine" : "/api/fines";
  const [rows, setRows]         = useState([]);
  const [pg, setPg]             = useState({ number:0, totalPages:0, totalElements:0 });
  const [loading, setLoading]   = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [modal, setModal]       = useState(null);
  const [msg, setMsg]           = useState(null);

  const load = useCallback(async (page = 0) => {
    setLoading(true); setMsg(null);
    try {
      const params = new URLSearchParams({ page, size: 15 });
      if (statusFilter) params.set("status", statusFilter);
      const data = await api(`${endpoint}?${params}`, {}, token);
      setRows(data.content);
      setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token, endpoint, statusFilter]);

  useEffect(() => { load(0); }, [load]);

  const markPaid = async (id) => {
    try {
      await api(`/api/fines/${id}/pay`, { method:"PATCH" }, token);
      setMsg({ type:"ok", text:"Fine marked as paid." }); load(pg.number);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  const waiveFine = async (id, reason) => {
    try {
      await api(`/api/fines/${id}/waive`, { method:"PATCH", body: JSON.stringify({ reason }) }, token);
      setModal(null); setMsg({ type:"ok", text:"Fine waived." }); load(pg.number);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">{isMember ? "My Fines" : "All Fines"}</h1>
        <span className="page-sub">{pg.totalElements} records</span>
      </div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <div className="filter-row">
        <div className="field"><label>Status</label>
          <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            <option value="UNPAID">Unpaid</option>
            <option value="PAID">Paid</option>
            <option value="WAIVED">Waived</option>
          </select>
        </div>
        <button className="btn btn-sm" onClick={() => load(0)}>Filter</button>
      </div>
      {loading ? <div className="loading">Loading fines…</div> : (
        <>
          <div className="tbl-wrap">
            <table>
              <thead><tr>
                <th>#</th>{!isMember && <th>Member</th>}
                <th>Loan #</th><th>Book</th><th>Amount</th><th>Created</th><th>Paid at</th><th>Status</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {rows.length === 0 && <tr><td colSpan={9}><div className="empty">No fines. Keep returning books on time!</div></td></tr>}
                {rows.map(f => (
                  <tr key={f.id}>
                    <td className="mono">{f.id}</td>
                    {!isMember && <td>{f.memberName}</td>}
                    <td className="mono">{f.loanId}</td>
                    <td style={{ maxWidth:150, fontSize:".88rem" }}>{f.bookTitle || "—"}</td>
                    <td className="mono" style={{ fontWeight:500, color: f.status==="UNPAID" ? "var(--bad)" : "inherit" }}>{money(f.amount)}</td>
                    <td className="mono" style={{ fontSize:".75rem" }}>{fmt(f.createdAt)}</td>
                    <td className="mono" style={{ fontSize:".75rem" }}>{fmt(f.paidAt)}</td>
                    <td><Badge status={f.status} /></td>
                    <td><div className="td-actions">
                      {canPay && f.status==="UNPAID" && <button className="btn btn-ok btn-sm" onClick={() => markPaid(f.id)}>Mark paid</button>}
                      {canWaive && f.status==="UNPAID" && <button className="btn btn-ghost btn-sm" onClick={() => setModal({ id:f.id })}>Waive</button>}
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
        </>
      )}
      {modal && (
        <Modal title="Waive fine" onClose={() => setModal(null)}>
          <WaiveForm fineId={modal.id} onWaive={waiveFine} onClose={() => setModal(null)} />
        </Modal>
      )}
    </div>
  );
}

function WaiveForm({ fineId, onWaive, onClose }) {
  const [reason, setReason] = useState("");
  return (
    <>
      <div className="field"><label>Reason for waiving</label><textarea required value={reason} onChange={e => setReason(e.target.value)} /></div>
      <div className="dialog-actions">
        <button className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
        <button className="btn btn-sm" disabled={!reason.trim()} onClick={() => onWaive(fineId, reason)}>Confirm waiver</button>
      </div>
    </>
  );
}

/* ─── REPORTS PAGE ────────────────────────────────────────── */
function ReportsPage({ auth }) {
  const { token } = auth;
  const [overdue, setOverdue]       = useState([]);
  const [overdueTotal, setOverdueTotal] = useState(0);
  const [loading, setLoading]       = useState(false);
  const [summaryForm, setSummaryForm] = useState({ from:"", to:"" });
  const [summary, setSummary]       = useState(null);
  const [msg, setMsg]               = useState(null);

  const loadOverdue = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api("/api/reports/overdue?size=20", {}, token);
      setOverdue(data.content); setOverdueTotal(data.totalElements);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token]);

  useEffect(() => { loadOverdue(); }, [loadOverdue]);

  const loadSummary = async (e) => {
    e.preventDefault();
    try {
      const params = new URLSearchParams({ fromDate: summaryForm.from, toDate: summaryForm.to });
      const data = await api(`/api/reports/fines-summary?${params}`, {}, token);
      setSummary(data);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head"><h1 className="page-title">Reports</h1></div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <h2 style={{ fontSize:"1.15rem", fontWeight:400, marginBottom:"1rem" }}>
        Overdue loans <span style={{ fontFamily:"monospace", fontSize:".75rem", color:"var(--muted)" }}>({overdueTotal})</span>
      </h2>
      {loading ? <div className="loading">Loading…</div> : (
        <div className="tbl-wrap" style={{ marginBottom:"2.5rem" }}>
          <table>
            <thead><tr><th>#</th><th>Member</th><th>Univ. ID</th><th>Book</th><th>Due</th><th>Copy</th></tr></thead>
            <tbody>
              {overdue.length === 0 && <tr><td colSpan={6}><div className="empty">No overdue loans.</div></td></tr>}
              {overdue.map(l => (
                <tr key={l.id}>
                  <td className="mono">{l.id}</td>
                  <td>{l.memberName}</td>
                  <td className="mono" style={{ fontSize:".72rem", color:"var(--accent)" }}>{l.universityId || "—"}</td>
                  <td>{l.bookTitle}</td>
                  <td className="mono" style={{ color:"var(--bad)", fontSize:".78rem" }}>{fmt(l.dueDate)}</td>
                  <td className="mono">{l.copyNumber}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <h2 style={{ fontSize:"1.15rem", fontWeight:400, marginBottom:"1rem" }}>Fines summary</h2>
      <form onSubmit={loadSummary}>
        <div className="filter-row">
          <div className="field"><label>From</label><input type="date" required value={summaryForm.from} onChange={e => setSummaryForm(f => ({ ...f, from: e.target.value }))} /></div>
          <div className="field"><label>To</label><input type="date" required value={summaryForm.to} onChange={e => setSummaryForm(f => ({ ...f, to: e.target.value }))} /></div>
          <button type="submit" className="btn btn-sm">Generate</button>
        </div>
      </form>
      {summary && (
        <div className="stats-grid">
          <div className="stat-cell"><div className="stat-n">{money(summary.totalAmount ?? 0)}</div><div className="stat-l">Total collected</div></div>
          <div className="stat-cell"><div className="stat-n">{summary.totalCount ?? 0}</div><div className="stat-l">Fines paid</div></div>
        </div>
      )}
    </div>
  );
}

/* ─── USERS PAGE ──────────────────────────────────────────── */
function UsersPage({ auth }) {
  const { token } = auth;
  const [rows, setRows]         = useState([]);
  const [pg, setPg]             = useState({ number:0, totalPages:0, totalElements:0 });
  const [loading, setLoading]   = useState(false);
  const [filters, setFilters]   = useState({ role:"", active:"" });
  const [modal, setModal]       = useState(null);
  const [msg, setMsg]           = useState(null);

  const load = useCallback(async (page = 0) => {
    setLoading(true); setMsg(null);
    try {
      const params = new URLSearchParams({ page, size: 15 });
      if (filters.role)   params.set("role", filters.role);
      if (filters.active) params.set("active", filters.active);
      const data = await api(`/api/admin/users?${params}`, {}, token);
      setRows(data.content);
      setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
    finally { setLoading(false); }
  }, [token, filters]);

  useEffect(() => { load(0); }, [load]);

  const deactivate = async (id) => {
    try { await api(`/api/admin/users/${id}/deactivate`, { method:"PATCH" }, token); setMsg({ type:"ok", text:"User deactivated." }); load(pg.number); }
    catch(e) { setMsg({ type:"err", text: e.message }); }
  };
  const activate = async (id) => {
    try { await api(`/api/admin/users/${id}/activate`, { method:"PATCH" }, token); setMsg({ type:"ok", text:"User activated." }); load(pg.number); }
    catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">Users</h1>
        <span className="page-sub">{pg.totalElements} accounts</span>
        <button className="btn btn-sm" onClick={() => setModal("staff")}>+ Create staff</button>
      </div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <div className="filter-row">
        <div className="field"><label>Role</label>
          <select value={filters.role} onChange={e => setFilters(f => ({ ...f, role: e.target.value }))}>
            <option value="">All</option>
            {["ADMIN","LIBRARIAN","STUDENT","FACULTY"].map(r => <option key={r}>{r}</option>)}
          </select>
        </div>
        <div className="field"><label>Status</label>
          <select value={filters.active} onChange={e => setFilters(f => ({ ...f, active: e.target.value }))}>
            <option value="">All</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </div>
        <button className="btn btn-sm" onClick={() => load(0)}>Filter</button>
      </div>
      {loading ? <div className="loading">Loading users…</div> : (
        <>
          <div className="tbl-wrap">
            <table>
              <thead><tr><th>#</th><th>Full Name</th><th>University ID</th><th>Email</th><th>Role</th><th>Status</th><th>Joined</th><th>Actions</th></tr></thead>
              <tbody>
                {rows.length === 0 && <tr><td colSpan={8}><div className="empty">No users found.</div></td></tr>}
                {rows.map(u => (
                  <tr key={u.id}>
                    <td className="mono">{u.id}</td>
                    <td>{u.fullName}</td>
                    <td className="mono" style={{ color:"var(--accent)", fontSize:".72rem" }}>{u.universityId || "—"}</td>
                    <td className="mono" style={{ fontSize:".72rem" }}>{u.email}</td>
                    <td><Badge status={u.role} /></td>
                    <td><span className="badge" style={{ color: u.active ? "var(--ok)" : "var(--muted)", borderColor: u.active ? "var(--ok)" : "var(--border)" }}>{u.active ? "active" : "inactive"}</span></td>
                    <td className="mono" style={{ fontSize:".72rem" }}>{fmt(u.createdAt)}</td>
                    <td><div className="td-actions">
                      {u.active  && <button className="btn btn-danger btn-sm" onClick={() => deactivate(u.id)}>Deactivate</button>}
                      {!u.active && <button className="btn btn-ok btn-sm"     onClick={() => activate(u.id)}>Activate</button>}
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
        </>
      )}
      {modal === "staff" && (
        <CreateStaffModal token={token} onClose={() => setModal(null)} onDone={() => { setModal(null); setMsg({ type:"ok", text:"Staff account created." }); load(0); }} />
      )}
    </div>
  );
}

function CreateStaffModal({ token, onClose, onDone }) {
  const [form, setForm] = useState({ fullName:"", email:"", password:"", role:"LIBRARIAN", staffId:"" });
  const [loading, setLoading] = useState(false);
  const [err, setErr]         = useState("");
  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      await api("/api/admin/staff", { method:"POST", body: JSON.stringify(form) }, token);
      onDone();
    } catch(e) { setErr(e.message); setLoading(false); }
  };
  return (
    <Modal title="Create staff account" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field"><label>Full Name</label><input required value={form.fullName} onChange={set("fullName")} /></div>
        <div className="field">
          <label>Staff ID</label>
          <input
            required
            value={form.staffId}
            onChange={e => setForm(f => ({ ...f, staffId: e.target.value.toUpperCase() }))}
            placeholder="LIB/XXXX/YY or ADM/XXXX/YY"
            pattern="^(LIB|ADM)/\d{3,6}/\d{2}$"
            title="LIB/XXXX/YY for librarians · ADM/XXXX/YY for admins"
          />
          <div className="field-hint">LIB/XXXX/YY for librarians · ADM/XXXX/YY for admins</div>
        </div>
        <div className="field"><label>Email</label><input required type="email" value={form.email} onChange={set("email")} /></div>
        <div className="field"><label>Password</label><input required type="password" minLength={8} value={form.password} onChange={set("password")} /></div>
        <div className="field"><label>Role</label>
          <select value={form.role} onChange={set("role")}>
            <option value="LIBRARIAN">Librarian</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>
        {err && <div className="msg msg-err">{err}</div>}
        <div className="dialog-actions">
          <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>Cancel</button>
          <button className="btn btn-sm" disabled={loading}>{loading ? "Creating…" : "Create account"}</button>
        </div>
      </form>
    </Modal>
  );
}

/* ─── REGISTRY PAGE ───────────────────────────────────────── */
function RegistryPage({ auth }) {
  const { token } = auth;
  const [rows, setRows] = useState([]);
  const [pg, setPg]     = useState({ number:0, totalPages:0, totalElements:0 });
  const [modal, setModal] = useState(false);
  const [form, setForm]   = useState({ universityId:"", fullName:"", role:"STUDENT", department:"" });
  const [msg, setMsg]     = useState(null);

  const load = useCallback(async (page = 0) => {
    try {
      const data = await api(`/api/admin/registry?page=${page}&size=20`, {}, token);
      setRows(data.content); setPg(parsePage(data));
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  }, [token]);

  useEffect(() => { load(0); }, [load]);

  const add = async (e) => {
    e.preventDefault();
    try {
      await api("/api/admin/registry", { method:"POST", body: JSON.stringify(form) }, token);
      setModal(false); setMsg({ type:"ok", text:"Entry added." }); load(0);
    } catch(e) { setMsg({ type:"err", text: e.message }); }
  };

  return (
    <div className="main">
      <div className="page-head">
        <h1 className="page-title">University Registry</h1>
        <span className="page-sub">{pg.totalElements} entries</span>
        <button className="btn btn-sm" onClick={() => setModal(true)}>+ Add entry</button>
      </div>
      {msg && <div className={`msg msg-${msg.type}`}>{msg.text}</div>}
      <div className="tbl-wrap">
        <table>
          <thead><tr><th>University ID</th><th>Full Name</th><th>Role</th><th>Department</th><th>Status</th></tr></thead>
          <tbody>
            {rows.length === 0 && <tr><td colSpan={5}><div className="empty">No entries.</div></td></tr>}
            {rows.map(r => (
              <tr key={r.id}>
                <td className="mono" style={{ color:"var(--accent)" }}>{r.universityId}</td>
                <td>{r.fullName}</td>
                <td><Badge status={r.role} /></td>
                <td style={{ color:"var(--muted)", fontSize:".88rem" }}>{r.department || "—"}</td>
                <td><span className="badge" style={{ color: r.active ? "var(--ok)" : "var(--muted)" }}>{r.active ? "active" : "inactive"}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pager page={pg.number} totalPages={pg.totalPages} onPage={load} />
      {modal && (
        <Modal title="Add registry entry" onClose={() => setModal(false)}>
          <form onSubmit={add}>
            <div className="field"><label>University ID *</label>
              <input required placeholder="e.g. ATE/8809/14" value={form.universityId}
                onChange={e => setForm(f => ({ ...f, universityId: e.target.value.toUpperCase() }))}
                pattern="^[A-Z]{2,5}/\d{3,6}/\d{2}$" /></div>
            <div className="field"><label>Full Name *</label>
              <input required value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} /></div>
            <div className="field"><label>Role</label>
              <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                <option value="STUDENT">Student</option>
                <option value="FACULTY">Faculty</option>
              </select></div>
            <div className="field"><label>Department</label>
              <input value={form.department} onChange={e => setForm(f => ({ ...f, department: e.target.value }))} /></div>
            <div className="dialog-actions">
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setModal(false)}>Cancel</button>
              <button className="btn btn-sm">Add entry</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

/* ─── SIDEBAR ─────────────────────────────────────────────── */
function Sidebar({ auth, page, onPage, onLogout }) {
  const { role, username, universityId } = auth;
  const isAdmin      = role === "ADMIN";
  const isLibOrAdmin = ["ADMIN","LIBRARIAN"].includes(role);
  const isMember     = ["STUDENT","FACULTY"].includes(role);
  const link = (label, id) => (
    <button key={id} className={`nav-btn ${page === id ? "active" : ""}`} onClick={() => onPage(id)}>{label}</button>
  );
  return (
    <nav className="sidebar">
      <div className="sidebar-wm">LIBRA<em>TRACK</em></div>
      <div>{link("Catalogue", "books")}</div>
      {isMember && (<>
        <div className="nav-section-label">Library</div>
        {link("My Loans", "loans")}
        {link("My Reservations", "reservations")}
        {link("My Fines", "fines")}
      </>)}
      {isLibOrAdmin && (<>
        <div className="nav-section-label">Operations</div>
        {link("Loans", "loans")}
        {link("Reservations", "reservations")}
        {link("Fines", "fines")}
        {link("Reports", "reports")}
      </>)}
      {isAdmin && (<>
        <div className="nav-section-label">Administration</div>
        {link("Users", "users")}
        {link("Registry", "registry")}
      </>)}
      <div className="sidebar-foot">
        <div className="user-chip">
          <strong>{username}</strong>
          {universityId && <span className="uid">{universityId}</span>}
          {role}
        </div>
        <button className="btn btn-ghost btn-sm" style={{ width:"100%", justifyContent:"center" }} onClick={onLogout}>
          Sign out
        </button>
      </div>
    </nav>
  );
}

/* ─── ROOT ────────────────────────────────────────────────── */
// FIX 3 (continued): Added `successMsg` state that flows login→register→login,
//         and `welcome` toast that auto-dismisses after 4 s on successful sign-in.
export default function LibraTrack() {
  const [auth, setAuth]           = useState(null);
  const [screen, setScreen]       = useState("login");
  const [page, setPage]           = useState("books");
  const [successMsg, setSuccessMsg] = useState("");   // registration → login message
  const [welcome, setWelcome]     = useState("");     // post-login toast

  const handleLogin = (a) => {
    setSuccessMsg("");
    setWelcome(`Welcome back, ${a.username}!`);
    setTimeout(() => setWelcome(""), 4000);
    setAuth(a);
    setPage("books");
  };

  const handleLogout = () => {
    if (auth?.token) api("/api/auth/logout", { method:"POST" }, auth.token).catch(() => {});
    setAuth(null);
    setScreen("login");
    setWelcome("");
  };

  return (
    <>
      <style>{CSS}</style>

      {/* Welcome toast — shown immediately after login, auto-hides after 4 s */}
      {welcome && (
        <div className="welcome-toast">✓ {welcome}</div>
      )}

      {!auth ? (
        screen === "login"
          ? <LoginPage
              onLogin={handleLogin}
              onRegister={() => { setSuccessMsg(""); setScreen("register"); }}
              successMessage={successMsg}
            />
          : <RegisterPage
              onBack={() => { setSuccessMsg(""); setScreen("login"); }}
              onSuccess={(msg) => { setSuccessMsg(msg); setScreen("login"); }}
            />
      ) : (
        <div className="app">
          <Sidebar auth={auth} page={page} onPage={setPage} onLogout={handleLogout} />
          {page === "books"        && <BooksPage auth={auth} />}
          {page === "loans"        && <LoansPage auth={auth} />}
          {page === "reservations" && <ReservationsPage auth={auth} />}
          {page === "fines"        && <FinesPage auth={auth} />}
          {page === "reports"      && <ReportsPage auth={auth} />}
          {page === "users"        && <UsersPage auth={auth} />}
          {page === "registry"     && <RegistryPage auth={auth} />}
        </div>
      )}
    </>
  );
}