# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/cryptomator/hub/compare/1.3.4...HEAD)

### Added

- This CHANGELOG file
- WoT: Users will now have an ECDH as well as ECDSA key (#282)
- WoT: Users can now mutually verify their identity, hardening Hub against injection of malicious public keys (#281)
- WoT: Admins can adjust WoT parameters (#297)
- Permission to create new vaults can now be controlled via the `create-vaults` role in Keycloak (#206)
- Preserver user locale setting (#313)
- Italian, Korean, Dutch and Portuguese translation
- Audit log filter by event type

### Changed

- Updated Keycloak to 25.0.6
- Updated to Java 21 (#272)
- Updated to Quarkus 3.8.x LTS (#272)
- Updated to tailwindcss 4
- Updated to Vite 6
- Reduced number of transitive dependencies
- Bumped build time dependencies
- Migrated remaining commonjs modules in frontend build to ESM (#291)
- Memoize infrequently changing data, reducing XHR roundtrips
- Switched to JWK thumbprint format in user profile
- Switched to Repository Pattern (#273)
- Redesigned Admin Panel (#308)

### Fixed

- Fixed incorrect ARIA roles improving accessibility
- Fixed incorrect `Content-Type` header for `/api/vaults/{vaultId}/access-token` (#284)

### Security

- CVE-2023-45133: Babel vulnerable to arbitrary code execution when compiling specifically crafted malicious code 
- CVE-2024-4067: Regular Expression Denial of Service (ReDoS) in micromatch
- CVE-2024-4068: Uncontrolled resource consumption in braces
- CVE-2024-21538: Regular Expression Denial of Service (ReDoS) in cross-spawn
- CVE-2024-21539: Regular Expression Denial of Service (ReDoS) in @eslint/plugin-kit
- CVE-2024-39338: Server-Side Request Forgery in axios
- CVE-2024-45811: Vite's `server.fs.deny` is bypassed when using `?import&raw`
- CVE-2024-45812: Vite DOM Clobbering gadget found in vite bundled scripts that leads to XSS
- CVE-2024-47068: DOM Clobbering Gadget found in rollup bundled scripts that leads to XSS
- CVE-2024-52809: vue-i18n has cross-site scripting vulnerability with prototype pollution
- CVE-2024-52810: @intlify/shared Prototype Pollution vulnerability
- CVE-2024-55565: Predictable results in nanoid generation when given non-integer values
- CVE-2025-24010: Vite development server responded to arbitrary requests
