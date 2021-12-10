-- noinspection SqlNoDataSourceInspectionForFile

INSERT INTO user (id, name)
VALUES
    ('user1', 'User Name 1'),
    ('user2', 'User Name 2');

INSERT INTO vault (id, user_id, name, salt, iterations, masterkey)
VALUES
    ('vault1', 'user1', 'Vault 1', 'salt1', 'iterations1', 'masterkey1'),
    ('vault2', 'user2', 'Vault 2', 'salt2', 'iterations2', 'masterkey2');

INSERT INTO vault_user (vault_id, user_id)
VALUES
    ('vault1', 'user1'),
    ('vault1', 'user2'),
    ('vault2', 'user1');

INSERT INTO device (id, user_id, name, publickey)
VALUES
    ('device1', 'user1', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'Computer 2', 'publickey2'),
    ('device3', 'user1', 'Computer 3', 'publickey3');

INSERT INTO access (device_id, user_id, vault_id, jwe)
VALUES
    ('device1', 'user1', 'vault1', 'jwe1'),
    ('device2', 'user2', 'vault1', 'jwe2'),
    ('device1', 'user1', 'vault2', 'jwe3');
