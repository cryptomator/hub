ALTER TABLE vault ADD uvf_metadata_file VARCHAR UNIQUE; -- vault.uvf file, encrypted as JWE
ALTER TABLE vault ADD uvf_jwks VARCHAR UNIQUE; -- encoded as JWKs