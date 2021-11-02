package org.cryptomator.hub.spi.keycloak;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.InputStream;

@RegisterRestClient
@Path("/admin/realms")
public interface RealmsService extends Closeable {

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	void importRealm(@HeaderParam("Authorization") String authHeader, InputStream realmSpec);

}
