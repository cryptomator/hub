-- noinspection SqlNoDataSourceInspectionForFile

UPDATE "billing"
SET "hub_id" = '42', "token" = 'eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm'
WHERE "id" = 0;

INSERT INTO "authority" ("id", "type", "name")
VALUES
    ('user1', 'user', 'User Name 1'),
    ('user2', 'user', 'User Name 2');

INSERT INTO "vault" ("id", "owner_id", "owner_type", "name", "description", "creation_time", "salt", "iterations", "masterkey")
VALUES
    ('vault1', 'user1', 'user', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 'iterations1', 'masterkey1'),
    ('vault2', 'user2', 'user', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 'iterations2', 'masterkey2');

INSERT INTO "vault_access" ("vault_id", "authority_id", "authority_type")
VALUES
    ('vault1', 'user1', 'user'),
    ('vault1', 'user2', 'user'),
    ('vault2', 'user1', 'user');

INSERT INTO "device" ("id", "owner_id", "owner_type", "name", "publickey")
VALUES
    ('device1', 'user1', 'user', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'user', 'Computer 2', 'publickey2'),
    ('device3', 'user1', 'user', 'Computer 3', 'publickey3');

INSERT INTO "access_token" ("device_id", "user_id", "user_type", "vault_id", "jwe")
VALUES
    ('device1', 'user1', 'user', 'vault1', 'jwe1'),
    ('device2', 'user2', 'user', 'vault1', 'jwe2'),
    ('device1', 'user1', 'user', 'vault2', 'jwe3');
