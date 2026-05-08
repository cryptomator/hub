# Cryptomator Frontend

This project uses Vue 3 + Typescript + Vite.

## Recommended IDE Setup

- [VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=johnsoncodehk.volar)

## Package Manager

This project uses [pnpm](https://pnpm.io/). The version is pinned via the
`packageManager` field in `package.json` and managed by Corepack:

```shell script
corepack enable
```

Do not use `npm` or `yarn` — they bypass the security gates configured in
`pnpm-workspace.yaml` (lifecycle script allowlist, install cooldown,
non-registry source blocking).

### Install Cooldown

When adding a brand-new dependency or upgrading to a freshly published
version, pnpm will resolve to the most recent version older than the
3 days. If the very latest version is required (e.g. a
critical security fix), add the package to `minimumReleaseAgeExclude` in
`pnpm-workspace.yaml`.

### Dependency Upgrades

Don't run `pnpm up --latest` on the default branch. Routine bumps land
through Dependabot (monthly grouped minor/patch PRs, configured in
`.github/dependabot.yml`). For an out-of-cycle upgrade, open a PR with
explicit version pins in `package.json` so the change is reviewable.

## Dev Mode

You can run your application in dev mode that enables live coding using:

```shell script
pnpm install
pnpm dev
```

## Production Build

To build an optimized (production) version of the app, run:

```shell script
pnpm install
pnpm dist
```
