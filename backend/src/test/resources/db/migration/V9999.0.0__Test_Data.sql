-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO grp (id, name)
VALUES
    ('group1', 'Group Name 1'),
    ('group2', 'Group Name 2');

INSERT INTO user (id, name)
VALUES
    ('user1', 'User Name 1'),
    ('user2', 'User Name 2');

INSERT INTO group_user (group_id, user_id)
VALUES
    ('group1', 'user1');

INSERT INTO vault (id, user_id, name, salt, iterations, masterkey)
VALUES
    ('vault1', 'user1', 'Vault 1', 'salt1', 'iterations1', 'masterkey1'),
    ('vault2', 'user2', 'Vault 2', 'salt2', 'iterations2', 'masterkey2'),
    ('vault3', 'user1', 'Vault 3', 'salt3', 'iterations3', 'masterkey3');

INSERT INTO vault_user (vault_id, user_id)
VALUES
    ('vault1', 'user1'),
    ('vault1', 'user2'),
    ('vault2', 'user1'); // User2 too

INSERT INTO vault_group (vault_id, group_id)
VALUES
    ('vault3', 'group1');

INSERT INTO device (id, user_id, name, publickey)
VALUES
    ('device1', 'user1', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'Computer 2', 'publickey2'),
    ('device3', 'user1', 'Computer 3', 'publickey3'),
    ('device4', 'user1', 'Computer 3', 'publickey4');

INSERT INTO user_access (device_id, user_id, vault_id, jwe)
VALUES
    ('device1', 'user1', 'vault1', 'jwe1'),
    ('device2', 'user2', 'vault1', 'jwe2'),
    ('device1', 'user1', 'vault2', 'jwe3');

INSERT INTO group_access (device_id, group_id, vault_id, jwe)
VALUES
    ('device1', 'group1', 'vault3', 'jwe1'),
    ('device4', 'group1', 'vault3', 'jwe2');