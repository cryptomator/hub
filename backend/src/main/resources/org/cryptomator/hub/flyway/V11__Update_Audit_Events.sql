-- Delete all audit events so that we don't have to deal with migration of old events
-- This is fine because audit logging hasn't been introduced in a stable release yet
TRUNCATE "audit_event" CASCADE;
ALTER SEQUENCE "audit_event_id_seq" RESTART;

ALTER TABLE "create_vault_event"
ADD COLUMN "vault_name" VARCHAR(255) NOT NULL,
ADD COLUMN "vault_description" VARCHAR(255);

CREATE TABLE "grant_vault_access_event"
(
    "id"           BIGINT NOT NULL,
    "user_id"      VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"     UUID NOT NULL,
    "authority_id" VARCHAR(255) COLLATE "C" NOT NULL,
    CONSTRAINT "GRANT_VAULT_ACCESS_EVENT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "GRANT_VAULT_ACCESS_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "register_device_event"
(
    "id"          BIGINT NOT NULL,
    "user_id"     VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id"   VARCHAR(64) COLLATE "C" NOT NULL,
    "device_name" VARCHAR(255) NOT NULL,
    "device_type" VARCHAR(50) NOT NULL,
    CONSTRAINT "REGISTER_DEVICE_EVENT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "REGISTER_DEVICE_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "remove_device_event"
(
    "id"        BIGINT NOT NULL,
    "user_id"   VARCHAR(255) COLLATE "C" NOT NULL,
    "device_id" VARCHAR(64) COLLATE "C" NOT NULL,
    CONSTRAINT "REMOVE_DEVICE_EVENT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "REMOVE_DEVICE_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "update_vault_event"
(
    "id"                BIGINT NOT NULL,
    "user_id"           VARCHAR(255) COLLATE "C" NOT NULL,
    "vault_id"          UUID NOT NULL,
    "vault_name"        VARCHAR(255) NOT NULL,
    "vault_description" VARCHAR(255),
    "vault_archived"    BOOLEAN NOT NULL,
    CONSTRAINT "UPDATE_VAULT_EVENT_PK" PRIMARY KEY ("id"),
    CONSTRAINT "UPDATE_VAULT_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
