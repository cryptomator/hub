-- noinspection SqlNoDataSourceInspectionForFile

-- top down lookups ("get members of a group") is already efficient due to PK
-- bottom up lookups ("get groups of a member") need an additional index:
CREATE INDEX GROUP_MEMBERSHIP_IDX_MEMBER ON "group_membership" ("member_id");

-- temporarily drop views
DROP VIEW "effective_vault_access";
DROP VIEW "effective_group_membership";

-- replace effective_group_membership with a table to optimize lookups
CREATE TABLE "effective_group_membership" (
    "group_id" VARCHAR(36) NOT NULL,
    "member_id" VARCHAR(36) NOT NULL,
    "path" TEXT NOT NULL,
    PRIMARY KEY ("group_id", "member_id"),
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_FK_GROUP" FOREIGN KEY ("group_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_FK_MEMBER" FOREIGN KEY ("member_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "EFFECTIVE_GROUP_MEMBERSHIP_CHK_NOTSAME" CHECK ("group_id" <> "member_id")
);

-- top down lookups ("get members of a group") is already efficient due to PK
-- bottom up lookups ("get groups of a member") need an additional index:
CREATE INDEX EFFECTIVE_GROUP_MEMBERSHIP_IDX_MEMBER ON "effective_group_membership" ("member_id");

-- @formatter:off
INSERT INTO "effective_group_membership" ("group_id", "member_id", "path")
WITH RECURSIVE "members" ("root", "member_id", "depth", "path") AS (
    SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
        FROM "group_membership"
    UNION
    SELECT "parent"."root", "child"."member_id", "parent"."depth" + 1, "parent"."path" || '/' || "child"."member_id"
        FROM "group_membership" "child"
        INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id"
        WHERE "parent"."depth" < 10
) SELECT "root", "member_id", "path" FROM "members"
ON CONFLICT DO NOTHING;

CREATE VIEW "effective_vault_access" ("vault_id", "authority_id", "role") AS
	SELECT "va"."vault_id", "va"."authority_id", "va"."role" FROM "vault_access" "va"
	UNION
	SELECT "va"."vault_id", "gm"."member_id", "va"."role" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";
-- @formatter:on