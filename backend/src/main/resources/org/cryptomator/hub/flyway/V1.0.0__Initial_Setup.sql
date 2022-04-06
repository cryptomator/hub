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
	CONSTRAINT "AUTHORITY_PK" PRIMARY KEY ("id", "type"),
	CONSTRAINT "AUTHORITY_CHK_TYPE" CHECK ("type" = 'user' OR "type" = 'group')
);

CREATE TABLE "group_membership"
(
	"group_id"          VARCHAR(255) NOT NULL,
	"group_type"        VARCHAR(5) NOT NULL,
	"member_id"         VARCHAR(255) NOT NULL,
    "member_type"       VARCHAR(5) NOT NULL,
    CONSTRAINT "GROUP_MEMBERSHIP_PK" PRIMARY KEY ("group_id", "group_type", "member_id", "member_type"),
	CONSTRAINT "GROUP_MEMBERSHIP_FK_GROUP" FOREIGN KEY ("group_id", "group_type") REFERENCES "authority" ("id", "type") ON DELETE CASCADE,
	CONSTRAINT "GROUP_MEMBERSHIP_FK_MEMBER" FOREIGN KEY ("member_id", "member_type") REFERENCES "authority" ("id", "type") ON DELETE CASCADE,
	CONSTRAINT "GROUP_MEMBERSHIP_CHK_TYPE" CHECK ("group_type" = 'group')
);

CREATE VIEW "effective_group_membership" ("group_id", "member_id", "member_type") AS
WITH RECURSIVE "members" ("group_id", "member_id", "member_type") AS (
	SELECT "gm"."group_id", "gm"."member_id", "gm"."member_type" FROM "group_membership" "gm"
	UNION
	SELECT "mem"."group_id", "gm"."member_id", "gm"."member_type" FROM "group_membership" "gm"
		INNER JOIN "members" "mem" ON "gm"."group_id" = "mem"."member_id" AND "gm"."group_type" = "mem"."member_type"
) SELECT * FROM "members";

-- demo daten
INSERT INTO "authority" (id, type, name) VALUES
	('1', 'group', 'Group1'),
	('2', 'group', 'Group2'),
	('1', 'user', 'User1'),
	('2', 'user', 'User2'),
	('3', 'user', 'User3'),
	('4', 'user', 'User4');

INSERT INTO "group_membership" ("group_id", "group_type", "member_id", "member_type") VALUES
	('1', 'group', '2', 'group'),
	('1', 'group', '1', 'user'),
	('2', 'group', '2', 'user'),
	('1', 'group', '3', 'user'),
	('2', 'group', '3', 'user');
-- end demo daten

CREATE TABLE "user_details"
(
	"id"          VARCHAR(255) NOT NULL,
	"type"        VARCHAR(5) NOT NULL,
	"picture_url" VARCHAR(255),
	"email"       VARCHAR(255),
    CONSTRAINT "USER_DETAIL_PK" PRIMARY KEY ("id", "type"),
	CONSTRAINT "USER_DETAIL_FK_USER" FOREIGN KEY ("id", "type") REFERENCES "authority" ("id", "type") ON DELETE CASCADE,
	CONSTRAINT "USER_DETAIL_CHK_TYPE" CHECK ("type" = 'user')
);

CREATE TABLE "vault"
(
	"id"            VARCHAR(255) NOT NULL,
	"owner"         VARCHAR(255) NOT NULL,
	"owner_type"    VARCHAR(5) NOT NULL,
	"name"          VARCHAR(255) NOT NULL,
	"description"   VARCHAR(255),
	"creation_time" TIMESTAMP NOT NULL,
	"salt"          VARCHAR(255) NOT NULL,
	"iterations"    VARCHAR(255) NOT NULL,
	"masterkey"     VARCHAR(255) NOT NULL,
	CONSTRAINT "VAULT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "VAULT_FK_OWNER" FOREIGN KEY ("owner", "owner_type") REFERENCES "authority" ("id", "type"),
	CONSTRAINT "VAULT_UNIQUE_NAME" UNIQUE ("name"),
	CONSTRAINT "VAULT_CHK_OWNER_TYPE" CHECK ("owner_type" = 'user')
);

CREATE TABLE "vault_access"
(
	"vault_id"       VARCHAR(255) NOT NULL,
	"authority_id"   VARCHAR(255) NOT NULL,
	"authority_type" VARCHAR(5) NOT NULL,
	CONSTRAINT "VAULT_ACCESS_PK" PRIMARY KEY ("vault_id", "authority_id", "authority_type"),
	CONSTRAINT "VAULT_ACCESS_FK_VAULT" FOREIGN KEY ("vault_id") REFERENCES "vault" ("id") ON DELETE CASCADE,
	CONSTRAINT "VAULT_ACCESS_FK_AUTHORITY" FOREIGN KEY ("authority_id", "authority_type") REFERENCES "authority" ("id", "type") ON DELETE CASCADE
);

CREATE TABLE "device"
(
	"id"        VARCHAR(255) NOT NULL,
	"user_id"   VARCHAR(255) NOT NULL,
	"user_type" VARCHAR(5) NOT NULL,
	"name"      VARCHAR(255) NOT NULL,
	"publickey" VARCHAR(255) NOT NULL,
	CONSTRAINT "DEVICE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "DEVICE_FK_USER" FOREIGN KEY ("user_id", "user_type") REFERENCES "authority" ("id", "type"),
	CONSTRAINT "DEVICE_UNIQUE_NAME_PER_USER" UNIQUE ("user_id", "user_type", "name"),
	CONSTRAINT "DEVICE_CHK_USER_TYPE" CHECK ("user_type" = 'user')
);

CREATE TABLE "access"
(
	"device_id" VARCHAR(255) NOT NULL,
	"user_id"   VARCHAR(255) NOT NULL,
	"user_type" VARCHAR(5) NOT NULL,
	"vault_id"  VARCHAR(255) NOT NULL,
	"jwe"       VARCHAR(2000) NOT NULL UNIQUE,
	CONSTRAINT "ACCESS_PK" PRIMARY KEY ("device_id", "user_id", "vault_id"),
	CONSTRAINT "ACCESS_FK_DEVICE" FOREIGN KEY ("device_id") REFERENCES "device" ("id") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_FK_VAULT_ACCESS" FOREIGN KEY ("vault_id", "user_id", "user_type") REFERENCES "vault_access" ("vault_id", "authority_id", "authority_type") ON DELETE CASCADE,
	CONSTRAINT "ACCESS_CHK_USER_TYPE" CHECK ("user_type" = 'user')
);