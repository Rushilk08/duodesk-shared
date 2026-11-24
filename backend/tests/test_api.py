from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
import uuid
import pytest
from fastapi import HTTPException, status
from fastapi.testclient import TestClient

from app.core.deps import get_service
from app.core.security import AuthenticatedUser, get_current_user
from app.main import app
from app.services.supabase_service import ALLOWED_MIME_TYPES, MAX_FILE_BYTES, sanitize_filename


class FakeDuoDeskService:
    """In-memory test double mimicking SupabaseService access rules and 2-member cap."""

    def __init__(self) -> None:
        self.profiles: Dict[str, Dict[str, Any]] = {
            "user-1": {
                "id": "user-1",
                "name": "Rushil",
                "fullName": "Rushil Mehta",
                "email": "rushil@example.com",
                "role": "Owner",
                "avatarUrl": "",
                "accentHex": "#E684A7",
            },
            "user-2": {
                "id": "user-2",
                "name": "Alex",
                "fullName": "Alex Rivera",
                "email": "alex@example.com",
                "role": "Member",
                "avatarUrl": "",
                "accentHex": "#9B82F3",
            },
            "user-3": {
                "id": "user-3",
                "name": "Taylor",
                "fullName": "Taylor Kim",
                "email": "taylor@example.com",
                "role": "Guest",
                "avatarUrl": "",
                "accentHex": "#9B82F3",
            },
        }
        self.workspaces: Dict[str, Dict[str, Any]] = {
            "ws-1": {"id": "ws-1", "name": "DuoDesk Shared Space", "ownerId": "user-1"}
        }
        self.members: Dict[str, List[str]] = {"ws-1": ["user-1"]}
        self.invitations: Dict[str, Dict[str, Any]] = {}
        self.tasks: List[Dict[str, Any]] = []
        self.reminders: List[Dict[str, Any]] = []
        self.assignments: List[Dict[str, Any]] = []
        self.notes: List[Dict[str, Any]] = []
        self.documents: List[Dict[str, Any]] = []
        self.notifications: List[Dict[str, Any]] = []
        self.prefs: Dict[str, Dict[str, Any]] = {}

    def _ws_for(self, user_id: str) -> Optional[str]:
        for ws_id, m_ids in self.members.items():
            if user_id in m_ids:
                return ws_id
        return None

    def _can_access(self, item: Dict[str, Any], user_id: str) -> bool:
        if item["visibility"] == "private":
            return item["ownerId"] == user_id
        return item.get("workspaceId") == self._ws_for(user_id)

    def get_profile(self, user_id: str) -> Dict[str, Any]:
        return self.profiles[user_id]

    def update_profile(self, user_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        self.profiles[user_id].update({k: v for k, v in patch.items() if v is not None})
        return self.profiles[user_id]

    def get_workspace_details(self, user_id: str) -> Dict[str, Any]:
        ws_id = self._ws_for(user_id)
        if not ws_id:
            raise HTTPException(status_code=403, detail="Not in a workspace")
        ws = self.workspaces[ws_id]
        return {
            "id": ws["id"],
            "name": ws["name"],
            "ownerId": ws["ownerId"],
            "members": [self.profiles[uid] for uid in self.members[ws_id]],
        }

    def create_invitation(self, user_id: str, invited_email: Optional[str]) -> Dict[str, Any]:
        ws_id = self._ws_for(user_id)
        if not ws_id:
            raise HTTPException(status_code=403, detail="Not in a workspace")
        if len(self.members[ws_id]) >= 2:
            raise HTTPException(status_code=409, detail="Workspace already has 2 members.")
        code = "DUO-TESTCODE"
        self.invitations[code] = {
            "workspaceId": ws_id,
            "invitedEmail": invited_email,
            "used": False,
        }
        return {
            "inviteCode": code,
            "inviteUrl": f"http://localhost:3000/?invite={code}",
            "expiresAt": "2030-01-01T00:00:00Z",
            "workspaceId": ws_id,
        }

    def join_workspace(self, user_id: str, invite_code: str) -> Dict[str, Any]:
        inv = self.invitations.get(invite_code)
        if not inv or inv["used"]:
            raise HTTPException(status_code=400, detail="Invalid invite code")
        ws_id = inv["workspaceId"]
        if len(self.members[ws_id]) >= 2:
            raise HTTPException(status_code=409, detail="Workspace already has 2 authorized members.")
        self.members[ws_id].append(user_id)
        inv["used"] = True
        return self.get_workspace_details(user_id)

    def list_tasks(self, user_id: str) -> List[Dict[str, Any]]:
        return [t for t in self.tasks if self._can_access(t, user_id)]

    def create_task(self, user_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        now = datetime.now(timezone.utc).isoformat()
        ws_id = self._ws_for(user_id) if payload["visibility"] == "shared" else None
        task = {
            "id": str(uuid.uuid4()),
            "title": payload["title"],
            "description": payload.get("description"),
            "ownerId": user_id,
            "lastModifiedBy": user_id,
            "visibility": payload["visibility"],
            "workspaceId": ws_id,
            "priority": payload.get("priority", "medium"),
            "dueDate": payload["dueDate"],
            "dueTime": payload.get("dueTime"),
            "completed": payload.get("completed", False),
            "category": payload.get("category"),
            "attachments": [],
            "createdAt": now,
            "updatedAt": now,
        }
        self.tasks.insert(0, task)
        return task

    def update_task(self, user_id: str, task_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        for t in self.tasks:
            if t["id"] == task_id:
                if not self._can_access(t, user_id):
                    raise HTTPException(status_code=403, detail="Access denied.")
                t.update({k: v for k, v in patch.items() if v is not None})
                t["lastModifiedBy"] = user_id
                t["updatedAt"] = datetime.now(timezone.utc).isoformat()
                return t
        raise HTTPException(status_code=404, detail="Task not found.")

    def delete_task(self, user_id: str, task_id: str) -> None:
        for i, t in enumerate(self.tasks):
            if t["id"] == task_id:
                if not self._can_access(t, user_id):
                    raise HTTPException(status_code=403, detail="Access denied.")
                self.tasks.pop(i)
                return
        raise HTTPException(status_code=404, detail="Task not found.")

    def list_reminders(self, user_id: str) -> List[Dict[str, Any]]:
        return [r for r in self.reminders if self._can_access(r, user_id)]

    def list_assignments(self, user_id: str) -> List[Dict[str, Any]]:
        return [a for a in self.assignments if self._can_access(a, user_id)]

    def upload_file(
        self,
        user_id: str,
        raw_filename: str,
        mime_type: str,
        content_bytes: bytes,
        visibility: str,
        entity_type: str = "document",
        entity_id: Optional[str] = None,
        summary: Optional[str] = None,
    ) -> Dict[str, Any]:
        if len(content_bytes) == 0 or len(content_bytes) > MAX_FILE_BYTES:
            raise HTTPException(status_code=400, detail="Invalid size")
        if mime_type and mime_type not in ALLOWED_MIME_TYPES and mime_type != "application/octet-stream":
            raise HTTPException(status_code=400, detail="Unsupported MIME type")
        clean_name, kind, ext = sanitize_filename(raw_filename)
        doc = {
            "id": str(uuid.uuid4()),
            "name": clean_name,
            "kind": kind,
            "extension": ext,
            "mimeType": mime_type,
            "sizeBytes": len(content_bytes),
            "ownerId": user_id,
            "lastModifiedBy": user_id,
            "visibility": visibility,
            "workspaceId": self._ws_for(user_id) if visibility == "shared" else None,
            "uploadedAt": datetime.now(timezone.utc).isoformat(),
            "previewUrl": None,
            "downloadUrl": "https://signed.example.com/file",
            "summary": summary,
        }
        self.documents.append(doc)
        return doc

    def list_documents(self, user_id: str) -> List[Dict[str, Any]]:
        return [d for d in self.documents if self._can_access(d, user_id)]

    def get_signed_download_url(self, user_id: str, doc_id: str) -> str:
        for d in self.documents:
            if d["id"] == doc_id:
                if not self._can_access(d, user_id):
                    raise HTTPException(status_code=403, detail="Access denied.")
                return "https://signed.example.com/download-token"
        raise HTTPException(status_code=404, detail="File not found.")

    def get_calendar_events(
        self, user_id: str, start: Optional[str] = None, end: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        return [
            {
                "id": f"t-{t['id']}",
                "entityId": t["id"],
                "entityType": "task",
                "title": t["title"],
                "date": t["dueDate"],
                "time": t.get("dueTime"),
                "visibility": t["visibility"],
                "priority": t["priority"],
                "completed": t["completed"],
            }
            for t in self.list_tasks(user_id)
        ]


@pytest.fixture()
def api_env():
    svc = FakeDuoDeskService()
    active_user = {"id": "user-1", "email": "rushil@example.com"}

    def fake_current_user() -> AuthenticatedUser:
        return AuthenticatedUser(
            id=active_user["id"], email=active_user["email"], access_token="test-jwt"
        )

    app.dependency_overrides[get_service] = lambda: svc
    app.dependency_overrides[get_current_user] = fake_current_user
    client = TestClient(app)
    yield client, svc, active_user
    app.dependency_overrides.clear()


def test_unauthenticated_request_rejected():
    app.dependency_overrides.clear()
    client = TestClient(app)
    res = client.get("/api/tasks")
    assert res.status_code == status.HTTP_401_UNAUTHORIZED


def test_two_user_invite_and_member_cap(api_env):
    client, _svc, active_user = api_env

    # User 1 creates invite
    inv = client.post("/api/workspaces/invite", json={"invitedEmail": "alex@example.com"})
    assert inv.status_code == 200
    code = inv.json()["inviteCode"]

    # User 2 joins
    active_user["id"] = "user-2"
    joined = client.post("/api/workspaces/join", json={"inviteCode": code})
    assert joined.status_code == 200
    assert len(joined.json()["members"]) == 2

    # User 1 cannot generate another invite once 2/2 seats filled
    active_user["id"] = "user-1"
    capped = client.post("/api/workspaces/invite", json={})
    assert capped.status_code == 409


def test_private_isolation_and_shared_collaboration(api_env):
    client, svc, active_user = api_env
    svc.members["ws-1"].append("user-2")

    # User 1 creates private and shared task
    active_user["id"] = "user-1"
    priv_res = client.post(
        "/api/tasks",
        json={"title": "User 1 Secret Task", "visibility": "private", "dueDate": "2026-12-01"},
    )
    shared_res = client.post(
        "/api/tasks",
        json={"title": "Shared Sprint Review", "visibility": "shared", "dueDate": "2026-12-02"},
    )
    assert priv_res.status_code == 201
    assert shared_res.status_code == 201
    priv_id = priv_res.json()["id"]
    shared_id = shared_res.json()["id"]

    # User 2 only sees shared task, cannot update/delete private task
    active_user["id"] = "user-2"
    u2_tasks = client.get("/api/tasks").json()
    assert [t["title"] for t in u2_tasks] == ["Shared Sprint Review"]

    denied = client.patch(f"/api/tasks/{priv_id}", json={"completed": True})
    assert denied.status_code == 403

    # User 2 completes shared task -> lastModifiedBy updates to user-2
    completed = client.patch(f"/api/tasks/{shared_id}", json={"completed": True})
    assert completed.status_code == 200
    assert completed.json()["completed"] is True
    assert completed.json()["lastModifiedBy"] == "user-2"

    # Calendar endpoint returns User 2's accessible events only
    cal = client.get("/api/calendar/events").json()
    assert len(cal) == 1
    assert cal[0]["title"] == "Shared Sprint Review"


def test_file_upload_validation_and_private_signed_url_access(api_env):
    client, svc, active_user = api_env
    svc.members["ws-1"].append("user-2")

    # Reject disallowed extension
    bad = client.post(
        "/api/documents/upload",
        data={"visibility": "private"},
        files={"file": ("payload.exe", b"mz-data", "application/octet-stream")},
    )
    assert bad.status_code == 400

    # Accept valid private PDF for User 1
    ok = client.post(
        "/api/documents/upload",
        data={"visibility": "private", "summary": "Personal notes"},
        files={"file": ("notes.pdf", b"%PDF-1.4 content", "application/pdf")},
    )
    assert ok.status_code == 201
    doc_id = ok.json()["id"]

    # User 2 cannot fetch signed download URL for User 1's private document
    active_user["id"] = "user-2"
    forbidden = client.get(f"/api/documents/{doc_id}/download")
    assert forbidden.status_code == 403
