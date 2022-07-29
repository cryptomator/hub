package org.cryptomator.hub.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class VueHistoryModeFilterTest {

	private HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
	private HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
	private FilterChain chain = Mockito.mock(FilterChain.class);

	private VueHistoryModeFilter filter = new VueHistoryModeFilter();

	@BeforeEach
	public void setup() {
		filter.apiPathPrefix = "/api";
	}

	@ParameterizedTest(name = "path = {0}")
	@ValueSource(strings = {"/ctx/api", "/ctx/api/foo?k=v", "/ctx/api/foo/bar/", "/ctx/api/"})
	@DisplayName("don't filter requests to /ctx/api/*")
	public void testDoNotFilterApi(String reqUri) throws ServletException, IOException {
		Mockito.doReturn(reqUri).when(req).getRequestURI();
		Mockito.doReturn("/ctx").when(req).getContextPath();
		Mockito.doReturn(404).when(res).getStatus();

		filter.doFilter(req, res, chain);

		Mockito.verify(chain).doFilter(req, res);
		Mockito.verify(req).getRequestURI();
		Mockito.verify(req).getContextPath();
		Mockito.verify(res).getStatus();
		Mockito.verifyNoMoreInteractions(res, req, chain);
	}

	@ParameterizedTest(name = "statuscode = {0}")
	@ValueSource(ints = {200, 201, 301, 401, 403})
	@DisplayName("don't filter non-404 responses")
	public void testDoNotFilterNon404(int status) throws ServletException, IOException {
		Mockito.doReturn("/ctx/foo").when(req).getRequestURI();
		Mockito.doReturn("/ctx").when(req).getContextPath();
		Mockito.doReturn(status).when(res).getStatus();

		filter.doFilter(req, res, chain);

		Mockito.verify(chain).doFilter(req, res);
		Mockito.verify(req).getRequestURI();
		Mockito.verify(req).getContextPath();
		Mockito.verify(res).getStatus();
		Mockito.verifyNoMoreInteractions(res, req, chain);
	}

	@ParameterizedTest(name = "path = {0}")
	@ValueSource(strings = {"/ctx", "/ctx/foo?k=v", "/ctx/foo/bar/"})
	@DisplayName("filter 404 response to non-api resources")
	public void testDoFilterNonApi(String reqUri) throws ServletException, IOException {
		var dispatcher = Mockito.mock(RequestDispatcher.class);
		var out = Mockito.mock(ServletOutputStream.class);
		Mockito.doReturn(reqUri).when(req).getRequestURI();
		Mockito.doReturn("/ctx").when(req).getContextPath();
		Mockito.doReturn(404).when(res).getStatus();
		Mockito.doReturn(dispatcher).when(req).getRequestDispatcher("/");
		Mockito.doReturn(out).when(res).getOutputStream();

		filter.doFilter(req, res, chain);

		Mockito.verify(res).setStatus(200);
		Mockito.verify(req).getRequestDispatcher("/");
		Mockito.verify(dispatcher).forward(req, res);
		Mockito.verify(out).close();
	}

}