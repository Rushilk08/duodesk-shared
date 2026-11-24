from fastapi import APIRouter, Depends, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import (
    AuthSessionResponse,
    LoginRequest,
    PasswordResetRequest,
    ProfileOut,
    ProfileUpdateRequest,
    RegisterRequest,
    UpdatePasswordRequest,
)
from ..services.supabase_service import SupabaseService

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register", response_model=AuthSessionResponse, status_code=status.HTTP_201_CREATED)
async def register(
    body: RegisterRequest,
    svc: SupabaseService = Depends(get_service),
):
    return await svc.register_user(
        email=body.email,
        password=body.password,
        name=body.name,
        full_name=body.full_name,
        role=body.role,
        invite_code=body.invite_code,
    )


@router.post("/login", response_model=AuthSessionResponse)
async def login(
    body: LoginRequest,
    svc: SupabaseService = Depends(get_service),
):
    return await svc.login_user(email=body.email, password=body.password)


@router.post("/logout")
async def logout(
    _user: AuthenticatedUser = Depends(get_current_user),
):
    return {"status": "signed_out"}


@router.post("/password-reset")
async def password_reset(
    body: PasswordResetRequest,
    svc: SupabaseService = Depends(get_service),
):
    await svc.send_password_reset(body.email)
    return {"status": "sent"}


@router.post("/update-password")
async def update_password(
    body: UpdatePasswordRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    await svc.update_password(user.id, body.new_password)
    return {"status": "updated"}


@router.get("/me", response_model=ProfileOut)
def get_me(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.get_profile(user.id)


@router.patch("/profile", response_model=ProfileOut)
def update_my_profile(
    body: ProfileUpdateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_profile(user.id, body.model_dump(exclude_unset=True))
