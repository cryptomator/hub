-- Delete all audit events so that we don't have to deal with migration of old events
-- This is fine because audit logging hasn't been introduced in a stable release yet
TRUNCATE "audit_event" CASCADE;
ALTER SEQUENCE "audit_event_id_seq" RESTART;

ALTER TABLE "create_vault_event"
ADD COLUMN "vault_name" VARCHAR(255) NOT NULL,
ADD COLUMN "vault_description" VARCHAR(255);

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
