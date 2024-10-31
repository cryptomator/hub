# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased](https://github.com/cryptomator/hub/compare/1.3.4...HEAD)

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

