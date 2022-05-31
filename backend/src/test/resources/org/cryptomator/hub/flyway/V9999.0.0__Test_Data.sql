-- noinspection SqlNoDataSourceInspectionForFile

UPDATE "billing"
SET "hub_id" = '42'
WHERE "id" = 0;

INSERT INTO "authority" ("id", "type", "name")
VALUES
	('user1', 'USER', 'User Name 1'),
	('user2', 'USER', 'User Name 2'),
	('group1', 'GROUP', 'User Name 1');

INSERT INTO "user_details" ("id")
VALUES
	('user1'),
	('user2');

INSERT INTO "group_details" ("id")
VALUES
	('group1');

INSERT INTO "group_membership" ("group_id", "member_id")
VALUES
	('group1', 'user1');

INSERT INTO "vault" ("id", "owner_id", "name", "description", "creation_time", "salt", "iterations", "masterkey")
VALUES
	('vault1', 'user1', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 'iterations1', 'masterkey1'),
	('vault2', 'user2', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 'iterations2', 'masterkey2');

INSERT INTO "vault_access" ("vault_id", "authority_id")
VALUES
	('vault1', 'user1'),
	('vault1', 'user2'),
	('vault2', 'group1'); --user1, part of group1, has access to vault2

INSERT INTO "device" ("id", "owner_id", "name", "publickey")
VALUES
	('device1', 'user1', 'Computer 1', 'publickey1'),
	('device2', 'user2', 'Computer 2', 'publickey2'),
	('device3', 'user1', 'Computer 3', 'publickey3'); --user1 is part of group1

INSERT INTO "access_token" ("device_id", "vault_id", "jwe")
VALUES
	('device1', 'vault1', 'jwe1'),
	('device2', 'vault1', 'jwe2'),
	('device3', 'vault2', 'jwe3'); --device3 of user1, part of group1, has access to vault2
