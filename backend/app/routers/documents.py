from typing import List, Literal, Optional
from fastapi import APIRouter, Depends, File, Form, HTTPException, Response, UploadFile, status
from ..core.deps import get_service
from ..core.security import AuthenticatedUser, get_current_user
from ..schemas.models import DocumentOut, DocumentUpdateRequest, Visibility
from ..services.supabase_service import MAX_FILE_BYTES, SupabaseService

router = APIRouter(prefix="/api/documents", tags=["documents"])


@router.get("", response_model=List[DocumentOut])
def list_documents(
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.list_documents(user.id)


@router.post("/upload", response_model=DocumentOut, status_code=status.HTTP_201_CREATED)
async def upload_document(
    file: UploadFile = File(...),
    visibility: Visibility = Form("private"),
    entity_type: Literal["document", "task", "assignment"] = Form("document"),
    entity_id: Optional[str] = Form(default=None),
    summary: Optional[str] = Form(default=None),
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    content = await file.read(MAX_FILE_BYTES + 1)
    if len(content) > MAX_FILE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="File exceeds 20 MB maximum upload limit.",
        )
    return svc.upload_file(
        user_id=user.id,
        raw_filename=file.filename or "document",
        mime_type=file.content_type or "application/octet-stream",
        content_bytes=content,
        visibility=visibility,
        entity_type=entity_type,
        entity_id=entity_id,
        summary=summary,
    )


@router.get("/{doc_id}/download")
def download_document(
    doc_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return {"url": svc.get_signed_download_url(user.id, doc_id)}


@router.patch("/{doc_id}", response_model=DocumentOut)
def update_document(
    doc_id: str,
    body: DocumentUpdateRequest,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    return svc.update_document(user.id, doc_id, body.model_dump(exclude_unset=True))


@router.delete("/{doc_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_document(
    doc_id: str,
    user: AuthenticatedUser = Depends(get_current_user),
    svc: SupabaseService = Depends(get_service),
):
    svc.delete_document(user.id, doc_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
