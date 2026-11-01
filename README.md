# Orbit Chat

A dark, responsive chat interface preview for web and desktop.

## Current status

This repository contains a frontend preview only. It does **not** connect to an AI provider yet, and it does not use Claude login, cookies, session tokens, or API keys.

## Run locally

Open `index.html` in a browser, or run:

```bash
npm start
```

## Files

- `index.html` — app layout
- `style.css` — responsive dark theme
- `app.js` — local demo interactions
- `package.json` — basic start script

## Next steps

1. Add an authorized backend for an AI provider.
2. Keep provider credentials on the server, never in browser code.
3. Add secure desktop packaging after the web app is working.