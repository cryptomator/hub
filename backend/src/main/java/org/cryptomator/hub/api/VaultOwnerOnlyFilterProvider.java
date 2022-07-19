package org.cryptomator.hub.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.cryptomator.hub.entities.Vault;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Provider
@VaultOwnerOnlyFilter
public class VaultOwnerOnlyFilterProvider implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext containerRequestContext) throws IOException {
		var clientJwt = containerRequestContext.getHeaderString("Client-Jwt");
		if (clientJwt != null) {
			var unveridifedVaultId = JWT.decode(clientJwt).getHeaderClaim("vaultId");
			if (!unveridifedVaultId.isNull()) {
				var vault = Vault.<Vault>findByIdOptional(unveridifedVaultId.asString()).orElseThrow(NotFoundException::new);
				var algorithm = Algorithm.ECDSA384(decodePublicKey(vault.authenticationPublicKey), null);
				try {
					JWT.require(algorithm).build().verify(clientJwt);
				} catch (TokenExpiredException e) {
					// TODO choose other HTTP status code do differenticate between no Client-Jwt provided and expired?
					throw new VaultOwnerTokenExpiredException("Token of client-jwt expired");
				} catch (JWTVerificationException e) {
					throw new VaultOwnerValidationFailedException("Different key used to sign the client-jwt");
				}
			} else {
				throw new VaultOwnerValidationFailedException("vaultId not provided");
			}
		} else {
			throw new VaultOwnerNotProvidedException("client-jwt not provided");
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
