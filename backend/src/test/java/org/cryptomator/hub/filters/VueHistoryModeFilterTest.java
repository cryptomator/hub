package org.cryptomator.hub.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

public class VueHistoryModeFilterTest {

	private HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
	private HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
	private FilterChain chain = Mockito.mock(FilterChain.class);
	private VueHistoryModeFilter filter = new VueHistoryModeFilter();

	@Test
	@DisplayName("redirect /app/* subresources to index.html")
	public void testFilter() throws ServletException, IOException {
		var dispatcher = Mockito.mock(RequestDispatcher.class);
		Mockito.doReturn(dispatcher).when(req).getRequestDispatcher("/index.html");

		filter.doFilter(req, res, chain);

		Mockito.verify(dispatcher).forward(req, res);
	}

}