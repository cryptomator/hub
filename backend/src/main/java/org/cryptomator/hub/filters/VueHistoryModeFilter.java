package org.cryptomator.hub.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A http filter which redirects all requests to subresources of /app to the frontend index.html. Necessary for using <a href="https://v3.router.vuejs.org/guide/essentials/history-mode.html#example-server-configurations">history mode in the vue router</a>
 */
public class VueHistoryModeFilter extends HttpFilter {

	@Override
	protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
		req.getRequestDispatcher("/index.html").forward(req, res);
	}

}
