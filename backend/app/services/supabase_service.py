import os
import re
import secrets
from datetime import datetime, timedelta, timezone
from pathlib import PurePosixPath
from typing import Any, Dict, List, Optional, Tuple
from fastapi import HTTPException, status
from supabase import Client, create_client
from ..core.config import Settings

ALLOWED_EXTENSIONS = {"pdf", "doc", "docx", "png", "jpg", "jpeg", "webp"}
ALLOWED_MIME_TYPES = {
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/png",
    "image/jpeg",
    "image/webp",
}
MAX_FILE_BYTES = 20 * 1024 * 1024  # 20 MB


def sanitize_filename(raw_name: str) -> Tuple[str, str, str]:
    base = PurePosixPath(raw_name.replace("\\", "/")).name
    cleaned = re.sub(r"[^A-Za-z0-9._-]", "_", base).strip("._") or "document"
    ext = cleaned.rsplit(".", 1)[-1].lower() if "." in cleaned else ""
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file type '.{ext}'. Allowed: PDF, DOC, DOCX, PNG, JPG, JPEG.",
        )
    kind = "pdf" if ext == "pdf" else "image" if ext in {"png", "jpg", "jpeg", "webp"} else "doc"
    return cleaned, kind, ext.upper()


class SupabaseService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self._admin_client: Optional[Client] = None
        self._anon_client: Optional[Client] = None

    @property
    def admin(self) -> Client:
        if not self.settings.is_supabase_configured:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Supabase credentials are not configured on the backend.",
            )
        if self._admin_client is None:
            self._admin_client = create_client(
                self.settings.supabase_url, self.settings.supabase_service_role_key
            )
        return self._admin_client

    @property
    def anon(self) -> Client:
        if not self.settings.supabase_url or not self.settings.supabase_anon_key:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Supabase URL or anon key is not configured.",
            )
        if self._anon_client is None:
            self._anon_client = create_client(
                self.settings.supabase_url, self.settings.supabase_anon_key
            )
        return self._anon_client

    # -------------------------------------------------------------------------
    # AUTHENTICATION
    # -------------------------------------------------------------------------
    async def verify_access_token(self, token: str) -> Optional[Dict[str, str]]:
        res = self.admin.auth.get_user(token)
        if not res or not res.user:
            return None
        return {"id": str(res.user.id), "email": res.user.email or ""}

    async def register_user(
        self,
        email: str,
        password: str,
        name: str,
        full_name: Optional[str],
        role: str,
        invite_code: Optional[str],
    ) -> Dict[str, Any]:
        # Validate invite code before creating account if supplied
        invite_row = None
        if invite_code:
            inv_res = (
                self.admin.table("workspace_invitations")
                .select("*")
                .eq("invite_code", invite_code.strip())
                .is_("accepted_at", "null")
                .execute()
            )
            if not inv_res.data:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Invalid or already-used workspace invitation code.",
                )
            invite_row = inv_res.data[0]
            if invite_row["expires_at"] < datetime.now(timezone.utc).isoformat():
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Invitation code has expired.",
                )
            members_res = (
                self.admin.table("workspace_members")
                .select("id")
                .eq("workspace_id", invite_row["workspace_id"])
                .execute()
            )
            if len(members_res.data or []) >= 2:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This DuoDesk workspace already has 2 members.",
                )

        auth_res = self.anon.auth.sign_up({"email": email, "password": password})
        if not auth_res.user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Unable to register user with Supabase Auth.",
            )

        user_id = str(auth_res.user.id)
        profile_payload = {
            "id": user_id,
            "name": name.strip(),
            "full_name": (full_name or name).strip(),
            "email": email.lower().strip(),
            "role": role.strip() or "Member",
            "avatar_url": "",
            "accent_hex": "#9B82F3" if invite_row else "#E684A7",
        }
        self.admin.table("profiles").upsert(profile_payload).execute()
        self.admin.table("notification_preferences").upsert({"user_id": user_id}).execute()

        workspace_id: Optional[str] = None
        if invite_row:
            workspace_id = invite_row["workspace_id"]
            self.admin.table("workspace_members").insert(
                {"workspace_id": workspace_id, "user_id": user_id, "role": "member"}
            ).execute()
            self.admin.table("workspace_invitations").update(
                {
                    "accepted_by": user_id,
                    "accepted_at": datetime.now(timezone.utc).isoformat(),
                }
            ).eq("id", invite_row["id"]).execute()
        else:
            ws_res = (
                self.admin.table("workspaces")
                .insert({"name": "DuoDesk Shared Space", "owner_id": user_id})
                .execute()
            )
            workspace_id = ws_res.data[0]["id"]
            self.admin.table("workspace_members").insert(
                {"workspace_id": workspace_id, "user_id": user_id, "role": "owner"}
            ).execute()

        access_token = auth_res.session.access_token if auth_res.session else ""
        refresh_token = auth_res.session.refresh_token if auth_res.session else None
        return {
            "accessToken": access_token,
            "refreshToken": refresh_token,
            "user": self._map_profile(profile_payload),
            "workspaceId": workspace_id,
        }

    async def login_user(self, email: str, password: str) -> Dict[str, Any]:
        try:
            auth_res = self.anon.auth.sign_in_with_password(
                {"email": email.lower().strip(), "password": password}
            )
        except Exception as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid email or password.",
            ) from exc

        if not auth_res.session or not auth_res.user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid email or password.",
            )

        user_id = str(auth_res.user.id)
        profile = self.get_profile(user_id)
        ws_id = self.get_user_workspace_id(user_id)
        return {
            "accessToken": auth_res.session.access_token,
            "refreshToken": auth_res.session.refresh_token,
            "user": profile,
            "workspaceId": ws_id,
        }

    async def send_password_reset(self, email: str) -> None:
        self.anon.auth.reset_password_email(
            email.lower().strip(),
            {"redirect_to": f"{self.settings.app_url}/login"},
        )

    async def update_password(self, user_id: str, new_password: str) -> None:
        self.admin.auth.admin.update_user_by_id(user_id, {"password": new_password})

    # -------------------------------------------------------------------------
    # PROFILES & WORKSPACE MEMBERSHIP
    # -------------------------------------------------------------------------
    def _map_profile(self, row: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "id": str(row["id"]),
            "name": row["name"],
            "fullName": row.get("full_name", row["name"]),
            "email": row["email"],
            "role": row.get("role", "Member"),
            "avatarUrl": row.get("avatar_url", ""),
            "accentHex": row.get("accent_hex", "#9B82F3"),
        }

    def get_profile(self, user_id: str) -> Dict[str, Any]:
        res = self.admin.table("profiles").select("*").eq("id", user_id).execute()
        if not res.data:
            raise HTTPException(status_code=404, detail="Profile not found.")
        return self._map_profile(res.data[0])

    def update_profile(self, user_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        db_patch: Dict[str, Any] = {}
        if "name" in patch and patch["name"] is not None:
            db_patch["name"] = patch["name"]
        if "fullName" in patch and patch["fullName"] is not None:
            db_patch["full_name"] = patch["fullName"]
        if "email" in patch and patch["email"] is not None:
            db_patch["email"] = patch["email"]
        if "role" in patch and patch["role"] is not None:
            db_patch["role"] = patch["role"]
        if "avatarUrl" in patch and patch["avatarUrl"] is not None:
            db_patch["avatar_url"] = patch["avatarUrl"]

        res = self.admin.table("profiles").update(db_patch).eq("id", user_id).execute()
        if not res.data:
            raise HTTPException(status_code=404, detail="Profile not found.")
        return self._map_profile(res.data[0])

    def get_user_workspace_id(self, user_id: str) -> Optional[str]:
        res = (
            self.admin.table("workspace_members")
            .select("workspace_id")
            .eq("user_id", user_id)
            .limit(1)
            .execute()
        )
        if res.data:
            return str(res.data[0]["workspace_id"])
        return None

    def require_workspace_id(self, user_id: str) -> str:
        ws_id = self.get_user_workspace_id(user_id)
        if not ws_id:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="User does not belong to a DuoDesk workspace yet.",
            )
        return ws_id

    def get_workspace_details(self, user_id: str) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id)
        ws_res = self.admin.table("workspaces").select("*").eq("id", ws_id).single().execute()
        members_res = (
            self.admin.table("workspace_members")
            .select("user_id, profiles(*)")
            .eq("workspace_id", ws_id)
            .execute()
        )
        members = [self._map_profile(m["profiles"]) for m in (members_res.data or []) if m.get("profiles")]
        return {
            "id": str(ws_res.data["id"]),
            "name": ws_res.data["name"],
            "ownerId": str(ws_res.data["owner_id"]),
            "members": members,
        }

    def create_invitation(self, user_id: str, invited_email: Optional[str]) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id)
        members_res = (
            self.admin.table("workspace_members")
            .select("id")
            .eq("workspace_id", ws_id)
            .execute()
        )
        if len(members_res.data or []) >= 2:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Your DuoDesk workspace already has both members (2 / 2 seats).",
            )
        code = f"DUO-{secrets.token_urlsafe(6).upper()}"
        expires_at = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat()
        self.admin.table("workspace_invitations").insert(
            {
                "workspace_id": ws_id,
                "invited_by": user_id,
                "invite_code": code,
                "invited_email": invited_email.lower().strip() if invited_email else None,
                "expires_at": expires_at,
            }
        ).execute()
        return {
            "inviteCode": code,
            "inviteUrl": f"{self.settings.app_url}/?invite={code}",
            "expiresAt": expires_at,
            "workspaceId": ws_id,
        }

    def join_workspace(self, user_id: str, invite_code: str) -> Dict[str, Any]:
        inv_res = (
            self.admin.table("workspace_invitations")
            .select("*")
            .eq("invite_code", invite_code.strip())
            .is_("accepted_at", "null")
            .execute()
        )
        if not inv_res.data:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or already-used invitation code.",
            )
        inv = inv_res.data[0]
        if inv["expires_at"] < datetime.now(timezone.utc).isoformat():
            raise HTTPException(status_code=400, detail="Invitation code expired.")

        ws_id = str(inv["workspace_id"])
        members_res = (
            self.admin.table("workspace_members")
            .select("user_id")
            .eq("workspace_id", ws_id)
            .execute()
        )
        existing_ids = {str(m["user_id"]) for m in (members_res.data or [])}
        if user_id in existing_ids:
            return self.get_workspace_details(user_id)
        if len(existing_ids) >= 2:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Workspace already has 2 authorized members.",
            )

        # Move user from solo placeholder workspace if any, and join invited workspace
        self.admin.table("workspace_members").delete().eq("user_id", user_id).execute()
        self.admin.table("workspace_members").insert(
            {"workspace_id": ws_id, "user_id": user_id, "role": "member"}
        ).execute()
        self.admin.table("workspace_invitations").update(
            {
                "accepted_by": user_id,
                "accepted_at": datetime.now(timezone.utc).isoformat(),
            }
        ).eq("id", inv["id"]).execute()
        return self.get_workspace_details(user_id)

    # -------------------------------------------------------------------------
    # ACCESS CONTROL HELPERS
    # -------------------------------------------------------------------------
    def _can_access_record(self, row: Dict[str, Any], user_id: str, ws_id: Optional[str]) -> bool:
        if row["visibility"] == "private":
            return str(row["owner_id"]) == user_id
        return ws_id is not None and str(row.get("workspace_id")) == ws_id

    def _emit_shared_notification(
        self,
        ws_id: str,
        actor_id: str,
        title: str,
        message: str,
        category: str,
        target_route: str,
    ) -> None:
        self.admin.table("notifications").insert(
            {
                "workspace_id": ws_id,
                "actor_id": actor_id,
                "visibility": "shared",
                "title": title,
                "message": message,
                "category": category,
                "target_route": target_route,
                "read": False,
            }
        ).execute()

    # -------------------------------------------------------------------------
    # TASKS
    # -------------------------------------------------------------------------
    def list_tasks(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        q = self.admin.table("tasks").select("*").order("created_at", desc=True)
        rows = q.execute().data or []
        visible = [r for r in rows if self._can_access_record(r, user_id, ws_id)]
        att_map = self._load_attachments_map("task", [r["id"] for r in visible])
        return [self._map_task(r, att_map.get(r["id"], [])) for r in visible]

    def create_task(self, user_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id) if payload["visibility"] == "shared" else self.get_user_workspace_id(user_id)
        row = {
            "workspace_id": ws_id if payload["visibility"] == "shared" else None,
            "owner_id": user_id,
            "last_modified_by": user_id,
            "visibility": payload["visibility"],
            "title": payload["title"],
            "description": payload.get("description"),
            "priority": payload.get("priority", "medium"),
            "due_date": payload["dueDate"],
            "due_time": payload.get("dueTime"),
            "completed": payload.get("completed", False),
            "category": payload.get("category"),
        }
        res = self.admin.table("tasks").insert(row).execute()
        created = res.data[0]
        if created["visibility"] == "shared" and ws_id:
            actor = self.get_profile(user_id)
            self._emit_shared_notification(
                ws_id,
                user_id,
                f"New shared task from {actor['name']}",
                created["title"],
                "shared_task",
                "/shared-space",
            )
        return self._map_task(created, [])

    def update_task(self, user_id: str, task_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("tasks").select("*").eq("id", task_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Task not found.")
        existing = existing_res.data[0]
        if not self._can_access_record(existing, user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")

        db_patch: Dict[str, Any] = {"last_modified_by": user_id}
        mapping = {
            "title": "title",
            "description": "description",
            "visibility": "visibility",
            "priority": "priority",
            "dueDate": "due_date",
            "dueTime": "due_time",
            "completed": "completed",
            "category": "category",
        }
        for k, col in mapping.items():
            if k in patch and patch[k] is not None:
                db_patch[col] = patch[k]
        if db_patch.get("visibility") == "shared":
            db_patch["workspace_id"] = self.require_workspace_id(user_id)
        elif db_patch.get("visibility") == "private":
            db_patch["workspace_id"] = None

        res = self.admin.table("tasks").update(db_patch).eq("id", task_id).execute()
        updated = res.data[0]
        if updated["visibility"] == "shared" and ws_id and patch.get("completed") is True and not existing["completed"]:
            actor = self.get_profile(user_id)
            self._emit_shared_notification(
                ws_id,
                user_id,
                f"{actor['name']} completed a shared task",
                updated["title"],
                "completed",
                "/shared-space",
            )
        att_map = self._load_attachments_map("task", [task_id])
        return self._map_task(updated, att_map.get(task_id, []))

    def delete_task(self, user_id: str, task_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("tasks").select("*").eq("id", task_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Task not found.")
        if not self._can_access_record(existing_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        self.admin.table("tasks").delete().eq("id", task_id).execute()

    def _map_task(self, r: Dict[str, Any], attachments: List[Dict[str, Any]]) -> Dict[str, Any]:
        return {
            "id": str(r["id"]),
            "title": r["title"],
            "description": r.get("description"),
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "priority": r["priority"],
            "dueDate": str(r["due_date"]),
            "dueTime": str(r["due_time"])[:5] if r.get("due_time") else None,
            "completed": bool(r["completed"]),
            "category": r.get("category"),
            "attachments": attachments,
            "createdAt": str(r["created_at"]),
            "updatedAt": str(r["updated_at"]),
        }

    # -------------------------------------------------------------------------
    # REMINDERS
    # -------------------------------------------------------------------------
    def list_reminders(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        rows = self.admin.table("reminders").select("*").order("remind_date").execute().data or []
        return [self._map_reminder(r) for r in rows if self._can_access_record(r, user_id, ws_id)]

    def create_reminder(self, user_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id) if payload["visibility"] == "shared" else None
        row = {
            "workspace_id": ws_id,
            "owner_id": user_id,
            "last_modified_by": user_id,
            "visibility": payload["visibility"],
            "title": payload["title"],
            "description": payload.get("description", ""),
            "remind_date": payload["date"],
            "remind_time": payload["time"],
            "priority": payload.get("priority", "medium"),
            "repeat_option": payload.get("repeat", "none"),
            "notification_pref": payload.get("notificationPref", "push_in_app"),
            "completed": payload.get("completed", False),
        }
        res = self.admin.table("reminders").insert(row).execute()
        return self._map_reminder(res.data[0])

    def update_reminder(self, user_id: str, rem_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("reminders").select("*").eq("id", rem_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Reminder not found.")
        if not self._can_access_record(existing_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        db_patch: Dict[str, Any] = {"last_modified_by": user_id}
        mapping = {
            "title": "title",
            "description": "description",
            "visibility": "visibility",
            "date": "remind_date",
            "time": "remind_time",
            "priority": "priority",
            "repeat": "repeat_option",
            "notificationPref": "notification_pref",
            "completed": "completed",
        }
        for k, col in mapping.items():
            if k in patch and patch[k] is not None:
                db_patch[col] = patch[k]
        if db_patch.get("visibility") == "shared":
            db_patch["workspace_id"] = self.require_workspace_id(user_id)
        elif db_patch.get("visibility") == "private":
            db_patch["workspace_id"] = None
        res = self.admin.table("reminders").update(db_patch).eq("id", rem_id).execute()
        return self._map_reminder(res.data[0])

    def delete_reminder(self, user_id: str, rem_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("reminders").select("*").eq("id", rem_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Reminder not found.")
        if not self._can_access_record(existing_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        self.admin.table("reminders").delete().eq("id", rem_id).execute()

    def _map_reminder(self, r: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "id": str(r["id"]),
            "title": r["title"],
            "description": r.get("description", ""),
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "date": str(r["remind_date"]),
            "time": str(r["remind_time"])[:5],
            "priority": r["priority"],
            "repeat": r["repeat_option"],
            "notificationPref": r["notification_pref"],
            "completed": bool(r["completed"]),
            "createdAt": str(r["created_at"]),
        }

    # -------------------------------------------------------------------------
    # ASSIGNMENTS
    # -------------------------------------------------------------------------
    def list_assignments(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        rows = self.admin.table("assignments").select("*").order("deadline").execute().data or []
        visible = [r for r in rows if self._can_access_record(r, user_id, ws_id)]
        att_map = self._load_attachments_map("assignment", [r["id"] for r in visible])
        return [self._map_assignment(r, att_map.get(r["id"], [])) for r in visible]

    def create_assignment(self, user_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id) if payload["visibility"] == "shared" else None
        row = {
            "workspace_id": ws_id,
            "owner_id": user_id,
            "last_modified_by": user_id,
            "visibility": payload["visibility"],
            "title": payload["title"],
            "subject": payload.get("subject", "General"),
            "description": payload.get("description", ""),
            "deadline": payload["deadline"],
            "deadline_time": payload.get("deadlineTime"),
            "priority": payload.get("priority", "medium"),
            "status": payload.get("status", "pending"),
        }
        res = self.admin.table("assignments").insert(row).execute()
        created = res.data[0]
        if created["visibility"] == "shared" and ws_id:
            actor = self.get_profile(user_id)
            self._emit_shared_notification(
                ws_id,
                user_id,
                f"New shared assignment from {actor['name']}",
                f"{created['title']} ({created['subject']})",
                "shared_task",
                "/shared-space",
            )
        return self._map_assignment(created, [])

    def update_assignment(self, user_id: str, asg_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("assignments").select("*").eq("id", asg_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Assignment not found.")
        if not self._can_access_record(existing_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        db_patch: Dict[str, Any] = {"last_modified_by": user_id}
        mapping = {
            "title": "title",
            "subject": "subject",
            "description": "description",
            "visibility": "visibility",
            "deadline": "deadline",
            "deadlineTime": "deadline_time",
            "priority": "priority",
            "status": "status",
        }
        for k, col in mapping.items():
            if k in patch and patch[k] is not None:
                db_patch[col] = patch[k]
        if db_patch.get("visibility") == "shared":
            db_patch["workspace_id"] = self.require_workspace_id(user_id)
        elif db_patch.get("visibility") == "private":
            db_patch["workspace_id"] = None
        res = self.admin.table("assignments").update(db_patch).eq("id", asg_id).execute()
        att_map = self._load_attachments_map("assignment", [asg_id])
        return self._map_assignment(res.data[0], att_map.get(asg_id, []))

    def delete_assignment(self, user_id: str, asg_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        existing_res = self.admin.table("assignments").select("*").eq("id", asg_id).execute()
        if not existing_res.data:
            raise HTTPException(status_code=404, detail="Assignment not found.")
        if not self._can_access_record(existing_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        self.admin.table("assignments").delete().eq("id", asg_id).execute()

    def _map_assignment(self, r: Dict[str, Any], attachments: List[Dict[str, Any]]) -> Dict[str, Any]:
        return {
            "id": str(r["id"]),
            "title": r["title"],
            "subject": r["subject"],
            "description": r.get("description", ""),
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "deadline": str(r["deadline"]),
            "deadlineTime": str(r["deadline_time"])[:5] if r.get("deadline_time") else None,
            "priority": r["priority"],
            "status": r["status"],
            "attachments": attachments,
            "createdAt": str(r["created_at"]),
        }

    # -------------------------------------------------------------------------
    # NOTES & TODO LISTS / CHECKLISTS
    # -------------------------------------------------------------------------
    def list_notes(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        note_rows = self.admin.table("notes").select("*").execute().data or []
        todo_rows = self.admin.table("todo_lists").select("*, checklist_items(*)").execute().data or []
        out: List[Dict[str, Any]] = []
        for r in note_rows:
            if self._can_access_record(r, user_id, ws_id):
                out.append(self._map_note(r))
        for t in todo_rows:
            if self._can_access_record(t, user_id, ws_id):
                out.append(self._map_todo_list(t))
        out.sort(key=lambda x: x["updatedAt"], reverse=True)
        return out

    def create_note(self, user_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.require_workspace_id(user_id) if payload["visibility"] == "shared" else None
        if payload.get("type") == "checklist":
            row = {
                "workspace_id": ws_id,
                "owner_id": user_id,
                "last_modified_by": user_id,
                "visibility": payload["visibility"],
                "title": payload["title"],
                "description": payload.get("content", ""),
            }
            created = self.admin.table("todo_lists").insert(row).execute().data[0]
            items_in = payload.get("checklist", [])
            if items_in:
                self.admin.table("checklist_items").insert(
                    [
                        {
                            "todo_list_id": created["id"],
                            "text": item["text"],
                            "checked": bool(item.get("checked", False)),
                            "position": idx,
                        }
                        for idx, item in enumerate(items_in)
                    ]
                ).execute()
            full = (
                self.admin.table("todo_lists")
                .select("*, checklist_items(*)")
                .eq("id", created["id"])
                .single()
                .execute()
                .data
            )
            return self._map_todo_list(full)
        else:
            row = {
                "workspace_id": ws_id,
                "owner_id": user_id,
                "last_modified_by": user_id,
                "visibility": payload["visibility"],
                "title": payload["title"],
                "content": payload.get("content", ""),
            }
            created = self.admin.table("notes").insert(row).execute().data[0]
            return self._map_note(created)

    def update_note(self, user_id: str, note_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.get_user_workspace_id(user_id)
        # Check notes table first, then todo_lists
        n_res = self.admin.table("notes").select("*").eq("id", note_id).execute()
        if n_res.data:
            if not self._can_access_record(n_res.data[0], user_id, ws_id):
                raise HTTPException(status_code=403, detail="Access denied.")
            db_patch: Dict[str, Any] = {"last_modified_by": user_id}
            if "title" in patch and patch["title"] is not None:
                db_patch["title"] = patch["title"]
            if "content" in patch and patch["content"] is not None:
                db_patch["content"] = patch["content"]
            if "visibility" in patch and patch["visibility"] is not None:
                db_patch["visibility"] = patch["visibility"]
                db_patch["workspace_id"] = self.require_workspace_id(user_id) if patch["visibility"] == "shared" else None
            updated = self.admin.table("notes").update(db_patch).eq("id", note_id).execute().data[0]
            return self._map_note(updated)

        t_res = self.admin.table("todo_lists").select("*").eq("id", note_id).execute()
        if not t_res.data:
            raise HTTPException(status_code=404, detail="Note or checklist not found.")
        if not self._can_access_record(t_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")

        db_patch = {"last_modified_by": user_id}
        if "title" in patch and patch["title"] is not None:
            db_patch["title"] = patch["title"]
        if "content" in patch and patch["content"] is not None:
            db_patch["description"] = patch["content"]
        if "visibility" in patch and patch["visibility"] is not None:
            db_patch["visibility"] = patch["visibility"]
            db_patch["workspace_id"] = self.require_workspace_id(user_id) if patch["visibility"] == "shared" else None
        self.admin.table("todo_lists").update(db_patch).eq("id", note_id).execute()

        if "checklist" in patch and patch["checklist"] is not None:
            self.admin.table("checklist_items").delete().eq("todo_list_id", note_id).execute()
            if patch["checklist"]:
                self.admin.table("checklist_items").insert(
                    [
                        {
                            "todo_list_id": note_id,
                            "text": item["text"],
                            "checked": bool(item.get("checked", False)),
                            "position": idx,
                        }
                        for idx, item in enumerate(patch["checklist"])
                    ]
                ).execute()

        full = (
            self.admin.table("todo_lists")
            .select("*, checklist_items(*)")
            .eq("id", note_id)
            .single()
            .execute()
            .data
        )
        return self._map_todo_list(full)

    def delete_note(self, user_id: str, note_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        n_res = self.admin.table("notes").select("*").eq("id", note_id).execute()
        if n_res.data:
            if not self._can_access_record(n_res.data[0], user_id, ws_id):
                raise HTTPException(status_code=403, detail="Access denied.")
            self.admin.table("notes").delete().eq("id", note_id).execute()
            return
        t_res = self.admin.table("todo_lists").select("*").eq("id", note_id).execute()
        if not t_res.data:
            raise HTTPException(status_code=404, detail="Note not found.")
        if not self._can_access_record(t_res.data[0], user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        self.admin.table("todo_lists").delete().eq("id", note_id).execute()

    def _map_note(self, r: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "id": str(r["id"]),
            "title": r["title"],
            "content": r.get("content", ""),
            "type": "note",
            "checklist": [],
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "updatedAt": str(r["updated_at"]),
            "createdAt": str(r["created_at"]),
        }

    def _map_todo_list(self, r: Dict[str, Any]) -> Dict[str, Any]:
        items = sorted(r.get("checklist_items") or [], key=lambda c: c.get("position", 0))
        return {
            "id": str(r["id"]),
            "title": r["title"],
            "content": r.get("description", ""),
            "type": "checklist",
            "checklist": [
                {"id": str(c["id"]), "text": c["text"], "checked": bool(c["checked"])}
                for c in items
            ],
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "updatedAt": str(r["updated_at"]),
            "createdAt": str(r["created_at"]),
        }

    # -------------------------------------------------------------------------
    # DOCUMENTS & STORAGE (SIGNED URLS)
    # -------------------------------------------------------------------------
    def _load_attachments_map(self, entity_type: str, entity_ids: List[str]) -> Dict[str, List[Dict[str, Any]]]:
        if not entity_ids:
            return {}
        rows = (
            self.admin.table("attachments")
            .select("*")
            .eq("entity_type", entity_type)
            .in_("entity_id", entity_ids)
            .execute()
            .data
            or []
        )
        out: Dict[str, List[Dict[str, Any]]] = {}
        for r in rows:
            eid = str(r["entity_id"])
            out.setdefault(eid, []).append(
                {
                    "id": str(r["id"]),
                    "name": r["file_name"],
                    "sizeBytes": int(r["size_bytes"]),
                    "kind": r["file_kind"],
                    "mimeType": r["mime_type"],
                    "previewUrl": self._signed_url(r["storage_bucket"], r["storage_path"])
                    if r["file_kind"] == "image"
                    else None,
                }
            )
        return out

    def _signed_url(self, bucket: str, path: str, expires_in: int = 3600) -> Optional[str]:
        try:
            res = self.admin.storage.from_(bucket).create_signed_url(path, expires_in)
            if isinstance(res, dict):
                return res.get("signedURL") or res.get("signedUrl")
        except Exception:
            return None
        return None

    def list_documents(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        rows = (
            self.admin.table("attachments")
            .select("*")
            .eq("entity_type", "document")
            .order("created_at", desc=True)
            .execute()
            .data
            or []
        )
        return [
            self._map_document(r)
            for r in rows
            if self._can_access_record(r, user_id, ws_id)
        ]

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
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="File must be between 1 byte and 20 MB.",
            )
        if mime_type and mime_type not in ALLOWED_MIME_TYPES and mime_type != "application/octet-stream":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Unsupported file MIME type.",
            )
        clean_name, file_kind, extension = sanitize_filename(raw_filename)
        ws_id = self.require_workspace_id(user_id) if visibility == "shared" else self.get_user_workspace_id(user_id)

        if entity_type in {"task", "assignment"} and entity_id:
            table_name = "tasks" if entity_type == "task" else "assignments"
            parent_res = self.admin.table(table_name).select("*").eq("id", entity_id).execute()
            if not parent_res.data:
                raise HTTPException(status_code=404, detail=f"{entity_type.capitalize()} not found.")
            if not self._can_access_record(parent_res.data[0], user_id, ws_id):
                raise HTTPException(status_code=403, detail="Access denied to target entity.")

        bucket = (
            self.settings.storage_shared_bucket
            if visibility == "shared"
            else self.settings.storage_private_bucket
        )
        prefix = ws_id if visibility == "shared" else user_id
        unique_name = f"{secrets.token_hex(6)}_{clean_name}"
        storage_path = f"{prefix}/{unique_name}"

        self.admin.storage.from_(bucket).upload(
            storage_path,
            content_bytes,
            file_options={"content-type": mime_type or "application/octet-stream"},
        )

        row = {
            "workspace_id": ws_id if visibility == "shared" else None,
            "owner_id": user_id,
            "last_modified_by": user_id,
            "visibility": visibility,
            "entity_type": entity_type,
            "entity_id": entity_id,
            "file_name": clean_name,
            "file_kind": file_kind,
            "extension": extension,
            "mime_type": mime_type or "application/octet-stream",
            "size_bytes": len(content_bytes),
            "storage_bucket": bucket,
            "storage_path": storage_path,
            "summary": summary,
        }
        created = self.admin.table("attachments").insert(row).execute().data[0]
        if visibility == "shared" and ws_id and entity_type == "document":
            actor = self.get_profile(user_id)
            self._emit_shared_notification(
                ws_id,
                user_id,
                f"{actor['name']} uploaded {clean_name}",
                "Available in Shared Space.",
                "file",
                "/shared-space",
            )
        return self._map_document(created)

    def get_signed_download_url(self, user_id: str, doc_id: str) -> str:
        ws_id = self.get_user_workspace_id(user_id)
        res = self.admin.table("attachments").select("*").eq("id", doc_id).execute()
        if not res.data:
            raise HTTPException(status_code=404, detail="File not found.")
        row = res.data[0]
        if not self._can_access_record(row, user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        url = self._signed_url(row["storage_bucket"], row["storage_path"], 300)
        if not url:
            raise HTTPException(status_code=500, detail="Unable to generate signed download URL.")
        return url

    def update_document(self, user_id: str, doc_id: str, patch: Dict[str, Any]) -> Dict[str, Any]:
        ws_id = self.get_user_workspace_id(user_id)
        res = self.admin.table("attachments").select("*").eq("id", doc_id).execute()
        if not res.data:
            raise HTTPException(status_code=404, detail="Document not found.")
        row = res.data[0]
        if not self._can_access_record(row, user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        db_patch: Dict[str, Any] = {"last_modified_by": user_id}
        if "summary" in patch and patch["summary"] is not None:
            db_patch["summary"] = patch["summary"]
        if "visibility" in patch and patch["visibility"] is not None:
            db_patch["visibility"] = patch["visibility"]
            db_patch["workspace_id"] = self.require_workspace_id(user_id) if patch["visibility"] == "shared" else None
        updated = self.admin.table("attachments").update(db_patch).eq("id", doc_id).execute().data[0]
        return self._map_document(updated)

    def delete_document(self, user_id: str, doc_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        res = self.admin.table("attachments").select("*").eq("id", doc_id).execute()
        if not res.data:
            raise HTTPException(status_code=404, detail="Document not found.")
        row = res.data[0]
        if not self._can_access_record(row, user_id, ws_id):
            raise HTTPException(status_code=403, detail="Access denied.")
        try:
            self.admin.storage.from_(row["storage_bucket"]).remove([row["storage_path"]])
        except Exception:
            pass
        self.admin.table("attachments").delete().eq("id", doc_id).execute()

    def get_calendar_events(
        self, user_id: str, start: Optional[str] = None, end: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        events: List[Dict[str, Any]] = []
        for t in self.list_tasks(user_id):
            d = t["dueDate"]
            if (not start or d >= start) and (not end or d <= end):
                events.append(
                    {
                        "id": f"t-{t['id']}",
                        "entityId": t["id"],
                        "entityType": "task",
                        "title": t["title"],
                        "date": d,
                        "time": t.get("dueTime"),
                        "visibility": t["visibility"],
                        "priority": t["priority"],
                        "completed": t["completed"],
                    }
                )
        for r in self.list_reminders(user_id):
            d = r["date"]
            if (not start or d >= start) and (not end or d <= end):
                events.append(
                    {
                        "id": f"r-{r['id']}",
                        "entityId": r["id"],
                        "entityType": "reminder",
                        "title": r["title"],
                        "date": d,
                        "time": r.get("time"),
                        "visibility": r["visibility"],
                        "priority": r["priority"],
                        "completed": r["completed"],
                    }
                )
        for a in self.list_assignments(user_id):
            d = a["deadline"]
            if (not start or d >= start) and (not end or d <= end):
                events.append(
                    {
                        "id": f"a-{a['id']}",
                        "entityId": a["id"],
                        "entityType": "assignment",
                        "title": a["title"],
                        "date": d,
                        "time": a.get("deadlineTime"),
                        "visibility": a["visibility"],
                        "priority": a["priority"],
                        "completed": a["status"] == "completed",
                    }
                )
        events.sort(key=lambda ev: (ev["date"], ev["time"] or "23:59"))
        return events

    def _map_document(self, r: Dict[str, Any]) -> Dict[str, Any]:
        signed = self._signed_url(r["storage_bucket"], r["storage_path"])
        return {
            "id": str(r["id"]),
            "name": r["file_name"],
            "kind": r["file_kind"],
            "extension": r["extension"],
            "mimeType": r["mime_type"],
            "sizeBytes": int(r["size_bytes"]),
            "ownerId": str(r["owner_id"]),
            "lastModifiedBy": str(r["last_modified_by"]) if r.get("last_modified_by") else None,
            "visibility": r["visibility"],
            "uploadedAt": str(r["created_at"]),
            "previewUrl": signed if r["file_kind"] == "image" else None,
            "downloadUrl": signed,
            "summary": r.get("summary"),
        }

    # -------------------------------------------------------------------------
    # NOTIFICATIONS & PREFERENCES
    # -------------------------------------------------------------------------
    def list_notifications(self, user_id: str) -> List[Dict[str, Any]]:
        ws_id = self.get_user_workspace_id(user_id)
        rows = (
            self.admin.table("notifications")
            .select("*")
            .order("created_at", desc=True)
            .execute()
            .data
            or []
        )
        out = []
        for r in rows:
            is_private_mine = r["visibility"] == "private" and str(r.get("recipient_id")) == user_id
            is_shared_mine = r["visibility"] == "shared" and ws_id and str(r.get("workspace_id")) == ws_id
            if is_private_mine or is_shared_mine:
                out.append(
                    {
                        "id": str(r["id"]),
                        "title": r["title"],
                        "message": r["message"],
                        "category": r["category"],
                        "targetRoute": r.get("target_route", "/"),
                        "actorId": str(r["actor_id"]) if r.get("actor_id") else user_id,
                        "visibility": r["visibility"],
                        "ownerId": str(r["recipient_id"]) if r.get("recipient_id") else None,
                        "read": bool(r["read"]),
                        "createdAt": str(r["created_at"]),
                    }
                )
        return out

    def mark_notification_read(self, user_id: str, notif_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        res = self.admin.table("notifications").select("*").eq("id", notif_id).execute()
        if not res.data:
            return
        r = res.data[0]
        if (r["visibility"] == "private" and str(r.get("recipient_id")) == user_id) or (
            r["visibility"] == "shared" and ws_id and str(r.get("workspace_id")) == ws_id
        ):
            self.admin.table("notifications").update({"read": True}).eq("id", notif_id).execute()

    def mark_all_notifications_read(self, user_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        self.admin.table("notifications").update({"read": True}).eq("recipient_id", user_id).execute()
        if ws_id:
            self.admin.table("notifications").update({"read": True}).eq("workspace_id", ws_id).eq("visibility", "shared").execute()

    def clear_notifications(self, user_id: str) -> None:
        ws_id = self.get_user_workspace_id(user_id)
        self.admin.table("notifications").delete().eq("recipient_id", user_id).execute()
        if ws_id:
            self.admin.table("notifications").delete().eq("workspace_id", ws_id).eq("visibility", "shared").execute()

    def get_notification_preferences(self, user_id: str) -> Dict[str, Any]:
        res = (
            self.admin.table("notification_preferences")
            .select("*")
            .eq("user_id", user_id)
            .execute()
        )
        if not res.data:
            return {
                "deadlineAlerts": True,
                "sharedUpdates": True,
                "fileUploads": True,
                "dailyDigest": False,
            }
        r = res.data[0]
        return {
            "deadlineAlerts": bool(r["deadline_alerts"]),
            "sharedUpdates": bool(r["shared_updates"]),
            "fileUploads": bool(r["file_uploads"]),
            "dailyDigest": bool(r["daily_digest"]),
        }

    def update_notification_preferences(self, user_id: str, prefs: Dict[str, Any]) -> Dict[str, Any]:
        self.admin.table("notification_preferences").upsert(
            {
                "user_id": user_id,
                "deadline_alerts": bool(prefs.get("deadlineAlerts", True)),
                "shared_updates": bool(prefs.get("sharedUpdates", True)),
                "file_uploads": bool(prefs.get("fileUploads", True)),
                "daily_digest": bool(prefs.get("dailyDigest", False)),
            }
        ).execute()
        return self.get_notification_preferences(user_id)


_service_instance: Optional[SupabaseService] = None


def get_supabase_service(settings: Settings) -> SupabaseService:
    global _service_instance
    if _service_instance is None:
        _service_instance = SupabaseService(settings)
    return _service_instance
