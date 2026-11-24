import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'motion/react';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import { InspectableItem, ItemDetailModal } from '../components/ItemDetailModal';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations, useDuoDeskQueries } from '../hooks/useDuoDeskData';
import {
  addDaysIso,
  formatFullDate,
  formatTime12h,
  getTodayIso,
} from '../utils/formatters';

type CalendarView = 'week' | 'month' | 'day';

interface CalEvent {
  id: string;
  date: string;
  time?: string;
  title: string;
  visibility: 'private' | 'shared';
  kindLabel: string;
  raw: InspectableItem;
}

export function CalendarPage() {
  const { setCreateDefaults } = useWorkspace();
  const navigate = useNavigate();
  const { usersQuery, tasksQuery, remindersQuery, assignmentsQuery } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();

  const todayIso = getTodayIso();
  const [view, setView] = useState<CalendarView>('week');
  const [cursorIso, setCursorIso] = useState<string>(todayIso);
  const [inspectTarget, setInspectTarget] = useState<InspectableItem | null>(null);

  const users = usersQuery.data;

  const events = useMemo(() => {
    const list: CalEvent[] = [];

    (tasksQuery.data ?? []).forEach((t) =>
      list.push({
        id: `t-${t.id}`,
        date: t.dueDate,
        time: t.dueTime,
        title: t.title,
        visibility: t.visibility,
        kindLabel: t.visibility === 'shared' ? 'Shared task' : 'Personal task',
        raw: { type: 'task', item: t },
      })
    );

    (remindersQuery.data ?? []).forEach((r) =>
      list.push({
        id: `r-${r.id}`,
        date: r.date,
        time: r.time,
        title: r.title,
        visibility: r.visibility,
        kindLabel: r.visibility === 'shared' ? 'Shared reminder' : 'Reminder',
        raw: { type: 'reminder', item: r },
      })
    );

    (assignmentsQuery.data ?? []).forEach((a) =>
      list.push({
        id: `a-${a.id}`,
        date: a.deadline,
        time: a.deadlineTime,
        title: a.title,
        visibility: a.visibility,
        kindLabel: `Assignment · ${a.subject}`,
        raw: { type: 'assignment', item: a },
      })
    );

    return list;
  }, [tasksQuery.data, remindersQuery.data, assignmentsQuery.data]);

  const [cy, cm, cd] = cursorIso.split('-').map(Number);
  const cursorDate = new Date(cy, cm - 1, cd);

  const stepPeriod = (dir: -1 | 1) => {
    if (view === 'month') {
      const next = new Date(cy, cm - 1 + dir, 1);
      setCursorIso(
        `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-01`
      );
    } else if (view === 'week') {
      setCursorIso(addDaysIso(cursorIso, dir * 7));
    } else {
      setCursorIso(addDaysIso(cursorIso, dir));
    }
  };

  const weekDays = useMemo(() => {
    const dow = cursorDate.getDay();
    const startIso = addDaysIso(cursorIso, -dow);
    return Array.from({ length: 7 }).map((_, idx) => {
      const iso = addDaysIso(startIso, idx);
      const [y, m, d] = iso.split('-').map(Number);
      const dt = new Date(y, m - 1, d);
      return {
        iso,
        shortDay: dt.toLocaleDateString('en-US', { weekday: 'short' }),
        dayNum: d,
        events: events.filter((e) => e.date === iso),
      };
    });
  }, [cursorIso, cursorDate, events]);

  const monthCells = useMemo(() => {
    const first = new Date(cy, cm - 1, 1);
    const offset = first.getDay();
    const gridStart = new Date(cy, cm - 1, 1 - offset);

    return Array.from({ length: 35 }).map((_, i) => {
      const dt = new Date(
        gridStart.getFullYear(),
        gridStart.getMonth(),
        gridStart.getDate() + i
      );
      const iso = `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(
        dt.getDate()
      ).padStart(2, '0')}`;
      return {
        iso,
        dayNum: dt.getDate(),
        inMonth: dt.getMonth() === cm - 1,
        events: events.filter((e) => e.date === iso),
      };
    });
  }, [cy, cm, events]);

  const selectedDayEvents = useMemo(
    () =>
      events
        .filter((e) => e.date === cursorIso)
        .sort((a, b) => (a.time ?? '23:59').localeCompare(b.time ?? '23:59')),
    [events, cursorIso]
  );

  const periodHeading = cursorDate.toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  });

  const eventChipStyle = (vis: 'private' | 'shared') =>
    vis === 'shared'
      ? 'bg-[#E6DFFB] text-[#5B469C] hover:bg-[#DDD3FA] dark:bg-[#2D283E] dark:text-[#E6DFFB]'
      : 'bg-[#F8D8E4] text-[#8E3B5B] hover:bg-[#F5C6D8] dark:bg-[#3A2632] dark:text-[#F8D8E4]';

  const openCreateOnDate = (dateIso: string) => {
    setCreateDefaults({ kind: 'task', date: dateIso });
    navigate('/create');
  };

  return (
    <div className="space-y-6">
      {/* Top Controls */}
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex items-center gap-3">
          <h1 className="font-display text-2xl font-bold text-[#18181F] dark:text-white">
            {periodHeading}
          </h1>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => stepPeriod(-1)}
              className="rounded-full border border-[#EAE4DC] bg-white p-2 text-[#6E6A75] hover:text-[#18181F] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
              aria-label="Previous"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => setCursorIso(todayIso)}
              className="rounded-full border border-[#EAE4DC] bg-white px-3.5 py-1.5 text-xs font-semibold text-[#18181F] hover:bg-[#F5F1EB] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:text-white"
            >
              Today
            </button>
            <button
              type="button"
              onClick={() => stepPeriod(1)}
              className="rounded-full border border-[#EAE4DC] bg-white p-2 text-[#6E6A75] hover:text-[#18181F] dark:border-[#2E2D3B] dark:bg-[#1F1F29]"
              aria-label="Next"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>

        <div className="flex items-center gap-1 rounded-full border border-[#EAE4DC] bg-white p-1 self-start dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          {(['week', 'month', 'day'] as const).map((mode) => (
            <button
              key={mode}
              type="button"
              onClick={() => setView(mode)}
              className={`rounded-full px-4 py-1.5 text-xs font-semibold capitalize transition-colors ${
                view === mode
                  ? 'bg-[#18181F] text-white dark:bg-[#9B82F3] dark:text-[#16161D]'
                  : 'text-[#6E6A75] hover:text-[#18181F] dark:text-[#A19CA8]'
              }`}
            >
              {mode}
            </button>
          ))}
        </div>
      </div>

      {/* Subtle Pastel Legend */}
      <div className="flex flex-wrap items-center gap-5 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
        <span className="inline-flex items-center gap-2">
          <span className="h-2.5 w-2.5 rounded-full bg-[#E684A7]" />
          Personal deadlines
        </span>
        <span className="inline-flex items-center gap-2">
          <span className="h-2.5 w-2.5 rounded-full bg-[#9B82F3]" />
          Shared deadlines
        </span>
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={`${view}-${cursorIso}`}
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -6 }}
          transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
        >
          {view === 'week' && (
            <div className="space-y-5">
              {/* Cute Rounded 7-Day Strip */}
              <div className="grid grid-cols-7 gap-2 rounded-3xl border border-[#EAE4DC] bg-white p-3 sm:p-4 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
                {weekDays.map((day) => {
                  const isSelected = day.iso === cursorIso;
                  const isToday = day.iso === todayIso;
                  return (
                    <button
                      key={day.iso}
                      type="button"
                      onClick={() => setCursorIso(day.iso)}
                      className={`flex flex-col items-center rounded-2xl py-3 transition-all ${
                        isSelected
                          ? 'bg-[#18181F] text-white shadow-sm dark:bg-[#9B82F3] dark:text-[#16161D]'
                          : isToday
                          ? 'bg-[#F8D8E4]/65 text-[#8E3B5B] dark:bg-[#2D283E] dark:text-white'
                          : 'text-[#6E6A75] hover:bg-[#FAF7F2] dark:hover:bg-[#272733]'
                      }`}
                    >
                      <span className="text-[11px] font-medium">{day.shortDay}</span>
                      <span className="mt-1 font-mono text-base font-bold tabular-nums">
                        {day.dayNum}
                      </span>
                      <div className="mt-1.5 flex h-1.5 gap-1">
                        {day.events.slice(0, 3).map((ev) => (
                          <span
                            key={ev.id}
                            className={`h-1.5 w-1.5 rounded-full ${
                              ev.visibility === 'shared' ? 'bg-[#9B82F3]' : 'bg-[#E684A7]'
                            }`}
                          />
                        ))}
                      </div>
                    </button>
                  );
                })}
              </div>

              {/* Selected Day Agenda */}
              <div className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h2 className="font-display text-base font-bold text-[#18181F] dark:text-white">
                      {formatFullDate(cursorIso)}
                    </h2>
                    <p className="text-xs text-[#6E6A75]">
                      {selectedDayEvents.length} scheduled item(s)
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => openCreateOnDate(cursorIso)}
                    className="inline-flex items-center gap-1 rounded-full bg-[#FAF7F2] px-3.5 py-2 text-xs font-semibold text-[#18181F] hover:bg-[#E6DFFB] dark:bg-[#272733] dark:text-white"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    <span>Add</span>
                  </button>
                </div>

                {selectedDayEvents.length === 0 ? (
                  <p className="py-8 text-center text-xs text-[#6E6A75]">
                    No deadlines or reminders on this day.
                  </p>
                ) : (
                  <div className="space-y-2.5">
                    {selectedDayEvents.map((ev) => (
                      <button
                        key={ev.id}
                        type="button"
                        onClick={() => setInspectTarget(ev.raw)}
                        className={`flex w-full items-center justify-between gap-4 rounded-2xl px-4 py-3.5 text-left transition-colors ${eventChipStyle(
                          ev.visibility
                        )}`}
                      >
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold">{ev.title}</p>
                          <p className="mt-0.5 text-xs opacity-80">{ev.kindLabel}</p>
                        </div>
                        <span className="shrink-0 font-mono text-xs tabular-nums">
                          {ev.time ? formatTime12h(ev.time) : 'All day'}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {view === 'month' && (
            <div className="overflow-hidden rounded-3xl border border-[#EAE4DC] bg-white dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
              <div className="grid grid-cols-7 border-b border-[#F2EDE6] text-center text-[11px] font-semibold text-[#6E6A75] dark:border-[#2E2D3B]">
                {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
                  <div key={d} className="py-3">
                    {d}
                  </div>
                ))}
              </div>
              <div className="grid grid-cols-7 divide-x divide-y divide-[#F2EDE6] dark:divide-[#2E2D3B]">
                {monthCells.map((cell) => {
                  const isToday = cell.iso === todayIso;
                  return (
                    <div
                      key={cell.iso}
                      className={`group min-h-[100px] p-2 ${
                        cell.inMonth
                          ? ''
                          : 'bg-[#FAF7F2]/60 text-[#9A94A3] dark:bg-[#16161D]/40'
                      }`}
                    >
                      <div className="mb-1 flex items-center justify-between">
                        <span
                          className={`inline-flex h-6 w-6 items-center justify-center rounded-full font-mono text-xs tabular-nums ${
                            isToday
                              ? 'bg-[#18181F] font-bold text-white dark:bg-[#9B82F3] dark:text-[#16161D]'
                              : ''
                          }`}
                        >
                          {cell.dayNum}
                        </span>
                        <button
                          type="button"
                          onClick={() => openCreateOnDate(cell.iso)}
                          className="rounded-full p-1 text-[#8E8996] opacity-0 group-hover:opacity-100 hover:bg-[#FAF7F2]"
                          aria-label={`Add item on ${cell.iso}`}
                        >
                          <Plus className="h-3 w-3" />
                        </button>
                      </div>
                      <div className="space-y-1">
                        {cell.events.slice(0, 2).map((ev) => (
                          <button
                            key={ev.id}
                            type="button"
                            onClick={() => setInspectTarget(ev.raw)}
                            className={`block w-full truncate rounded-lg px-1.5 py-1 text-left text-[11px] font-medium ${eventChipStyle(
                              ev.visibility
                            )}`}
                          >
                            {ev.title}
                          </button>
                        ))}
                        {cell.events.length > 2 && (
                          <button
                            type="button"
                            onClick={() => {
                              setCursorIso(cell.iso);
                              setView('day');
                            }}
                            className="px-1 text-[10px] font-semibold text-[#6E6A75] hover:underline"
                          >
                            +{cell.events.length - 2} more
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {view === 'day' && (
            <div className="rounded-3xl border border-[#EAE4DC] bg-white p-6 dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
              <div className="mb-5 flex items-center justify-between">
                <div>
                  <h2 className="font-display text-lg font-bold text-[#18181F] dark:text-white">
                    {formatFullDate(cursorIso)}
                  </h2>
                  <p className="text-xs text-[#6E6A75]">
                    {selectedDayEvents.length} item(s) scheduled
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => openCreateOnDate(cursorIso)}
                  className="inline-flex items-center gap-1.5 rounded-full bg-[#18181F] px-4 py-2 text-xs font-semibold text-white dark:bg-[#9B82F3] dark:text-[#16161D]"
                >
                  <Plus className="h-3.5 w-3.5" />
                  <span>Add item</span>
                </button>
              </div>
              {selectedDayEvents.length === 0 ? (
                <p className="py-12 text-center text-sm text-[#6E6A75]">
                  Nothing scheduled for this day.
                </p>
              ) : (
                <div className="space-y-3">
                  {selectedDayEvents.map((ev) => (
                    <button
                      key={ev.id}
                      type="button"
                      onClick={() => setInspectTarget(ev.raw)}
                      className={`flex w-full items-center justify-between gap-4 rounded-2xl px-4 py-4 text-left ${eventChipStyle(
                        ev.visibility
                      )}`}
                    >
                      <div>
                        <p className="text-sm font-semibold">{ev.title}</p>
                        <p className="mt-0.5 text-xs opacity-80">{ev.kindLabel}</p>
                      </div>
                      <span className="font-mono text-xs tabular-nums">
                        {ev.time ? formatTime12h(ev.time) : 'All day'}
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </motion.div>
      </AnimatePresence>

      {users && (
        <ItemDetailModal
          target={inspectTarget}
          users={users}
          onClose={() => setInspectTarget(null)}
          onSaveTask={async (id, patch) => {
            await mutations.updateTask.mutateAsync({ id, patch });
          }}
          onDeleteTask={async (id) => {
            await mutations.deleteTask.mutateAsync(id);
          }}
          onSaveReminder={async (id, patch) => {
            await mutations.updateReminder.mutateAsync({ id, patch });
          }}
          onDeleteReminder={async (id) => {
            await mutations.deleteReminder.mutateAsync(id);
          }}
          onSaveAssignment={async (id, patch) => {
            await mutations.updateAssignment.mutateAsync({ id, patch });
          }}
          onDeleteAssignment={async (id) => {
            await mutations.deleteAssignment.mutateAsync(id);
          }}
        />
      )}
    </div>
  );
}
