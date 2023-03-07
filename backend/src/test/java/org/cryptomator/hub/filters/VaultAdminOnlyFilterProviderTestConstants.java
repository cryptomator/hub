package org.cryptomator.hub.filters;

import java.time.Instant;

final class VaultAdminOnlyFilterProviderTestConstants {

	// { "alg": "ES384", "typ": "JWT", "vaultId": "vault2" } { "iat": 1516239015 (2018-01-18T01:30:15) }
	static final String VALID_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMTV9.QYG_3b8d-Hglp68UAX3-Sn679TCb2v4alS6ruExL_gUr3Nrk1zCV5Gqjr5_h0rTsYL8t-bQh9u7NHkAuxcA5wdFTH0fEc45-2RGsMj0Mz4Cduv7WmOEMh28z-J5QTvlS";

	// { "alg": "ES384", "typ": "JWT", "vaultId": "vault3000" } { "iat": 1516239015 (2018-01-18T01:30:15) }
	static final String VALID_TOKEN_VAULT_3000 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDMwMDAifQ.eyJpYXQiOjE1MTYyMzkwMTV9.GpYk3tflY1K-rdWLT8wFeNAabW_Q0tQCaSe8It6Fr0xqPMxnrhbukONrrNPGSlhjlrmpVtl3DABaiO8921o7hpsaqQQVFvRFmYWpsajOl_2pi7YhJmKCrdyaTswBGAo3";
	static final String INVALID_SIGNATURE_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.cGZDCqzJQgcBHNVPcmBc8JfeGzUf3CHUrwSAMwOA0Dcy9aUZvsAm1dr1MKzuPW_UFHRfMnNi2EwASOA6t-vPWvPFolAHFn5REt2Y9Aw9mIz-qxSBLpz6OMZD16tysQcd";
	static final String MALFORMED_TOKEN = "hello world";
	static final Instant NOW  = Instant.ofEpochSecond(1516239015); // 2018-01-18T01:30:15

}
