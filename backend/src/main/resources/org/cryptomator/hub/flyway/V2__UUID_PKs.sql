-- temporarily drop views
DROP VIEW "effective_vault_access";
DROP VIEW "effective_group_membership";

-- temporarily disable foreign key constraints
ALTER TABLE "vault_access" DROP CONSTRAINT "VAULT_ACCESS_FK_VAULT";
ALTER TABLE "access_token" DROP CONSTRAINT "ACCESS_FK_VAULT";
ALTER TABLE "access_token" DROP CONSTRAINT "ACCESS_FK_DEVICE";

-- collate C is more efficient on indexed columns (sadly user ids are generated externally and there is no guarantee for them to be UUIDs):
ALTER TABLE "authority" ALTER COLUMN "id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "group_membership" ALTER COLUMN "group_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "group_membership" ALTER COLUMN "member_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "user_details" ALTER COLUMN "id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "group_details" ALTER COLUMN "id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "vault_access" ALTER COLUMN "authority_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "device" ALTER COLUMN "owner_id" SET DATA TYPE VARCHAR(255) COLLATE "C";
ALTER TABLE "device" ALTER COLUMN "id" SET DATA TYPE VARCHAR(64) COLLATE "C";
ALTER TABLE "access_token" ALTER COLUMN "device_id" SET DATA TYPE VARCHAR(64) COLLATE "C";

-- UUIDs are even more efficient:
ALTER TABLE "vault" ALTER COLUMN "id" SET DATA TYPE UUID USING "id"::uuid;
ALTER TABLE "vault_access" ALTER COLUMN "vault_id" SET DATA TYPE UUID USING "vault_id"::uuid;
ALTER TABLE "access_token" ALTER COLUMN "vault_id" SET DATA TYPE UUID USING "vault_id"::uuid;

-- re-add constraints
ALTER TABLE "vault_access" ADD CONSTRAINT "VAULT_ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE;
ALTER TABLE "access_token" ADD CONSTRAINT "ACCESS_FK_DEVICE" FOREIGN KEY ("device_id") REFERENCES "device" ("id") ON DELETE CASCADE;
ALTER TABLE "access_token" ADD CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE;

-- @formatter:off
CREATE OR REPLACE VIEW "effective_group_membership" ("group_id", "member_id", "path") AS
WITH RECURSIVE "members" ("root", "member_id", "depth", "path") AS (
	SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
	    FROM "group_membership"
	UNION
	SELECT "parent"."root", "child"."member_id", "parent"."depth" + 1, "parent"."path" || '/' || "child"."member_id"
	    FROM "group_membership" "child"
		INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id"
		WHERE "parent"."depth" < 10
) SELECT "root", "member_id", "path" FROM "members";

CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id") AS
	SELECT * FROM "vault_access"
	UNION
	SELECT "va"."vault_id", "gm"."member_id" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on