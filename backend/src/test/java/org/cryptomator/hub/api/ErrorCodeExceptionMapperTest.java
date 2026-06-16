package org.cryptomator.hub.api;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorCodeExceptionMapperTest {

	private final ErrorCodeExceptionMapper mapper = new ErrorCodeExceptionMapper();

	@Test
	@DisplayName("adds an error code carried as exception message to the response body")
	void testAddsErrorCodeToBody() {
		var response = mapper.toResponse(new ErrorCodeException(ErrorCode.CREATE_USER_FAILED));

		Assertions.assertEquals(500, response.getStatus());
		Assertions.assertEquals("CREATE_USER_FAILED", response.getEntity());
		Assertions.assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
	}

	@Test
	@DisplayName("preserves the status of client errors")
	void testPreservesStatus() {
		var response = mapper.toResponse(new ErrorCodeException(ErrorCode.EMAIL_EXISTS));

		Assertions.assertEquals(409, response.getStatus());
		Assertions.assertEquals("EMAIL_EXISTS", response.getEntity());
	}
}
