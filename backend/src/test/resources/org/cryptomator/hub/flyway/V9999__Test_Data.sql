-- noinspection SqlNoDataSourceInspectionForFile

-- This test data can be used in unit tests. A test should leave this data as it found it, either by transaction rollback or other means. See https://github.com/cryptomator/hub/pull/182 for some options

UPDATE "settings"
SET "hub_id" = '42',
	"license_key"  = 'eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm'
WHERE "id" = 0;

INSERT INTO "authority" ("id", "type", "name")
VALUES
	('user1', 'USER', 'User Name 1'),
	('user2', 'USER', 'User Name 2'),
	('group1', 'GROUP', 'Group Name 1'),
    ('group2', 'GROUP', 'Group Name 2');

INSERT INTO "user_details" ("id")
VALUES
	('user1'),
	('user2');

INSERT INTO "group_details" ("id")
VALUES
	('group1'),
	('group2');

INSERT INTO "group_membership" ("group_id", "member_id")
VALUES
	('group1', 'user1'),
	('group2', 'user2');

INSERT INTO "vault" ("id", "name", "description", "creation_time", "salt", "iterations", "masterkey", "auth_pubkey", "auth_prvkey")
VALUES
	('7E57C0DE-0000-4000-8000-000100001111', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 42, 'masterkey1',
	 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAElS+JW3VaBvVr9GKZGn1399WDTd61Q9fwQMmZuBGAYPdl/rWk705QY6WhlmbokmEVva/mEHSoNQ98wFm9FBCqzh45IGd/DGwZ04Xhi5ah+1bKbkVhtds8nZtHRdSJokYp',
	 'MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAa57e0Q/KAqmIVOVcWX7b+Sm5YVNRUx8W7nc4wk1IBj2QJmsj+MeShQRHG4ozTE9KhZANiAASVL4lbdVoG9Wv0YpkafXf31YNN3rVD1/BAyZm4EYBg92X+taTvTlBjpaGWZuiSYRW9r+YQdKg1D3zAWb0UEKrOHjkgZ38MbBnTheGLlqH7VspuRWG12zydm0dF1ImiRik='),
	('7E57C0DE-0000-4000-8000-000100002222', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 42, 'masterkey2',
	 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEC1uWSXj2czCDwMTLWV5BFmwxdM6PX9p+Pk9Yf9rIf374m5XP1U8q79dBhLSIuaojsvOT39UUcPJROSD1FqYLued0rXiooIii1D3jaW6pmGVJFhodzC31cy5sfOYotrzF',
	 'MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU=');

INSERT INTO "vault_access" ("vault_id", "authority_id")
VALUES
	('7E57C0DE-0000-4000-8000-000100001111', 'user1'),
	('7E57C0DE-0000-4000-8000-000100001111', 'user2'),
	('7E57C0DE-0000-4000-8000-000100002222', 'group1');

INSERT INTO "device" ("id", "owner_id", "name", "publickey", "creation_time", "user_key_jwe")
VALUES
	('device1', 'user1', 'Computer 1', 'publickey1', '2020-02-20 20:20:20', 'jwe.jwe.jwe.user1.device1'),
	('device2', 'user2', 'Computer 2', 'publickey2', '2020-02-20 20:20:20', 'jwe.jwe.jwe.user2.device2'),
	('device3', 'user1', 'Computer 3', 'publickey3', '2020-02-20 20:20:20', NULL);

INSERT INTO "access_token" ("user_id", "vault_id", "vault_key_jwe")
VALUES
	('user1', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user1'), -- direct access
	('user2', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user2'), -- direct access
	('user1', '7E57C0DE-0000-4000-8000-000100002222', 'jwe.jwe.jwe.vault2.user1'); -- access via group1

-- DEPRECATED:
INSERT INTO "access_token_legacy" ("device_id", "vault_id", "jwe")
VALUES
	('device1', '7E57C0DE-0000-4000-8000-000100001111', 'legacy.jwe.jwe.vault1.device1'), -- direct access
	('device2', '7E57C0DE-0000-4000-8000-000100001111', 'legacy.jwe.jwe.vault1.device2'), -- direct access
	('device3', '7E57C0DE-0000-4000-8000-000100002222', 'legacy.jwe.jwe.vault2.device3'); -- access via group1