package org.cryptomator.hub.spi.keycloak;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
@Path("/realms/master")
public interface AuthService {

	@POST
	@Path("/protocol/openid-connect/token")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	TokenResponse authorize(@FormParam("grant_type") String grantType, @FormParam("client_id") String clientId, @FormParam("username") String user, @FormParam("password") String password);

}
