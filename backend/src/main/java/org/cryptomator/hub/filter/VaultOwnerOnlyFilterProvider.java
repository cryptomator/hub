package org.cryptomator.hub.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
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
@VaultOwnerOnlyFilter
public class VaultOwnerOnlyFilterProvider implements ContainerRequestFilter {

	static final String CLIENT_JWT = "Client-JWT";
	static final String VAULT_ID = "vaultId";

	@Override
	public void filter(ContainerRequestContext containerRequestContext) {
		var vaultIdQueryParameter = getVaultIdQueryParameter(containerRequestContext);
		var clientJwt = getClientJwt(containerRequestContext);
		var unveridifedVaultId = getUnverifiedVaultId(clientJwt);
		if (vaultIdQueryParameter.equals(unveridifedVaultId)) {
			var vault = Vault.<Vault>findByIdOptional(unveridifedVaultId).orElseThrow(NotFoundException::new);
			var algorithm = Algorithm.ECDSA384(decodePublicKey(vault.authenticationPublicKey), null);
			try {
				JWT.require(algorithm).build().verify(clientJwt);
			} catch (TokenExpiredException e) {
				throw new VaultOwnerTokenExpiredException("Token of Client-JWT expired");
			} catch (JWTVerificationException e) {
				throw new VaultOwnerValidationFailedException("Different key used to sign the Client-JWT");
			}
		} else {
			throw new VaultOwnerValidationFailedException("Other vaultId provided");
		}
	}

	String getVaultIdQueryParameter(ContainerRequestContext containerRequestContext) {
		var vauldIdQueryParameters = containerRequestContext.getUriInfo().getPathParameters().get(VAULT_ID);
		if (vauldIdQueryParameters == null || vauldIdQueryParameters.size() != 1) {
			throw new VaultOwnerValidationFailedException("VaultId not provided");
		}
		return vauldIdQueryParameters.get(0);
	}

	String getClientJwt(ContainerRequestContext containerRequestContext) {
		var clientJwt = containerRequestContext.getHeaderString(CLIENT_JWT);
		if (clientJwt != null) {
			return clientJwt;
		} else {
			throw new VaultOwnerNotProvidedException("Client-JWT not provided");
		}
	}

	String getUnverifiedVaultId(String clientJwt) {
		try {
			var unveridifedVaultId = JWT.decode(clientJwt).getHeaderClaim("vaultId");
			if (!unveridifedVaultId.isNull() && unveridifedVaultId.asString() != null) {
				return unveridifedVaultId.asString();
			} else {
				throw new VaultOwnerValidationFailedException("No Client-JWT provided");
			}
		} catch (JWTDecodeException e) {
			throw new VaultOwnerValidationFailedException("Malformed Client-JWT provided");
		}
	}

	private static ECPublicKey decodePublicKey(String pemEncodedPublicKey) {
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
			throw new VaultOwnerValidationFailedException("Wrong key provided", e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
