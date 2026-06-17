ALTER TABLE "device"
	ADD "last_access_time" TIMESTAMP WITH TIME ZONE,
	ADD "last_ip_address" VARCHAR(46);

-- backfill from the most recent vault key retrieval per device
UPDATE "device" SET
	"last_access_time" = s."timestamp",
	"last_ip_address" = s."ip_address"
FROM (
	SELECT DISTINCT ON (v."device_id")
		v."device_id", a."timestamp", v."ip_address"
	FROM "audit_event_vault_key_retrieve" v
	JOIN "audit_event" a ON a."id" = v."id"
	WHERE v."device_id" IS NOT NULL
		AND v."result" = 'SUCCESS'
	ORDER BY v."device_id", a."timestamp" DESC, a."id" DESC
) s
WHERE "device"."id" = s."device_id";
