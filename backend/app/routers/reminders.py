from typing import List
from fastapi import APIRouter, Depends, Response, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import ReminderCreateRequest, ReminderOut, ReminderUpdateRequest
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/reminders", tags=["reminders"])


@router.get("", response_model=List[ReminderOut])
def list_reminders(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.list_reminders(user.id)


@router.post("", response_model=ReminderOut, status_code=status.HTTP_201_CREATED)
def create_reminder(
    body: ReminderCreateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.create_reminder(user.id, body.model_dump())


@router.patch("/{reminder_id}", response_model=ReminderOut)
def update_reminder(
    reminder_id: str,
    body: ReminderUpdateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_reminder(user.id, reminder_id, body.model_dump(exclude_unset=True))


@router.delete("/{reminder_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_reminder(
    reminder_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.delete_reminder(user.id, reminder_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
