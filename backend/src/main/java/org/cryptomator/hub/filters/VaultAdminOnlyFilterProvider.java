package org.cryptomator.hub.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
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
import java.util.Base64;

@Provider
@VaultAdminOnlyFilter
public class VaultAdminOnlyFilterProvider implements ContainerRequestFilter {

	static final String CLIENT_JWT = "Client-JWT";
	static final String VAULT_ID = "vaultId";

	@Override
	public void filter(ContainerRequestContext containerRequestContext) {
		var vaultIdQueryParameter = getVaultIdQueryParameter(containerRequestContext);
		var clientJWT = getClientJWT(containerRequestContext);
		var unveridifedVaultId = getUnverifiedVaultId(clientJWT);
		if (vaultIdQueryParameter.equals(unveridifedVaultId)) {
			var vault = Vault.<Vault>findByIdOptional(unveridifedVaultId).orElseThrow(NotFoundException::new);
			var algorithm = Algorithm.ECDSA384(decodePublicKey(vault.authenticationPublicKey), null);
			verify(buildVerification(algorithm), clientJWT);
		} else {
			throw new VaultAdminValidationFailedException("Other vaultId provided");
		}
	}

	//visible for testing
	void verify(JWTVerifier verifier, DecodedJWT clientJWT) {
		try {
			verifier.verify(clientJWT);
		} catch (TokenExpiredException e) {
			throw new VaultAdminTokenExpiredException("Token of Client-JWT expired");
		} catch (IncorrectClaimException e) {
			if (e.getClaimName().equals(RegisteredClaims.ISSUED_AT) || e.getClaimName().equals(RegisteredClaims.NOT_BEFORE)) {
				throw new VaultAdminTokenNotYetValidException("Token of Client-JWT not yet valid");
			} else {
				throw new VaultAdminValidationFailedException("Incorrect claim exception");
			}
		} catch (JWTVerificationException e) {
			throw new VaultAdminValidationFailedException("Different key used to sign the Client-JWT");
		}
	}

	//visible for testing
	JWTVerifier buildVerification(Algorithm algorithm) {
		return buildVerifier(algorithm).build();
	}

	//visible for testing
	Verification buildVerifier(Algorithm algorithm) {
		return JWT.require(algorithm);
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
	DecodedJWT getClientJWT(ContainerRequestContext containerRequestContext) {
		var clientJwt = containerRequestContext.getHeaderString(CLIENT_JWT);
		if (clientJwt != null) {
			try {
				var jwt = JWT.decode(clientJwt);
				if (jwt.getIssuedAt() != null && jwt.getNotBefore() != null && jwt.getExpiresAt() != null) {
					return jwt;
				} else {
					throw new VaultAdminValidationFailedException("No dates provided");
				}
			} catch (JWTDecodeException e) {
				throw new VaultAdminValidationFailedException("Malformed Client-JWT provided");
			}
		} else {
			throw new VaultAdminNotProvidedException("Client-JWT not provided");
		}
	}

	//visible for testing
	String getUnverifiedVaultId(DecodedJWT clientJWT) {
		var unveridifedVaultId = clientJWT.getHeaderClaim("vaultId");
		if (!unveridifedVaultId.isNull() && unveridifedVaultId.asString() != null) {
			return unveridifedVaultId.asString();
		} else {
			throw new VaultAdminValidationFailedException("No Client-JWT provided");
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
