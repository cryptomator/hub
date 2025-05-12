# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.1](https://github.com/cryptomator/hub/compare/1.4.0...1.4.1)

### Fixed

- Fixed licence refresh, which had not been executed since version 1.4.0 (#341)

## [1.4.0](https://github.com/cryptomator/hub/compare/1.3.4...1.4.0)

### Added

- This CHANGELOG file
- WoT: Users will now have an ECDH as well as ECDSA key (#282)
- WoT: Users can now mutually verify their identity, hardening Hub against injection of malicious public keys (#281)
- WoT: Admins can adjust WoT parameters (#297)
- Permission to create new vaults can now be controlled via the `create-vaults` role in Keycloak (#206)
- Preserver user locale setting (#313)
- New log event entries: UserAccountReset, UserKeysChange and UserSetupCodeChange (#310)
- Audit log filter by event type (#312)
- Show last IP address and last vault access timestamp of devices in user profile (#320)
- Dutch, French, Italian, Korean, Portuguese and Turkish translation
- Added provenance attestation for our container images (#322)
- Show legacy devices in user profile (#331)
- Show direct member count of groups in vault details (#329)

### Changed

- Updated Keycloak to 26.1.5
- Updated to Java 21 (#272)
- Updated to Quarkus 3.15.4 LTS
- Updated to Tailwind CSS 4
- Updated to Vite 6
- Reduced number of transitive dependencies
- Bumped build time dependencies
- Migrated remaining commonjs modules in frontend build to ESM (#291)
- Memoize infrequently changing data, reducing XHR roundtrips
- Switched to JWK thumbprint format in user profile
- Switched to Repository Pattern (#273)
- Redesigned Admin Panel (#308)
- Enhanced audit log VaultKeyRetrievedEvent, contains now IP address and device ID (#320)
- Migrate syncer user to cryptomatorhub-system client (#336)

### Fixed

- Fixed incorrect ARIA roles improving accessibility
- Fixed incorrect `Content-Type` header for `/api/vaults/{vaultId}/access-token` (#284)
- Show legacy device name in audit log (#331)
- Added "Browser Language" option to language selection dropdown, enabling users to revert to browser default language (#324)

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
- CVE-2025-27597: Vue I18n Allows Prototype Pollution in `handleFlatJson`
- CVE-2025-27152: axios Requests Vulnerable To Possible SSRF and Credential Leakage via Absolute URL

## [1.4.0-rc3](https://github.com/cryptomator/hub/compare/1.4.0-rc2...1.4.0-rc3) (2025-03-27)

### Changed

- Migrate syncer user to cryptomatorhub-system client (#336)
- Updated to Quarkus 3.15.4 LTS

## [1.4.0-rc2](https://github.com/cryptomator/hub/compare/1.4.0-rc1...1.4.0-rc2) (2025-03-17)

### Added

- Show direct member count of groups in vault details (#329)

### Fixed

- Added "Browser Language" option to language selection dropdown, enabling users to revert to browser default language (#324)
- Reload device lists upon device removal
- Added pointer cursor to device remove "button" text
- Show device only when available in audit log vault key retrieve event

## [1.4.0-rc1](https://github.com/cryptomator/hub/compare/1.4.0-beta3...1.4.0-rc1) (2025-03-14)

### Added

- Show legacy devices in user profile (#331)

### Changed

- Updated to Quarkus 3.15.3.1 LTS
- Updated Keycloak to 26.1.4

### Fixed

- Show legacy device name in audit log (#331)

### Security

- CVE-2025-27597: Vue I18n Allows Prototype Pollution in `handleFlatJson`
- CVE-2025-27152: axios Requests Vulnerable To Possible SSRF and Credential Leakage via Absolute URL

## [1.4.0-beta3](https://github.com/cryptomator/hub/compare/1.4.0-beta2...1.4.0-beta3) (2025-02-22)

### Added

- Audit log filter by event type (#312)
- Show last IP address and last vault access timestamp of devices in user profile (#320)
- Added provenance attestation for our container images (#322)

### Changed

- Updated to Quarkus 3.15.3 LTS
- Enhanced audit log VaultKeyRetrievedEvent, contains now IP address and device ID (#320)

## [1.4.0-beta2](https://github.com/cryptomator/hub/compare/1.4.0-beta1...1.4.0-beta2) (2025-02-13)

### Added

- New log event entries: UserAccountReset, UserKeysChange and UserSetupCodeChange (#310)
- WoT: Admins can adjust WoT parameters (#297)
- Preserver user locale setting (#313)
- Add Italian, Korean, Dutch and Portuguese translation

### Changed

- Updated Keycloak to 26.1.2
- Updated to Quarkus 3.15.2 LTS
- Updated to Tailwind CSS 4
- Updated to Vite 6
- Reduced number of transitive dependencies
- Bumped build time dependencies
- Redesigned Admin Panel (#308)

### Security

- CVE-2024-4067: Regular Expression Denial of Service (ReDoS) in micromatch
- CVE-2024-21538: Regular Expression Denial of Service (ReDoS) in cross-spawn
- CVE-2024-21539: Regular Expression Denial of Service (ReDoS) in @eslint/plugin-kit
- CVE-2024-45811: Vite's `server.fs.deny` is bypassed when using `?import&raw`
- CVE-2024-45812: Vite DOM Clobbering gadget found in vite bundled scripts that leads to XSS
- CVE-2024-47068: DOM Clobbering Gadget found in rollup bundled scripts that leads to XSS
- CVE-2024-52809: vue-i18n has cross-site scripting vulnerability with prototype pollution
- CVE-2024-52810: @intlify/shared Prototype Pollution vulnerability
- CVE-2024-55565: Predictable results in nanoid generation when given non-integer values
- CVE-2025-24010: Vite development server responded to arbitrary requests

## [1.4.0-beta1](https://github.com/cryptomator/hub/compare/1.3.4...1.4.0-beta1) (2024-10-31)

### Added

- This CHANGELOG file
- WoT: Users will now have an ECDH as well as ECDSA key (#282)
- WoT: Users can now mutually verify their identity, hardening Hub against injection of malicious public keys (#281)
- Permission to create new vaults can now be controlled via the `create-vaults` role in Keycloak (#206)

### Changed

- Updated Keycloak to 25.0.6
- Updated to Java 21 (#272)
- Updated to Quarkus 3.15.x LTS
- Bumped build time dependencies
- Migrated remaining commonjs modules in frontend build to ESM (#291)
- Memoize infrequently changing data, reducing XHR roundtrips
- Switched to JWK thumbprint format in user profile
- Switched to Repository Pattern (#273)

### Fixed

- Fixed incorrect ARIA roles improving accessibility
- Fixed incorrect `Content-Type` header for `/api/vaults/{vaultId}/access-token` (#284)

### Security

- CVE-2023-45133: Babel vulnerable to arbitrary code execution when compiling specifically crafted malicious code 
- CVE-2024-4068: Uncontrolled resource consumption in braces
- CVE-2024-39338: Server-Side Request Forgery in axios