package org.cryptomator.hub.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/test")
public class ValidationTestResource {


	@GET
	@Path("/nothing")
	public Response probeNothing() {
		return Response.ok().build();
	}

	@GET
	@Path("/validid/{id}")
	public Response probeValidId(@PathParam("id") @ValidId String id) {
		return Response.ok().build();
	}

	@GET
	@Path("/onlybase64chars/{b64string}")
	public Response probeOnlyBase64Chars(@PathParam("b64string") @OnlyBase64Chars String base64String) {
		return Response.ok().build();
	}

	@GET
	@Path("/onlybase64urlchars/{b64urlstring}")
	public Response probeOnlyBase64UrlChars(@PathParam("b64urlstring") @OnlyBase64UrlChars String base64UrlString) {
		return Response.ok().build();
	}

	@GET
	@Path("/validjwe/{jwe}")
	public Response probeValidJWE(@PathParam("jwe") @ValidJWE String jwe) {
		return Response.ok().build();
	}

	@GET
	@Path("/validjws/{jws}")
	public Response probeValidJWS(@PathParam("jws") @ValidJWS String jws) {
		return Response.ok().build();
	}

	record NoHtmlOrScriptCharsDto(@JsonProperty("data") @NoHtmlOrScriptChars String data) {
	}

}
