import { db } from '../db/db';

export async function requestNotifyPermission(): Promise<NotificationPermission> {
  if (!('Notification' in window)) return 'denied';
  if (Notification.permission === 'granted') return 'granted';
  return Notification.requestPermission();
}

export async function registerNotifyWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) return null;
  try {
    const reg = await navigator.serviceWorker.register('/sw.js');
    return reg;
  } catch {
    return null;
  }
}

export async function syncReminders(): Promise<void> {
  const tasks = await db.tasks.toArray();
  const jobs = tasks
    .filter((t) => t.state !== 'done' && t.due)
    .flatMap((t) =>
      (t.reminders ?? []).map((when) => ({
        id: `${t.id}:${when}`,
        taskId: t.id,
        title: t.title,
        due: t.due!,
        when,
      })),
    );
  const payload = { type: 'SYNC_REMINDERS', jobs };
  navigator.serviceWorker?.controller?.postMessage(payload);
  localStorage.setItem('hush.reminderJobs', JSON.stringify(jobs));
  armLocalTimers(jobs);
}

type Job = { id: string; taskId: string; title: string; due: string; when: string };
const armed = new Set<string>();
const timers = new Map<string, number>();

function armLocalTimers(jobs: Job[]) {
  const now = Date.now();
  for (const [id, handle] of timers) {
    if (!jobs.some((j) => j.id === id)) {
      window.clearTimeout(handle);
      timers.delete(id);
      armed.delete(id);
    }
  }
  for (const job of jobs) {
    const delay = new Date(job.when).getTime() - now;
    if (delay <= 0 || delay > 2_147_000_000) continue;
    if (timers.has(job.id)) continue;
    const handle = window.setTimeout(() => {
      fireReminder(job);
      timers.delete(job.id);
    }, delay);
    timers.set(job.id, handle);
  }
}

export function fireReminder(job: Job) {
  if (armed.has(job.id)) return;
  armed.add(job.id);
  if (Notification.permission !== 'granted') return;
  const payload = {
    type: 'SHOW_REMINDER',
    job,
  };
  if (navigator.serviceWorker?.controller) {
    navigator.serviceWorker.controller.postMessage(payload);
    return;
  }
  const n = new Notification(job.title, {
    body: `Due ${new Date(job.due).toLocaleString()}`,
    tag: job.id,
    data: { taskId: job.taskId, jobId: job.id },
    requireInteraction: true,
  });
  n.onclick = () => window.focus();
}

export async function maybeMorningSummary(): Promise<void> {
  const today = new Date().toISOString().slice(0, 10);
  if (localStorage.getItem('hush.morning') === today) return;
  const hour = new Date().getHours();
  if (hour < 7 || hour > 11) return;
  const tasks = await db.tasks.toArray();
  const start = new Date(); start.setHours(0, 0, 0, 0);
  const week = start.getTime() + 7 * 86400000;
  const n = tasks.filter((t) => t.state !== 'done' && t.due && new Date(t.due).getTime() < week && new Date(t.due).getTime() >= start.getTime()).length;
  localStorage.setItem('hush.morning', today);
  if (n === 0 || Notification.permission !== 'granted') return;
  new Notification(`${n} thing${n === 1 ? '' : 's'} due this week`, {
    body: 'Open Hush to review deadlines.',
    tag: 'hush-morning',
  });
}

export async function initNotifications(): Promise<void> {
  await registerNotifyWorker();
  if (!('Notification' in window)) return;
  if (Notification.permission === 'default') {
    await Notification.requestPermission();
  }
  if (Notification.permission === 'granted') {
    await syncReminders();
    await maybeMorningSummary();
  }
}
