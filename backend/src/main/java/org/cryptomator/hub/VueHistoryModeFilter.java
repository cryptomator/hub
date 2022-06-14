package org.cryptomator.hub;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * A global http filter which redirects all 404-responses to the frontend app root. Necessary for using <a href="https://v3.router.vuejs.org/guide/essentials/history-mode.html#example-server-configurations">history mode in the vue router</a>
 * <p>
 * Implemention taken from <a href="https://quarkus.io/blog/quarkus-and-web-ui-development-mode/#handle-angular-routes">https://quarkus.io/blog/quarkus-and-web-ui-development-mode/#handle-angular-routes</a>
 */
@WebFilter(urlPatterns = "/*")
public class VueHistoryModeFilter extends HttpFilter {

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile(".*[.][a-zA-Z\\d]+");

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		chain.doFilter(request, response);

		if (response.getStatus() == 404) {
			String path = request.getRequestURI().substring(
					request.getContextPath().length()).replaceAll("[/]+$", "");
			if (!FILE_NAME_PATTERN.matcher(path).matches()) {
				// We could not find the resource, i.e. it is not anything known to the server (i.e. it is not a REST
				// endpoint or a servlet), and does not look like a file so try handling it in the front-end routes
				// and reset the response status code to 200.
				try {
					response.setStatus(200);
					request.getRequestDispatcher("/").forward(request, response);
				} finally {
					response.getOutputStream().close();
				}
			}
		}
	}

}
