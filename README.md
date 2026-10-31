<<<<<<< HEAD
# Hush — your quiet AI to-do

A solo AI-powered to-do app with the dreamy visual language of
[supaste.com](https://www.supaste.com/) — rolling-hills wallpaper,
Instrument Serif + Inter + Fragment Mono typography, grain overlay,
soft glass panels, indigo accents — and the agent-first task UX of
twodo.design, stripped of the couples/two-person bits.

## What it does

- **Voice or text capture** into natural-language quick-add
  (`call mom tomorrow 5pm #personal !p2`)
- **AI agent surfaces** on every task — a slow gold ring when it's
  on it, an amber "Needs you" card with the agent's output and
  **Looks good / Try again** buttons
- **Multiple views** — Today, Inbox, Upcoming, Someday, Completed,
  per-project; list / board / calendar
- **Projects, priorities, due dates, tags, markdown notes**
- **⌘K command palette**, keyboard shortcuts (`n` new, `a` agent,
  `t` today, `i` inbox)
- **Local-first** persistence via Dexie / IndexedDB
- **Dark + light themes** (Evening + Paper)

## Stack

- Vite + React 18 + TypeScript
- Tailwind CSS with a custom theme matched to supaste's tokens
- Framer Motion for the bloom, agent ring, and panel transitions
- Dexie for IndexedDB
- Zustand for app state
- date-fns + rrule.js for scheduling
- Fonts: Instrument Serif, Inter, Fragment Mono (via Google Fonts)

## Run it

```bash
npm install
npm run dev
```

Open http://localhost:5173 and tap "Enter the app."

## Roadmap

v1 ships UI/UX-complete with AI stubbed behind a clean interface
(`src/ai/agent.ts`). v2 wires a real AI backend — the stub exposes
the exact signature the real one will implement, so the UI needs no
changes.

## Design doc

Full design in [docs/superpowers/specs/](docs/superpowers/specs/).
=======
# Monday
>>>>>>> 98b4fc455270894b9b7758be8a2d515785cb3309
