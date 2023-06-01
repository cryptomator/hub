-- Cryptomator Hub 1.1.0

CREATE SEQUENCE audit_event_id_seq AS BIGINT;
CREATE TABLE "audit_event"
(
	"id"        BIGINT NOT NULL DEFAULT nextval('audit_event_id_seq'),
	"type"      VARCHAR(50) NOT NULL,
	"timestamp" TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT "AUDIT_EVENT_PK" PRIMARY KEY ("id")
);
ALTER SEQUENCE audit_event_id_seq OWNED BY audit_event.id;

CREATE TABLE "create_vault_event"
(
	"id"        BIGINT NOT NULL,
	"user_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"  UUID NOT NULL,
	CONSTRAINT "CREATE_VAULT_EVENT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "CREATE_VAULT_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "unlock_vault_event"
(
	"id"        BIGINT NOT NULL,
	"user_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"  UUID NOT NULL,
	"device_id" VARCHAR(64) COLLATE "C" NOT NULL,
	"result"    VARCHAR(50) NOT NULL,
	CONSTRAINT "UNLOCK_VAULT_EVENT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "UNLOCK_VAULT_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

CREATE TABLE "update_vault_membership_event"
(
	"id"            BIGINT NOT NULL,
	"user_id"       VARCHAR(255) COLLATE "C" NOT NULL,
	"vault_id"      UUID NOT NULL,
	"authority_id"  VARCHAR(255) COLLATE "C" NOT NULL,
	"operation"     VARCHAR(50) NOT NULL,
	CONSTRAINT "UPDATE_VAULT_MEMBERSHIP_EVENT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "UPDATE_VAULT_MEMBERSHIP_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
