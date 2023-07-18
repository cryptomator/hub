-- Delete all audit events so that we don't have to deal with migration of old events
-- This is fine because audit logging hasn't been introduced in a stable release yet
TRUNCATE "audit_event" CASCADE;
ALTER SEQUENCE "audit_event_id_seq" RESTART;

-- In the following, we will recreate audit event tables with these goals in mind:
-- - The tables are named using the pattern "audit_event_<entity>_<action>".
-- - The "user_id" column is renamed to "<action_verb_past_participle>_by".

-- "vault_name" and "vault_description" are new columns
DROP TABLE "create_vault_event";
CREATE TABLE "audit_event_vault_create"
(
	"id"                BIGINT NOT NULL,
	"created_by"        VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"          UUID NOT NULL,
	"vault_name"        VARCHAR(255) NOT NULL,
	"vault_description" VARCHAR(255),
	CONSTRAINT "AUDIT_EVENT_VAULT_CREATE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_CREATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

DROP TABLE "unlock_vault_event";
CREATE TABLE "audit_event_vault_unlock"
(
	"id"          BIGINT NOT NULL,
	"unlocked_by" VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"    UUID NOT NULL,
	"device_id"   VARCHAR(64) COLLATE "C" NOT NULL,
	"result"      VARCHAR(50) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_UNLOCK_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_UNLOCK_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

-- Split up update vault membership event into add and remove events
DROP TABLE "update_vault_membership_event";
CREATE TABLE "audit_event_vault_member_add"
(
	"id"           BIGINT NOT NULL,
	"added_by"     VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_ADD_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_ADD_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
CREATE TABLE "audit_event_vault_member_remove"
(
	"id"           BIGINT NOT NULL,
	"removed_by"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"     UUID NOT NULL,
	"authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_REMOVE_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_VAULT_MEMBER_REMOVE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

-- Create new events
CREATE TABLE "audit_event_device_register"
(
    "id"            BIGINT NOT NULL,
    "registered_by" VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id"     VARCHAR(64) COLLATE "C" NOT NULL,
    "device_name"   VARCHAR(255) NOT NULL,
    "device_type"   VARCHAR(50) NOT NULL,
    CONSTRAINT "AUDIT_EVENT_DEVICE_REGISTER_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_DEVICE_REGISTER_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_device_remove"
(
    "id"         BIGINT NOT NULL,
    "removed_by" VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id"  VARCHAR(64) COLLATE "C" NOT NULL,
    CONSTRAINT "AUDIT_EVENT_DEVICE_REMOVE_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_DEVICE_REMOVE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_update"
(
    "id"                BIGINT NOT NULL,
    "updated_by"        VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"          UUID NOT NULL,
    "vault_name"        VARCHAR(255) NOT NULL,
    "vault_description" VARCHAR(255),
    "vault_archived"    BOOLEAN NOT NULL,
    CONSTRAINT "AUDIT_EVENT_VAULT_UPDATE_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_VAULT_UPDATE_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_vault_access_grant"
(
    "id"           BIGINT NOT NULL,
    "granted_by"   VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"     UUID NOT NULL,
    "authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
    CONSTRAINT "AUDIT_EVENT_VAULT_ACCESS_GRANT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "AUDIT_EVENT_VAULT_ACCESS_GRANT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
