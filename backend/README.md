# DuoDesk FastAPI + Supabase Backend

## 1. Supabase Setup
1. Create a Supabase project at <https://supabase.com>.
2. Open the **SQL Editor** in Supabase and run `/supabase/migrations/001_duodesk_schema_and_rls.sql`.
   - This creates all 13 PostgreSQL tables, `enforce_two_member_limit()` trigger, Row Level Security (RLS) policies, private/shared Storage buckets (`duodesk-private`, `duodesk-shared`), and enables Supabase Realtime for shared workspace tables.
3. In **Authentication -> URL Configuration**, add your frontend URL (e.g. `http://localhost:3000`) to **Redirect URLs** for password reset emails.

## 2. Environment Variables
Copy `.env.example` to `.env` in the project root (and/or inside `backend/.env`) and fill in your credentials:

Frontend (safe public values in `.env`):
- `VITE_API_BASE_URL=http://localhost:8000`
- `VITE_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co`
- `VITE_SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY`

Backend (keep private, never expose with `VITE_`):
- `SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co`
- `SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY=YOUR_SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_JWT_SECRET=YOUR_SUPABASE_JWT_SECRET`
- `ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000`
- `APP_URL=http://localhost:3000`

## 3. Run the FastAPI Server
```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## 4. Run Backend Tests
```bash
cd backend
pytest -v
```

## 5. Two-User Verification Checklist
1. Open the website and register **User 1** (leave Invite Code blank). User 1 gets a private workspace and owns the new DuoDesk shared workspace (`1 / 2 members`).
2. Go to **Profile -> DuoDesk Members** and generate an **Invite Code** (`DUO-...`).
3. Open a second browser/incognito window, register **User 2** with that Invite Code.
4. Create private items in **My Space** (confirm User 2 cannot see them) and shared items in **Shared Space** (confirm live sync via Supabase Realtime).
