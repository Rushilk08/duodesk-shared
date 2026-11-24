from typing import List, Literal, Optional
from pydantic import BaseModel, EmailStr, Field

Visibility = Literal["private", "shared"]
Priority = Literal["low", "medium", "high"]
AssignmentStatus = Literal["pending", "in_progress", "completed"]
RepeatOption = Literal["none", "daily", "weekly", "monthly"]
NotificationPreference = Literal["push_in_app", "in_app", "email_digest"]
DocumentKind = Literal["pdf", "doc", "image"]


# -----------------------------------------------------------------------------
# Auth & Profiles
# -----------------------------------------------------------------------------
class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    name: str = Field(min_length=1, max_length=60)
    full_name: Optional[str] = Field(default=None, max_length=120)
    role: str = Field(default="Member", max_length=80)
    invite_code: Optional[str] = Field(default=None, max_length=64)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class PasswordResetRequest(BaseModel):
    email: EmailStr


class UpdatePasswordRequest(BaseModel):
    new_password: str = Field(min_length=8, max_length=128)


class ProfileOut(BaseModel):
    id: str
    name: str
    fullName: str
    email: str
    role: str
    avatarUrl: str
    accentHex: str


class ProfileUpdateRequest(BaseModel):
    name: Optional[str] = Field(default=None, min_length=1, max_length=60)
    fullName: Optional[str] = Field(default=None, min_length=1, max_length=120)
    email: Optional[EmailStr] = None
    role: Optional[str] = Field(default=None, max_length=80)
    avatarUrl: Optional[str] = Field(default=None, max_length=500)


class AuthSessionResponse(BaseModel):
    accessToken: str
    refreshToken: Optional[str] = None
    user: ProfileOut
    workspaceId: Optional[str] = None


# -----------------------------------------------------------------------------
# Workspaces & Invitations
# -----------------------------------------------------------------------------
class InviteCreateRequest(BaseModel):
    invitedEmail: Optional[EmailStr] = None


class InviteResponse(BaseModel):
    inviteCode: str
    inviteUrl: str
    expiresAt: str
    workspaceId: str


class JoinWorkspaceRequest(BaseModel):
    inviteCode: str = Field(min_length=4, max_length=64)


class WorkspaceOut(BaseModel):
    id: str
    name: str
    ownerId: str
    members: List[ProfileOut]


# -----------------------------------------------------------------------------
# Attachments & Documents
# -----------------------------------------------------------------------------
class AttachmentRefOut(BaseModel):
    id: str
    name: str
    sizeBytes: int
    kind: DocumentKind
    mimeType: str
    previewUrl: Optional[str] = None


class DocumentOut(BaseModel):
    id: str
    name: str
    kind: DocumentKind
    extension: str
    mimeType: str
    sizeBytes: int
    ownerId: str
    lastModifiedBy: Optional[str] = None
    visibility: Visibility
    uploadedAt: str
    previewUrl: Optional[str] = None
    downloadUrl: Optional[str] = None
    summary: Optional[str] = None


class DocumentUpdateRequest(BaseModel):
    visibility: Optional[Visibility] = None
    summary: Optional[str] = Field(default=None, max_length=500)


# -----------------------------------------------------------------------------
# Tasks
# -----------------------------------------------------------------------------
class TaskCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: Optional[str] = Field(default=None, max_length=2000)
    visibility: Visibility = "private"
    priority: Priority = "medium"
    dueDate: str
    dueTime: Optional[str] = None
    completed: bool = False
    category: Optional[str] = Field(default=None, max_length=80)


class TaskUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    description: Optional[str] = Field(default=None, max_length=2000)
    visibility: Optional[Visibility] = None
    priority: Optional[Priority] = None
    dueDate: Optional[str] = None
    dueTime: Optional[str] = None
    completed: Optional[bool] = None
    category: Optional[str] = Field(default=None, max_length=80)


