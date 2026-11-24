from typing import List
from fastapi import APIRouter, Depends, Response, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import (
    AssignmentCreateRequest,
    AssignmentOut,
    AssignmentUpdateRequest,
)
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/assignments", tags=["assignments"])


@router.get("", response_model=List[AssignmentOut])
def list_assignments(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.list_assignments(user.id)


@router.post("", response_model=AssignmentOut, status_code=status.HTTP_201_CREATED)
def create_assignment(
    body: AssignmentCreateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.create_assignment(user.id, body.model_dump())


@router.patch("/{assignment_id}", response_model=AssignmentOut)
def update_assignment(
    assignment_id: str,
    body: AssignmentUpdateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_assignment(user.id, assignment_id, body.model_dump(exclude_unset=True))


@router.delete("/{assignment_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_assignment(
    assignment_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.delete_assignment(user.id, assignment_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
