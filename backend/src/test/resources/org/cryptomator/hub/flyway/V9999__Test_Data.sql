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

INSERT INTO "user_details" ("id", "publickey", "privatekey", "setupcode")
VALUES
	('user1', 'public1', 'private1', 'setup1'),
	('user2', NULL, NULL, NULL);

INSERT INTO "group_details" ("id")
VALUES
	('group1'),
	('group2');

INSERT INTO "group_membership" ("group_id", "member_id")
VALUES
	('group1', 'user1'),
	('group2', 'user2');

INSERT INTO "vault" ("id", "name", "description", "creation_time", "salt", "iterations", "masterkey", "auth_pubkey", "auth_prvkey", "archived" , "metadata" )
VALUES
	('7E57C0DE-0000-4000-8000-000100001111', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 42, 'masterkey1',
	 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAElS+JW3VaBvVr9GKZGn1399WDTd61Q9fwQMmZuBGAYPdl/rWk705QY6WhlmbokmEVva/mEHSoNQ98wFm9FBCqzh45IGd/DGwZ04Xhi5ah+1bKbkVhtds8nZtHRdSJokYp',
	 'MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDAa57e0Q/KAqmIVOVcWX7b+Sm5YVNRUx8W7nc4wk1IBj2QJmsj+MeShQRHG4ozTE9KhZANiAASVL4lbdVoG9Wv0YpkafXf31YNN3rVD1/BAyZm4EYBg92X+taTvTlBjpaGWZuiSYRW9r+YQdKg1D3zAWb0UEKrOHjkgZ38MbBnTheGLlqH7VspuRWG12zydm0dF1ImiRik=',
	 FALSE, 'm1'
	 ),
	('7E57C0DE-0000-4000-8000-000100002222', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 42, 'masterkey2',
	 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEC1uWSXj2czCDwMTLWV5BFmwxdM6PX9p+Pk9Yf9rIf374m5XP1U8q79dBhLSIuaojsvOT39UUcPJROSD1FqYLued0rXiooIii1D3jaW6pmGVJFhodzC31cy5sfOYotrzF',
	 'MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU=',
	 FALSE, 'm2'
	 ),
	('7E57C0DE-0000-4000-8000-00010000AAAA', 'Vault Archived', 'This is a archived vault.', '2020-02-20 20:20:20', 'salt3', 42, 'masterkey3',
	 'MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEC1uWSXj2czCDwMTLWV5BFmwxdM6PX9p+Pk9Yf9rIf374m5XP1U8q79dBhLSIuaojsvOT39UUcPJROSD1FqYLued0rXiooIii1D3jaW6pmGVJFhodzC31cy5sfOYotrzF',
	 'MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCAHpFQ62QnGCEvYh/pE9QmR1C9aLcDItRbslbmhen/h1tt8AyMhskeenT+rAyyPhGhZANiAAQLW5ZJePZzMIPAxMtZXkEWbDF0zo9f2n4+T1h/2sh/fviblc/VTyrv10GEtIi5qiOy85Pf1RRw8lE5IPUWpgu553SteKigiKLUPeNpbqmYZUkWGh3MLfVzLmx85ii2vMU=',
	 TRUE, 'm3'
	 );

INSERT INTO "vault_access" ("vault_id", "authority_id", "role")
VALUES
	('7E57C0DE-0000-4000-8000-000100001111', 'user1', 'OWNER'),
	('7E57C0DE-0000-4000-8000-000100001111', 'user2', 'MEMBER'),
	('7E57C0DE-0000-4000-8000-00010000AAAA', 'user1', 'OWNER'),
	('7E57C0DE-0000-4000-8000-000100002222', 'group2', 'OWNER'),
	('7E57C0DE-0000-4000-8000-000100002222', 'group1', 'MEMBER');

INSERT INTO "device" ("id", "owner_id", "name", "type", "publickey", "creation_time", "user_privatekey")
VALUES
	('device1', 'user1', 'Computer 1', 'DESKTOP', 'publickey1', '2020-02-20 20:20:20', 'jwe.jwe.jwe.user1.device1'),
	('device2', 'user2', 'Computer 2', 'DESKTOP', 'publickey2', '2020-02-20 20:20:20', 'jwe.jwe.jwe.user2.device2'),
	('device3', 'user1', 'Computer 3', 'DESKTOP', 'publickey3', '2020-02-20 20:20:20', 'jwe.jwe.jwe.user1.device3');