class TaskOut(BaseModel):
    id: str
    title: str
    description: Optional[str] = None
    ownerId: str
    lastModifiedBy: Optional[str] = None
    visibility: Visibility
    priority: Priority
    dueDate: str
    dueTime: Optional[str] = None
    completed: bool
    category: Optional[str] = None
    attachments: List[AttachmentRefOut] = []
    createdAt: str
    updatedAt: str


# -----------------------------------------------------------------------------
# Reminders
# -----------------------------------------------------------------------------
class ReminderCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(default="", max_length=2000)
    visibility: Visibility = "private"
    date: str
    time: str
    priority: Priority = "medium"
    repeat: RepeatOption = "none"
    notificationPref: NotificationPreference = "push_in_app"
    completed: bool = False


class ReminderUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    description: Optional[str] = Field(default=None, max_length=2000)
    visibility: Optional[Visibility] = None
    date: Optional[str] = None
    time: Optional[str] = None
    priority: Optional[Priority] = None
    repeat: Optional[RepeatOption] = None
    notificationPref: Optional[NotificationPreference] = None
    completed: Optional[bool] = None


class ReminderOut(BaseModel):
    id: str
    title: str
    description: str
    ownerId: str
    lastModifiedBy: Optional[str] = None
    visibility: Visibility
    date: str
    time: str
    priority: Priority
    repeat: RepeatOption
    notificationPref: NotificationPreference
    completed: bool
    createdAt: str


# -----------------------------------------------------------------------------
# Assignments
# -----------------------------------------------------------------------------
class AssignmentCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    subject: str = Field(default="General", max_length=120)
    description: str = Field(default="", max_length=2000)
    visibility: Visibility = "private"
    deadline: str
    deadlineTime: Optional[str] = None
    priority: Priority = "medium"
    status: AssignmentStatus = "pending"


class AssignmentUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    subject: Optional[str] = Field(default=None, max_length=120)
    description: Optional[str] = Field(default=None, max_length=2000)
    visibility: Optional[Visibility] = None
    deadline: Optional[str] = None
    deadlineTime: Optional[str] = None
    priority: Optional[Priority] = None
    status: Optional[AssignmentStatus] = None


class AssignmentOut(BaseModel):
    id: str
    title: str
    subject: str
    description: str
    ownerId: str
    lastModifiedBy: Optional[str] = None
    visibility: Visibility
    deadline: str
    deadlineTime: Optional[str] = None
    priority: Priority
    status: AssignmentStatus
    attachments: List[AttachmentRefOut] = []
    createdAt: str


# -----------------------------------------------------------------------------
# Notes & Checklists
# -----------------------------------------------------------------------------
class ChecklistRowIn(BaseModel):
    id: Optional[str] = None
    text: str = Field(min_length=1, max_length=400)
    checked: bool = False


class NoteCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    content: str = Field(default="", max_length=20000)
    type: Literal["note", "checklist"] = "note"
    checklist: List[ChecklistRowIn] = []
    visibility: Visibility = "private"


class NoteUpdateRequest(BaseModel):
    title: Optional[str] = Field(default=None, min_length=1, max_length=200)
    content: Optional[str] = Field(default=None, max_length=20000)
    checklist: Optional[List[ChecklistRowIn]] = None
    visibility: Optional[Visibility] = None


class ChecklistRowOut(BaseModel):
    id: str
    text: str
    checked: bool


class NoteOut(BaseModel):
    id: str
    title: str
    content: str
    type: Literal["note", "checklist"]
    checklist: List[ChecklistRowOut] = []
    ownerId: str
    lastModifiedBy: Optional[str] = None
    visibility: Visibility
    updatedAt: str
    createdAt: str


# -----------------------------------------------------------------------------
# Notifications
# -----------------------------------------------------------------------------
class NotificationOut(BaseModel):
    id: str
    title: str
    message: str
    category: Literal["deadline", "reminder", "shared_task", "file", "completed"]
    targetRoute: str
    actorId: str
    visibility: Visibility
    ownerId: Optional[str] = None
    read: bool
    createdAt: str


class NotificationSettingsOut(BaseModel):
    deadlineAlerts: bool = True
    sharedUpdates: bool = True
    fileUploads: bool = True
    dailyDigest: bool = False
