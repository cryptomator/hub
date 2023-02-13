-- Cryptomator Hub 1.1.0

CREATE TABLE "audit_event"
(
	"id"        VARCHAR(255)  NOT NULL,
	"type"      VARCHAR(255) NOT NULL,
	"timestamp" TIMESTAMP  NOT NULL,
	"message"   VARCHAR(2000) NOT NULL,
	CONSTRAINT "AUDIT_EVENT_PK" PRIMARY KEY ("id")
);

CREATE TABLE "unlock_event"
(
	"id"             VARCHAR(255) NOT NULL,
	"user_id"        VARCHAR(255) NOT NULL,
	"vault_id"       VARCHAR(255) NOT NULL,
	"device_id"       VARCHAR(255) NOT NULL,
	CONSTRAINT "UNLOCK_EVENT_PK" PRIMARY KEY ("id"),
	CONSTRAINT "UNLOCK_EVENT_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);
