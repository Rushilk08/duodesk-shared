import { createClient, RealtimeChannel, SupabaseClient } from '@supabase/supabase-js';

const SUPABASE_URL = (import.meta.env.VITE_SUPABASE_URL as string | undefined)?.trim() || '';
const SUPABASE_ANON_KEY = (import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined)?.trim() || '';

let client: SupabaseClient | null = null;

export const isRealtimeConfigured = Boolean(SUPABASE_URL && SUPABASE_ANON_KEY);

function getSupabaseClient(): SupabaseClient | null {
  if (!isRealtimeConfigured) return null;
  if (!client) {
    client = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    });
  }
  return client;
}

const SHARED_TABLES = [
  'tasks',
  'reminders',
  'assignments',
  'notes',
  'todo_lists',
  'attachments',
  'notifications',
] as const;

export function subscribeToWorkspaceRealtime(
  accessToken: string | null,
  workspaceId: string | null,
  onRemoteChange: () => void
): () => void {
  const supabase = getSupabaseClient();
  if (!supabase || !accessToken || !workspaceId) {
    return () => {};
  }

  supabase.realtime.setAuth(accessToken);

  const channel: RealtimeChannel = supabase.channel(`duodesk-ws-${workspaceId}`);

  for (const table of SHARED_TABLES) {
    channel.on(
      'postgres_changes',
      {
        event: '*',
        schema: 'public',
        table,
        filter: `workspace_id=eq.${workspaceId}`,
      },
      () => onRemoteChange()
    );
  }

  channel.on(
    'postgres_changes',
    {
      event: '*',
      schema: 'public',
      table: 'checklist_items',
    },
    () => onRemoteChange()
  );

  channel.subscribe();

  return () => {
    supabase.removeChannel(channel);
  };
}
