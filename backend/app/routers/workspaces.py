from fastapi import APIRouter, Depends
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import (
    InviteCreateRequest,
    InviteResponse,
    JoinWorkspaceRequest,
    WorkspaceOut,
)
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/workspaces", tags=["workspaces"])


@router.get("/current", response_model=WorkspaceOut)
def get_current_workspace(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.get_workspace_details(user.id)


@router.post("/invite", response_model=InviteResponse)
def create_workspace_invite(
    body: InviteCreateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.create_invitation(user.id, body.invitedEmail)


@router.post("/join", response_model=WorkspaceOut)
def join_workspace_by_invite(
    body: JoinWorkspaceRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.join_workspace(user.id, body.inviteCode)
