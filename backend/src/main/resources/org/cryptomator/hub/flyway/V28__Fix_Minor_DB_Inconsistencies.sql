DROP VIEW "effective_vault_access";

-- align effective_group_membership authority id columns with "authority"."id" by collating them
ALTER TABLE "effective_group_membership" ALTER COLUMN "group_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "effective_group_membership" ALTER COLUMN "member_id" SET DATA TYPE VARCHAR(255) COLLATE "C";

-- make device_id columns the same width as in all other tables
ALTER TABLE "audit_event_device_register" ALTER COLUMN "device_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "audit_event_device_remove" ALTER COLUMN "device_id" SET DATA TYPE VARCHAR(255) COLLATE "C";

-- align device id columns with "legacy_device" columns by collating them
ALTER TABLE "device" ALTER COLUMN "id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "device" ALTER COLUMN "owner_id" SET DATA TYPE VARCHAR(255) COLLATE "C";

-- @formatter:off
CREATE VIEW "effective_vault_access" ("vault_id", "authority_id", "role") AS
SELECT "va"."vault_id", "va"."authority_id", "va"."role" FROM "vault_access" "va"
UNION
SELECT "va"."vault_id", "gm"."member_id", "va"."role" FROM "vault_access" "va"
															   INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on