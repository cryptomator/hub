-- noinspection SqlNoDataSourceInspectionForFile

-- temporarily drop views
DROP VIEW "effective_vault_access";

-- temporarily disable foreign key constraints
ALTER TABLE "vault_access" DROP CONSTRAINT "VAULT_ACCESS_FK_VAULT";
ALTER TABLE "access_token" DROP CONSTRAINT "ACCESS_FK_VAULT";

-- UUIDs are even more efficient:
ALTER TABLE "vault" ALTER COLUMN "id" SET DATA TYPE UUID USING "id"::uuid;
ALTER TABLE "vault_access" ALTER COLUMN "vault_id" SET DATA TYPE UUID USING "vault_id"::uuid;
ALTER TABLE "access_token" ALTER COLUMN "vault_id" SET DATA TYPE UUID USING "vault_id"::uuid;

-- re-add constraints
ALTER TABLE "vault_access" ADD CONSTRAINT "VAULT_ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE;
ALTER TABLE "access_token" ADD CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE;

-- @formatter:off
CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id") AS
	SELECT * FROM "vault_access"
	UNION
	SELECT "va"."vault_id", "gm"."member_id" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on