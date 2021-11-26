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
    ('vault1', 'user2');

INSERT INTO device (id, user_id, name, publickey)
VALUES
    ('device1', 'user1', 'Computer 1', 'publickey1'),
    ('device2', 'user2', 'Computer 2', 'publickey2');

INSERT INTO access (device_id, user_id, vault_id, device_specific_masterkey, ephemeral_public_key)
VALUES
    ('device1', 'user1', 'vault1', 'dsm1', 'epk1'),
    ('device2', 'user2', 'vault1', 'dsm2', 'epk2');