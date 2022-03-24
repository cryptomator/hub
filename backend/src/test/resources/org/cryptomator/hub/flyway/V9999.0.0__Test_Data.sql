-- noinspection SqlNoDataSourceInspectionForFile

UPDATE "billing"
SET "hub_id" = '42', "token" = 'eyJhbGciOiJFUzUxMiJ9.eyJqdGkiOiI0MiIsImlhdCI6MTY0ODA0OTM2MCwiaXNzIjoiU2t5bWF0aWMiLCJhdWQiOiJDcnlwdG9tYXRvciBIdWIiLCJzdWIiOiJodWJAY3J5cHRvbWF0b3Iub3JnIiwic2VhdHMiOjUsImV4cCI6MjUzNDAyMjE0NDAwLCJyZWZyZXNoVXJsIjoiaHR0cDovL2xvY2FsaG9zdDo4Nzg3L2h1Yi9zdWJzY3JpcHRpb24_aHViX2lkPTQyIn0.AKyoZ0WQ8xhs8vPymWPHCsc6ch6pZpfxBcrF5QjVLSQVnYz2s5QF3nnkwn4AGR7V14TuhkJMZLUZxMdQAYLyL95sAV2Fu0E4-e1v3IVKlNKtze89eqYvEs6Ak9jWjtecOgPWNWjz2itI4MfJBDmbFtTnehOtqRqUdsDoC9NFik2C7tHm'
WHERE "id" = 0;

INSERT INTO "user" ("id", "name")
VALUES
    ('user1', 'User Name 1'),
    ('user2', 'User Name 2');

INSERT INTO "vault" ("id", "user_id", "name", "description", "creation_time", "salt", "iterations", "masterkey")
VALUES
    ('vault1', 'user1', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 'iterations1', 'masterkey1'),
    ('vault2', 'user2', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 'iterations2', 'masterkey2');

INSERT INTO "vault_user" ("vault_id", "user_id")
VALUES
    ('vault1', 'user1'),
    ('vault1', 'user2'),
    ('vault2', 'user1');

INSERT INTO "device" ("id", "user_id", "name", "publickey")
VALUES
    ('device1', 'user1', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'Computer 2', 'publickey2'),
    ('device3', 'user1', 'Computer 3', 'publickey3');

INSERT INTO "access" ("device_id", "user_id", "vault_id", "jwe")
VALUES
    ('device1', 'user1', 'vault1', 'jwe1'),
    ('device2', 'user2', 'vault1', 'jwe2'),
    ('device1', 'user1', 'vault2', 'jwe3');
