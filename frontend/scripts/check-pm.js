#!/usr/bin/env node
// Refuse to run under any package manager other than pnpm.
//
// pnpm, npm, and yarn all set npm_config_user_agent on scripts they
// invoke. The string begins with "<pm>/<version>", so a prefix check
// is enough to tell them apart.
//
// Wired into package.json as `preinstall` (the security boundary —
// where dependencies are fetched and lifecycle scripts can run) and
// chained into `start` / `build` so a contributor who types
// `npm run build` gets a clear error instead of a confusing failure.

const ua = process.env.npm_config_user_agent || '';

if (!ua.startsWith('pnpm/')) {
  console.error('');
  console.error('  This repo is pinned to pnpm (see "packageManager" in package.json).');
  console.error('  Detected package manager: ' + (ua.split(' ')[0] || '(none)'));
  console.error('');
  console.error('  Use:  corepack enable && pnpm install');
  console.error('');
  process.exit(1);
}
