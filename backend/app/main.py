from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .core.config import get_settings
from .routers import (
    assignments,
    auth,
    calendar,
    documents,
    notes,
    notifications,
    reminders,
    tasks,
    workspaces,
)

settings = get_settings()

app = FastAPI(
    title="DuoDesk API",
    version="1.0.0",
    description="Two-user collaborative productivity backend for DuoDesk.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "supabaseConfigured": settings.is_supabase_configured,
    }


app.include_router(auth.router)
app.include_router(workspaces.router)
app.include_router(tasks.router)
app.include_router(reminders.router)
app.include_router(assignments.router)
app.include_router(notes.router)
app.include_router(documents.router)
app.include_router(calendar.router)
app.include_router(notifications.router)
