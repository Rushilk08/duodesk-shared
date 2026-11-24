from fastapi import Depends
from .config import Settings, get_settings
from ..services.supabase_service import SupabaseService, get_supabase_service


def get_service(settings: Settings = Depends(get_settings)) -> SupabaseService:
    return get_supabase_service(settings)
