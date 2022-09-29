package org.cryptomator.hub.validation;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/test")
public class ValidationTestResource {


	@GET
	@Path("/nothing")
	public Response probeNothing() {
		return Response.ok().build();
	}

	@GET
	@Path("/validuuid/{uuid}")
	public Response probeValidUuid(@PathParam("uuid") @ValidUUID String uuid) {
		return Response.ok().build();
	}

	@GET
	@Path("/validid/{id}")
	public Response probeValidId(@PathParam("id") @ValidId String id) {
		return Response.ok().build();
	}

	@GET
	@Path("/validpseudobase64/{pb64string}")
	public Response probeValidPseudoBase64(@PathParam("pb64string") @ValidPseudoBase64 String psuedoBase64String) {
		return Response.ok().build();
	}

	@GET
	@Path("/validpseudobase64url/{pb64urlstring}")
	public Response probeValidPseudoBase64Url(@PathParam("pb64urlstring") @ValidPseudoBase64Url String psuedoBase64UrlString) {
		return Response.ok().build();
	}

	@GET
	@Path("/validjwe/{jwe}")
	public Response probeValidJWE(@PathParam("jwe") @ValidJWE String jwe) {
		return Response.ok().build();
	}
}
