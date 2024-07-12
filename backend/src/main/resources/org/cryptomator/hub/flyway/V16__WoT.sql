ALTER TABLE "settings" ADD "wot_max_depth" INTEGER NOT NULL DEFAULT 3;
ALTER TABLE "settings" ADD "wot_id_verify_len" INTEGER NOT NULL DEFAULT 2;
ALTER TABLE "settings" ADD CONSTRAINT "check_wot_max_depth" CHECK ("wot_max_depth" >= 0 AND "wot_max_depth" < 10);

CREATE TABLE "wot" (
    "user_id" VARCHAR(255) COLLATE "C" NOT NULL,
    "signer_id" VARCHAR(255) COLLATE "C" NOT NULL,
    "signature" VARCHAR NOT NULL,
    PRIMARY KEY ("user_id", "signer_id"),
    CONSTRAINT "fk_user_id" FOREIGN KEY ("user_id") REFERENCES "user_details" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_signer_id" FOREIGN KEY ("signer_id") REFERENCES "user_details" ("id") ON DELETE CASCADE
);

CREATE TABLE "audit_event_sign_wot_id"
(
	"id"          BIGINT NOT NULL,
	"user_id"     VARCHAR(255) COLLATE "C" NOT NULL,
	"signer_id"   VARCHAR(255) COLLATE "C" NOT NULL,
	"signer_key"  VARCHAR NOT NULL,
	"signature"   VARCHAR NOT NULL,
	CONSTRAINT "AUDIT_EVENT_SIGN_WOT_ID_PK" PRIMARY KEY ("id"),
	CONSTRAINT "AUDIT_EVENT_SIGN_WOT_ID_FK_AUDIT_EVENT" FOREIGN KEY ("id") REFERENCES "audit_event" ("id") ON DELETE CASCADE
);

-- @formatter:off
CREATE VIEW "effective_wot" ("trusting_user_id", "trusted_user_id", "signature_chain") AS
WITH RECURSIVE "r" ("trusting_user_id", "trusted_user_id", "depth", "signer_chain", "signature_chain") AS (
	-- Anchor member: Directly trusted users
	SELECT "signer_id", "user_id", 0, array["signer_id"]::varchar[], array["signature"]::varchar[]
	FROM "wot"

	UNION ALL

	-- Recursive member: Transitive trust
	SELECT "r"."trusting_user_id", "wot"."user_id", "r"."depth" + 1, ("r"."signer_chain" || "wot"."signer_id")::varchar[], ("r"."signature_chain" || "wot"."signature")::varchar[]
	FROM  "wot"
	INNER JOIN "r"
	    ON "wot"."signer_id" = "r"."trusted_user_id"  -- primary recursion criteria
	    AND "wot"."user_id" <> ALL("r"."signer_chain") -- only if user isn't part of signature chain already (avoid loops)
	INNER JOIN "settings" ON "settings"."id" = 0
	WHERE "r"."depth" < "settings"."wot_max_depth"
)
SELECT
    DISTINCT ON ("trusting_user_id", "trusted_user_id") -- keep only one relation (ordered by depth), i.e. the shortest path
    "trusting_user_id", "trusted_user_id", "signature_chain"
    FROM "r"
    ORDER BY "trusting_user_id", "trusted_user_id", "depth";
-- @formatter:on