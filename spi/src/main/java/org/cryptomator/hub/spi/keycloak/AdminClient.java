package org.cryptomator.hub.spi.keycloak;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AdminClient {

	private static final String GRANT_TYPE_PW = "password";
	private static final String ADMIN_CLIENT_ID = "admin-cli";

	private final URL baseUrl;
	private final String accessToken;

	private AdminClient(URL baseUrl, String accessToken) {
		this.baseUrl = baseUrl;
		this.accessToken = accessToken;
	}

	public static AdminClient connect(URL baseUrl, String user, String password) throws IOException {
		var restClientBuilder = RestClientBuilder.newBuilder().baseUrl(baseUrl);
		try (var authService = restClientBuilder.build(AuthService.class)) {
			var token = authService.authorize(GRANT_TYPE_PW, ADMIN_CLIENT_ID, user, password).accessToken;
			return new AdminClient(baseUrl, token);
		}
	}

	public void createRealm(String realmCfg) {
		var restClientBuilder = RestClientBuilder.newBuilder().baseUrl(baseUrl);
		try (var in = new ByteArrayInputStream(realmCfg.getBytes(StandardCharsets.UTF_8));
			 var realmsService = restClientBuilder.build(RealmsService.class)) {
			var authHeader = "Bearer " + accessToken;
			realmsService.importRealm(authHeader, in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
