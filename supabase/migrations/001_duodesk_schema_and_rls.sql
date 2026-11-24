-- =============================================================================
-- DuoDesk PostgreSQL Schema, Row Level Security (RLS), Storage & Realtime
-- =============================================================================

create extension if not exists "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. PROFILES
-- -----------------------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null,
  full_name text not null,
  email text not null unique,
  role text not null default 'Member',
  avatar_url text not null default '',
  accent_hex text not null default '#9B82F3',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 2. WORKSPACES (Shared 2-person workspace)
-- -----------------------------------------------------------------------------
create table if not exists public.workspaces (
  id uuid primary key default gen_random_uuid(),
  name text not null default 'DuoDesk Shared Space',
  owner_id uuid not null references public.profiles(id) on delete cascade,
  max_members integer not null default 2 check (max_members = 2),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 3. WORKSPACE MEMBERS (Enforces maximum of 2 authorized users per workspace)
-- -----------------------------------------------------------------------------
create table if not exists public.workspace_members (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner', 'member')),
  joined_at timestamptz not null default now(),
  unique (workspace_id, user_id)
);

create or replace function public.enforce_two_member_limit()
returns trigger
language plpgsql
as $$
declare
  member_count integer;
begin
  select count(*) into member_count
  from public.workspace_members
  where workspace_id = new.workspace_id;

  if member_count >= 2 then
    raise exception 'DuoDesk workspaces are limited to exactly 2 members.';
  end if;
  return new;
end;
$$;

drop trigger if exists trg_enforce_two_member_limit on public.workspace_members;
create trigger trg_enforce_two_member_limit
before insert on public.workspace_members
for each row execute function public.enforce_two_member_limit();

-- -----------------------------------------------------------------------------
-- 4. WORKSPACE INVITATIONS
-- -----------------------------------------------------------------------------
create table if not exists public.workspace_invitations (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces(id) on delete cascade,
  invited_by uuid not null references public.profiles(id) on delete cascade,
  invite_code text not null unique,
  invited_email text,
  expires_at timestamptz not null default (now() + interval '7 days'),
  accepted_by uuid references public.profiles(id) on delete set null,
  accepted_at timestamptz,
  created_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 5. TASKS
-- -----------------------------------------------------------------------------
create table if not exists public.tasks (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  description text,
  priority text not null default 'medium' check (priority in ('low', 'medium', 'high')),
  due_date date not null,
  due_time time,
  completed boolean not null default false,
  category text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_task_shared_workspace check (
    (visibility = 'private') or (visibility = 'shared' and workspace_id is not null)
  )
);

-- -----------------------------------------------------------------------------
-- 6. REMINDERS
-- -----------------------------------------------------------------------------
create table if not exists public.reminders (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  description text not null default '',
  remind_date date not null,
  remind_time time not null,
  priority text not null default 'medium' check (priority in ('low', 'medium', 'high')),
  repeat_option text not null default 'none' check (repeat_option in ('none', 'daily', 'weekly', 'monthly')),
  notification_pref text not null default 'push_in_app' check (notification_pref in ('push_in_app', 'in_app', 'email_digest')),
  completed boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_reminder_shared_workspace check (
    (visibility = 'private') or (visibility = 'shared' and workspace_id is not null)
  )
);

-- -----------------------------------------------------------------------------
-- 7. ASSIGNMENTS
-- -----------------------------------------------------------------------------
create table if not exists public.assignments (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  subject text not null default 'General',
  description text not null default '',
  deadline date not null,
  deadline_time time,
  priority text not null default 'medium' check (priority in ('low', 'medium', 'high')),
  status text not null default 'pending' check (status in ('pending', 'in_progress', 'completed')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_assignment_shared_workspace check (
    (visibility = 'private') or (visibility = 'shared' and workspace_id is not null)
  )
);

-- -----------------------------------------------------------------------------
-- 8. NOTES
-- -----------------------------------------------------------------------------
create table if not exists public.notes (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  content text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_note_shared_workspace check (
    (visibility = 'private') or (visibility = 'shared' and workspace_id is not null)
  )
);

-- -----------------------------------------------------------------------------
-- 9. TODO LISTS & 10. CHECKLIST ITEMS
-- -----------------------------------------------------------------------------
create table if not exists public.todo_lists (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  description text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_todolist_shared_workspace check (
    (visibility = 'private') or (visibility = 'shared' and workspace_id is not null)
  )
);

create table if not exists public.checklist_items (
  id uuid primary key default gen_random_uuid(),
  todo_list_id uuid not null references public.todo_lists(id) on delete cascade,
  text text not null,
  checked boolean not null default false,
  position integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 11. ATTACHMENTS (Documents, Images, Task/Assignment file references)
-- -----------------------------------------------------------------------------
create table if not exists public.attachments (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  last_modified_by uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  entity_type text not null default 'document' check (entity_type in ('document', 'task', 'assignment')),
  entity_id uuid,
  file_name text not null,
  file_kind text not null check (file_kind in ('pdf', 'doc', 'image')),
  extension text not null,
  mime_type text not null,
  size_bytes bigint not null check (size_bytes > 0),
  storage_bucket text not null,
  storage_path text not null,
  summary text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 12. NOTIFICATIONS & PREFERENCES
-- -----------------------------------------------------------------------------
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid references public.workspaces(id) on delete cascade,
  recipient_id uuid references public.profiles(id) on delete cascade,
  actor_id uuid references public.profiles(id) on delete set null,
  visibility text not null check (visibility in ('private', 'shared')),
  title text not null,
  message text not null,
  category text not null check (category in ('deadline', 'reminder', 'shared_task', 'file', 'completed')),
  target_route text not null default '/',
  read boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.notification_preferences (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  deadline_alerts boolean not null default true,
  shared_updates boolean not null default true,
  file_uploads boolean not null default true,
  daily_digest boolean not null default false,
  updated_at timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- UPDATED_AT TRIGGER HELPER
-- -----------------------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare
  tbl text;
begin
  for tbl in
    select unnest(array[
      'profiles','workspaces','tasks','reminders','assignments',
      'notes','todo_lists','checklist_items','attachments','notification_preferences'
    ])
  loop
    execute format('drop trigger if exists trg_touch_%I on public.%I', tbl, tbl);
    execute format(
      'create trigger trg_touch_%I before update on public.%I for each row execute function public.touch_updated_at()',
      tbl, tbl
    );
  end loop;
end;
$$;

-- -----------------------------------------------------------------------------
-- MEMBERSHIP HELPER & ROW LEVEL SECURITY (RLS)
-- -----------------------------------------------------------------------------
create or replace function public.is_workspace_member(ws_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.workspace_members wm
    where wm.workspace_id = ws_id
      and wm.user_id = auth.uid()
  );
$$;

alter table public.profiles enable row level security;
alter table public.workspaces enable row level security;
alter table public.workspace_members enable row level security;
alter table public.workspace_invitations enable row level security;
alter table public.tasks enable row level security;
alter table public.reminders enable row level security;
alter table public.assignments enable row level security;
alter table public.notes enable row level security;
alter table public.todo_lists enable row level security;
alter table public.checklist_items enable row level security;
alter table public.attachments enable row level security;
alter table public.notifications enable row level security;
alter table public.notification_preferences enable row level security;

-- Profiles: users can view their own profile or co-member's profile; update own only
create policy "profiles_select_self_or_comember" on public.profiles
  for select using (
    id = auth.uid()
    or exists (
      select 1
      from public.workspace_members m1
      join public.workspace_members m2 on m1.workspace_id = m2.workspace_id
      where m1.user_id = auth.uid() and m2.user_id = profiles.id
    )
  );

create policy "profiles_insert_own" on public.profiles
  for insert with check (id = auth.uid());

create policy "profiles_update_own" on public.profiles
  for update using (id = auth.uid()) with check (id = auth.uid());

-- Workspaces & members
create policy "workspaces_member_select" on public.workspaces
  for select using (public.is_workspace_member(id) or owner_id = auth.uid());

create policy "workspaces_owner_insert" on public.workspaces
  for insert with check (owner_id = auth.uid());

create policy "workspace_members_select" on public.workspace_members
  for select using (public.is_workspace_member(workspace_id));

create policy "workspace_invitations_member_manage" on public.workspace_invitations
  for all using (public.is_workspace_member(workspace_id))
  with check (public.is_workspace_member(workspace_id));

-- Reusable private vs shared policy pattern for tasks, reminders, assignments, notes, todo_lists, attachments
create policy "tasks_access" on public.tasks
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "reminders_access" on public.reminders
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "assignments_access" on public.assignments
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "notes_access" on public.notes
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "todo_lists_access" on public.todo_lists
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "checklist_items_access" on public.checklist_items
  for all
  using (
    exists (
      select 1 from public.todo_lists tl
      where tl.id = checklist_items.todo_list_id
        and (
          (tl.visibility = 'private' and tl.owner_id = auth.uid())
          or (tl.visibility = 'shared' and public.is_workspace_member(tl.workspace_id))
        )
    )
  )
  with check (
    exists (
      select 1 from public.todo_lists tl
      where tl.id = checklist_items.todo_list_id
        and (
          (tl.visibility = 'private' and tl.owner_id = auth.uid())
          or (tl.visibility = 'shared' and public.is_workspace_member(tl.workspace_id))
        )
    )
  );

create policy "attachments_access" on public.attachments
  for all
  using (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and owner_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "notifications_access" on public.notifications
  for all
  using (
    (visibility = 'private' and recipient_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  )
  with check (
    (visibility = 'private' and recipient_id = auth.uid())
    or (visibility = 'shared' and public.is_workspace_member(workspace_id))
  );

create policy "notification_preferences_own" on public.notification_preferences
  for all
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- -----------------------------------------------------------------------------
-- SUPABASE STORAGE BUCKETS & ACCESS POLICIES
-- -----------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  (
    'duodesk-private',
    'duodesk-private',
    false,
    20971520,
    array[
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'image/png',
      'image/jpeg',
      'image/webp'
    ]
  ),
  (
    'duodesk-shared',
    'duodesk-shared',
    false,
    20971520,
    array[
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'image/png',
      'image/jpeg',
      'image/webp'
    ]
  )
on conflict (id) do nothing;

-- Private storage objects: folder prefix must match auth.uid()
create policy "private_bucket_owner_only" on storage.objects
  for all
  using (
    bucket_id = 'duodesk-private'
    and (storage.foldername(name))[1] = auth.uid()::text
  )
  with check (
    bucket_id = 'duodesk-private'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Shared storage objects: folder prefix must be a workspace_id the user belongs to
create policy "shared_bucket_members_only" on storage.objects
  for all
  using (
    bucket_id = 'duodesk-shared'
    and public.is_workspace_member(((storage.foldername(name))[1])::uuid)
  )
  with check (
    bucket_id = 'duodesk-shared'
    and public.is_workspace_member(((storage.foldername(name))[1])::uuid)
  );

-- -----------------------------------------------------------------------------
-- SUPABASE REALTIME PUBLICATION (RLS prevents private rows from broadcasting)
-- -----------------------------------------------------------------------------
alter publication supabase_realtime add table
  public.tasks,
  public.reminders,
  public.assignments,
  public.notes,
  public.todo_lists,
  public.checklist_items,
  public.attachments,
  public.notifications;
