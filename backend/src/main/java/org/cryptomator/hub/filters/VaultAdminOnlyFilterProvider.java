package org.cryptomator.hub.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;
import org.cryptomator.hub.entities.Vault;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Provider
@VaultAdminOnlyFilter
public class VaultAdminOnlyFilterProvider implements ContainerRequestFilter {

	public static final String VAULT_ADMIN_AUTHORIZATION = "Cryptomator-Vault-Admin-Authorization";
	static final String VAULT_ID = "vaultId";
	static final int REQUEST_LEEWAY_IN_SECONDS = 15;

	@Override
	public void filter(ContainerRequestContext containerRequestContext) {
		var vaultIdQueryParameter = getVaultIdQueryParameter(containerRequestContext);
		var vaultAdminAuthorizationJWT = getUnverifiedvaultAdminAuthorizationJWT(containerRequestContext);
		var unveridifedVaultId = getUnverifiedVaultId(vaultAdminAuthorizationJWT);
		if (vaultIdQueryParameter.equals(unveridifedVaultId)) {
			var vault = Vault.<Vault>findByIdOptional(UUID.fromString(unveridifedVaultId)).orElseThrow(NotFoundException::new);
			var algorithm = Algorithm.ECDSA384(decodePublicKey(vault.authenticationPublicKey));
			verify(buildVerifier(algorithm), vaultAdminAuthorizationJWT);
		} else {
			throw new VaultAdminValidationFailedException("Other vaultId provided");
		}
	}

	//visible for testing
	void verify(JWTVerifier verifier, DecodedJWT vaultAdminAuthorizationJWT) {
		try {
			verifier.verify(vaultAdminAuthorizationJWT);
		} catch (IncorrectClaimException e) {
			if (e.getClaimName().equals(RegisteredClaims.ISSUED_AT)) {
				throw new VaultAdminTokenIAPNotValidException("ISSUED_AT claim of VaultAdminAuthorizationJWT not provided or not yet or no longer valid");
			} else {
				throw new VaultAdminValidationFailedException("Incorrect claim exception");
			}
		} catch (JWTVerificationException e) {
			throw new VaultAdminValidationFailedException("Different key used to sign the VaultAdminAuthorizationJWT");
		}
	}

	//visible for testing
	JWTVerifier buildVerifier(Algorithm algorithm) {
		return verification(algorithm).build();
	}

	private Verification verification(Algorithm algorithm) {
		return verification(algorithm, Instant.now());
	}

	//visible for testing
	Verification verification(Algorithm algorithm, Instant now) {
		return JWT.require(algorithm) //
				.withClaim(RegisteredClaims.ISSUED_AT, (claim, jwt) -> jwt.getIssuedAt() != null //
						&& !jwt.getIssuedAt().before(Date.from(now.minus(REQUEST_LEEWAY_IN_SECONDS, ChronoUnit.SECONDS))) //
						&& !jwt.getIssuedAt().after(Date.from(now.plus(REQUEST_LEEWAY_IN_SECONDS, ChronoUnit.SECONDS)))) //
				.ignoreIssuedAt();
	}

	//visible for testing
	String getVaultIdQueryParameter(ContainerRequestContext containerRequestContext) {
		var vauldIdQueryParameters = containerRequestContext.getUriInfo().getPathParameters().get(VAULT_ID);
		if (vauldIdQueryParameters == null || vauldIdQueryParameters.size() != 1) {
			throw new VaultAdminValidationFailedException("VaultId not provided");
		}
		return vauldIdQueryParameters.get(0);
	}

	//visible for testing
	DecodedJWT getUnverifiedvaultAdminAuthorizationJWT(ContainerRequestContext containerRequestContext) {
		var clientJwt = containerRequestContext.getHeaderString(VAULT_ADMIN_AUTHORIZATION);
		if (clientJwt != null) {
			try {
				return JWT.decode(clientJwt);
			} catch (JWTDecodeException e) {
				throw new VaultAdminValidationFailedException("Malformed VaultAdminAuthorizationJWT provided");
			}
		} else {
			throw new VaultAdminNotProvidedException("VaultAdminAuthorizationJWT not provided");
		}
	}

	//visible for testing
	String getUnverifiedVaultId(DecodedJWT vaultAdminAuthorizationJWT) {
		var unveridifedVaultId = vaultAdminAuthorizationJWT.getHeaderClaim(VAULT_ID);
		if (!unveridifedVaultId.isNull() && unveridifedVaultId.asString() != null) {
			return unveridifedVaultId.asString();
		} else {
			throw new VaultAdminValidationFailedException("No VaultAdminAuthorizationJWT provided");
		}
	}

	//visible for testing
	static ECPublicKey decodePublicKey(String pemEncodedPublicKey) {
		try {
			var publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pemEncodedPublicKey));

			var keyFactory = KeyFactory.getInstance("EC");
			var key = keyFactory.generatePublic(publicKeySpec);

			if (key instanceof ECPublicKey k) {
				return k;
			} else {
				throw new IllegalStateException("Key not an EC public key.");
			}
		} catch (InvalidKeySpecException e) {
			throw new VaultAdminValidationFailedException("Wrong key provided", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
