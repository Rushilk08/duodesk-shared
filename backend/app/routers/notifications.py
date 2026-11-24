from typing import List
from fastapi import APIRouter, Depends, Response, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import NotificationOut, NotificationSettingsOut
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/notifications", tags=["notifications"])


@router.get("", response_model=List[NotificationOut])
def list_notifications(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.list_notifications(user.id)


@router.post("/{notif_id}/read")
def mark_notification_read(
    notif_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.mark_notification_read(user.id, notif_id)
    return {"status": "ok"}


@router.post("/read-all")
def mark_all_notifications_read(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.mark_all_notifications_read(user.id)
    return {"status": "ok"}


@router.delete("", status_code=status.HTTP_204_NO_CONTENT)
def clear_notifications(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.clear_notifications(user.id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/preferences", response_model=NotificationSettingsOut)
def get_preferences(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.get_notification_preferences(user.id)


@router.put("/preferences", response_model=NotificationSettingsOut)
def update_preferences(
    body: NotificationSettingsOut,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_notification_preferences(user.id, body.model_dump())
