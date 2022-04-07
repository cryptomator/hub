CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id", "authority_type") AS
	SELECT * FROM "vault_access"
	UNION
	SELECT "va"."vault_id", "gm"."member_id", "gm"."member_type" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id" AND "va"."authority_type" = 'group';
