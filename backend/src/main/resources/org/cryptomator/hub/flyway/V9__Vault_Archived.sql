ALTER TABLE "vault" ADD COLUMN "archived" BOOLEAN NOT NULL DEFAULT false;

-- @formatter:off
CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id") AS
	SELECT "vault_id", "authority_id" FROM "vault_access" "va"
	    INNER JOIN "vault" "v" ON "v"."id" = "va"."vault_id"
    WHERE NOT "v"."archived"
	UNION
	SELECT "va"."vault_id", "gm"."member_id" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id"
	    INNER JOIN "vault" "v" ON "v"."id" = "va"."vault_id"
    WHERE NOT "v"."archived";
-- @formatter:on
