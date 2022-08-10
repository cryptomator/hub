package org.cryptomator.hub.filters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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