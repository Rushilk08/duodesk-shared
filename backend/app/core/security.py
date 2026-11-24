from dataclasses import dataclass
from typing import Optional
import jwt
from fastapi import Depends, Header, HTTPException, status
from .config import Settings, get_settings


@dataclass
class AuthenticatedUser:
    id: str
    email: str
    access_token: str


def decode_supabase_jwt(token: str, settings: Settings) -> Optional[dict]:
    """Verify Supabase JWT locally when SUPABASE_JWT_SECRET is configured."""
    if not settings.supabase_jwt_secret:
        return None
    try:
        return jwt.decode(
            token,
            settings.supabase_jwt_secret,
            algorithms=["HS256"],
            options={"verify_aud": False},
        )
    except jwt.PyJWTError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token.",
        ) from exc


async def get_current_user(
    authorization: Optional[str] = Header(default=None),
    settings: Settings = Depends(get_settings),
) -> AuthenticatedUser:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing Bearer authentication token.",
        )

    token = authorization.removeprefix("Bearer ").strip()
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Empty authentication token.",
        )

    # 1. Local HS256 JWT verification if secret is set
    claims = decode_supabase_jwt(token, settings)
    if claims and claims.get("sub"):
        return AuthenticatedUser(
            id=str(claims["sub"]),
            email=str(claims.get("email", "")),
            access_token=token,
        )

    # 2. Fallback to Supabase Auth user verification endpoint
    from ..services.supabase_service import get_supabase_service

    svc = get_supabase_service(settings)
    user = await svc.verify_access_token(token)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired session.",
        )
    return AuthenticatedUser(id=user["id"], email=user["email"], access_token=token)
