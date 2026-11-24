import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { ArrowRight, CalendarClock, CheckCircle2, Lock, Plus, Users } from 'lucide-react';
import { AnimatedCheckbox } from '../components/AnimatedCheckbox';
import { EmptyState } from '../components/EmptyState';
import { InspectableItem, ItemDetailModal } from '../components/ItemDetailModal';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useDuoDeskMutations, useDuoDeskQueries } from '../hooks/useDuoDeskData';
import {
  formatShortDate,
  formatTime12h,
  getPriorityMeta,
  getTodayIso,
} from '../utils/formatters';

export function HomePage() {
  const { setCreateDefaults } = useWorkspace();
  const navigate = useNavigate();
  const { usersQuery, tasksQuery, assignmentsQuery, remindersQuery } = useDuoDeskQueries();
  const mutations = useDuoDeskMutations();

  const [inspectTarget, setInspectTarget] = useState<InspectableItem | null>(null);

  const users = usersQuery.data;
  const tasks = tasksQuery.data ?? [];
  const assignments = assignmentsQuery.data ?? [];
  const reminders = remindersQuery.data ?? [];
  const todayIso = getTodayIso();

  // Today's Progress stats
  const todayTasksAll = useMemo(() => {
    const dueToday = tasks.filter((t) => t.dueDate === todayIso);
    return dueToday.length > 0 ? dueToday : tasks.slice(0, 6);
  }, [tasks, todayIso]);

  const totalCount = todayTasksAll.length;
  const completedCount = todayTasksAll.filter((t) => t.completed).length;
  const pendingCount = totalCount - completedCount;
  const completionPct = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

  // Today's Tasks cards (max 4 to keep Home calm & minimal)
  const displayTodayTasks = useMemo(() => {
    return [...todayTasksAll]
      .sort((a, b) => {
        if (a.completed !== b.completed) return a.completed ? 1 : -1;
        return (a.dueTime ?? '23:59').localeCompare(b.dueTime ?? '23:59');
      })
      .slice(0, 4);
  }, [todayTasksAll]);

  // Compact Upcoming deadlines (next 3 items after today or non-completed assignments/reminders)
  const upcomingItems = useMemo(() => {
    const items: {
      id: string;
      title: string;
      date: string;
      time?: string;
      visibility: 'private' | 'shared';
      kind: 'Assignment' | 'Reminder' | 'Task';
      raw: InspectableItem;
    }[] = [];

    assignments
      .filter((a) => a.status !== 'completed')
      .forEach((a) =>
        items.push({
          id: `a-${a.id}`,
          title: a.title,
          date: a.deadline,
          time: a.deadlineTime,
          visibility: a.visibility,
          kind: 'Assignment',
          raw: { type: 'assignment', item: a },
        })
      );

    reminders
      .filter((r) => !r.completed && r.date >= todayIso)
      .forEach((r) =>
        items.push({
          id: `r-${r.id}`,
          title: r.title,
          date: r.date,
          time: r.time,
          visibility: r.visibility,
          kind: 'Reminder',
          raw: { type: 'reminder', item: r },
        })
      );

    tasks
      .filter((t) => !t.completed && t.dueDate > todayIso)
      .forEach((t) =>
        items.push({
          id: `t-${t.id}`,
          title: t.title,
          date: t.dueDate,
          time: t.dueTime,
          visibility: t.visibility,
          kind: 'Task',
          raw: { type: 'task', item: t },
        })
      );

    return items.sort((a, b) => a.date.localeCompare(b.date)).slice(0, 3);
  }, [assignments, reminders, tasks, todayIso]);

  return (
    <div className="space-y-8">
      {/* Main Greeting Headline */}
      <section className="pt-1">
        <h1 className="max-w-lg font-display text-3xl font-bold tracking-tight text-balance text-[#18181F] sm:text-4xl dark:text-white">
          Let&apos;s make today productive.
        </h1>
      </section>

      {/* Single Today's Progress Card */}
      <motion.section
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
        className="relative overflow-hidden rounded-3xl border border-[#EAE4DC] bg-gradient-to-br from-[#F8D8E4]/75 via-[#E6DFFB]/70 to-[#FCE6D4]/75 p-6 shadow-[0_12px_36px_rgba(24,24,31,0.04)] sm:p-7 dark:border-[#2E2D3B] dark:from-[#2C2334] dark:via-[#262338] dark:to-[#2E2623]"
      >
        <div className="flex flex-col justify-between gap-6 sm:flex-row sm:items-center">
          <div className="space-y-2">
            <p className="text-xs font-semibold tracking-wide text-[#5B469C] uppercase dark:text-[#D5C9FA]">
              Today&apos;s Progress
            </p>
            <div className="flex items-baseline gap-3">
              <span className="font-display text-4xl font-bold tracking-tight text-[#18181F] tabular-nums sm:text-5xl dark:text-white">
                {completionPct}%
              </span>
              <span className="text-sm font-medium text-[#5A5463] dark:text-[#C4BED0]">
                completed today
              </span>
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-5 pt-1 text-xs font-medium text-[#4A4552] dark:text-[#D1CBD9]">
              <span className="tabular-nums">
                <strong className="font-semibold text-[#18181F] dark:text-white">
                  {totalCount}
                </strong>{' '}
                Total
              </span>
              <span aria-hidden="true">·</span>
              <span className="tabular-nums">
                <strong className="font-semibold text-[#18181F] dark:text-white">
                  {completedCount}
                </strong>{' '}
                Completed
              </span>
              <span aria-hidden="true">·</span>
              <span className="tabular-nums">
                <strong className="font-semibold text-[#18181F] dark:text-white">
                  {pendingCount}
                </strong>{' '}
                Pending
              </span>
            </div>
          </div>

          {/* Soft Circular Progress Ring */}
          <div className="flex items-center gap-4 self-start sm:self-center">
            <div className="relative flex h-20 w-20 items-center justify-center rounded-full bg-white/75 shadow-xs dark:bg-[#1B1B24]/70">
              <svg className="h-16 w-16 -rotate-90" viewBox="0 0 64 64">
                <circle
                  cx="32"
                  cy="32"
                  r="26"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="6"
                  className="text-[#EAE4DC] dark:text-[#353344]"
                />
                <motion.circle
                  cx="32"
                  cy="32"
                  r="26"
                  fill="none"
                  stroke="#9B82F3"
                  strokeWidth="6"
                  strokeLinecap="round"
                  strokeDasharray={2 * Math.PI * 26}
                  initial={{ strokeDashoffset: 2 * Math.PI * 26 }}
                  animate={{
                    strokeDashoffset:
                      2 * Math.PI * 26 * (1 - completionPct / 100),
                  }}
                  transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
                />
              </svg>
              <CheckCircle2 className="absolute h-5 w-5 text-[#9B82F3]" />
            </div>
          </div>
        </div>
      </motion.section>

      {/* Today's Tasks Section */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-bold text-[#18181F] dark:text-white">
            Today&apos;s Tasks
          </h2>
          <button
            type="button"
            onClick={() => navigate('/my-space')}
            className="inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold text-[#6E6A75] transition-colors hover:bg-white hover:text-[#18181F] dark:text-[#A19CA8] dark:hover:bg-[#23222E] dark:hover:text-white"
          >
            <span>View All</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        {displayTodayTasks.length === 0 ? (
          <EmptyState
            icon={CheckCircle2}
            title="Nothing queued for today"
            description="Add a task to your personal space or collaborate with Alex in Shared Space."
            actionLabel="Create task"
            onAction={() => {
              setCreateDefaults({ kind: 'task', visibility: 'private' });
              navigate('/create');
            }}
          />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {displayTodayTasks.map((task, idx) => {
              const pMeta = getPriorityMeta(task.priority);
              const isShared = task.visibility === 'shared';

              return (
                <motion.div
                  key={task.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.2, delay: idx * 0.04 }}
                  onClick={() => setInspectTarget({ type: 'task', item: task })}
                  className="group flex cursor-pointer items-start gap-3.5 rounded-3xl border border-[#EAE4DC] bg-white p-5 shadow-[0_6px_20px_rgba(24,24,31,0.03)] transition-all duration-150 hover:-translate-y-0.5 hover:border-[#D9D1C7] dark:border-[#2E2D3B] dark:bg-[#1F1F29] dark:hover:border-[#3E3C4F]"
                >
                  <div className="pt-0.5">
                    <AnimatedCheckbox
                      checked={task.completed}
                      accent={isShared ? 'lavender' : 'pink'}
                      label={`Complete ${task.title}`}
                      onChange={() =>
                        mutations.updateTask.mutate({
                          id: task.id,
                          patch: { completed: !task.completed },
                        })
                      }
                    />
                  </div>

                  <div className="min-w-0 flex-1">
                    <p
                      className={`truncate text-sm font-semibold ${
                        task.completed
                          ? 'text-[#9A94A3] line-through'
                          : 'text-[#18181F] dark:text-white'
                      }`}
                    >
                      {task.title}
                    </p>

                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
                      {task.dueTime && (
                        <>
                          <span className="font-mono tabular-nums">
                            {formatTime12h(task.dueTime)}
                          </span>
                          <span aria-hidden="true">·</span>
                        </>
                      )}
                      <span className="inline-flex items-center gap-1.5">
                        <span
                          className="h-2 w-2 rounded-full"
                          style={{ backgroundColor: pMeta.dotColor }}
                        />
                        {task.priority === 'high'
                          ? 'High'
                          : task.priority === 'medium'
                          ? 'Medium'
                          : 'Low'}
                      </span>
                      <span aria-hidden="true">·</span>
                      <span className="inline-flex items-center gap-1">
                        {isShared ? (
                          <Users className="h-3 w-3 text-[#9B82F3]" />
                        ) : (
                          <Lock className="h-3 w-3 text-[#E684A7]" />
                        )}
                        {isShared ? 'Shared' : 'Personal'}
                      </span>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </section>

      {/* Compact Upcoming Section */}
      <section className="space-y-3.5">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-bold text-[#18181F] dark:text-white">
            Upcoming
          </h2>
          <button
            type="button"
            onClick={() => navigate('/calendar')}
            className="inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold text-[#6E6A75] transition-colors hover:bg-white hover:text-[#18181F] dark:text-[#A19CA8] dark:hover:bg-[#23222E]"
          >
            <span>Open calendar</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="divide-y divide-[#F2EDE6] overflow-hidden rounded-3xl border border-[#EAE4DC] bg-white shadow-[0_6px_20px_rgba(24,24,31,0.03)] dark:divide-[#2E2D3B] dark:border-[#2E2D3B] dark:bg-[#1F1F29]">
          {upcomingItems.length === 0 ? (
            <div className="flex items-center justify-between px-6 py-5 text-xs text-[#6E6A75]">
              <span>No upcoming deadlines in the next few days.</span>
              <button
                type="button"
                onClick={() => navigate('/create')}
                className="inline-flex items-center gap-1 font-semibold text-[#9B82F3] hover:underline"
              >
                <Plus className="h-3.5 w-3.5" /> Add one
              </button>
            </div>
          ) : (
            upcomingItems.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setInspectTarget(item.raw)}
                className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left transition-colors hover:bg-[#FAF7F2] dark:hover:bg-[#272733]"
              >
                <div className="flex min-w-0 items-center gap-3.5">
                  <span
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl ${
                      item.visibility === 'shared'
                        ? 'bg-[#E6DFFB] text-[#5B469C] dark:bg-[#2D283E] dark:text-[#D5C9FA]'
                        : 'bg-[#FCE6D4] text-[#9A5528] dark:bg-[#332A24] dark:text-[#FCE6D4]'
                    }`}
                  >
                    <CalendarClock className="h-4 w-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-[#18181F] dark:text-white">
                      {item.title}
                    </p>
                    <p className="mt-0.5 text-xs text-[#6E6A75] dark:text-[#A19CA8]">
                      {item.kind} · {item.visibility === 'shared' ? 'Shared Space' : 'My Space'}
                    </p>
                  </div>
                </div>

                <span className="shrink-0 font-mono text-xs font-medium text-[#6E6A75] tabular-nums dark:text-[#A19CA8]">
                  {formatShortDate(item.date)}
                </span>
              </button>
            ))
          )}
        </div>
      </section>

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
