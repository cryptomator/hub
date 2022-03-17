-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO grp (id, name)
VALUES
    ('group1', 'Group Name 1'),
    ('group2', 'Group Name 2'),
    ('group3', 'Group Name 3');

INSERT INTO user (id, name)
VALUES
    ('user1', 'User Name 1'),
    ('user2', 'User Name 2'),
    ('user3', 'User Name 3');

INSERT INTO group_user (group_id, user_id)
VALUES
    ('group1', 'user1'),
    ('group1', 'user3'),
    ('group2', 'user3'),
    ('group3', 'user3'),
    ('group3', 'user1');

INSERT INTO vault (id, user_id, name, description, creation_time, salt, iterations, masterkey)
VALUES
    ('vault1', 'user1', 'Vault 1', 'This is a testvault.', '2020-02-20 20:20:20', 'salt1', 'iterations1', 'masterkey1'),
    ('vault2', 'user2', 'Vault 2', 'This is a testvault.', '2020-02-20 20:20:20', 'salt2', 'iterations2', 'masterkey2'),
    ('vault3', 'user1', 'Vault 3', 'This is a testvault.', '2020-02-20 20:20:20', 'salt3', 'iterations3', 'masterkey3'),
    ('vault4', 'user1', 'Vault 4', 'This is a testvault.', '2020-02-20 20:20:20', 'salt4', 'iterations4', 'masterkey4');

INSERT INTO vault_user (vault_id, user_id)
VALUES
    ('vault1', 'user1'),
    ('vault1', 'user2'),
    ('vault2', 'user1');

INSERT INTO vault_group (vault_id, group_id)
VALUES
    ('vault3', 'group1'),
    ('vault2', 'group2'),
    ('vault3', 'group3');

INSERT INTO Device (id, user_id, name, publickey)
VALUES
    ('device1', 'user1', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'Computer 2', 'publickey2'),
    ('device3', 'user1', 'Computer 3', 'publickey3'),
    ('device4', 'user1', 'Computer 4', 'publickey4'),
    ('device5', 'user3', 'Computer 5', 'publickey5'),
    ('device6', 'user1', 'Computer 6', 'publickey6');

INSERT INTO Access (device_id, user_id, vault_id, jwe)
VALUES
    ('device1', 'user1', 'vault1', 'jwe1'),
    ('device2', 'user2', 'vault1', 'jwe2'),
    ('device1', 'user1', 'vault2', 'jwe3'),
    ('device5', 'user3', 'vault3', 'jwe4'),
    ('device6', 'user1', 'vault3', 'jwe5');
