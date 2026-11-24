from typing import List
from fastapi import APIRouter, Depends, Response, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import NoteCreateRequest, NoteOut, NoteUpdateRequest
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/notes", tags=["notes"])


@router.get("", response_model=List[NoteOut])
def list_notes(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.list_notes(user.id)


@router.post("", response_model=NoteOut, status_code=status.HTTP_201_CREATED)
def create_note(
    body: NoteCreateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.create_note(user.id, body.model_dump())


@router.patch("/{note_id}", response_model=NoteOut)
def update_note(
    note_id: str,
    body: NoteUpdateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_note(user.id, note_id, body.model_dump(exclude_unset=True))


@router.delete("/{note_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_note(
    note_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.delete_note(user.id, note_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
