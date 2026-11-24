from typing import Any, Dict, List, Optional
from fastapi import APIRouter, Depends, Query
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/calendar", tags=["calendar"])


@router.get("/events", response_model=List[Dict[str, Any]])
def list_calendar_events(
    start: Optional[str] = Query(default=None),
    end: Optional[str] = Query(default=None),
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.get_calendar_events(user.id, start=start, end=end)