INSERT INTO "access_token" ("user_id", "vault_id", "vault_masterkey")
VALUES
	('user1', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user1'), -- direct access
	('user2', '7E57C0DE-0000-4000-8000-000100001111', 'jwe.jwe.jwe.vault1.user2'), -- direct access
	('user1', '7E57C0DE-0000-4000-8000-000100002222', 'jwe.jwe.jwe.vault2.user1'); -- access via group1

-- DEPRECATED:
INSERT INTO "device_legacy" ("id", "owner_id", "name", "type", "publickey", "creation_time")
VALUES
	('legacyDevice1', 'user1', 'Computer 1', 'DESKTOP', 'publickey1', '2020-02-20 20:20:20'),
	('legacyDevice2', 'user2', 'Computer 2', 'DESKTOP', 'publickey2', '2020-02-20 20:20:20'),
	('legacyDevice3', 'user1', 'Computer 3', 'DESKTOP', 'publickey3', '2020-02-20 20:20:20');

INSERT INTO "access_token_legacy" ("device_id", "vault_id", "jwe")
VALUES
	('legacyDevice1', '7E57C0DE-0000-4000-8000-000100001111', 'legacy.jwe.jwe.vault1.device1'), -- direct access
	('legacyDevice2', '7E57C0DE-0000-4000-8000-000100001111', 'legacy.jwe.jwe.vault1.device2'), -- direct access
	('legacyDevice3', '7E57C0DE-0000-4000-8000-000100002222', 'legacy.jwe.jwe.vault2.device3'); -- access via group1

INSERT INTO "audit_event" ("id", "timestamp", "type")
VALUES
    (10, '2020-02-20T20:20:20.010Z', 'VAULT_CREATE'),
    (11, '2020-02-20T20:20:20.011Z', 'VAULT_MEMBER_ADD'),
    (12, '2020-02-20T20:20:20.012Z', 'VAULT_MEMBER_ADD'),
    (20, '2020-02-20T20:20:20.020Z', 'VAULT_CREATE'),
    (21, '2020-02-20T20:20:20.021Z', 'VAULT_MEMBER_ADD'),
    (22, '2020-02-20T20:20:20.022Z', 'VAULT_MEMBER_ADD'),
    (23, '2020-02-20T20:20:20.023Z', 'VAULT_MEMBER_REMOVE'),
    (24, '2020-02-20T20:20:20.024Z', 'VAULT_MEMBER_UPDATE'),
    (25, '2020-02-20T20:20:20.025Z', 'VAULT_MEMBER_UPDATE'),
    (30, '2020-02-20T20:20:20.030Z', 'VAULT_CREATE'),
    (31, '2020-02-20T20:20:20.031Z', 'VAULT_MEMBER_ADD'),
    (100, '2020-02-20T20:20:20.100Z', 'DEVICE_REGISTER'),
    (101, '2020-02-20T20:20:20.101Z', 'DEVICE_REGISTER'),
    (102, '2020-02-20T20:20:20.102Z', 'DEVICE_REGISTER'),
    (200, '2020-02-20T20:20:20.200Z', 'DEVICE_REGISTER'),
    (201, '2020-02-20T20:20:20.201Z', 'DEVICE_REMOVE'),
    (1111, '2020-02-20T20:20:21.111Z', 'VAULT_KEY_RETRIEVE'),
    (2000, '2020-02-20T20:20:22.000Z', 'VAULT_ACCESS_GRANT'),
    (2001, '2020-02-20T20:20:22.001Z', 'VAULT_ACCESS_GRANT'),
    (2002, '2020-02-20T20:20:22.002Z', 'VAULT_ACCESS_GRANT'),
    (2003, '2020-02-20T20:20:22.003Z', 'VAULT_ACCESS_GRANT'),
    (3000, '2020-02-20T20:20:23.000Z', 'VAULT_UPDATE'),
    (4242, '2020-02-20T20:20:24.242Z', 'VAULT_KEY_RETRIEVE');

SELECT SETVAL('audit_event_id_seq', (SELECT MAX(id) FROM audit_event), true);

INSERT INTO "audit_event_vault_create" ("id", "created_by", "vault_id", "vault_name", "vault_description")
VALUES
    (10, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'Vault 1', 'This is a testvault.'),
    (20, 'user1', '7E57C0DE-0000-4000-8000-000100002222', 'Vault 2', 'This is a testvault.'),
    (30, 'user2', '7E57C0DE-0000-4000-8000-00010000AAAA', 'Vault 3', 'This is a testvault.');

INSERT INTO "audit_event_vault_member_add" ("id", "added_by", "vault_id", "authority_id", "role")
VALUES
    (11, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user1', 'OWNER'),
    (12, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user2', 'MEMBER'),
    (21, 'user1', '7E57C0DE-0000-4000-8000-000100002222', 'user1', 'MEMBER'),
    (22, 'user1', '7E57C0DE-0000-4000-8000-000100002222', 'group1', 'MEMBER'),
    (31, 'user2', '7E57C0DE-0000-4000-8000-00010000AAAA', 'user1', 'MEMBER');

INSERT INTO "audit_event_vault_member_remove" ("id", "removed_by", "vault_id", "authority_id")
VALUES
    (23, 'user1', '7E57C0DE-0000-4000-8000-000100002222', 'user1');

INSERT INTO "audit_event_vault_member_update" ("id", "updated_by", "vault_id", "authority_id", "role")
VALUES
    (24, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user2', 'OWNER'),
    (25, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user2', 'MEMBER');

INSERT INTO "audit_event_device_register" ("id", "registered_by", "device_id", "device_name", "device_type")
VALUES
    (100, 'user1', 'device1', 'Computer 1', 'DESKTOP'),
    (101, 'user2', 'device2', 'Computer 2', 'DESKTOP'),
    (102, 'user1', 'device3', 'Computer 3', 'DESKTOP'),
    (200, 'user2', 'device4', 'Computer 4', 'DESKTOP');

INSERT INTO "audit_event_device_remove" ("id", "removed_by", "device_id")
VALUES
    (201, 'user2', 'device4');

INSERT INTO "audit_event_vault_key_retrieve" ("id", "retrieved_by", "vault_id", "result")
VALUES
    (1111, 'user2', '7E57C0DE-0000-4000-8000-000100001111', 'UNAUTHORIZED'),
    (4242, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'SUCCESS');

INSERT INTO "audit_event_vault_access_grant" ("id", "granted_by", "vault_id", "authority_id")
VALUES
    (2000, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user1'),
    (2001, 'user1', '7E57C0DE-0000-4000-8000-000100001111', 'user2'),
    (2002, 'user1', '7E57C0DE-0000-4000-8000-00010000AAAA', 'user1'),
    (2003, 'user1', '7E57C0DE-0000-4000-8000-000100002222', 'group1');

INSERT INTO "audit_event_vault_update" ("id", "updated_by", "vault_id", "vault_name", "vault_description", "vault_archived")
VALUES
    (3000, 'user1', '7E57C0DE-0000-4000-8000-00010000AAAA', 'Vault Archived', 'This is a archived vault.', TRUE);
