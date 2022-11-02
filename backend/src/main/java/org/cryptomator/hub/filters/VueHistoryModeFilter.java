package org.cryptomator.hub.filters;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
