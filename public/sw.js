const fired = new Set();
let jobs = [];
let timer = null;

self.addEventListener('install', (e) => {
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(self.clients.claim());
  arm();
});

self.addEventListener('message', (e) => {
  const data = e.data || {};
  if (data.type === 'SYNC_REMINDERS' && Array.isArray(data.jobs)) {
    jobs = data.jobs;
    arm();
  }
  if (data.type === 'COMPLETE' && data.taskId) {
    jobs = jobs.filter((j) => j.taskId !== data.taskId);
  }
  if (data.type === 'SHOW_REMINDER' && data.job) {
    const job = data.job;
    fired.add(job.id);
    self.registration.showNotification(job.title, {
      body: job.due ? `Due ${new Date(job.due).toLocaleString()}` : 'Deadline reminder',
      tag: job.id,
      data: { taskId: job.taskId, jobId: job.id },
      actions: [
        { action: 'done', title: 'Done' },
        { action: 'snooze', title: 'Snooze' },
      ],
    });
  }
});

self.addEventListener('notificationclick', (e) => {
  const action = e.action;
  const taskId = e.notification.data && e.notification.data.taskId;
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      const msg = { type: action === 'snooze' ? 'SNOOZE_TASK' : action === 'done' ? 'COMPLETE_TASK' : 'OPEN', taskId };
      if (clients[0]) {
        clients[0].postMessage(msg);
        return clients[0].focus();
      }
      return self.clients.openWindow('/');
    }),
  );
});

function arm() {
  if (timer) clearTimeout(timer);
  const now = Date.now();
  const next = jobs
    .map((j) => ({ j, t: new Date(j.when).getTime() }))
    .filter((x) => x.t > now && !fired.has(x.j.id))
    .sort((a, b) => a.t - b.t)[0];
  if (!next) return;
  const delay = Math.min(next.t - now, 60 * 60 * 1000);
  timer = setTimeout(() => {
    tick();
    arm();
  }, delay);
}

function tick() {
  const now = Date.now();
  for (const job of jobs) {
    const t = new Date(job.when).getTime();
    if (t <= now && !fired.has(job.id)) {
      fired.add(job.id);
      self.registration.showNotification(job.title, {
        body: 'Deadline reminder',
        tag: job.id,
        data: { taskId: job.taskId, jobId: job.id },
        actions: [
          { action: 'done', title: 'Done' },
          { action: 'snooze', title: 'Snooze' },
        ],
      });
    }
  }
}

setInterval(tick, 60 * 1000);
