package org.cryptomator.hub.filters;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A http filter which redirects all requests to subresources of /app to the frontend index.html. Necessary for using <a href="https://v3.router.vuejs.org/guide/essentials/history-mode.html#example-server-configurations">history mode in the vue router</a>
 */
@WebFilter(urlPatterns = "/app/*")
public class VueHistoryModeFilter extends HttpFilter {

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		request.getRequestDispatcher("/index.html").forward(request, response);
	}

}
