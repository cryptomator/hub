-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE "billing"
(
    "id"     INT4 NOT NULL,
    "hub_id" VARCHAR(255) NOT NULL,
    "token"  VARCHAR(2000),
    CONSTRAINT "BILLING_PK" PRIMARY KEY ("id")
);

CREATE TABLE "authority"
(
	"id"          VARCHAR(255) NOT NULL,
	"type"        VARCHAR(5) NOT NULL,
	"name"        VARCHAR(255) NOT NULL,
	CONSTRAINT "AUTHORITY_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUTHORITY_CHK_TYPE" CHECK ("type" = 'USER' OR "type" = 'GROUP')
);

CREATE TABLE "group_membership"
(
	"group_id"          VARCHAR(255) NOT NULL,
	"member_id"         VARCHAR(255) NOT NULL,
    CONSTRAINT "GROUP_MEMBERSHIP_PK" PRIMARY KEY ("group_id", "member_id"),
	CONSTRAINT "GROUP_MEMBERSHIP_FK_GROUP" FOREIGN KEY ("group_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "GROUP_MEMBERSHIP_FK_MEMBER" FOREIGN KEY ("member_id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

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

CREATE TABLE "user_details"
(
	"id"          VARCHAR(255) NOT NULL,
	"picture_url" VARCHAR(255),
	"email"       VARCHAR(255),
    CONSTRAINT "USER_DETAIL_PK" PRIMARY KEY ("id"),
	CONSTRAINT "USER_DETAIL_FK_USER" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE TABLE "group_details"
(
    "id"          VARCHAR(255) NOT NULL,
    CONSTRAINT "GROUP_DETAIL_PK" PRIMARY KEY ("id"),
    CONSTRAINT "GROUP_DETAIL_FK_GROUP" FOREIGN KEY ("id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE TABLE "vault"
(
	"id"            VARCHAR(255) NOT NULL,
	"owner_id"      VARCHAR(255) NOT NULL,
	"name"          VARCHAR(255) NOT NULL,
	"description"   VARCHAR(255),
	"creation_time" TIMESTAMP NOT NULL,
	"salt"          VARCHAR(255) NOT NULL,
	"iterations"    VARCHAR(255) NOT NULL,
	"masterkey"     VARCHAR(255) NOT NULL,
	CONSTRAINT "VAULT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "VAULT_FK_OWNER" FOREIGN KEY ("owner_id") REFERENCES "authority" ("id") ON DELETE RESTRICT,
	CONSTRAINT "VAULT_UNIQUE_NAME" UNIQUE ("name")
);

CREATE TABLE "vault_access"
(
	"vault_id"       VARCHAR(255) NOT NULL,
	"authority_id"   VARCHAR(255) NOT NULL,
	CONSTRAINT "VAULT_ACCESS_PK" PRIMARY KEY ("vault_id", "authority_id"),
	CONSTRAINT "VAULT_ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "VAULT_ACCESS_FK_AUTHORITY" FOREIGN KEY ("authority_id") REFERENCES "authority" ("id") ON DELETE CASCADE
);

CREATE OR REPLACE VIEW "effective_vault_access" ("vault_id", "authority_id") AS
	SELECT * FROM "vault_access"
	UNION
	SELECT "va"."vault_id", "gm"."member_id" FROM "vault_access" "va"
		INNER JOIN "effective_group_membership" "gm" ON "va"."authority_id" = "gm"."group_id";

CREATE TABLE "device"
(
	"id"         VARCHAR(255) NOT NULL,
	"owner_id"   VARCHAR(255) NOT NULL,
	"name"       VARCHAR(255) NOT NULL,
	"publickey"  VARCHAR(255) NOT NULL,
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_FK_USER" FOREIGN KEY ("owner_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "DEVICE_UNIQUE_NAME_PER_OWNER" UNIQUE ("owner_id", "name")
);

CREATE TABLE "access_token"
(
	"device_id" VARCHAR(255) NOT NULL,
	"user_id"   VARCHAR(255) NOT NULL,
	"vault_id"  VARCHAR(255) NOT NULL,
	"jwe"       VARCHAR(2000) NOT NULL UNIQUE,
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("device_id", "user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_DEVICE" FOREIGN KEY ("device_id") REFERENCES "device" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_USER" FOREIGN KEY ("user_id") REFERENCES "authority" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE
);