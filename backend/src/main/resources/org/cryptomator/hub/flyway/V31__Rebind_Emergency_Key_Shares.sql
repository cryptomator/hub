-- Key shares created before context binding (critical `ctx` JWE header parameter) can no longer be validated by the
-- frontend and must be re-created by the vault owners. See GHSL finding 21613 (cross-vault decryption oracle).
DELETE FROM "emergency_recovery_processes"; -- cascades to recovered_emergency_key_shares
DELETE FROM "emergency_key_shares";
UPDATE "vault" SET "required_emergency_key_shares" = 0;
