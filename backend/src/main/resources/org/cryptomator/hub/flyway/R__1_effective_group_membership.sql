CREATE OR REPLACE VIEW "effective_group_membership" ("group_id", "member_id", "member_type") AS
WITH RECURSIVE "members" ("root", "parent", "member_id", "member_type") AS (
	SELECT "group_id", "group_id", "member_id", "member_type" FROM "group_membership"
	UNION
	SELECT "parent"."root", "parent"."member_id", "child"."member_id", "child"."member_type" FROM "group_membership" "child"
		INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id" AND "child"."group_type" = "parent"."member_type"
) SELECT "root", "member_id", "member_type" FROM "members";
